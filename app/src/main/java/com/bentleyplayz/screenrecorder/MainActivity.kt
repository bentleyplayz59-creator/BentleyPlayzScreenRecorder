package com.bentleyplayz.screenrecorder

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.Volume
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import java.io.File
import java.io.FileInputStream
import java.io.IOException

@UnstableApi
class MainActivity : ComponentActivity() {
    private var videoUri: Uri? = null
    private var player: ExoPlayer? = null
    private var recorder: MediaRecorder? = null
    private var voiceFile: File? = null
    private var voiceUri: Uri? = null
    private var isRecording = false
    private var transformer: Transformer? = null
    private lateinit var playerView: PlayerView
    private lateinit var status: TextView
    private lateinit var recordButton: Button
    private lateinit var exportButton: Button
    private lateinit var originalVolumeLabel: TextView
    private lateinit var voiceVolumeLabel: TextView
    private var originalGain = 0.25f
    private var voiceGain = 1.5f

    private val openVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            loadVideo(uri)
        }
    }

    private val requestMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceover() else toast("Microphone permission is required to record narration.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildInterface()
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(16))
            setBackgroundColor(0xFF10141D.toInt())
        }
        fun text(value: String, size: Float, color: Int = 0xFFFFFFFF.toInt()): TextView =
            TextView(this).apply {
                this.text = value
                textSize = size
                setTextColor(color)
            }
        val heading = text("BENTLEYPLAYZ VIDEO VOICEOVER", 20f).apply {
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        root.addView(heading, LinearLayout.LayoutParams(-1, dp(48)))
        root.addView(text("Import an MP4, record narration while it plays, then mix both audio tracks into a new video.", 14f, 0xFFB9C4D4.toInt()).apply {
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, dp(58)))

        playerView = PlayerView(this).apply {
            useController = true
            setBackgroundColor(0xFF000000.toInt())
        }
        root.addView(playerView, LinearLayout.LayoutParams(-1, dp(225)))

        status = text("No video selected", 14f, 0xFFFFD166.toInt()).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(status)

        root.addView(actionButton("IMPORT MP4") { openVideo.launch(arrayOf("video/mp4")) })
        recordButton = actionButton("RECORD VOICEOVER") {
            if (isRecording) stopVoiceover() else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startVoiceover()
                } else requestMic.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        root.addView(recordButton)
        root.addView(actionButton("REVIEW VOICEOVER") { reviewVoiceover() })
        root.addView(text("MIX LEVELS", 15f).apply {
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(12), 0, dp(4))
        })
        originalVolumeLabel = text("Game / original audio: 25%", 14f)
        root.addView(originalVolumeLabel)
        root.addView(SeekBar(this).apply {
            max = 200
            progress = 25
            setOnSeekBarChangeListener(slider { value ->
                originalGain = value / 100f
                originalVolumeLabel.text = "Game / original audio: $value%"
            })
        })
        voiceVolumeLabel = text("Voiceover: 150%", 14f)
        root.addView(voiceVolumeLabel)
        root.addView(SeekBar(this).apply {
            max = 300
            progress = 150
            setOnSeekBarChangeListener(slider { value ->
                voiceGain = value / 100f
                voiceVolumeLabel.text = "Voiceover: $value%"
            })
        })
        exportButton = actionButton("EXPORT MIXED MP4") { exportVideo() }
        exportButton.isEnabled = false
        root.addView(exportButton)
        root.addView(text("The source video is kept unchanged. Export creates a separate MP4 in Movies/BentleyPlayz Video Voiceover.", 12f, 0xFF9BA9BC.toInt()).apply {
            setPadding(0, dp(10), 0, 0)
        })
        setContentView(root)
    }

    private fun slider(change: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) change(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }

    private fun loadVideo(uri: Uri) {
        if (isRecording) stopVoiceover()
        videoUri = uri
        voiceFile?.delete()
        voiceFile = null
        voiceUri = null
        player?.release()
        player = ExoPlayer.Builder(this).build().also {
            playerView.player = it
            it.setMediaItem(MediaItem.fromUri(uri))
            it.prepare()
            it.playWhenReady = false
        }
        status.text = "MP4 loaded. Play to preview, then record from the beginning."
        exportButton.isEnabled = false
    }

    private fun startVoiceover() {
        val uri = videoUri
        if (uri == null) {
            toast("Import an MP4 first.")
            return
        }
        try {
            player?.pause()
            player?.seekTo(0)
            val output = File(cacheDir, "voiceover-${System.currentTimeMillis()}.m4a")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioSamplingRate(48000)
            mediaRecorder.setAudioChannels(1)
            mediaRecorder.setAudioEncodingBitRate(128000)
            mediaRecorder.setOutputFile(output.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
            voiceFile = output
            isRecording = true
            recordButton.text = "STOP VOICEOVER"
            status.text = "Recording narration. Video playback is running from 00:00."
            player?.playWhenReady = true
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            isRecording = false
            recordButton.text = "RECORD VOICEOVER"
            status.text = "Could not start microphone recording."
            toast(e.message ?: "Recording failed")
        }
    }

    private fun stopVoiceover() {
        if (!isRecording) return
        player?.pause()
        try {
            recorder?.stop()
            voiceUri = voiceFile?.let { Uri.fromFile(it) }
            status.text = "Voiceover recorded. Review it or export the mixed MP4."
            exportButton.isEnabled = videoUri != null && voiceFile?.exists() == true
        } catch (e: RuntimeException) {
            voiceFile?.delete()
            voiceFile = null
            status.text = "Recording was too short. Record again."
            toast("Recording was too short.")
        } finally {
            recorder?.release()
            recorder = null
            isRecording = false
            recordButton.text = "RECORD VOICEOVER"
        }
    }

    private fun reviewVoiceover() {
        val file = voiceFile
        if (file == null || !file.exists() || isRecording) {
            toast("Record a voiceover first.")
            return
        }
        player?.pause()
        playerView.player = null
        player?.release()
        player = ExoPlayer.Builder(this).build().also {
            playerView.player = it
            it.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            it.prepare()
            it.playWhenReady = true
        }
        status.text = "Reviewing voiceover. Import the video again to return to video preview."
    }

    private fun exportVideo() {
        val source = videoUri
        val voice = voiceFile
        if (source == null || voice == null || !voice.exists() || isRecording) {
            toast("Import a video and finish recording first.")
            return
        }
        exportButton.isEnabled = false
        status.text = "Exporting. Keep the app open until export completes."
        val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(source))
            .setEffects(Effects(listOf(Volume(originalGain)), emptyList()))
            .build()
        val voiceItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(voice)))
            .setEffects(Effects(listOf(Volume(voiceGain)), emptyList()))
            .build()
        val videoSequence = EditedMediaItemSequence.Builder(listOf(videoItem)).build()
        val voiceSequence = EditedMediaItemSequence.Builder(listOf(voiceItem)).build()
        val composition = Composition.Builder(videoSequence, voiceSequence).build()
        val output = File(cacheDir, "BentleyVoiceover-${System.currentTimeMillis()}.mp4")
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: androidx.media3.transformer.ExportResult) {
                transformer = null
                try {
                    val saved = saveToMovies(output)
                    status.text = "Export complete: $saved"
                    toast("Saved to Movies/BentleyPlayz Video Voiceover")
                } catch (e: Exception) {
                    status.text = "Export finished, but saving failed: ${e.message}"
                    toast("Could not save exported MP4.")
                } finally {
                    output.delete()
                    exportButton.isEnabled = true
                }
            }

            override fun onError(
                composition: Composition,
                exportResult: androidx.media3.transformer.ExportResult,
                exportException: androidx.media3.transformer.ExportException
            ) {
                transformer = null
                status.text = "Export failed: ${exportException.message}"
                exportButton.isEnabled = true
                toast("Export failed. Try another MP4.")
            }
        }
        try {
            transformer = Transformer.Builder(this).addListener(listener).build()
            transformer?.start(composition, output.absolutePath)
        } catch (e: Exception) {
            transformer = null
            exportButton.isEnabled = true
            status.text = "Could not start export: ${e.message}"
            toast("Could not start export.")
        }
    }

    private fun saveToMovies(source: File): String {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "BentleyVoiceover-${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/BentleyPlayz Video Voiceover")
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("MediaStore could not create the output file")
        try {
            contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(source).use { input -> input.copyTo(output) }
            } ?: throw IOException("Could not open the output destination")
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
        return uri.toString()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        if (isRecording) {
            try { recorder?.stop() } catch (_: Exception) { }
            recorder?.release()
            recorder = null
        }
        transformer?.cancel()
        player?.release()
        player = null
        super.onDestroy()
    }
}
