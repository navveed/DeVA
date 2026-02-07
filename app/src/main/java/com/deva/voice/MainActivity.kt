package com.deva.voice

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
import com.deva.voice.v2.AgentService
import com.deva.voice.utilities.FreemiumManager
import com.deva.voice.utilities.Logger
import com.deva.voice.utilities.OnboardingManager
import com.deva.voice.utilities.PermissionManager
import com.deva.voice.utilities.UserIdManager
import com.deva.voice.utilities.UserProfileManager
import com.deva.voice.utilities.VideoAssetManager
import com.deva.voice.utilities.WakeWordManager
import com.deva.voice.utilities.DeVAState
import com.deva.voice.utilities.DeVAStateManager
import com.deva.voice.utilities.DeltaStateColorMapper
import com.deva.voice.views.DeltaSymbolView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File

class MainActivity : BaseNavigationActivity() {

    private lateinit var handler: Handler
    private lateinit var managePermissionsButton: TextView
    private lateinit var tvPermissionStatus: TextView
    private lateinit var userId: String
    private lateinit var permissionManager: PermissionManager
    private lateinit var wakeWordManager: WakeWordManager
    private lateinit var auth: FirebaseAuth
    private lateinit var tasksLeftTag: View
    private lateinit var freemiumManager: FreemiumManager
    private lateinit var wakeWordHelpLink: TextView
    private lateinit var increaseLimitsLink: TextView
    private lateinit var onboardingManager: OnboardingManager
    private lateinit var requestRoleLauncher: ActivityResultLauncher<Intent>
    private lateinit var billingStatusTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var loadingOverlay: View
    private lateinit var devaStateManager: DeVAStateManager
    private lateinit var stateChangeListener: (DeVAState) -> Unit
    private lateinit var proSubscriptionTag: View
    private lateinit var permissionsTag: View
    private lateinit var permissionsStatusTag: TextView
    private lateinit var tasksLeftText: TextView
    private lateinit var deltaSymbol: DeltaSymbolView


    private lateinit var root: View
    companion object {
        const val ACTION_WAKE_WORD_FAILED = "com.deva.voice.WAKE_WORD_FAILED"
        const val ACTION_PURCHASE_UPDATED = "com.deva.voice.PURCHASE_UPDATED"
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            // Save permission result for Service usage
            com.deva.voice.services.ScreenCaptureService.resultCode = result.resultCode
            com.deva.voice.services.ScreenCaptureService.projectionIntent = result.data!!
            
            // NOW start the Overlay Service
            startOverlayService()
            Toast.makeText(this, "DeVA Vision Ready! Tap the floating icon.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Overlay Permission Launcher
    private val overlayPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            // Overlay granted, now ask for Screen Capture
            requestScreenCaptureWithMediaProjection()
        } else {
            Toast.makeText(this, "Overlay permission is required for DeVA Vision", Toast.LENGTH_LONG).show()
        }
    }
    
