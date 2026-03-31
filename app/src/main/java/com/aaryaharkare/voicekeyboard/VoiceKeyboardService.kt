package com.aaryaharkare.voicekeyboard

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class VoiceKeyboardService : InputMethodService() {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateInputView(): View {
        val density = resources.displayMetrics.density
        val buttonSize = (72 * density).toInt()
        val bottomPadding = (128 * density).toInt()
        val keyboardHeight = (400 * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, bottomPadding)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                keyboardHeight
            )
            setBackgroundColor(Color.LTGRAY)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val recordButton = Button(this).apply {
            text = "Mic"
            setTextColor(Color.WHITE)
            textSize = 12f
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.RED)
            }
            background = shape
            layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
        }

        recordButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRecording()
                    recordButton.text = "..."
                    recordButton.alpha = 0.5f
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopRecording()
                    recordButton.isEnabled = false
                    recordButton.text = "..."
                    recordButton.alpha = 0.4f
                    transcribeAndInsert {
                        recordButton.isEnabled = true
                        recordButton.text = "Mic"
                        recordButton.alpha = 1.0f
                    }
                }
            }
            true
        }

        container.addView(recordButton)
        root.addView(container)

        return root
    }

    private fun startRecording() {
        audioFile = File(externalCacheDir, "voice_input.m4a")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            release()
        }
        mediaRecorder = null
    }

    private fun transcribeAndInsert(onDone: () -> Unit) {
        val file = audioFile ?: run { onDone(); return }
        Thread {
            val text = callOpenAITranscription(file)
            mainHandler.post {
                if (!text.isNullOrBlank()) {
                    currentInputConnection?.commitText(text, 1)
                }
                onDone()
            }
        }.start()
    }

    private fun callOpenAITranscription(file: File): String? {
        val boundary = "Boundary${System.currentTimeMillis()}"
        val conn = URL("https://api.openai.com/v1/audio/transcriptions")
            .openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.doOutput = true

            conn.outputStream.use { out ->
                // model field
                out.write("--$boundary\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\ngpt-4o-mini-transcribe\r\n".toByteArray())
                // file field
                out.write("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\nContent-Type: audio/mp4\r\n\r\n".toByteArray())
                out.write(file.readBytes())
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                JSONObject(response).optString("text")
            } else {
                val error = conn.errorStream?.bufferedReader()?.readText()
                android.util.Log.e("VoiceKeyboard", "Transcription error ${conn.responseCode}: $error")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            conn.disconnect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
    }
}
