package uz.ardo.tvhub

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** Ovoz balandligi o'zgarganda ekran pastida chiqadigan kichik indikator. */
class VolumeIndicator(private val activity: Activity) {

    val view: LinearLayout
    private val glyph: TextView
    private val fill: View
    private val rest: View
    private val percent: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var shown = false

    private val hideRunnable = Runnable { fadeOut() }

    private fun fadeOut() {
        shown = false
        view.animate().alpha(0f).setDuration(250).withEndAction {
            if (!shown) view.visibility = View.GONE
        }.start()
    }

    private fun dp(v: Int) = (v * activity.resources.displayMetrics.density).toInt()

    init {
        view = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22), dp(10), dp(26), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(40).toFloat()
                setColor(Color.argb(232, 16, 20, 36))
            }
            elevation = dp(12).toFloat()
            visibility = View.GONE
        }
        glyph = TextView(activity).apply {
            textSize = 26f
            setTextColor(Color.WHITE)
            setPadding(0, 0, dp(14), 0)
        }
        fill = View(activity).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(5).toFloat()
                setColor(Color.parseColor("#FFFFFF"))
            }
        }
        rest = View(activity)
        val track = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                cornerRadius = dp(5).toFloat()
                setColor(Color.argb(70, 255, 255, 255))
            }
            layoutParams = LinearLayout.LayoutParams(dp(240), dp(10))
        }
        track.addView(fill, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0f))
        track.addView(rest, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 100f))
        percent = TextView(activity).apply {
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.END
            minWidth = dp(56)
            setPadding(dp(14), 0, 0, 0)
        }
        view.addView(glyph)
        view.addView(track)
        view.addView(percent)
    }

    /** @param level 0..100 */
    fun show(level: Int, muted: Boolean) {
        val pct = if (muted) 0 else level.coerceIn(0, 100)
        glyph.text = when {
            muted || pct == 0 -> "\uD83D\uDD07"
            pct < 34 -> "\uD83D\uDD08"
            pct < 67 -> "\uD83D\uDD09"
            else -> "\uD83D\uDD0A"
        }
        percent.text = if (muted) "\u2014" else pct.toString()
        fill.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, pct.toFloat())
        rest.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (100 - pct).toFloat())

        shown = true
        handler.removeCallbacks(hideRunnable)
        view.animate().cancel()
        view.alpha = 1f
        view.visibility = View.VISIBLE
        handler.postDelayed(hideRunnable, 1800)
    }

    fun cancel() {
        handler.removeCallbacks(hideRunnable)
        view.animate().cancel()
        shown = false
        view.visibility = View.GONE
    }
}
