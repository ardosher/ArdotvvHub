package uz.ardo.tvhub

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ekran saqlovchi: qora fon, sekin suzuvchi rangli dog'lar va katta soat.
 * Soat matni har 20 soniyada sekin siljiydi (ekranda "kuyib qolmasligi" uchun).
 * Har qanday tugma bosilsa MainActivity uni yopadi.
 */
class ScreensaverView(context: Context) : FrameLayout(context) {

    /** false bo'lsa dog'lar harakatlanmaydi (Sozlamalar > Animatsiyalar o'chiq bo'lganda). */
    var animated: Boolean = true

    var isShowing: Boolean = false
        private set

    private val aurora = AuroraView(context)
    private val content = LinearLayout(context)
    private val clock = TextClock(context)
    private val date = TextView(context)
    private val weather = TextView(context)

    private val drift = object : Runnable {
        override fun run() {
            driftContent()
            postDelayed(this, DRIFT_MS)
        }
    }

    init {
        setBackgroundColor(Color.BLACK)
        visibility = View.GONE
        isClickable = true

        addView(aurora, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        content.orientation = LinearLayout.VERTICAL
        content.gravity = Gravity.CENTER_HORIZONTAL

        clock.format24Hour = "HH:mm"
        clock.format12Hour = "HH:mm"
        clock.textSize = 110f
        clock.includeFontPadding = false
        clock.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        clock.setTextColor(Color.WHITE)
        clock.gravity = Gravity.CENTER

        date.textSize = 28f
        date.setTextColor(Color.parseColor("#B4BCDD"))
        date.gravity = Gravity.CENTER

        weather.textSize = 26f
        weather.setTextColor(Color.parseColor("#DDE3FF"))
        weather.gravity = Gravity.CENTER
        weather.setPadding(0, dp(10), 0, 0)
        weather.visibility = View.GONE

        content.addView(clock)
        content.addView(date)
        content.addView(weather)
        addView(content, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    /** Ob-havo qatori (masalan "☀ 23°C · Toshkent"). null yoki bo'sh bo'lsa yashiriladi. */
    fun setWeather(text: String?) {
        weather.text = text ?: ""
        weather.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    fun show() {
        if (isShowing) return
        isShowing = true
        date.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
        aurora.running = animated
        content.animate().cancel()
        content.translationX = 0f
        content.translationY = 0f
        animate().cancel()
        alpha = 0f
        visibility = View.VISIBLE
        animate().alpha(1f).setDuration(700).start()
        removeCallbacks(drift)
        postDelayed(drift, DRIFT_MS)
    }

    fun hide(immediate: Boolean = false) {
        if (!isShowing) return
        isShowing = false
        removeCallbacks(drift)
        aurora.running = false
        animate().cancel()
        if (immediate) {
            alpha = 0f
            visibility = View.GONE
        } else {
            animate().alpha(0f).setDuration(250).withEndAction {
                if (!isShowing) visibility = View.GONE
            }.start()
        }
    }

    private fun driftContent() {
        val maxX = width * 0.18f
        val maxY = height * 0.16f
        val tx = (Random.nextFloat() * 2f - 1f) * maxX
        val ty = (Random.nextFloat() * 2f - 1f) * maxY
        content.animate().translationX(tx).translationY(ty).setDuration(4000).start()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(drift)
        super.onDetachedFromWindow()
    }

    /** Sekin harakatlanuvchi yumshoq rangli dog'lar. ~25 kadr/soniya. */
    private class AuroraView(context: Context) : View(context) {

        var running: Boolean = false
            set(value) {
                field = value
                if (value) invalidate()
            }

        private class Blob(
            val r: Int, val g: Int, val b: Int, val alpha: Int,
            val radiusFrac: Float, val speedX: Double, val speedY: Double,
            val phaseX: Double, val phaseY: Double
        )

        private val blobs = listOf(
            Blob(59, 91, 219, 150, 0.45f, 0.11, 0.07, 0.0, 1.3),
            Blob(139, 92, 246, 120, 0.40f, 0.08, 0.10, 2.1, 0.4),
            Blob(15, 118, 110, 120, 0.38f, 0.06, 0.09, 4.0, 2.7)
        )
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var shaders: List<Shader> = emptyList()
        private var radii: List<Float> = emptyList()

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            val base = minOf(w, h).toFloat()
            radii = blobs.map { it.radiusFrac * base * 1.6f }
            shaders = blobs.mapIndexed { i, b ->
                RadialGradient(
                    0f, 0f, radii[i].coerceAtLeast(1f),
                    Color.argb(b.alpha, b.r, b.g, b.b),
                    Color.argb(0, b.r, b.g, b.b),
                    Shader.TileMode.CLAMP
                )
            }
        }

        override fun onDraw(canvas: Canvas) {
            if (shaders.isEmpty()) return
            val t = SystemClock.uptimeMillis() / 1000.0
            for (i in blobs.indices) {
                val b = blobs[i]
                val cx = width * (0.5 + 0.35 * sin(t * b.speedX + b.phaseX)).toFloat()
                val cy = height * (0.5 + 0.30 * sin(t * b.speedY + b.phaseY)).toFloat()
                paint.shader = shaders[i]
                canvas.save()
                canvas.translate(cx, cy)
                canvas.drawCircle(0f, 0f, radii[i], paint)
                canvas.restore()
            }
            if (running) postInvalidateDelayed(40)
        }
    }

    companion object {
        private const val DRIFT_MS = 20_000L
    }
}
