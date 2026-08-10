package com.sakinah.mobile

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.sin

enum class SceneTheme { SUKOON, NOOR, FAJR }

data class SceneState(
    var surah: String = "سورة الرحمن",
    var verse: String = "فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ",
    var message: String = "استمع بقلبك…",
    var theme: SceneTheme = SceneTheme.SUKOON,
    var showTimer: Boolean = true,
    var startedAt: Long = 0L
)

class SakinahSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    val state = SceneState()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans", Typeface.BOLD)
    }
    private val messages = listOf(
        "استمع بقلبك…",
        "دع الدنيا قليلًا… واستمع",
        "أي آية توقفت عندها؟",
        "شارك هذه اللحظة مع من تحب"
    )
    private var messageIndex = 0
    private var downX = 0f
    private var downAt = 0L

    fun cycleTheme() {
        state.theme = when (state.theme) {
            SceneTheme.SUKOON -> SceneTheme.NOOR
            SceneTheme.NOOR -> SceneTheme.FAJR
            SceneTheme.FAJR -> SceneTheme.SUKOON
        }
        invalidate()
    }

    fun cycleMessage() {
        messageIndex = (messageIndex + 1) % messages.size
        state.message = messages[messageIndex]
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val t = SystemClock.uptimeMillis() / 1000f

        val colors = when (state.theme) {
            SceneTheme.SUKOON -> intArrayOf(Color.rgb(4, 5, 16), Color.rgb(25, 13, 56), Color.rgb(5, 12, 34))
            SceneTheme.NOOR -> intArrayOf(Color.rgb(8, 7, 18), Color.rgb(75, 48, 22), Color.rgb(12, 10, 25))
            SceneTheme.FAJR -> intArrayOf(Color.rgb(5, 17, 39), Color.rgb(22, 61, 98), Color.rgb(48, 42, 86))
        }
        paint.shader = LinearGradient(0f, 0f, 0f, h, colors, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val glow = when (state.theme) {
            SceneTheme.NOOR -> Color.rgb(255, 218, 137)
            SceneTheme.FAJR -> Color.rgb(93, 216, 255)
            SceneTheme.SUKOON -> Color.rgb(151, 96, 255)
        }
        val pulse = (90 + 30 * sin(t * 1.2f)).toInt()
        paint.shader = RadialGradient(
            w / 2f, h * .48f, w * .52f,
            Color.argb(pulse, Color.red(glow), Color.green(glow), Color.blue(glow)),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(w / 2f, h * .48f, w * .52f, paint)
        paint.shader = null

        for (i in 0 until 24) {
            val x = (i * 137f + t * (5 + i % 4)) % w
            val y = (i * 223f - t * (9 + i % 5) + h * 2f) % h
            paint.color = Color.argb(120, 240, 240, 255)
            canvas.drawCircle(x, y, 1.5f + (i % 3), paint)
        }

        drawBook(canvas, w, h)
        drawTextLayer(canvas, w, h)
        postInvalidateOnAnimation()
    }

    private fun drawBook(canvas: Canvas, w: Float, h: Float) {
        val cy = h * .56f
        val mid = w / 2f
        val left = w * .19f
        val right = w * .81f
        paint.color = Color.rgb(247, 243, 225)
        val p1 = Path().apply {
            moveTo(left, cy - h * .075f)
            quadTo(w * .34f, cy - h * .11f, mid - 5, cy - h * .025f)
            lineTo(mid - 5, cy + h * .115f)
            quadTo(w * .34f, cy + h * .04f, left + 15, cy + h * .075f)
            close()
        }
        val p2 = Path().apply {
            moveTo(right, cy - h * .075f)
            quadTo(w * .66f, cy - h * .11f, mid + 5, cy - h * .025f)
            lineTo(mid + 5, cy + h * .115f)
            quadTo(w * .66f, cy + h * .04f, right - 15, cy + h * .075f)
            close()
        }
        canvas.drawPath(p1, paint)
        canvas.drawPath(p2, paint)
        paint.color = Color.rgb(120, 90, 120)
        paint.strokeWidth = 4f
        canvas.drawLine(mid, cy - h * .025f, mid, cy + h * .115f, paint)
        for (i in 0..4) {
            val yy = cy + i * h * .022f
            paint.strokeWidth = 2f
            canvas.drawLine(w * .25f, yy, w * .44f, yy, paint)
            canvas.drawLine(w * .56f, yy, w * .75f, yy, paint)
        }
    }

    private fun drawTextLayer(canvas: Canvas, w: Float, h: Float) {
        textPaint.color = Color.WHITE
        textPaint.textSize = w * .034f
        canvas.drawText("SAKINAH • LIVE QUR'AN", w / 2f, h * .08f, textPaint)
        textPaint.textSize = w * .06f
        canvas.drawText(state.surah, w / 2f, h * .16f, textPaint)
        textPaint.typeface = Typeface.create("sans", Typeface.NORMAL)
        textPaint.textSize = w * .043f
        canvas.drawText(state.verse.take(55), w / 2f, h * .78f, textPaint)
        textPaint.typeface = Typeface.create("sans", Typeface.BOLD)
        textPaint.textSize = w * .034f
        canvas.drawText(state.message, w / 2f, h * .90f, textPaint)
        if (state.showTimer && state.startedAt > 0L) {
            val seconds = ((SystemClock.elapsedRealtime() - state.startedAt) / 1000L).coerceAtLeast(0L)
            val timer = String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
            textPaint.textSize = w * .027f
            canvas.drawText("● LIVE  $timer", w / 2f, h * .965f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downAt = SystemClock.uptimeMillis()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (SystemClock.uptimeMillis() - downAt < 650L) {
                    if (downX < width / 2f) cycleTheme() else cycleMessage()
                }
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
