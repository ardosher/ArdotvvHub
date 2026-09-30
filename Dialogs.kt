package uz.ardo.tvhub

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.text.format.Formatter
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import java.util.Date

/** Barcha ichki oynalar (kontekst menyu, ilova haqida, qidiruv, grid, tanlov). Hammasi pult bilan boshqariladi. */
object Dialogs {

    private fun dp(ctx: Activity, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    // ------------------------------------------------------------------ umumiy qismlar

    /** Hozir ochiq dialoglar (ekran saqlovchi ochiq dialog ustidan chiqmasligi va profil almashtirganda yopish uchun). */
    private val openDialogs = ArrayList<Dialog>()

    fun hasOpenDialogs(): Boolean = openDialogs.any { it.isShowing }

    fun dismissAll() {
        val copy = ArrayList(openDialogs)
        openDialogs.clear()
        for (d in copy) {
            try {
                d.dismiss()
            } catch (e: Exception) {
                // oyna allaqachon yopilgan
            }
        }
    }

    fun create(activity: Activity, theme: HubTheme, fullscreen: Boolean): Dialog {
        val style = if (fullscreen) android.R.style.Theme_Material_NoActionBar_Fullscreen
        else android.R.style.Theme_Material_Dialog_NoActionBar
        val d = Dialog(activity, style)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setCanceledOnTouchOutside(true)
        openDialogs.add(d)
        d.setOnDismissListener { openDialogs.remove(d) }
        return d
    }

    private fun panel(activity: Activity, theme: HubTheme, radiusDp: Int = 24): GradientDrawable =
        GradientDrawable().apply {
            setColor(theme.surface)
            cornerRadius = dp(activity, radiusDp).toFloat()
        }

    fun title(activity: Activity, theme: HubTheme, text: String): TextView = TextView(activity).apply {
        this.text = text
        textSize = 30f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(theme.ink)
        setPadding(0, 0, 0, dp(activity, 14))
    }

    /** Fokus bo'lganda ramka chiqadigan qator. */
    fun row(activity: Activity, theme: HubTheme, text: String, value: String? = null, onClick: () -> Unit): View {
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            isClickable = true
            isFocusableInTouchMode = false
            setPadding(dp(activity, 20), dp(activity, 14), dp(activity, 20), dp(activity, 14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(activity, 4), 0, dp(activity, 4)) }
        }
        fun bg(focused: Boolean) = GradientDrawable().apply {
            cornerRadius = dp(activity, 14).toFloat()
            setColor(theme.surfaceAlt)
            if (focused) setStroke(dp(activity, 4), theme.focusRing)
        }
        row.background = bg(false)
        row.setOnFocusChangeListener { v, focused -> v.background = bg(focused) }
        row.addView(TextView(activity).apply {
            this.text = text
            textSize = 22f
            setTextColor(theme.ink)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (value != null) {
            row.addView(TextView(activity).apply {
                this.text = value
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(theme.focusRing)
                setPadding(dp(activity, 16), 0, 0, 0)
            })
        }
        row.setOnClickListener { onClick() }
        return row
    }

    private fun centeredShell(activity: Activity, theme: HubTheme, widthDp: Int): LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = panel(activity, theme)
            setPadding(dp(activity, 32), dp(activity, 28), dp(activity, 32), dp(activity, 28))
            layoutParams = FrameLayout.LayoutParams(dp(activity, widthDp), ViewGroup.LayoutParams.WRAP_CONTENT)
        }

    private fun show(dialog: Dialog, content: View, widthDp: Int?, activity: Activity) {
        dialog.setContentView(content)
        dialog.window?.let { w ->
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (widthDp != null) w.setLayout(dp(activity, widthDp), ViewGroup.LayoutParams.WRAP_CONTENT)
            else w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        dialog.show()
    }

    // ------------------------------------------------------------------ kontekst menyu

    fun showContextMenu(
        activity: Activity,
        theme: HubTheme,
        model: CardModel,
        onOpen: () -> Unit,
        onToggleFavorite: () -> Unit,
        onAbout: () -> Unit,
        onMarket: () -> Unit,
        onUninstall: () -> Unit
    ) {
        val dialog = create(activity, theme, false)
        val shell = centeredShell(activity, theme, 460)
        shell.addView(title(activity, theme, model.entry.name))
        var first: View? = null

        fun add(text: String, action: () -> Unit) {
            val r = row(activity, theme, text) {
                dialog.dismiss()
                action()
            }
            if (first == null) first = r
            shell.addView(r)
        }

        add(activity.getString(R.string.menu_open), onOpen)
        // Amal (settings, search...) kartalarida sevimli/o'chirish/haqida ma'nosiz
        if (!model.isAction) {
            add(
                activity.getString(if (model.favorite) R.string.menu_remove_fav else R.string.menu_add_fav),
                onToggleFavorite
            )
            add(activity.getString(R.string.menu_about), onAbout)
            add(activity.getString(R.string.menu_market), onMarket)
            if (model.installedPackage != null) {
                add(activity.getString(R.string.menu_uninstall), onUninstall)
            }
        }
        show(dialog, shell, 460, activity)
        first?.requestFocus()
    }

    // ------------------------------------------------------------------ ilova haqida

    fun showAppInfo(activity: Activity, theme: HubTheme, model: CardModel) {
        val dialog = create(activity, theme, false)
        val shell = centeredShell(activity, theme, 640)
        val entry = model.entry
        val pkg = model.installedPackage ?: entry.packages.firstOrNull()
        val info = if (model.installedPackage != null) InstalledApps.packageInfo(activity, model.installedPackage) else null
        val unknown = activity.getString(R.string.info_unknown)

        val head = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(activity, 16))
        }
        val iconView = ImageView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(dp(activity, 72), dp(activity, 72))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        var iconOk = false
        if (model.installedPackage != null) {
            val cached = IconCache.peek(activity, model.installedPackage)
            if (cached != null) {
                iconView.setImageDrawable(cached)
                iconOk = true
            } else {
                try {
                    iconView.setImageDrawable(activity.packageManager.getApplicationIcon(model.installedPackage))
                    iconOk = true
                } catch (e: Exception) {
                    iconOk = false
                }
            }
        }
        if (iconOk) {
            head.addView(iconView)
        } else {
            head.addView(TextView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(activity, 72), dp(activity, 72))
                gravity = Gravity.CENTER
                text = entry.name.take(1).uppercase()
                textSize = 34f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(try { Color.parseColor(entry.color) } catch (e: IllegalArgumentException) { Color.GRAY })
                }
            })
        }
        head.addView(TextView(activity).apply {
            text = entry.name
            textSize = 30f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(theme.ink)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(activity, 18), 0, 0, 0)
        })
        shell.addView(head)

        fun line(label: String, value: String) {
            shell.addView(TextView(activity).apply {
                text = "$label: $value"
                textSize = 21f
                setTextColor(theme.ink)
                setPadding(0, dp(activity, 5), 0, dp(activity, 5))
            })
        }

        line(activity.getString(R.string.info_package), pkg ?: unknown)
        line(
            activity.getString(R.string.info_status),
            activity.getString(if (model.isInstalled) R.string.info_installed else R.string.info_not_installed)
        )
        if (info != null) {
            line(activity.getString(R.string.info_version_name), info.versionName ?: unknown)
            line(activity.getString(R.string.info_version_code), InstalledApps.versionCode(info).toString())
            line(
                activity.getString(R.string.info_install_date),
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(info.firstInstallTime))
            )
        }
        if (!entry.description.isNullOrBlank()) {
            line(activity.getString(R.string.info_description), entry.description)
        }

        val close = row(activity, theme, activity.getString(R.string.close)) { dialog.dismiss() }
        (close.layoutParams as LinearLayout.LayoutParams).topMargin = dp(activity, 16)
        shell.addView(close)
        show(dialog, shell, 640, activity)
        close.requestFocus()
    }

    // ------------------------------------------------------------------ xabar

    fun showMessage(activity: Activity, theme: HubTheme, titleText: String, message: String) {
        val dialog = create(activity, theme, false)
        val shell = centeredShell(activity, theme, 720)
        shell.addView(title(activity, theme, titleText))
        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 260)
            )
            // Uzun matn pult bilan aylantirilishi uchun ScrollView fokus oladi
            isFocusable = true
            isFocusableInTouchMode = false
            fun bg(f: Boolean) = GradientDrawable().apply {
                cornerRadius = dp(activity, 10).toFloat()
                if (f) setStroke(dp(activity, 2), theme.inkDim)
            }
            background = bg(false)
            setOnFocusChangeListener { v, f -> v.background = bg(f) }
            addView(TextView(activity).apply {
                text = message
                textSize = 21f
                setTextColor(theme.ink)
                setLineSpacing(0f, 1.15f)
                setPadding(dp(activity, 8), dp(activity, 6), dp(activity, 8), dp(activity, 6))
            })
        }
        shell.addView(scroll)
        val close = row(activity, theme, activity.getString(R.string.close)) { dialog.dismiss() }
        shell.addView(close)
        show(dialog, shell, 720, activity)
        if (message.length > 260) scroll.requestFocus() else close.requestFocus()
    }

    // ------------------------------------------------------------------ tanlov

    fun showChoice(
        activity: Activity,
        theme: HubTheme,
        titleText: String,
        options: List<String>,
        selected: Int,
        onSelect: (Int) -> Unit
    ) {
        val dialog = create(activity, theme, false)
        val shell = centeredShell(activity, theme, 520)
        shell.addView(title(activity, theme, titleText))
        var focusTarget: View? = null
        options.forEachIndexed { i, name ->
            val marker = if (i == selected) "\u25CF " else "\u25CB "
            val r = row(activity, theme, marker + name) {
                dialog.dismiss()
                onSelect(i)
            }
            if (i == selected || focusTarget == null) focusTarget = r
            shell.addView(r)
        }
        show(dialog, shell, 520, activity)
        focusTarget?.requestFocus()
    }

    // ------------------------------------------------------------------ barcha ilovalar (grid)

    fun showAllApps(
        activity: Activity,
        theme: HubTheme,
        items: List<CardModel>,
        animations: Boolean,
        sizeScale: Float,
        onClick: (CardModel) -> Unit,
        onMenu: (CardModel) -> Unit
    ) {
        val dialog = create(activity, theme, true)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = theme.backgroundDrawable()
            setPadding(dp(activity, 48), dp(activity, 28), dp(activity, 48), dp(activity, 20))
        }
        root.addView(title(activity, theme, activity.getString(R.string.all_apps_title, items.size)))
        val adapter = AppAdapter(items, theme.focusRing, animations, sizeScale * 0.8f, onClick, onMenu, theme.dimAlpha)
        val grid = RecyclerView(activity).apply {
            layoutManager = GridLayoutManager(activity, 5)
            this.adapter = adapter
            clipToPadding = false
            clipChildren = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(grid)
        show(dialog, root, null, activity)
        grid.post { grid.getChildAt(0)?.requestFocus() }
    }

    // ------------------------------------------------------------------ qidiruv

    private val KEY_ROWS = listOf("1234567890", "QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")

    fun showSearch(
        activity: Activity,
        theme: HubTheme,
        allItems: () -> List<CardModel>,
        animations: Boolean,
        sizeScale: Float,
        onClick: (CardModel) -> Unit,
        onMenu: (CardModel) -> Unit,
        initialQuery: String = ""
    ) {
        val dialog = create(activity, theme, true)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            background = theme.backgroundDrawable()
            setPadding(dp(activity, 40), dp(activity, 28), dp(activity, 40), dp(activity, 20))
        }

        val left = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(dp(activity, 560), ViewGroup.LayoutParams.MATCH_PARENT)
        }
        left.addView(title(activity, theme, activity.getString(R.string.search_title)))

        val queryView = TextView(activity).apply {
            textSize = 30f
            setTextColor(theme.ink)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(activity, 18), dp(activity, 12), dp(activity, 18), dp(activity, 12))
            background = GradientDrawable().apply {
                cornerRadius = dp(activity, 14).toFloat()
                setColor(theme.surface)
                setStroke(dp(activity, 2), theme.inkDim)
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(activity, 14) }
        }
        left.addView(queryView)

        val resultsAdapter = AppAdapter(emptyList(), theme.focusRing, animations, sizeScale * 0.8f, onClick, onMenu, theme.dimAlpha)
        val emptyView = TextView(activity).apply {
            text = activity.getString(R.string.search_no_results)
            textSize = 24f
            setTextColor(theme.inkDim)
            gravity = Gravity.CENTER
            visibility = View.GONE
        }

        val query = StringBuilder(initialQuery.trim().lowercase())
        fun applyQuery() {
            val q = query.toString().trim().lowercase()
            queryView.text = if (query.isEmpty()) activity.getString(R.string.search_hint) else query.toString()
            queryView.setTextColor(if (query.isEmpty()) theme.inkDim else theme.ink)
            val source = allItems()
            val found = if (q.isEmpty()) source else source.filter { it.entry.name.lowercase().contains(q) }
            resultsAdapter.setItems(found)
            emptyView.visibility = if (found.isEmpty()) View.VISIBLE else View.GONE
        }

        fun keyView(text: String, weight: Float, action: () -> Unit): TextView = TextView(activity).apply {
            this.text = text
            textSize = 24f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(theme.ink)
            isFocusable = true
            isClickable = true
            isFocusableInTouchMode = false
            layoutParams = LinearLayout.LayoutParams(0, dp(activity, 56), weight).apply {
                setMargins(dp(activity, 3), dp(activity, 3), dp(activity, 3), dp(activity, 3))
            }
            fun bg(f: Boolean) = GradientDrawable().apply {
                cornerRadius = dp(activity, 10).toFloat()
                setColor(theme.surfaceAlt)
                if (f) setStroke(dp(activity, 4), theme.focusRing)
            }
            background = bg(false)
            setOnFocusChangeListener { v, f -> v.background = bg(f) }
            setOnClickListener { action() }
        }

        var firstKey: View? = null
        for (rowChars in KEY_ROWS) {
            val line = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
            for (ch in rowChars) {
                val k = keyView(ch.toString(), 1f) {
                    query.append(ch.lowercaseChar())
                    applyQuery()
                }
                if (firstKey == null) firstKey = k
                line.addView(k)
            }
            left.addView(line)
        }
        val bottom = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        bottom.addView(keyView(activity.getString(R.string.key_space), 3f) {
            if (query.isNotEmpty() && query.last() != ' ') { query.append(' '); applyQuery() }
        })
        bottom.addView(keyView(activity.getString(R.string.key_backspace), 1.5f) {
            if (query.isNotEmpty()) { query.deleteCharAt(query.length - 1); applyQuery() }
        })
        bottom.addView(keyView(activity.getString(R.string.key_clear), 2f) {
            query.setLength(0); applyQuery()
        })
        left.addView(bottom)
        root.addView(left)

        val right = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                leftMargin = dp(activity, 24)
            }
        }
        val grid = RecyclerView(activity).apply {
            layoutManager = GridLayoutManager(activity, 3)
            adapter = resultsAdapter
            clipToPadding = false
            clipChildren = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        right.addView(grid)
        right.addView(emptyView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        root.addView(right)

        // Haqiqiy klaviatura / pult harflari ham ishlaydi. BACK esa dialogni yopadi.
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when {
                keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z ||
                    keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                    val ch = event.unicodeChar
                    if (ch > 0) { query.append(ch.toChar().lowercaseChar()); applyQuery(); true } else false
                }
                keyCode == KeyEvent.KEYCODE_DEL -> {
                    if (query.isNotEmpty()) { query.deleteCharAt(query.length - 1); applyQuery() }
                    true
                }
                keyCode == KeyEvent.KEYCODE_SPACE -> {
                    if (query.isNotEmpty() && query.last() != ' ') { query.append(' '); applyQuery() }
                    true
                }
                else -> false
            }
        }

        applyQuery()
        show(dialog, root, null, activity)
        firstKey?.requestFocus()
    }

    // ------------------------------------------------------------------ sozlamalar

    fun showSettings(
        activity: Activity,
        theme: HubTheme,
        prefs: Prefs,
        profile: Profile,
        onChanged: () -> Unit,
        onManageFavorites: () -> Unit,
        onProfiles: () -> Unit
    ) {
        val dialog = create(activity, theme, true)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = theme.backgroundDrawable()
            setPadding(dp(activity, 60), dp(activity, 32), dp(activity, 60), dp(activity, 32))
        }
        root.addView(title(activity, theme, activity.getString(R.string.settings_title)))

        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll)

        fun onOff(b: Boolean) = activity.getString(if (b) R.string.on else R.string.off)
        val cardSizeNames = listOf(
            activity.getString(R.string.card_size_small),
            activity.getString(R.string.card_size_medium),
            activity.getString(R.string.card_size_large)
        )
        val themeNames = listOf(
            activity.getString(R.string.theme_dark),
            activity.getString(R.string.theme_light),
            activity.getString(R.string.theme_auto)
        )
        val bgNames = listOf(
            activity.getString(R.string.bg_default),
            activity.getString(R.string.bg_dark),
            activity.getString(R.string.bg_blue),
            activity.getString(R.string.bg_purple),
            activity.getString(R.string.bg_green)
        )

        val ssValues = listOf(0, 1, 2, 5, 10, 15)
        val ssNames = ssValues.map {
            if (it == 0) activity.getString(R.string.off) else activity.getString(R.string.screensaver_minutes, it)
        }
        val cityNames = Weather.CITIES.map { it.name }
        val voiceNames = MainActivity.VOICE_LANGS.map { activity.getString(it.second) }

        lateinit var rebuild: () -> Unit
        rebuild = {
            list.removeAllViews()
            var firstRow: View? = null
            fun add(v: View) { if (firstRow == null) firstRow = v; list.addView(v) }

            add(row(activity, theme, activity.getString(R.string.settings_profiles), "${profile.avatar} ${profile.name}") {
                dialog.dismiss()
                onProfiles()
            })
            add(row(activity, theme, activity.getString(R.string.settings_favorites)) {
                dialog.dismiss()
                onManageFavorites()
            })
            add(row(activity, theme, activity.getString(R.string.settings_card_size), cardSizeNames[prefs.cardSize]) {
                showChoice(activity, theme, activity.getString(R.string.settings_card_size), cardSizeNames, prefs.cardSize) {
                    prefs.cardSize = it; onChanged(); rebuild()
                }
            })
            add(row(activity, theme, activity.getString(R.string.settings_theme), themeNames[prefs.themeMode]) {
                showChoice(activity, theme, activity.getString(R.string.settings_theme), themeNames, prefs.themeMode) {
                    prefs.themeMode = it; onChanged(); rebuild()
                }
            })
            add(row(activity, theme, activity.getString(R.string.settings_background), bgNames[prefs.background]) {
                showChoice(activity, theme, activity.getString(R.string.settings_background), bgNames, prefs.background) {
                    prefs.background = it; onChanged(); rebuild()
                }
            })
            add(row(activity, theme, activity.getString(R.string.settings_animations), onOff(prefs.animations)) {
                prefs.animations = !prefs.animations; onChanged(); rebuild()
            })
            add(row(activity, theme, activity.getString(R.string.settings_screensaver), ssNames[ssValues.indexOf(prefs.screensaverMinutes).coerceAtLeast(0)]) {
                showChoice(activity, theme, activity.getString(R.string.settings_screensaver), ssNames, ssValues.indexOf(prefs.screensaverMinutes)) {
                    prefs.screensaverMinutes = ssValues[it]; onChanged(); rebuild()
                }
            })
            add(row(activity, theme, activity.getString(R.string.settings_show_banners), onOff(prefs.showBanners)) {
                prefs.showBanners = !prefs.showBanners; onChanged(); rebuild()
            })
            add(row(activity, theme, activity.getString(R.string.settings_show_volume), onOff(prefs.showVolume)) {
                prefs.showVolume = !prefs.showVolume; onChanged(); rebuild()
            })
            add(row(activity, theme, activity.getString(R.string.settings_show_weather), onOff(prefs.showWeather)) {
                prefs.showWeather = !prefs.showWeather; onChanged(); rebuild()
            })
            add(row(activity, theme, activity.getString(R.string.settings_weather_city), cityNames[prefs.weatherCity.coerceIn(0, cityNames.size - 1)]) {
                showChoice(activity, theme, activity.getString(R.string.settings_weather_city), cityNames, prefs.weatherCity.coerceIn(0, cityNames.size - 1)) {
                    prefs.weatherCity = it; onChanged(); rebuild()
                }
            })
            add(row(activity, theme, activity.getString(R.string.settings_voice_lang), voiceNames[prefs.voiceLang.coerceIn(0, voiceNames.size - 1)]) {
                showChoice(activity, theme, activity.getString(R.string.settings_voice_lang), voiceNames, prefs.voiceLang.coerceIn(0, voiceNames.size - 1)) {
                    prefs.voiceLang = it; onChanged(); rebuild()
                }
            })
            add(row(activity, theme, activity.getString(R.string.settings_show_clock), onOff(prefs.showClock)) {
                prefs.showClock = !prefs.showClock; onChanged(); rebuild()
            })
            add(row(activity, theme, activity.getString(R.string.settings_show_date), onOff(prefs.showDate)) {
                prefs.showDate = !prefs.showDate; onChanged(); rebuild()
            })
            add(row(activity, theme, activity.getString(R.string.settings_show_wifi), onOff(prefs.showWifi)) {
                prefs.showWifi = !prefs.showWifi; onChanged(); rebuild()
            })
            add(row(activity, theme, activity.getString(R.string.settings_show_internet), onOff(prefs.showInternet)) {
                prefs.showInternet = !prefs.showInternet; onChanged(); rebuild()
            })
            add(row(activity, theme, activity.getString(R.string.settings_auto_refresh), onOff(prefs.autoRefresh)) {
                prefs.autoRefresh = !prefs.autoRefresh; onChanged(); rebuild()
            })
            add(row(activity, theme, activity.getString(R.string.settings_show_recommended), onOff(prefs.showRecommended)) {
                prefs.showRecommended = !prefs.showRecommended; onChanged(); rebuild()
            })
            add(row(activity, theme, activity.getString(R.string.settings_show_hdmi), onOff(prefs.showHdmi)) {
                prefs.showHdmi = !prefs.showHdmi; onChanged(); rebuild()
            })
            add(row(activity, theme, activity.getString(R.string.settings_show_quick), onOff(prefs.showQuick)) {
                prefs.showQuick = !prefs.showQuick; onChanged(); rebuild()
            })
            add(row(activity, theme, activity.getString(R.string.settings_confirm_uninstall), onOff(prefs.confirmUninstall)) {
                prefs.confirmUninstall = !prefs.confirmUninstall; onChanged(); rebuild()
            })
            add(title(activity, theme, activity.getString(R.string.settings_section_data)).apply {
                textSize = 16f
                setPadding(dp(activity, 20), dp(activity, 14), 0, dp(activity, 2))
            })
            add(row(activity, theme, activity.getString(R.string.settings_clear_recent)) {
                showChoice(
                    activity, theme, activity.getString(R.string.confirm_clear_recent_title),
                    listOf(activity.getString(R.string.confirm_yes), activity.getString(R.string.confirm_no)), 1
                ) { i ->
                    if (i == 0) {
                        prefs.clearRecents()
                        android.widget.Toast.makeText(activity, R.string.toast_recent_cleared, android.widget.Toast.LENGTH_SHORT).show()
                        onChanged()
                    }
                }
            })
            add(row(activity, theme, activity.getString(R.string.settings_clear_cache)) {
                IconCache.invalidate(null)
                android.widget.Toast.makeText(activity, R.string.toast_cache_cleared, android.widget.Toast.LENGTH_SHORT).show()
                onChanged()
            })
            add(row(activity, theme, activity.getString(R.string.settings_reset)) {
                showChoice(
                    activity, theme, activity.getString(R.string.confirm_reset_title),
                    listOf(activity.getString(R.string.confirm_yes), activity.getString(R.string.confirm_no)), 1
                ) { i ->
                    if (i == 0) {
                        prefs.resetDisplaySettings()
                        android.widget.Toast.makeText(activity, R.string.toast_reset_done, android.widget.Toast.LENGTH_SHORT).show()
                        onChanged()
                        dialog.dismiss()
                    }
                }
            })
            add(row(activity, theme, activity.getString(R.string.settings_launcher_info)) {
                showMessage(
                    activity, theme,
                    activity.getString(R.string.settings_launcher_info),
                    activity.getString(R.string.launcher_info_text)
                )
            })
            val versionName = DeviceInfo.appVersion(activity).first
            add(row(
                activity, theme,
                activity.getString(R.string.settings_about),
                "${activity.getString(R.string.app_name)} ${activity.getString(R.string.settings_version)} $versionName"
            ) {
                showAbout(activity, theme)
            })
            add(row(activity, theme, activity.getString(R.string.close)) { dialog.dismiss() })
            firstRow?.requestFocus()
        }
        rebuild()
        show(dialog, root, null, activity)
    }

    // ------------------------------------------------------------------ sevimlilarni boshqarish

    fun showManageFavorites(
        activity: Activity,
        theme: HubTheme,
        favoriteModels: () -> List<CardModel>,
        animations: Boolean,
        sizeScale: Float,
        onRemove: (CardModel) -> Unit
    ) {
        val dialog = create(activity, theme, true)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = theme.backgroundDrawable()
            setPadding(dp(activity, 48), dp(activity, 28), dp(activity, 48), dp(activity, 20))
        }
        root.addView(title(activity, theme, activity.getString(R.string.settings_favorites)))

        val holder = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        lateinit var refresh: () -> Unit

        fun renderList(): View {
            val items = favoriteModels()
            if (items.isEmpty()) {
                return TextView(activity).apply {
                    text = activity.getString(R.string.favorites_empty)
                    textSize = 22f
                    setTextColor(theme.inkDim)
                    setPadding(0, dp(activity, 30), 0, 0)
                }
            }
            val adapter = AppAdapter(items, theme.focusRing, animations, sizeScale * 0.8f,
                onClick = { onRemove(it); refresh() },
                onMenu = { onRemove(it); refresh() },
                dimAlpha = theme.dimAlpha
            )
            return RecyclerView(activity).apply {
                layoutManager = GridLayoutManager(activity, 5)
                this.adapter = adapter
                overScrollMode = View.OVER_SCROLL_NEVER
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }
        }

        refresh = {
            holder.removeAllViews()
            val v = renderList()
            holder.addView(v)
            if (v is RecyclerView) v.post { v.getChildAt(0)?.requestFocus() }
        }
        refresh()
        root.addView(holder)
        root.addView(TextView(activity).apply {
            text = activity.getString(R.string.close)
            textSize = 20f
            setTextColor(theme.inkDim)
            setPadding(0, dp(activity, 12), 0, 0)
        })
        show(dialog, root, null, activity)
    }

    // ------------------------------------------------------------------ profillar

    fun showProfiles(
        activity: Activity,
        theme: HubTheme,
        manager: ProfileManager,
        onSwitch: (String) -> Unit,
        onChanged: () -> Unit
    ) {
        val dialog = create(activity, theme, false)
        val shell = centeredShell(activity, theme, 560)
        shell.addView(title(activity, theme, activity.getString(R.string.profiles_title)))
        var focusTarget: View? = null
        val activeId = manager.activeId()
        val all = manager.profiles()

        for (p in all) {
            val isActive = p.id == activeId
            val r = row(
                activity, theme, "${p.avatar}  ${p.name}",
                if (isActive) activity.getString(R.string.profile_active) else null
            ) {
                dialog.dismiss()
                if (!isActive) onSwitch(p.id)
            }
            if (isActive || focusTarget == null) focusTarget = r
            shell.addView(r)
        }

        if (manager.canAdd()) {
            shell.addView(row(activity, theme, activity.getString(R.string.profile_add)) {
                dialog.dismiss()
                val names = manager.availableNames()
                showChoice(activity, theme, activity.getString(R.string.profile_pick_name), names, -1) { i ->
                    val created = manager.add(names[i])
                    if (created != null) onSwitch(created.id)
                }
            })
        }

        shell.addView(row(activity, theme, activity.getString(R.string.profile_rename)) {
            dialog.dismiss()
            val names = manager.availableNames(activeId)
            showChoice(activity, theme, activity.getString(R.string.profile_pick_name), names, -1) { i ->
                manager.rename(activeId, names[i])
                onChanged()
            }
        })

        val deletable = all.filter { it.id != ProfileManager.DEFAULT_ID }
        if (deletable.isNotEmpty()) {
            shell.addView(row(activity, theme, activity.getString(R.string.profile_delete)) {
                dialog.dismiss()
                showChoice(
                    activity, theme, activity.getString(R.string.profile_delete),
                    deletable.map { "${it.avatar}  ${it.name}" }, -1
                ) { i ->
                    val target = deletable[i]
                    showChoice(
                        activity, theme, activity.getString(R.string.confirm_delete_profile, target.name),
                        listOf(activity.getString(R.string.confirm_yes), activity.getString(R.string.confirm_no)), 1
                    ) { answer ->
                        if (answer == 0 && manager.delete(target.id)) onChanged()
                    }
                }
            })
        }

        shell.addView(row(activity, theme, activity.getString(R.string.close)) { dialog.dismiss() })
        show(dialog, shell, 560, activity)
        focusTarget?.requestFocus()
    }

    // ------------------------------------------------------------------ ilova va qurilma haqida

    private fun usageBar(activity: Activity, theme: HubTheme, fraction: Float): View {
        val f = fraction.coerceIn(0f, 1f)
        val track = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                cornerRadius = dp(activity, 5).toFloat()
                setColor(theme.surfaceAlt)
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 10)
            ).apply { setMargins(0, dp(activity, 4), 0, dp(activity, 10)) }
        }
        track.addView(View(activity).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(activity, 5).toFloat()
                setColor(Color.parseColor(if (f > 0.9f) "#E24B4A" else "#3DBE6C"))
            }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, f)
        })
        track.addView(View(activity).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - f)
        })
        return track
    }

    fun showAbout(activity: Activity, theme: HubTheme) {
        val dialog = create(activity, theme, false)
        val shell = centeredShell(activity, theme, 720)
        shell.addView(title(activity, theme, activity.getString(R.string.about_title)))

        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 290))
        }
        val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)

        fun line(label: String, value: String) {
            body.addView(TextView(activity).apply {
                text = "$label: $value"
                textSize = 20f
                setTextColor(theme.ink)
                setPadding(0, dp(activity, 3), 0, dp(activity, 3))
            })
        }

        val version = DeviceInfo.appVersion(activity)
        line(activity.getString(R.string.app_name), "${activity.getString(R.string.settings_version)} ${version.first} (${version.second})")
        line(activity.getString(R.string.info_package), activity.packageName)
        line(activity.getString(R.string.about_device), DeviceInfo.deviceName())
        line("Android", DeviceInfo.androidVersion())

        val unknown = activity.getString(R.string.info_unknown)
        val storage = DeviceInfo.storage()
        if (storage != null) {
            line(
                activity.getString(R.string.about_storage),
                activity.getString(
                    R.string.about_free_of,
                    Formatter.formatFileSize(activity, storage.free),
                    Formatter.formatFileSize(activity, storage.total)
                )
            )
            body.addView(usageBar(activity, theme, storage.usedFraction))
        } else {
            line(activity.getString(R.string.about_storage), unknown)
        }
        val memory = DeviceInfo.memory(activity)
        if (memory != null) {
            line(
                activity.getString(R.string.about_ram),
                activity.getString(
                    R.string.about_free_of,
                    Formatter.formatFileSize(activity, memory.free),
                    Formatter.formatFileSize(activity, memory.total)
                )
            )
            body.addView(usageBar(activity, theme, memory.usedFraction))
        } else {
            line(activity.getString(R.string.about_ram), unknown)
        }
        shell.addView(scroll)

        val errorsRow = row(
            activity, theme,
            activity.getString(R.string.about_errors, ErrorLog.count(activity))
        ) { showErrorLog(activity, theme) }
        val close = row(activity, theme, activity.getString(R.string.close)) { dialog.dismiss() }
        shell.addView(errorsRow)
        shell.addView(close)
        show(dialog, shell, 720, activity)
        close.requestFocus()
    }

    // ------------------------------------------------------------------ xatoliklar jurnali

    fun showErrorLog(activity: Activity, theme: HubTheme) {
        val dialog = create(activity, theme, true)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = theme.backgroundDrawable()
            setPadding(dp(activity, 60), dp(activity, 32), dp(activity, 60), dp(activity, 32))
        }
        root.addView(title(activity, theme, activity.getString(R.string.errors_title)))
        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll)

        val fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        lateinit var rebuild: () -> Unit
        rebuild = {
            list.removeAllViews()
            val entries = ErrorLog.entries(activity)
            var first: View? = null
            fun add(v: View) { if (first == null) first = v; list.addView(v) }

            add(row(activity, theme, activity.getString(R.string.errors_clear)) {
                ErrorLog.clear(activity)
                android.widget.Toast.makeText(activity, R.string.toast_errors_cleared, android.widget.Toast.LENGTH_SHORT).show()
                rebuild()
            })
            if (entries.isEmpty()) {
                list.addView(TextView(activity).apply {
                    text = activity.getString(R.string.errors_empty)
                    textSize = 22f
                    setTextColor(theme.inkDim)
                    setPadding(dp(activity, 20), dp(activity, 20), 0, dp(activity, 20))
                })
            } else {
                for (e in entries) {
                    val summary = "${fmt.format(Date(e.time))}  [${e.tag}]  ${e.message}"
                    add(row(activity, theme, if (summary.length > 170) summary.take(170) + "\u2026" else summary) {
                        val full = if (e.detail.isNullOrBlank()) e.message else e.message + "\n\n" + e.detail
                        showMessage(activity, theme, "[${e.tag}] ${fmt.format(Date(e.time))}", full)
                    })
                }
            }
            add(row(activity, theme, activity.getString(R.string.close)) { dialog.dismiss() })
            first?.requestFocus()
        }
        rebuild()
        show(dialog, root, null, activity)
    }
}
