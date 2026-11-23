package com.bentleyplayz.screenrecorder

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val background = Color.rgb(12, 15, 27)
    private val panel = Color.rgb(25, 31, 51)
    private val accent = Color.rgb(91, 126, 255)
    private lateinit var overlayStatus: TextView

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(30), dp(22), dp(24))
            setBackgroundColor(background)
        }

        val brand = TextView(this).apply {
            text = "BENTLEYPLAYZ"
            textSize = 13f
            letterSpacing = 0.22f
            setTextColor(Color.rgb(139, 164, 255))
        }
        root.addView(brand)

        val title = TextView(this).apply {
            text = "Screen Recorder"
            textSize = 30f
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(4))
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Capture your screen, game audio and microphone."
            textSize = 14f
            setTextColor(Color.rgb(166, 176, 199))
        }
        root.addView(subtitle)

        val preview = TextView(this).apply {
            text = "RECORDING STUDIO\n\nVIDEO  •  INTERNAL AUDIO  •  MICROPHONE\n\nYour recordings will be saved as MP4."
            textSize = 15f
            setTextColor(Color.WHITE)
            setLineSpacing(dp(7).toFloat(), 1f)
            setPadding(dp(20), dp(22), dp(20), dp(22))
            setBackgroundColor(panel)
        }
        val previewParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        previewParams.topMargin = dp(28)
        root.addView(preview, previewParams)

        val section = TextView(this).apply {
            text = "QUICK CONTROLS"
            textSize = 12f
            letterSpacing = 0.12f
            setTextColor(Color.rgb(139, 164, 255))
            setPadding(0, dp(26), 0, dp(12))
        }
        root.addView(section)

        val enableOverlay = makeButton("Enable floating ball")
        enableOverlay.setOnClickListener { enableFloatingBall() }
        root.addView(enableOverlay)

        val disableOverlay = makeButton("Hide floating ball")
        disableOverlay.setOnClickListener {
            stopService(Intent(this, FloatingBallService::class.java))
            overlayStatus.text = "Floating control hidden"
        }
        root.addView(disableOverlay)

        overlayStatus = TextView(this).apply {
            text = if (Settings.canDrawOverlays(this@MainActivity)) {
                "Overlay permission available"
            } else {
                "Floating ball permission not enabled"
            }
            textSize = 13f
            setTextColor(Color.rgb(167, 183, 211))
            setPadding(0, dp(16), 0, dp(12))
        }
        root.addView(overlayStatus)

        val note = TextView(this).apply {
            text = "Recording engine is being built. Screen capture and synchronized audio will be added to the next build milestone."
            textSize = 12f
            setTextColor(Color.rgb(133, 145, 170))
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(note)

        setContentView(root)
    }

    private fun makeButton(label: String): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(accent)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)
            ).apply {
                bottomMargin = dp(10)
            }
        }

    private fun enableFloatingBall() {
        if (Settings.canDrawOverlays(this)) {
            startService(Intent(this, FloatingBallService::class.java))
            overlayStatus.text = "Floating control enabled"
        } else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(
                this,
                "Allow display over other apps, then return to the recorder.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::overlayStatus.isInitialized && Settings.canDrawOverlays(this)) {
            overlayStatus.text = "Overlay permission available"
        }
    }
}
