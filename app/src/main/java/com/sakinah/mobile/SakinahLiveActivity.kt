package com.sakinah.mobile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.pedro.common.ConnectChecker

class SakinahLiveActivity : AppCompatActivity(), ConnectChecker {
    private lateinit var scene: SakinahSceneView
    private lateinit var panel: View
    private lateinit var status: TextView
    private lateinit var server: TextInputEditText
    private lateinit var key: TextInputEditText
    private lateinit var surah: TextInputEditText
    private lateinit var verse: TextInputEditText
    private lateinit var audioMode: Spinner
    private lateinit var rtmpContainer: LinearLayout
    private var selectedAudio: Uri? = null
    private var player: MediaPlayer? = null
    private var wantStart = false

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val audioPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            selectedAudio = uri
            prefs().edit().putString("audio", uri.toString()).apply()
            toast("تم اختيار ملف التلاوة")
        }
    }

    private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && wantStart) {
            val svc = BroadcastService.INSTANCE ?: return@registerForActivityResult
            val mode = when (audioMode.selectedItemPosition) {
                0 -> BroadcastService.AudioMode.INTERNAL
                1 -> BroadcastService.AudioMode.MIX
                else -> BroadcastService.AudioMode.MICROPHONE
            }
            if (svc.prepareProjection(result.resultCode, result.data!!, mode)) {
                if (mode != BroadcastService.AudioMode.MICROPHONE) startLocalAudio()
                enterSceneMode()
                svc.startStream(endpoint())
            } else toast("تعذر تجهيز البث")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        buildUi()
        restore()
        scene.setOnLongClickListener {
            panel.visibility = if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            true
        }
    }

    private fun buildUi() {
        val root = FrameLayout(this)
        scene = SakinahSceneView(this)
        root.addView(scene, FrameLayout.LayoutParams(-1, -1))
        panel = buildPanel()
        root.addView(panel, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(root)
    }

    private fun buildPanel(): View {
        val card = MaterialCardView(this).apply {
            radius = 36f
            setCardBackgroundColor(0xF21A1926.toInt())
            setContentPadding(30, 26, 30, 34)
        }
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        status = TextView(this).apply {
            text = "● SAKINAH Studio — جاهز"
            textSize = 16f
            setTextColor(0xFF8FFFD6.toInt())
            setPadding(0, 0, 0, 14)
        }
        box.addView(status)

        surah = field(box, "السورة", "سورة الرحمن")
        verse = field(box, "الآية / النص الظاهر", "فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ")

        val choose = MaterialButton(this).apply {
            text = "اختيار ملف التلاوة MP3 / M4A"
            setOnClickListener { audioPicker.launch(arrayOf("audio/*")) }
        }
        box.addView(choose)

        val previewAudio = MaterialButton(this).apply {
            text = "تشغيل / إيقاف التلاوة"
            setOnClickListener {
                if (player?.isPlaying == true) player?.pause()
                else if (selectedAudio != null) startLocalAudio() else toast("اختر ملف التلاوة أولاً")
            }
        }
        box.addView(previewAudio)

        val theme = MaterialButton(this).apply {
            text = "المشهد: سكون ← نور ← فجر"
            setOnClickListener { scene.cycleTheme() }
        }
        box.addView(theme)

        val sceneOnly = MaterialButton(this).apply {
            text = "تشغيل المشهد ملء الشاشة"
            setOnClickListener {
                applyScene(); save(); if (selectedAudio != null) startLocalAudio(); enterSceneMode()
            }
        }
        box.addView(sceneOnly)

        rtmpContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        server = field(rtmpContainer, "RTMP / RTMPS Server", "rtmp://example.com/live")
        key = field(rtmpContainer, "Stream Key / Path", "")
        audioMode = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@SakinahLiveActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("صوت التطبيق", "التطبيق + الميكروفون", "الميكروفون فقط")
            )
        }
        rtmpContainer.addView(audioMode)
        box.addView(rtmpContainer)

        val start = MaterialButton(this).apply {
            text = "● بدء البث المباشر RTMP / RTMPS"
            textSize = 17f
            setOnClickListener { startBroadcast() }
        }
        box.addView(start)

        val back = MaterialButton(this).apply {
            text = "العودة إلى ATHAR"
            setOnClickListener { finish() }
        }
        box.addView(back)

        val hint = TextView(this).apply {
            text = "ضغط مطوّل أثناء المشهد يعيد لوحة التحكم. يمكن استخدام المشهد وحده كبث شاشة، أو استخدام RTMP/RTMPS عند توفر وجهة بث."
            textSize = 12f
            setTextColor(0xFFB9B7C9.toInt())
            setPadding(0, 14, 0, 0)
        }
        box.addView(hint)
        scroll.addView(box)
        card.addView(scroll, ViewGroup.LayoutParams(-1, (resources.displayMetrics.heightPixels * .66f).toInt()))
        return card
    }

    private fun field(parent: LinearLayout, label: String, value: String): TextInputEditText {
        val layout = TextInputLayout(this).apply { hint = label; setPadding(0, 6, 0, 6) }
        val edit = TextInputEditText(layout.context).apply {
            setText(value); setTextColor(0xFFF5F4FA.toInt()); setHintTextColor(0xFF9E9BAD.toInt())
        }
        layout.addView(edit)
        parent.addView(layout, ViewGroup.LayoutParams(-1, -2))
        return edit
    }

    private fun startBroadcast() {
        applyScene(); save()
        val ep = endpoint()
        if (!ep.startsWith("rtmp://") && !ep.startsWith("rtmps://")) {
            toast("أدخل عنوان RTMP أو RTMPS صحيحاً"); return
        }
        if (audioMode.selectedItemPosition != 0 && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            toast("اسمح بالميكروفون ثم أعد المحاولة"); return
        }
        val svc = BroadcastService.INSTANCE
        if (svc == null) {
            startService(Intent(this, BroadcastService::class.java))
            scene.postDelayed({ startBroadcast() }, 350)
            return
        }
        svc.setListener(this)
        wantStart = true
        projectionLauncher.launch(svc.projectionIntent())
    }

    private fun endpoint(): String {
        val base = server.text?.toString()?.trim().orEmpty()
        val k = key.text?.toString()?.trim().orEmpty()
        return if (k.isBlank()) base else base.trimEnd('/') + "/" + k.trimStart('/')
    }

    private fun startLocalAudio() {
        val uri = selectedAudio ?: return
        player?.release()
        player = MediaPlayer().apply {
            setDataSource(this@SakinahLiveActivity, uri)
            isLooping = true
            setOnPreparedListener { it.start() }
            setOnErrorListener { _, _, _ -> toast("تعذر تشغيل ملف التلاوة"); true }
            prepareAsync()
        }
    }

    private fun applyScene() {
        scene.state.surah = surah.text?.toString()?.ifBlank { "سورة الرحمن" } ?: "سورة الرحمن"
        scene.state.verse = verse.text?.toString().orEmpty()
        scene.invalidate()
    }

    private fun enterSceneMode() {
        scene.state.startedAt = SystemClock.elapsedRealtime()
        panel.visibility = View.GONE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun save() {
        prefs().edit()
            .putString("server", server.text?.toString())
            .putString("key", key.text?.toString())
            .putString("surah", surah.text?.toString())
            .putString("verse", verse.text?.toString())
            .putInt("audioMode", audioMode.selectedItemPosition)
            .apply()
    }

    private fun restore() {
        val p = prefs()
        server.setText(p.getString("server", server.text.toString()))
        key.setText(p.getString("key", ""))
        surah.setText(p.getString("surah", surah.text.toString()))
        verse.setText(p.getString("verse", verse.text.toString()))
        audioMode.setSelection(p.getInt("audioMode", 0))
        p.getString("audio", null)?.let { selectedAudio = Uri.parse(it) }
        applyScene()
    }

    private fun prefs() = getSharedPreferences("sakinah_live", MODE_PRIVATE)
    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    override fun onConnectionStarted(url: String) { runOnUiThread { status.text = "● جارٍ الاتصال…" } }
    override fun onConnectionSuccess() { runOnUiThread { status.text = "● LIVE — متصل"; toast("تم بدء البث") } }
    override fun onConnectionFailed(reason: String) { runOnUiThread { status.text = "تعذر الاتصال: $reason"; panel.visibility = View.VISIBLE; player?.pause() } }
    override fun onNewBitrate(bitrate: Long) { runOnUiThread { status.text = "● LIVE • ${bitrate / 1000} kbps" } }
    override fun onDisconnect() { runOnUiThread { status.text = "● تم إيقاف البث"; panel.visibility = View.VISIBLE; player?.pause() } }
    override fun onAuthError() { runOnUiThread { toast("خطأ في بيانات البث") } }
    override fun onAuthSuccess() { }

    override fun onDestroy() {
        player?.release()
        BroadcastService.INSTANCE?.setListener(null)
        super.onDestroy()
    }
}
