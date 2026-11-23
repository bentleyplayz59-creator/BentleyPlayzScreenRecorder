package com.bentleyplayz.screenrecorder

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class FloatingBallService : Service() {
    private lateinit var windowManager: WindowManager
    private var ball: TextView? = null
    private lateinit var layoutParams: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val density = resources.displayMetrics.density
        val diameter = (58 * density).toInt()

        layoutParams = WindowManager.LayoutParams(
            diameter,
            diameter,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16 * density).toInt()
            y = (180 * density).toInt()
        }

        val bubble = TextView(this).apply {
            text = "BP"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundResource(android.R.drawable.btn_default)
            background.setTint(Color.rgb(82, 111, 245))
            elevation = 12 * density
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        bubble.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = layoutParams.x
                    startY = layoutParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 5 || abs(dy) > 5) moved = true
                    layoutParams.x = startX + dx
                    layoutParams.y = startY + dy
                    windowManager.updateViewLayout(view, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        Toast.makeText(
                            this,
                            "BentleyPlayz Recorder controls",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                }
                else -> false
            }
        }

        ball = bubble
        windowManager.addView(bubble, layoutParams)
    }

    override fun onDestroy() {
        ball?.let {
            if (it.isAttachedToWindow) windowManager.removeView(it)
        }
        ball = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
