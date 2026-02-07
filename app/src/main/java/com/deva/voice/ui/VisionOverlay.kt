package com.deva.voice.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.deva.voice.R

class VisionOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scanLinePaint = Paint().apply {
        color = Color.CYAN
        strokeWidth = 5f
        style = Paint.Style.STROKE
        shader = LinearGradient(0f, 0f, 1000f, 0f, 
            intArrayOf(Color.TRANSPARENT, Color.CYAN, Color.TRANSPARENT), 
            null, Shader.TileMode.CLAMP)
    }
    
    private var scanY = 0f
    private var animator: ValueAnimator? = null
    private val statusText: TextView

    init {
        setBackgroundColor(Color.parseColor("#88000000")) // Semi-transparent black background
        
        statusText = TextView(context).apply {
            text = "DeVA Vision Analyzing..."
            textSize = 18f
            setTextColor(Color.WHITE)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.CENTER
            }
        }
        addView(statusText)
    }

    fun startScanning() {
        visibility = View.VISIBLE
        animator = ValueAnimator.ofFloat(0f, height.toFloat()).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                scanY = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopScanning() {
        animator?.cancel()
        visibility = View.GONE
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (visibility == View.VISIBLE) {
            canvas.drawLine(0f, scanY, width.toFloat(), scanY, scanLinePaint)
            
            // Draw slightly glowing rect behind the line
            scanLinePaint.alpha = 50
            canvas.drawRect(0f, scanY - 20, width.toFloat(), scanY, scanLinePaint)
            scanLinePaint.alpha = 255
        }
    }
}




