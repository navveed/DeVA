package com.deva.voice.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.deva.voice.R
import kotlin.math.sqrt

class SmallDeltaGlowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint for the circular black background
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        style = Paint.Style.FILL
        color = Color.BLACK
        alpha = 255
    }

    // Adjusted stroke widths for a smaller size
    private val thinStrokeWidthPx = 0.6f * resources.displayMetrics.density
    private val thickStrokeWidthPx = 1.5f * resources.displayMetrics.density

    // Paint objects for each side of the delta
    private val leftPaint = createStrokePaint(thinStrokeWidthPx)
    private val bottomPaint = createStrokePaint(thinStrokeWidthPx)
    private val rightPaint = createStrokePaint(thickStrokeWidthPx)
    private val allPaints = listOf(leftPaint, bottomPaint, rightPaint)

    // Path objects for each side of the delta
    private val leftPath = Path()
    private val bottomPath = Path()
    private val rightPath = Path()

    // Animation properties
    private var glowAnimator: ValueAnimator? = null
    private var currentGlowRadius = MIN_GLOW_RADIUS

    companion object {
        private const val ANIMATION_DURATION = 1500L
        // Adjusted glow radius for a smaller visual effect
        private const val MIN_GLOW_RADIUS = 4f
        private const val MAX_GLOW_RADIUS = 12f
        // Scale factor to determine the triangle's size relative to the view's diameter
        private const val TRIANGLE_SCALE_FACTOR = 0.5f
    }

    init {
        // Required for shadow layers (glow effect) to work
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private fun createStrokePaint(strokeWidth: Float): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = ContextCompat.getColor(context, R.color.delta_idle)
            strokeCap = Paint.Cap.ROUND // Makes corners look smoother
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Calculate triangle dimensions based on the view's size, not a fixed DP value
        val diameter = w.coerceAtMost(h).toFloat()
        val sideLengthPx = diameter * TRIANGLE_SCALE_FACTOR
        val triangleHeight = sideLengthPx * sqrt(3f) / 2f

        val centerX = w / 2f
        val centerY = h / 2f

        val topPoint = Pair(centerX, centerY - triangleHeight / 2f)
        val bottomLeftPoint = Pair(centerX - sideLengthPx / 2f, centerY + triangleHeight / 2f)
        val bottomRightPoint = Pair(centerX + sideLengthPx / 2f, centerY + triangleHeight / 2f)

        // Update the paths for each side
        leftPath.reset()
        leftPath.moveTo(topPoint.first, topPoint.second)
        leftPath.lineTo(bottomLeftPoint.first, bottomLeftPoint.second)

        bottomPath.reset()
        bottomPath.moveTo(bottomLeftPoint.first, bottomLeftPoint.second)
        bottomPath.lineTo(bottomRightPoint.first, bottomRightPoint.second)

        rightPath.reset()
        rightPath.moveTo(bottomRightPoint.first, bottomRightPoint.second)
        rightPath.lineTo(topPoint.first, topPoint.second)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = width.coerceAtMost(height) / 2f

        // 1. Draw the circular background first
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // 2. Draw the delta symbol on top
        canvas.drawPath(leftPath, leftPaint)
        canvas.drawPath(bottomPath, bottomPaint)
        canvas.drawPath(rightPath, rightPaint)
    }

    fun setColor(color: Int) {
        allPaints.forEach { it.color = color }
        if (glowAnimator?.isRunning == true) {
            allPaints.forEach { it.setShadowLayer(currentGlowRadius, 0f, 0f, color) }
        }
        invalidate()
    }

    fun startGlow() {
        if (glowAnimator?.isRunning == true) return

        glowAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                currentGlowRadius = MIN_GLOW_RADIUS + (MAX_GLOW_RADIUS - MIN_GLOW_RADIUS) * fraction
                allPaints.forEach { it.setShadowLayer(currentGlowRadius, 0f, 0f, it.color) }
                invalidate()
            }
        }
        glowAnimator?.start()
    }

    fun stopGlow() {
        glowAnimator?.cancel()
        glowAnimator = null
        allPaints.forEach { it.clearShadowLayer() }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopGlow() // Ensure animation is cleaned up
    }
}




