package com.deva.voice

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class RoleRequestActivity : AppCompatActivity() {

    private var launched = false
    private lateinit var roleLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launched = savedInstanceState?.getBoolean("launched") ?: false

        // Register once
        roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Set as default assistant successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Couldn’t become default assistant. Opening settings…", Toast.LENGTH_SHORT).show()
                Log.w("RoleRequestActivity", "Role request canceled or app not eligible.\n${explainAssistantEligibility()}")
                openAssistantSettingsFallback()
            }
            finish() // Return to caller (MainActivity should re-check status in onResume)
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (launched) return
        launched = true

        val rm = getSystemService(RoleManager::class.java)
        Log.d("RoleRequestActivity", explainAssistantEligibility())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            rm?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true &&
            !rm.isRoleHeld(RoleManager.ROLE_ASSISTANT)
        ) {
            // Launch AFTER window is visible to avoid BadToken issues
            window.decorView.post {
                try {
                    roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT))
                } catch (_: Exception) {
                    openAssistantSettingsFallback()
                }
            }
        } else {
            // Either role not available or already held — go to Settings (or just finish)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && rm?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true) {
                Toast.makeText(this, "Already the default assistant.", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                openAssistantSettingsFallback()
                // Don’t finish here; let user navigate back from Settings to MainActivity.
                // If you prefer to auto-finish, add: finish()
            }
        }
    }

    private fun openAssistantSettingsFallback() {
        val intents = listOf(
            Intent("android.settings.VOICE_INPUT_SETTINGS"),
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        )
        for (i in intents) {
            if (i.resolveActivity(packageManager) != null) {
                startActivity(i)
                return
            }
        }
        Toast.makeText(this, "Assistant settings unavailable on this device.", Toast.LENGTH_LONG).show()
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("launched", launched)
        super.onSaveInstanceState(outState)
    }
}




