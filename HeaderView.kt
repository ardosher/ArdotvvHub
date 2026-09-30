package uz.ardo.tvhub

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView

/**
 * Android TV originaliga o'xshash yupqa sarlavha qator: chapda kichik logotip + nom + profil,
 * o'ngda ob-havo, ikonka-tugmalar (Ovoz / Qidiruv / Barcha ilovalar / Wi-Fi / Sozlamalar) va soat.
 * Hammasi pult bilan fokuslanadi.
 */
class HeaderView(
    private val activity: Activity,
    initialTheme: HubTheme,
    prefs: Prefs,
    profile: Profile,
    private val onAction: (String) -> Unit
) {
    val view: LinearLayout
    private var theme: HubTheme = initialTheme
    private val appName: TextView
    private val profileChip: TextView
    private val clock: TextClock
    private val date: TextView
    private val wifiIcon: TextView
    private val internetDot: TextView
    private val iconButtons: List<TextView>
    private val weatherBox: LinearLayout
    private val weatherTemp: TextView
    private val weatherCity: TextView

    private var weather: WeatherData? = null
    private var weatherEnabled = true

    private fun dp(v: Int) = (v * activity.resources.displayMetrics.density).toInt()

    init {
        view = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(20) }
        }

        // Chap taraf: kichik logotip nuqtasi + nom + profil
        val left = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        left.addView(TextView(activity).apply {
            text = " "
            layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)).apply { marginEnd = dp(10) }
            background = GradientDrawable().apply {
                cornerRadius = dp(4).toFloat()
                setColor(Color.parseColor("#3B5BDB"))
            }
        })
        appName = TextView(activity).apply {
            text = activity.getString(R.string.app_name)
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(theme.ink)
        }
        left.addView(appName)

        profileChip = TextView(activity).apply {
            text = "${profile.avatar}  ${profile.name}"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(theme.inkDim)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            isFocusableInTouchMode = false
            setPadding(dp(14), dp(6), dp(16), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(18) }
            fun bg(focused: Boolean) = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(if (focused) theme.surfaceAlt else theme.surface)
                if (focused) setStroke(dp(2), theme.focusRing)
            }
            background = bg(false)
            setOnFocusChangeListener { v, f ->
                v.background = bg(f)
                (v as TextView).setTextColor(if (f) theme.ink else theme.inkDim)
            }
            setOnClickListener { onAction("profile") }
        }
        left.addView(profileChip)
        view.addView(left)

        // O'ng taraf: ob-havo + ikonka-tugmalar + soat/sana
        val right = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        weatherTemp = TextView(activity).apply {
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(theme.ink)
            gravity = Gravity.END
        }
        weatherCity = TextView(activity).apply {
            textSize = 10f
            setTextColor(theme.inkDim)
            gravity = Gravity.END
        }
        weatherBox = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(16) }
            addView(weatherTemp)
            addView(weatherCity)
        }
        right.addView(weatherBox)

        fun iconButton(glyph: String, action: String): TextView = TextView(activity).apply {
            text = glyph
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(theme.inkDim)
            isFocusable = true
            isClickable = true
            isFocusableInTouchMode = false
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(6) }
            fun bg(focused: Boolean) = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (focused) theme.surfaceAlt else Color.TRANSPARENT)
                if (focused) setStroke(dp(2), theme.focusRing)
            }
            background = bg(false)
            setOnFocusChangeListener { v, f ->
                v.background = bg(f)
                (v as TextView).setTextColor(if (f) theme.ink else theme.inkDim)
            }
            setOnClickListener { onAction(action) }
        }

        val voiceBtn = iconButton("\uD83C\uDFA4", "voice")
        val searchBtn = iconButton("\uD83D\uDD0D", "search")
        val appsBtn = iconButton("\u25A6", "all_apps")
        wifiIcon = iconButton("\uD83D\uDCF6", "wifi")
        val settingsBtn = iconButton("\u2699", "hub_settings")
        iconButtons = listOf(voiceBtn, searchBtn, appsBtn, wifiIcon, settingsBtn)

        internetDot = TextView(activity).apply {
            text = "\u25CF"
            textSize = 10f
            setPadding(0, 0, dp(10), 0)
        }

        for (b in iconButtons) right.addView(b)
        right.addView(internetDot)

        val timeBox = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(6) }
        }
        clock = TextClock(activity).apply {
            format24Hour = "HH:mm"
            format12Hour = "HH:mm"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(theme.ink)
            gravity = Gravity.END
        }
        date = TextView(activity).apply {
            textSize = 10f
            setTextColor(theme.inkDim)
            gravity = Gravity.END
        }
        timeBox.addView(clock)
        timeBox.addView(date)
        right.addView(timeBox)

        view.addView(right)

        applyTheme(initialTheme, prefs)
    }

    fun applyTheme(newTheme: HubTheme, prefs: Prefs) {
        theme = newTheme
        appName.setTextColor(theme.ink)
        profileChip.setTextColor(theme.inkDim)
        profileChip.background = GradientDrawable().apply {
            cornerRadius = dp(20).toFloat()
            setColor(theme.surface)
        }
        clock.visibility = if (prefs.showClock) View.VISIBLE else View.GONE
        date.visibility = if (prefs.showDate) View.VISIBLE else View.GONE
        wifiIcon.visibility = if (prefs.showWifi) View.VISIBLE else View.GONE
        internetDot.visibility = if (prefs.showInternet) View.VISIBLE else View.GONE
        clock.setTextColor(theme.ink)
        date.setTextColor(theme.inkDim)
        weatherTemp.setTextColor(theme.ink)
        weatherCity.setTextColor(theme.inkDim)
        // Sanani statik matn sifatida shakllantiramiz (TextClock formatidan foydalanmaymiz — ixchamroq)
        date.text = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(java.util.Date())
        for (b in iconButtons) b.setTextColor(theme.inkDim)
        weatherEnabled = prefs.showWeather
        renderWeather()
    }

    fun updateNet(state: NetState?) {
        if (state == null) return
        wifiIcon.alpha = if (state.wifiConnected) 1f else 0.4f
        internetDot.setTextColor(
            if (state.internet) Color.parseColor("#3DBE6C") else Color.parseColor("#E24B4A")
        )
    }

    /** Ob-havo ma'lumoti. null bo'lsa (yoki sozlamalarda o'chirilgan bo'lsa) vidjet yashiriladi. */
    fun updateWeather(w: WeatherData?) {
        weather = w
        renderWeather()
    }

    private fun renderWeather() {
        val w = weather
        if (w == null || !weatherEnabled) {
            weatherBox.visibility = View.GONE
            return
        }
        weatherTemp.text = "${Weather.emoji(w.code, w.isDay)} ${w.tempC}\u00B0C"
        weatherCity.text = w.city
        weatherBox.visibility = View.VISIBLE
    }
}