    private val wakeWordFailureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_WAKE_WORD_FAILED) {
                Logger.d("MainActivity", "Received wake word failure broadcast.")
                // The service stops itself, but we should refresh the UI state
                updateUI()
                showWakeWordFailureDialog()
            }
        }
    }
    
    private val purchaseUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_PURCHASE_UPDATED) {
                Logger.d("MainActivity", "Received purchase update broadcast.")
                // Refresh billing status
                showLoading(true)
                performBillingCheck()
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
            }
        }




    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        val currentUser = auth.currentUser
        val profileManager = UserProfileManager(this)

        // Skip auth check - no login required for this version
        onboardingManager = OnboardingManager(this)
        if (!onboardingManager.isOnboardingCompleted()) {
            Logger.d("MainActivity", "User is logged in but onboarding not completed. Relaunching permissions stepper.")
            startActivity(Intent(this, OnboardingPermissionsActivity::class.java))
            finish()
            return
        }

        requestRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Set as default assistant successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Couldn’t become default assistant. Opening settings…", Toast.LENGTH_SHORT).show()
                Logger.w("MainActivity", "Role request canceled or app not eligible.\n${explainAssistantEligibility()}")
                openAssistantPickerSettings()
            }
            showAssistantStatus(true)
        }


        setContentView(R.layout.activity_main_content)
        findViewById<TextView>(R.id.btn_set_default_assistant).setOnClickListener {
            startActivity(Intent(this, RoleRequestActivity::class.java))
        }
        updateDefaultAssistantButtonVisibility()

        handleIntent(intent)
        managePermissionsButton = findViewById(R.id.btn_manage_permissions) // ADDED

        val userIdManager = UserIdManager(applicationContext)
        userId = userIdManager.getOrCreateUserId()
        increaseLimitsLink = findViewById(R.id.increase_limits_link) // ADDED

        permissionManager = PermissionManager(this)
        permissionManager.initializePermissionLauncher()

        managePermissionsButton = findViewById(R.id.btn_manage_permissions)
        tasksLeftText = findViewById(R.id.tasks_left_tag_text)
        tasksLeftTag = findViewById(R.id.tasks_left_tag)
        tvPermissionStatus = findViewById(R.id.tv_permission_status)
        wakeWordHelpLink = findViewById(R.id.wakeWordHelpLink)
        billingStatusTextView = findViewById(R.id.billing_status_textview)
        statusTextView = findViewById(R.id.status_text)
        loadingOverlay = findViewById(R.id.loading_overlay)
        proSubscriptionTag = findViewById(R.id.pro_subscription_tag)
        permissionsTag = findViewById(R.id.permissions_tag)
        permissionsStatusTag = findViewById(R.id.permissions_status_tag)
        deltaSymbol = findViewById(R.id.delta_symbol)
        freemiumManager = FreemiumManager()
        updateStatusText(DeVAState.IDLE)
        setupProBanner()
        initializeDeVAStateManager()
        wakeWordManager = WakeWordManager(this, requestPermissionLauncher)
        handler = Handler(Looper.getMainLooper())
        setupClickListeners()
        showLoading(true)
        performBillingCheck()
        
        lifecycleScope.launch {
            val videoUrl = "https://storage.googleapis.com/blurr-app-assets/wake_word_demo.mp4"
            VideoAssetManager.getVideoFile(this@MainActivity, videoUrl)
        }

    }

    private fun openAssistantPickerSettings() {
        val specifics = listOf(
            Intent("android.settings.VOICE_INPUT_SETTINGS"),
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        )
        for (i in specifics) {
            if (i.resolveActivity(packageManager) != null) {
                startActivity(i); return
            }
        }
        Toast.makeText(this, "Assistant settings not available on this device.", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun showAssistantStatus(toast: Boolean = false) {
        val rm = getSystemService(RoleManager::class.java)
        val held = rm?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
        val msg = if (held) "This app is the default assistant." else "This app is NOT the default assistant."
        if (toast) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        Logger.d("MainActivity", msg)
    }

    private fun explainAssistantEligibility(): String {
        val pm = packageManager
        val pkg = packageName

        val assistIntent = Intent(Intent.ACTION_ASSIST).setPackage(pkg)
        val assistActivities = pm.queryIntentActivities(assistIntent, 0)

        val visIntent = Intent("android.service.voice.VoiceInteractionService").setPackage(pkg)
        val visServices = pm.queryIntentServices(visIntent, 0)

        return buildString {
            append("Assistant eligibility:\n")
            append("• ACTION_ASSIST activity: ${if (assistActivities.isNotEmpty()) "FOUND" else "NOT FOUND"}\n")
            append("• VoiceInteractionService: ${if (visServices.isNotEmpty()) "FOUND" else "NOT FOUND"}\n")
            append("Note: Many OEMs only list apps with a VoiceInteractionService as selectable assistants.\n")
        }
    }

    override fun onStart() {
        super.onStart()
        // Skip auth check - no login required for this version
        showLoading(true)
        performBillingCheck()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "com.deva.voice.WAKE_UP_DEVA") {
            Logger.d("MainActivity", "Wake up DeVA shortcut activated!")
            startConversationalAgent()
        }
    }

    private fun startConversationalAgent() {
        if (!ConversationalAgentService.isRunning) {
            val serviceIntent = Intent(this, ConversationalAgentService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
            Toast.makeText(this, "DeVA is waking up...", Toast.LENGTH_SHORT).show()
        } else {
            Logger.d("MainActivity", "ConversationalAgentService is already running.")
            Toast.makeText(this, "DeVA is already awake!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun getContentLayoutId(): Int = R.layout.activity_main_content
    
    override fun getCurrentNavItem(): BaseNavigationActivity.NavItem = BaseNavigationActivity.NavItem.HOME

    private fun setupClickListeners() {
        managePermissionsButton.setOnClickListener {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }
        increaseLimitsLink.setOnClickListener {
            requestLimitIncrease()
        }

        findViewById<TextView>(R.id.vision_trigger_link)?.setOnClickListener {
            requestScreenCapture()
        }

        wakeWordHelpLink.setOnClickListener {
            showWakeWordFailureDialog()
        }
        findViewById<TextView>(R.id.disclaimer_link).setOnClickListener {
            showDisclaimerDialog()
        }
        findViewById<TextView>(R.id.examples_link).setOnClickListener {
            showExamplesDialog()
        }
        
        // Add click listener to delta symbol
        deltaSymbol.setOnClickListener {
            // Only start conversational agent if in ready/idle state
            if (devaStateManager.getCurrentState() == DeVAState.IDLE || devaStateManager.getCurrentState() == DeVAState.ERROR) {
                startConversationalAgent()
            }
        }
    }

    private fun startScreenCaptureService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, com.deva.voice.services.ScreenCaptureService::class.java).apply {
            // Pass the permission result to the service via static fields or intent extras
             com.deva.voice.services.ScreenCaptureService.resultCode = resultCode
             com.deva.voice.services.ScreenCaptureService.projectionIntent = data
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        // Wait for result from service
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
             com.deva.voice.services.ScreenCaptureService.captureResultDeferred = kotlinx.coroutines.CompletableDeferred()
            val bitmap =  com.deva.voice.services.ScreenCaptureService.captureResultDeferred?.await()
            
            if (bitmap != null) {
                analyzeScreenContent(bitmap)
            } else {
                Toast.makeText(this@MainActivity, "Failed to capture screen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestScreenCapture() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
            return
        }
        
        // Overlay is ready, now request Screen Capture Permission
        requestScreenCaptureWithMediaProjection()
    }
    
    private fun requestScreenCaptureWithMediaProjection() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
    
    private fun startOverlayService() {
        val intent = Intent(this, com.deva.voice.services.VisionOverlayService::class.java)
        startService(intent)
    }

    private fun analyzeScreenContent(bitmap: android.graphics.Bitmap) {
        val processedBitmap = com.deva.voice.utilities.GeminiVisionHelper.prepareBitmapForGemini(bitmap)
        
        Toast.makeText(this, "Analyzing screen...", Toast.LENGTH_SHORT).show()
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
             try {
                // Fix: Properly construct the Content object or list as expected by GeminiApi
                // Assuming GeminiApi.generateContent takes a list of Content or similar.
                // Based on previous usage, let's look at the signature. 
                // If the signature expects (chat: List<Pair<String, List<Part>>>, ...), we need to match it.
                // However, the error says "String" mismatch. 
                // Let's rely on the simplest overload if available, or construct the parts correctly.
                 
                 val response = com.deva.voice.api.GeminiApi.generateContent(
                     prompt = """
                         You are DeVA, a smart, casual, and witty AI friend.
                         Look at this screenshot and tell me what is happening or what it says as if you are sitting next to me.
                         
                         RULES:
                         1. If there is text, an article, or chat: SUMMARIZE the key information concisely.
                         2. If it is an image or video: React to it or describe what's happening.
                         3. IGNORE system bars, battery percentage, time, and navigation buttons.
                         4. Be concise (1-2 sentences max).
                         5. Speak naturally, like a human. No robotic lists.
                     """.trimIndent(),
                     image = processedBitmap,
                     context = this@MainActivity
                 )
                 
                 withContext(kotlinx.coroutines.Dispatchers.Main) {
                     if (response != null) {
                         showDialog("DeVA Vision", response)
                     } else {
                         Toast.makeText(this@MainActivity, "Vision analysis failed", Toast.LENGTH_SHORT).show()
                     }
                 }
             } catch (e: Exception) {
                 withContext(kotlinx.coroutines.Dispatchers.Main) {
                     Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                 }
             }
        }
    }

    // Existing functions...
    private fun requestLimitIncrease() {
        val userEmail = auth.currentUser?.email
        if (userEmail.isNullOrEmpty()) {
            Toast.makeText(this, "Could not get your email. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        val recipient = "devanshupardeshi21@gmail.com"
        val subject = "I am facing issue in"
        val body = "Hello,\n\nI am facing issue for my account: $userEmail\n <issue-content>.... \n\nThank you."

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // Only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        // Verify that the intent will resolve to an activity
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No email application found.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupProBanner() {
        val proBanner = findViewById<View>(R.id.pro_upgrade_banner)
        val upgradeButton = findViewById<TextView>(R.id.upgrade_button)
        
        upgradeButton.setOnClickListener {
            // Navigate to Pro purchase screen (Requirement 2.3)
            val intent = Intent(this, ProPurchaseActivity::class.java)
            startActivity(intent)
        }
        
        // Initially hide the banner - it will be shown/hidden based on subscription status
        proBanner.visibility = View.GONE
    }

    /**
     * Initialize DeVAStateManager and set up state change listeners
     */
    private fun initializeDeVAStateManager() {
        devaStateManager = DeVAStateManager.getInstance(this)
        stateChangeListener = { newState ->
            updateStatusText(newState)

            updateDeltaVisuals(newState)
            Logger.d("MainActivity", "DeVA state changed to: ${newState.name}")
        }
        devaStateManager.addStateChangeListener(stateChangeListener)
    }
    private fun updateDeltaVisuals(state: DeVAState) {
        runOnUiThread {
            // Get the color for the current state
            val color = DeltaStateColorMapper.getColor(this, state)
            deltaSymbol.setColor(color)

            // Start or stop the glow based on whether the state is "active"
            if (DeltaStateColorMapper.isActiveState(state)) {
                deltaSymbol.startGlow()
            } else {
                deltaSymbol.stopGlow()
            }
        }
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        showLoading(true)
        performBillingCheck()
        displayDeveloperMessage()
        updateDeltaVisuals(devaStateManager.getCurrentState())
        updateUI()
        devaStateManager.startMonitoring()
        val wakeWordFilter = IntentFilter(ACTION_WAKE_WORD_FAILED)
        val purchaseFilter = IntentFilter(ACTION_PURCHASE_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeWordFailureReceiver, wakeWordFilter, RECEIVER_NOT_EXPORTED)
            registerReceiver(purchaseUpdateReceiver, purchaseFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wakeWordFailureReceiver, wakeWordFilter)
            registerReceiver(purchaseUpdateReceiver, purchaseFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        devaStateManager.stopMonitoring()
        try {
            unregisterReceiver(wakeWordFailureReceiver)
            unregisterReceiver(purchaseUpdateReceiver)
        } catch (e: IllegalArgumentException) {
            Logger.d("MainActivity", "Receivers were not registered")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::devaStateManager.isInitialized && ::stateChangeListener.isInitialized) {
            devaStateManager.removeStateChangeListener(stateChangeListener)
            devaStateManager.stopMonitoring()
        }
    }

    private fun showDisclaimerDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Disclaimer")
            .setMessage("DeVA is an experimental AI assistant and is still in development. It may not always be accurate or perform as expected. It does small task better. Your understanding is appreciated!")
            .setPositiveButton("Okay") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
            ContextCompat.getColor(this, R.color.white)
        )
    }

    private fun showExamplesDialog() {
        val examples = arrayOf(
            "Open YouTube and play music",
            "Send a text message",
            "Set an alarm for 30 minutes",
            "Open camera app",
            "Check weather forecast",
            "Open calculator",
            "Surprise me"
        )
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Example Commands")
            .setItems(examples) { _, which ->
                val selectedExample = examples[which]
                if (selectedExample == "Surprise me"){
                    AgentService.start(this, "play never gonna give you up on youtube")

                }
                AgentService.start(this, selectedExample)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
            ContextCompat.getColor(this, R.color.black)
        )
    }


    private fun showWakeWordFailureDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_wake_word_failure, null)
        val videoView = dialogView.findViewById<VideoView>(R.id.video_demo)
        val videoContainer = dialogView.findViewById<View>(R.id.video_container_card)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Got it") { dialog, _ ->
                dialog.dismiss()
            }
        val alertDialog = builder.create()
        lifecycleScope.launch {
            val videoUrl = "https://storage.googleapis.com/blurr-app-assets/wake_word_demo.mp4"
            val videoFile: File? = VideoAssetManager.getVideoFile(this@MainActivity, videoUrl)

            if (videoFile != null && videoFile.exists()) {
                videoContainer.visibility = View.VISIBLE
                videoView.setVideoURI(Uri.fromFile(videoFile))
                videoView.setOnPreparedListener { mp ->
                    mp.isLooping = true
                }
                alertDialog.setOnShowListener {
                    videoView.start()
                }
            } else {
                Logger.e("MainActivity", "Video file not found, hiding video container.")
                videoContainer.visibility = View.GONE
            }
        }

        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
            ContextCompat.getColor(this, R.color.white)
        )
    }
    private fun updateTaskCounter() {
        lifecycleScope.launch {
            val isUserSub = freemiumManager.isUserSubscribed()
            if(isUserSub){
                tasksLeftTag.visibility = View.GONE
            }
            val tasksLeft = freemiumManager.getTasksRemaining()
            tasksLeftText.text = if (tasksLeft != null) "$tasksLeft tasks left" else "Unlimited"
        }
    }

    private fun updateBillingStatus() {
        lifecycleScope.launch {
            try {
                val isSubscribed = freemiumManager.isUserSubscribed()
                val billingClientReady = MyApplication.isBillingClientReady.value
                val proBanner = findViewById<View>(R.id.pro_upgrade_banner)
                if (proBanner == null) {
                    Logger.w("MainActivity", "pro_banner view not found in updateBillingStatus")
                    return@launch
                }
                
                // FORCE HIDE for college project
                proBanner.visibility = View.GONE
                proSubscriptionTag.visibility = View.GONE

            } catch (e: Exception) {
                Logger.e("MainActivity", "Error updating billing status", e)
                proSubscriptionTag.visibility = View.GONE
                val proBanner = findViewById<View>(R.id.pro_upgrade_banner)
                if (proBanner != null) {
                    proBanner.visibility = View.GONE
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        val allPermissionsGranted = permissionManager.areAllPermissionsGranted()
        if (allPermissionsGranted) {
            tvPermissionStatus.text = "All required permissions are granted."
            tvPermissionStatus.visibility = View.GONE
            managePermissionsButton.visibility = View.GONE
            tvPermissionStatus.setTextColor(Color.parseColor("#4CAF50")) // Green
            permissionsTag.visibility = View.VISIBLE
        } else {
            tvPermissionStatus.text = "Some permissions are missing. Tap below to manage."
            tvPermissionStatus.setTextColor(Color.parseColor("#F44336")) // Red
            permissionsTag.visibility = View.GONE
        }
    }

    private fun isThisAppDefaultAssistant(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            rm?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
        } else {
            val flat = Settings.Secure.getString(contentResolver, "voice_interaction_service")
            val currentPkg = flat?.substringBefore('/')
            currentPkg == packageName
        }
    }

    private fun updateDefaultAssistantButtonVisibility() {
        val btn = findViewById<TextView>(R.id.btn_set_default_assistant)
        btn.visibility = if (isThisAppDefaultAssistant()) View.GONE else View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun performBillingCheck() {
        lifecycleScope.launch {
            try {
                waitForBillingClientReady()
                queryAndHandlePurchases()
                updateTaskCounter()
                updateBillingStatus()
                
            } catch (e: Exception) {
                Logger.e("MainActivity", "Error during billing check", e)
                updateTaskCounter()
                updateBillingStatus()
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun waitForBillingClientReady() {
        return withContext(Dispatchers.IO) {
            var attempts = 0
            val maxAttempts = 10
            
            while (!MyApplication.isBillingClientReady.value && attempts < maxAttempts) {
                kotlinx.coroutines.delay(500)
                attempts++
            }
            
            if (!MyApplication.isBillingClientReady.value) {
                Logger.w("MainActivity", "Billing client not ready after waiting")
            }
        }
    }

    private suspend fun queryAndHandlePurchases() {
        return withContext(Dispatchers.IO) {
            if (!MyApplication.isBillingClientReady.value) {
                Logger.e("MainActivity", "queryPurchases: BillingClient is not ready")
                return@withContext
            }

            try {
                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
                
                Logger.d("MainActivity", "queryPurchases: BillingClient is ready")

                val purchasesResult = MyApplication.billingClient.queryPurchasesAsync(params)
                val billingResult = purchasesResult.billingResult
                
                Logger.d("MainActivity", "queryPurchases: Got billing result: ${billingResult.responseCode}")

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Logger.d("MainActivity", "queryPurchases: Found ${purchasesResult.purchasesList.size} purchases")
                    purchasesResult.purchasesList.forEach { purchase ->
                        when (purchase.purchaseState) {
                            Purchase.PurchaseState.PURCHASED -> {
                                Logger.d("MainActivity", "Found purchased item: ${purchase.products}")
                                handlePurchase(purchase)
                            }
                            Purchase.PurchaseState.PENDING -> {
                                Logger.d("MainActivity", "Purchase is pending")
                            }
                            else -> {
                                Logger.d("MainActivity", "Purchase is not in a valid state: ${purchase.purchaseState}")
                            }
                        }
                    }
                } else {
                    Logger.e("MainActivity", "Failed to query purchases: ${billingResult.debugMessage}")
                }
            } catch (e: Exception) {
                Logger.e("MainActivity", "Exception during purchase query", e)
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        return withContext(Dispatchers.IO) {
            try {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        
                        MyApplication.billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                Logger.d("MainActivity", "Purchase acknowledged: ${purchase.orderId}")
                                lifecycleScope.launch {
                                    updateUserToPro()
                                }
                            } else {
                                Logger.e("MainActivity", "Failed to acknowledge purchase: ${billingResult.debugMessage}")
                            }
                        }
                    } else {
                        updateUserToPro()
                    }
                }
            } catch (e: Exception) {
                Logger.e("MainActivity", "Error handling purchase", e)
            }
        }
    }


    private suspend fun updateUserToPro() {
        val uid = Firebase.auth.currentUser?.uid
        if (uid == null) {
            Logger.e("MainActivity", "Cannot update user to pro: user is not authenticated.")
            withContext(Dispatchers.Main) {
            }
            return
        }

        withContext(Dispatchers.IO) {
            val db = Firebase.firestore
            try {
                val userDocRef = db.collection("users").document(uid)
                userDocRef.update("plan", "pro").await()
                Logger.d("MainActivity", "Successfully updated user $uid to 'pro' plan.")
                withContext(Dispatchers.Main) {
                }

            } catch (e: Exception) {
                Logger.e("MainActivity", "Error updating user to pro", e)
                withContext(Dispatchers.Main) {
                }
            }
        }
    }

    private fun displayDeveloperMessage() {
        //lifecycleScope.launch {
            try {
                // Check if message has been shown more than once
                val sharedPrefs = getSharedPreferences("developer_message_prefs", Context.MODE_PRIVATE)
                val displayCount = sharedPrefs.getInt("developer_message_count", 0)
                
                if (displayCount >= 1) {
                    Logger.d("MainActivity", "Developer message already shown $displayCount times, skipping display")
                    return
                }

                val remoteConfig = Firebase.remoteConfig

                // Fetch and activate the latest Remote Config values
                remoteConfig.fetchAndActivate()
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val updated = task.result
                            Log.d("MainActivity", "Remote Config params updated: $updated")

                            // Get the message from the activated config
                            val message = remoteConfig.getString("developerMessage")

                            if (message.isNotEmpty()) {
                                // Your existing dialog logic
                                val dialog = AlertDialog.Builder(this@MainActivity)
                                    .setTitle("Message from Developer")
                                    .setMessage(message)
                                    .setPositiveButton("OK") { dialogInterface, _ ->
                                        dialogInterface.dismiss()
                                        val editor = sharedPrefs.edit()
                                        editor.putInt("developer_message_count", displayCount + 1)
                                        editor.apply()
                                    }
                                    .show()
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                    ContextCompat.getColor(this@MainActivity, R.color.black)
                                )
                                Log.d("MainActivity", "Developer message displayed from Remote Config.")
                            } else {
                                Log.d("MainActivity", "No developer message found in Remote Config.")
                            }
                        } else {
                            Log.e("MainActivity", "Failed to fetch Remote Config.", task.exception)
                        }
                    }
                
//                val db = Firebase.firestore
//                val docRef = db.collection("settings").document("freemium")
//
//                docRef.get().addOnSuccessListener { document ->
//                    if (document != null && document.exists()) {
//                        val message = document.getString("developerMessage")
//                        if (!message.isNullOrEmpty()) {
//                            val dialog = AlertDialog.Builder(this@MainActivity)
//                                .setTitle("Message from Developer")
//                                .setMessage(message)
//                                .setPositiveButton("OK") { dialogInterface, _ ->
//                                    dialogInterface.dismiss()
//                                    // Increment the display count after user dismisses
//                                    val editor = sharedPrefs.edit()
//                                    editor.putInt("developer_message_count", displayCount + 1)
//                                    editor.apply()
//                                    Logger.d("MainActivity", "Developer message display count updated to ${displayCount + 1}")
//                                }
//                                .show()
//                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
//                                ContextCompat.getColor(this@MainActivity, R.color.black)
//                            )
//                            Logger.d("MainActivity", "Developer message displayed in dialog")
//                        } else {
//                            Logger.d("MainActivity", "Developer message is empty")
//                        }
//                    } else {
//                        Logger.d("MainActivity", "Developer message document does not exist")
//                    }
//                }.addOnFailureListener { exception ->
//                    Logger.e("MainActivity", "Error fetching developer message", exception)
//                }
            } catch (e: Exception) {
                Logger.e("MainActivity", "Exception in displayDeveloperMessage", e)
            }
        //}
    }

    private fun showDialog(title: String, message: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
            ContextCompat.getColor(this, R.color.black)
        )
    }

    /**
     * Update the status text based on the current DeVAState
     */
    fun updateStatusText(state: DeVAState) {
        runOnUiThread {
            try {
                val statusText = DeltaStateColorMapper.getStatusText(state)
                statusTextView.text = statusText
                Logger.d("MainActivity", "Status text updated to: $statusText for state: ${state.name}")
            } catch (e: Exception) {
                Logger.e("MainActivity", "Error updating status text", e)
                statusTextView.text = "Ready" // Fallback to default
            }
        }
    }

    /**
     * Update the status text with custom text (overrides state-based text)
     */
    fun updateStatusText(customText: String) {
        runOnUiThread {
            try {
                statusTextView.text = customText
                Logger.d("MainActivity", "Status text updated to custom text: $customText")
            } catch (e: Exception) {
                Logger.e("MainActivity", "Error updating status text with custom text", e)
                statusTextView.text = "Ready" // Fallback to default
            }
        }
    }

}






