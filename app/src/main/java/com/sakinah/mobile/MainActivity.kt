package com.sakinah.mobile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.pedro.common.ConnectChecker

class MainActivity : AppCompatActivity(), ConnectChecker {
    private lateinit var scene: SakinahSceneView
    private lateinit var panel: View
    private lateinit var status: TextView
    private lateinit var server: TextInputEditText
    private lateinit var key: TextInputEditText
    private lateinit var surah: TextInputEditText
    private lateinit var verse: TextInputEditText
    private lateinit var broadcastMode: Spinner
    private lateinit var audioMode: Spinner
    private lateinit var rtmpContainer: LinearLayout
    private lateinit var rtmpAudioContainer: LinearLayout
    private lateinit var tiktokGuide: TextView
    private lateinit var primaryButton: MaterialButton
    private var selectedAudio: Uri? = null
    private var player: MediaPlayer? = null
    private var wantStart = false
    private var awaitingTikTokReturn = false

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val audioPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Some document providers do not expose persistable permission. The URI can still work this session.
            }
            selectedAudio = uri
            getPreferences(MODE_PRIVATE).edit().putString("audio", uri.toString()).apply()
            Toast.makeText(this, "تم اختيار ملف التلاوة", Toast.LENGTH_SHORT).show()
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
                startLocalAudioIfNeeded(mode)
                enterSceneMode()
                svc.startStream(endpoint())
            } else {
                toast("تعذر تجهيز البث")
            }
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

    override fun onResume() {
        super.onResume()
        if (awaitingTikTokReturn) {
            awaitingTikTokReturn = false
            scene.postDelayed({
                startTikTokScene()
            }, 450)
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
        val sc = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        status = TextView(this).apply {
            text = "● جاهز — وضع TikTok Mobile لا يحتاج Stream Key"
            textSize = 16f
            setTextColor(0xFF8FFFD6.toInt())
            setPadding(0, 0, 0, 14)
        }
        box.addView(status)

        broadcastMode = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    "TikTok — بث شاشة الهاتف بدون Stream Key",
                    "RTMP / RTMPS — عند تفعيل مفتاح البث لاحقًا"
                )
            )
        }
        box.addView(broadcastMode)

        surah = field(box, "السورة", "سورة الرحمن")
        verse = field(box, "الآية / النص الظاهر", "فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ")

        tiktokGuide = TextView(this).apply {
            text = "الوضع الحالي: سكينة تجهّز المشهد والصوت على الهاتف، وTikTok نفسه يقوم بالبث عبر خيار مشاركة/بث الشاشة. لا يحتاج هذا الوضع إلى RTMP أو LIVE Studio. بعد بدء بث الشاشة في TikTok ارجع إلى سكينة، وسيبدأ المشهد تلقائيًا."
            textSize = 13f
            setTextColor(0xFFE8E6F2.toInt())
            setPadding(0, 12, 0, 12)
        }
        box.addView(tiktokGuide)

        val choose = MaterialButton(this).apply {
            text = "اختيار ملف التلاوة MP3 / M4A"
            setOnClickListener { audioPicker.launch(arrayOf("audio/*")) }
        }
        box.addView(choose)

        val testAudio = MaterialButton(this).apply {
            text = "اختبار / إيقاف التلاوة"
            setOnClickListener { toggleAudioPreview() }
        }
        box.addView(testAudio)

        val theme = MaterialButton(this).apply {
            text = "تبديل المشهد: سكون ← نور ← فجر"
            setOnClickListener { scene.cycleTheme() }
        }
        box.addView(theme)

        val preview = MaterialButton(this).apply {
            text = "تحديث المعاينة"
            setOnClickListener { applyScene() }
        }
        box.addView(preview)

        rtmpContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        server = field(rtmpContainer, "RTMP / RTMPS Server", "rtmp://example.com/live")
        key = field(rtmpContainer, "Stream Key أو اسم المسار", "")
        box.addView(rtmpContainer)

        rtmpAudioContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        audioMode = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("صوت التطبيق", "صوت التطبيق + الميكروفون", "الميكروفون فقط")
            )
        }
        rtmpAudioContainer.addView(audioMode)
        box.addView(rtmpAudioContainer)

        primaryButton = MaterialButton(this).apply {
            text = "1) افتح TikTok وابدأ بث الشاشة"
            textSize = 17f
            setOnClickListener {
                if (broadcastMode.selectedItemPosition == 0) prepareTikTokBridge()
                else startBroadcast()
            }
        }
        box.addView(primaryButton)

        val sceneOnly = MaterialButton(this).apply {
            text = "2) تشغيل المشهد الآن / العودة إليه"
            setOnClickListener { startTikTokScene() }
        }
        box.addView(sceneOnly)

        val stop = MaterialButton(this).apply {
            text = "إيقاف التلاوة وإظهار التحكم"
            setOnClickListener {
                player?.pause()
                scene.state.startedAt = 0L
                panel.visibility = View.VISIBLE
                showSystemUi()
            }
        }
        box.addView(stop)

        val hint = TextView(this).apply {
            text = "أثناء المشهد: ضغط مطوّل على الشاشة يُظهر لوحة التحكم. في وضع TikTok، البث نفسه يبدأ وينتهي من تطبيق TikTok. سكينة لا تتجاوز صلاحيات TikTok ولا تفعّل LIVE للحساب غير المؤهل."
            textSize = 12f
            setTextColor(0xFFB9B7C9.toInt())
            setPadding(0, 14, 0, 0)
        }
        box.addView(hint)

        broadcastMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateModeUi(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        sc.addView(box)
        card.addView(sc, ViewGroup.LayoutParams(-1, (resources.displayMetrics.heightPixels * .62f).toInt()))
        return card
    }

    private fun field(parent: LinearLayout, label: String, value: String): TextInputEditText {
        val layout = TextInputLayout(this).apply {
            hint = label
            setPadding(0, 6, 0, 6)
        }
        val edit = TextInputEditText(layout.context).apply {
            setText(value)
            setTextColor(0xFFF5F4FA.toInt())
            setHintTextColor(0xFF9E9BAD.toInt())
        }
        layout.addView(edit)
        parent.addView(layout, ViewGroup.LayoutParams(-1, -2))
        return edit
    }

    private fun updateModeUi(position: Int) {
        val tiktok = position == 0
        rtmpContainer.visibility = if (tiktok) View.GONE else View.VISIBLE
        rtmpAudioContainer.visibility = if (tiktok) View.GONE else View.VISIBLE
        tiktokGuide.visibility = if (tiktok) View.VISIBLE else View.GONE
        primaryButton.text = if (tiktok) {
            "1) افتح TikTok وابدأ بث الشاشة"
        } else {
            "● ابدأ RTMP / RTMPS من الهاتف"
        }
        status.text = if (tiktok) {
            "● TikTok Mobile Bridge — بدون Stream Key"
        } else {
            "● RTMP جاهز — يحتاج عنوان ومفتاح بث صالحين"
        }
    }

    private fun prepareTikTokBridge() {
        applyScene()
        save()
        AlertDialog.Builder(this)
            .setTitle("TikTok Mobile Bridge")
            .setMessage(
                "الخطوات:\n\n" +
                    "1. اضغط «فتح TikTok».\n" +
                    "2. من TikTok ابدأ LIVE واختر بث/مشاركة الشاشة أو Mobile Gaming إذا كان الخيار متاحًا لحسابك.\n" +
                    "3. بعد أن يبدأ TikTok مشاركة الشاشة، ارجع إلى تطبيق سكينة.\n" +
                    "4. سكينة ستعرض المشهد القرآني وتشغّل ملف التلاوة تلقائيًا.\n\n" +
                    "هذا الوضع لا يحتاج LIVE Studio ولا Stream Key، لكنه يحتاج أن يكون LIVE عبر الهاتف متاحًا أصلًا في حساب TikTok."
            )
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("فتح TikTok") { _, _ ->
                awaitingTikTokReturn = true
                if (!openTikTok()) {
                    awaitingTikTokReturn = false
                    toast("لم أجد تطبيق TikTok على الهاتف")
                }
            }
            .show()
    }

    private fun openTikTok(): Boolean {
        val packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
        for (pkg in packages) {
            val launch = packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
                return true
            }
        }
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/")))
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun startTikTokScene() {
        applyScene()
        save()
        if (selectedAudio == null) {
            toast("اختر ملف التلاوة أولًا، أو شغّل المشهد دون صوت من سكينة")
        } else {
            startLocalAudioPlayback()
        }
        status.text = "● TikTok Screen Share — المشهد يعمل"
        enterSceneMode()
    }

    private fun toggleAudioPreview() {
        if (player?.isPlaying == true) {
            player?.pause()
            toast("تم إيقاف التلاوة مؤقتًا")
        } else {
            if (selectedAudio == null) {
                toast("اختر ملف التلاوة أولًا")
                return
            }
            startLocalAudioPlayback()
        }
    }

    private fun startLocalAudioPlayback() {
        val uri = selectedAudio ?: return
        player?.release()
        player = MediaPlayer().apply {
            setDataSource(this@MainActivity, uri)
            isLooping = true
            setOnPreparedListener { it.start() }
            setOnErrorListener { _, _, _ ->
                runOnUiThread { toast("تعذر تشغيل ملف التلاوة") }
                true
            }
            prepareAsync()
        }
    }

    private fun enterSceneMode() {
        scene.state.startedAt = SystemClock.elapsedRealtime()
        panel.visibility = View.GONE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun showSystemUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    private fun startBroadcast() {
        applyScene()
        save()
        val ep = endpoint()
        if (!ep.startsWith("rtmp://") && !ep.startsWith("rtmps://")) {
            toast("أدخل عنوان RTMP أو RTMPS صحيحًا")
            return
        }
        if (audioMode.selectedItemPosition != 0 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            toast("اسمح بالميكروفون ثم اضغط بدء البث مرة أخرى")
            return
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

    private fun startLocalAudioIfNeeded(mode: BroadcastService.AudioMode) {
        if (mode == BroadcastService.AudioMode.MICROPHONE) return
        if (selectedAudio != null) startLocalAudioPlayback()
    }

    private fun endpoint(): String {
        val base = server.text?.toString()?.trim().orEmpty()
        val k = key.text?.toString()?.trim().orEmpty()
        return if (k.isBlank()) base else base.trimEnd('/') + "/" + k.trimStart('/')
    }

    private fun applyScene() {
        scene.state.surah = surah.text?.toString()?.ifBlank { "سورة الرحمن" } ?: "سورة الرحمن"
        scene.state.verse = verse.text?.toString().orEmpty()
        scene.invalidate()
    }

    private fun save() {
        getPreferences(MODE_PRIVATE).edit()
            .putString("server", server.text?.toString())
            .putString("key", key.text?.toString())
            .putString("surah", surah.text?.toString())
            .putString("verse", verse.text?.toString())
            .putInt("broadcastMode", broadcastMode.selectedItemPosition)
            .putInt("audioMode", audioMode.selectedItemPosition)
            .apply()
    }

    private fun restore() {
        val p = getPreferences(MODE_PRIVATE)
        server.setText(p.getString("server", server.text.toString()))
        key.setText(p.getString("key", ""))
        surah.setText(p.getString("surah", surah.text.toString()))
        verse.setText(p.getString("verse", verse.text.toString()))
        broadcastMode.setSelection(p.getInt("broadcastMode", 0))
        audioMode.setSelection(p.getInt("audioMode", 0))
        p.getString("audio", null)?.let { selectedAudio = Uri.parse(it) }
        updateModeUi(broadcastMode.selectedItemPosition)
        applyScene()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onConnectionStarted(url: String) { runOnUiThread { status.text = "● جارٍ الاتصال…" } }
    override fun onConnectionSuccess() { runOnUiThread { status.text = "● LIVE — متصل"; toast("تم بدء البث") } }
    override fun onConnectionFailed(reason: String) {
        runOnUiThread {
            status.text = "تعذر الاتصال: $reason"
            panel.visibility = View.VISIBLE
            player?.pause()
            BroadcastService.INSTANCE?.stopStream()
            showSystemUi()
        }
    }
    override fun onNewBitrate(bitrate: Long) { runOnUiThread { status.text = "● LIVE  •  ${bitrate / 1000} kbps" } }
    override fun onDisconnect() {
        runOnUiThread {
            status.text = "● تم إيقاف البث"
            scene.state.startedAt = 0
            panel.visibility = View.VISIBLE
            player?.pause()
            showSystemUi()
        }
    }
    override fun onAuthError() { runOnUiThread { toast("خطأ في مفتاح البث") } }
    override fun onAuthSuccess() { }

    override fun onDestroy() {
        player?.release()
        BroadcastService.INSTANCE?.setListener(null)
        super.onDestroy()
    }
}
