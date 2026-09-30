package uz.ardo.tvhub

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * ArdoTv Hub bosh ekrani. TV pulti bilan boshqariladi (touchscreen shart emas).
 *
 * Tartib: sarlavha -> reklama bannerlari -> Tavsiya etilganlar -> Sevimli ilovalar -> Tomosha qilishni
 * davom ettiring -> Oxirgi ochilganlar -> apps.json kategoriyalari -> Kirishlar (HDMI) -> Tezkor tugmalar.
 * Bo'sh bo'lim avtomatik yashiriladi.
 *
 * Qo'shimcha: ekran saqlovchi (harakatsizlikdan keyin), ovoz indikatori, ovozli qidiruv,
 * profillar (har birining o'z sevimlilari va sozlamalari), sarlavhada ob-havo.
 */
class MainActivity : Activity(), HubActions {

    private lateinit var profiles: ProfileManager
    private lateinit var prefs: Prefs
    private var theme: HubTheme = HubTheme(true, 0)

    private lateinit var container: FrameLayout
    private lateinit var root: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var header: HeaderView
    private lateinit var screensaver: ScreensaverView
    private lateinit var volume: VolumeIndicator
    private lateinit var bannerBox: LinearLayout
    private lateinit var bannerAdapter: BannerAdapter

    private var loadResult: LoadResult = LoadResult(emptyList(), emptyList(), null)
    /** apps.json'dagi barcha statik yozuvlar, key -> AppEntry. */
    private var staticByKey: Map<String, AppEntry> = emptyMap()

    private val sections = LinkedHashMap<String, Section>()
    private var installedCache: List<InstalledApp> = emptyList()

    private var netMonitor: NetworkMonitor? = null
    private var isHome = false

    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable { onIdle() }
    private val volumeHandler = Handler(Looper.getMainLooper())
    private val volumeRunnable = Runnable { showVolume() }
    private val weatherHandler = Handler(Looper.getMainLooper())
    private val weatherTick = object : Runnable {
        override fun run() {
            updateWeather()
            weatherHandler.postDelayed(this, WEATHER_TICK_MS)
        }
    }

    private class Section(val box: LinearLayout, val adapter: AppAdapter, val label: TextView, val title: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profiles = ProfileManager(this)
        prefs = Prefs(this, profiles.activeId())
        theme = HubTheme.resolve(prefs)
        isHome = intent?.hasCategory(Intent.CATEGORY_HOME) == true

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        loadResult = try {
            AppRepository.loadSafe(this)
        } catch (e: Exception) {
            ErrorLog.log(this, "apps.json", e.message ?: "error", e)
            LoadResult(emptyList(), emptyList(), e.message ?: "error")
        }
        staticByKey = (loadResult.categories.flatMap { it.apps } + loadResult.quick).associateBy { it.key }

        buildUi()
        applyTheme()

        netMonitor = NetworkMonitor(this) { state ->
            runOnUiThread {
                header.updateNet(state)
                if (state.internet) updateWeather() // internet qaytganda eskirgan ob-havo yangilanadi
            }
        }
    }

    // ------------------------------------------------------------------ UI qurish

    private fun px(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            setPadding(px(48), px(24), px(48), px(40))
        }

        header = HeaderView(this, theme, prefs, profiles.active()) { onOpenAction(it) }
        root.addView(header.view)

        if (loadResult.error != null) {
            root.addView(TextView(this).apply {
                text = getString(R.string.apps_json_error, loadResult.error)
                textSize = 20f
                setTextColor(theme.ink)
                setPadding(0, px(30), 0, 0)
            })
        }

        addBannerRow()

        addSection("recommended", getString(R.string.section_recommended), CategoryIcons.RECOMMENDED)
        addSection("favorites", getString(R.string.section_favorites), CategoryIcons.FAVORITES)
        addSection("continue", getString(R.string.section_continue), CategoryIcons.CONTINUE)
        addSection("recent", getString(R.string.section_recent), CategoryIcons.RECENT)
        for (cat in loadResult.categories) {
            addSection("cat:${cat.title}", cat.title, CategoryIcons.forCategory(cat), cat.apps)
        }
        addSection("hdmi", getString(R.string.section_hdmi), CategoryIcons.HDMI)
        if (loadResult.quick.isNotEmpty()) {
            addSection("quick", getString(R.string.section_quick), CategoryIcons.QUICK, loadResult.quick)
        }

        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipChildren = false
            isVerticalScrollBarEnabled = false
            addView(root)
        }

        screensaver = ScreensaverView(this)
        volume = VolumeIndicator(this)

        container = FrameLayout(this).apply {
            addView(scroll, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ))
            addView(screensaver, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ))
            addView(volume.view, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply { bottomMargin = px(56) })
        }
        setContentView(container)
    }

    private fun cardSizeScale(): Float = when (prefs.cardSize) {
        0 -> 0.82f
        2 -> 1.18f
        else -> 1f
    }

    private fun addBannerRow() {
        bannerAdapter = BannerAdapter(
            items = emptyList(),
            focusRing = theme.focusRing,
            animations = prefs.animations,
            dimAlpha = theme.dimAlpha,
            sizeScale = cardSizeScale(),
            onClick = { onBannerClick(it) }
        )
        val row = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = bannerAdapter
            clipToPadding = false
            clipChildren = false
            setPadding(0, 0, px(40), 0)
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        bannerBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            visibility = View.GONE
            addView(row)
        }
        root.addView(bannerBox)
    }

    private fun addSection(id: String, title: String, icon: String, staticEntries: List<AppEntry> = emptyList()) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val label = TextView(this).apply {
            text = "$icon  $title"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(theme.ink)
            setPadding(px(10), px(26), 0, px(4))
        }
        box.addView(label)

        val adapter = AppAdapter(
            items = toModels(staticEntries),
            focusRing = theme.focusRing,
            animations = prefs.animations,
            sizeScale = cardSizeScale(),
            onClick = { onCardClick(it) },
            onMenu = { onCardMenu(it) },
            dimAlpha = theme.dimAlpha
        )
        val row = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            this.adapter = adapter
            clipToPadding = false
            clipChildren = false
            setPadding(0, 0, px(40), 0)
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        box.addView(row)
        box.visibility = View.GONE
        root.addView(box)
        sections[id] = Section(box, adapter, label, title)
    }

    private fun toModels(entries: List<AppEntry>): List<CardModel> {
        val favs = prefs.favorites().toHashSet()
        val now = System.currentTimeMillis()
        return entries.filter { !it.hidden }.map { e ->
            val installed = if (e.action != null) null else AppLauncher.installedPackage(this, e)
            CardModel(e, installed, favs.contains(e.key), e.isNew(now))
        }
    }

    /** Faqat bitta bo'limni yangilaydi (kerak bo'lganda), qaysi ma'lumot o'zgarganiga qarab. */
    private fun setSection(id: String, entries: List<AppEntry>) {
        val s = sections[id] ?: return
        val models = toModels(entries)
        s.adapter.setItems(models)
        s.box.visibility = if (models.isEmpty()) View.GONE else View.VISIBLE
    }

    // ------------------------------------------------------------------ yangilash

    override fun onResume() {
        super.onResume()
        applyTheme()
        netMonitor?.start()
        refresh(rescanPackages = true)
        weatherHandler.removeCallbacks(weatherTick)
        weatherTick.run()
        resetIdle()
    }

    override fun onPause() {
        super.onPause()
        netMonitor?.stop()
        weatherHandler.removeCallbacks(weatherTick)
        idleHandler.removeCallbacks(idleRunnable)
        volumeHandler.removeCallbacks(volumeRunnable)
        volume.cancel()
        screensaver.hide(immediate = true)
    }

    override fun onDestroy() {
        idleHandler.removeCallbacksAndMessages(null)
        weatherHandler.removeCallbacksAndMessages(null)
        volumeHandler.removeCallbacksAndMessages(null)
        Dialogs.dismissAll()
        super.onDestroy()
    }

    private fun applyTheme() {
        theme = HubTheme.resolve(prefs)
        scroll.setBackgroundColor(0)
        root.setBackgroundDrawable(theme.backgroundDrawable())
        header.applyTheme(theme, prefs)
        for (s in sections.values) s.label.setTextColor(theme.ink)
        screensaver.animated = prefs.animations
    }

    private fun refresh(rescanPackages: Boolean) {
        if (rescanPackages) {
            installedCache = try {
                InstalledApps.scan(this)
            } catch (e: Exception) {
                Log.w("ArdoTvHub", "scan failed", e)
                ErrorLog.log(this, "scan", "Ilovalarni skanerlash xatosi: ${e.message}", e)
                installedCache
            }
        }
        val allEntries = installedCache.map {
            AppEntry(name = it.label, packages = listOf(it.pkg), color = colorFor(it.pkg), key = "p:${it.pkg}")
        }

        val hdmi = hdmiEntries()
        val recommended = (loadResult.categories.flatMap { it.apps } + loadResult.quick).filter { it.featured }

        val favKeys = prefs.favorites()
        val lookup = (staticByKey.values + allEntries + hdmi).associateBy { it.key }
        val favEntries = favKeys.mapNotNull { lookup[it] }
        prefs.setFavorites(favEntries.map { it.key }) // o'chirilgan ilovalarni ro'yxatdan tozalaydi

        val recentKeys = prefs.recents()
        val recentEntries = recentKeys.mapNotNull { lookup[it] }
        prefs.setRecents(recentEntries.map { it.key })

        val continueEntries = try {
            ContinueWatchingRepository.load(this).map { it.toEntry() }
        } catch (e: Exception) {
            ErrorLog.log(this, "continue", "Tomosha tarixi xatosi: ${e.message}", e)
            emptyList()
        }

        val banners = if (prefs.showBanners) activeBanners() else emptyList()
        bannerAdapter.setItems(banners)
        bannerBox.visibility = if (banners.isEmpty()) View.GONE else View.VISIBLE

        setSection("recommended", if (prefs.showRecommended) recommended else emptyList())
        setSection("favorites", favEntries)
        setSection("continue", continueEntries)
        setSection("recent", recentEntries)
        for (cat in loadResult.categories) {
            setSection("cat:${cat.title}", cat.apps)
        }
        setSection("hdmi", if (prefs.showHdmi) hdmi else emptyList())
        if (loadResult.quick.isNotEmpty()) {
            setSection("quick", if (prefs.showQuick) loadResult.quick else emptyList())
        }

        header.updateNet(netMonitor?.current())
    }

    /** Muddati (`until`) o'tmagan bannerlar. */
    private fun activeBanners(): List<Banner> {
        val now = System.currentTimeMillis()
        return loadResult.banners.filter { it.expiresAt == null || it.expiresAt >= now }
    }

    private fun colorFor(pkg: String): String {
        val hue = ((pkg.hashCode() and 0x7fffffff) % 360)
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.55f, 0.72f)) and 0xFFFFFF
        return String.format("#%06X", rgb)
    }

    private fun hdmiEntries(): List<AppEntry> = try {
        val tm = getSystemService(Context.TV_INPUT_SERVICE) as? android.media.tv.TvInputManager
        tm?.tvInputList
            ?.filter { it.isPassthroughInput }
            ?.map { info ->
                val label = info.loadLabel(this)?.toString() ?: "HDMI"
                AppEntry(name = label, packages = emptyList(), color = "#0F766E", action = "hdmi:${info.id}", key = "hdmi:${info.id}")
            } ?: emptyList()
    } catch (e: Exception) {
        ErrorLog.log(this, "hdmi", "HDMI kirishlarini o'qib bo'lmadi: ${e.message}", e)
        emptyList()
    }

    private fun allAppModels(): List<CardModel> = toModels(
        installedCache.map { AppEntry(it.label, listOf(it.pkg), colorFor(it.pkg), key = "p:${it.pkg}") }
    )

    // ------------------------------------------------------------------ ob-havo

    private fun updateWeather() {
        if (!prefs.showWeather) {
            header.updateWeather(null)
            screensaver.setWeather(null)
            return
        }
        val city = Weather.city(prefs.weatherCity)
        applyWeather(Weather.cached(this, city))
        Weather.refresh(this, city, false) { fresh ->
            // Foydalanuvchi so'rov paytida shaharni almashtirgan bo'lishi mumkin
            if (fresh.city == Weather.city(prefs.weatherCity).name && prefs.showWeather) applyWeather(fresh)
        }
    }

    private fun applyWeather(w: WeatherData?) {
        header.updateWeather(w)
        screensaver.setWeather(
            if (w == null) null else "${Weather.emoji(w.code, w.isDay)} ${w.tempC}\u00B0C \u00B7 ${w.city}"
        )
    }

    // ------------------------------------------------------------------ ekran saqlovchi

    private fun resetIdle() {
        idleHandler.removeCallbacks(idleRunnable)
        val minutes = prefs.screensaverMinutes
        if (minutes > 0 && !screensaver.isShowing) {
            idleHandler.postDelayed(idleRunnable, minutes * 60_000L)
        }
    }

    private fun onIdle() {
        if (Dialogs.hasOpenDialogs()) {
            resetIdle() // dialog ochiq: keyinroq qayta tekshiramiz
            return
        }
        screensaver.show()
    }

    // ------------------------------------------------------------------ tugmalar

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        val isVolumeKey = code == KeyEvent.KEYCODE_VOLUME_UP ||
            code == KeyEvent.KEYCODE_VOLUME_DOWN ||
            code == KeyEvent.KEYCODE_VOLUME_MUTE

        // Ekran saqlovchi ochiq: istalgan tugma uni yopadi (ovoz tugmalaridan tashqari)
        if (screensaver.isShowing && !isVolumeKey) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                screensaver.hide()
                resetIdle()
            }
            return true
        }
        resetIdle()

        // Pultdagi mikrofon / qidiruv tugmasi (ba'zi pultlar shu kodlarni ilovaga yuboradi)
        if (code == KeyEvent.KEYCODE_SEARCH ||
            code == KeyEvent.KEYCODE_VOICE_ASSIST ||
            code == KeyEvent.KEYCODE_ASSIST
        ) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) startVoiceSearch()
            return true
        }

        if (isVolumeKey) {
            // Ovozni tizimning o'zi o'zgartiradi; biz faqat natijani ekranda ko'rsatamiz
            val handled = super.dispatchKeyEvent(event)
            if (event.action == KeyEvent.ACTION_DOWN && prefs.showVolume) {
                volumeHandler.removeCallbacks(volumeRunnable)
                volumeHandler.postDelayed(volumeRunnable, 90)
            }
            return handled
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showVolume() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (am.isVolumeFixed) return // qat'iy ovoz (masalan HDMI orqali): indikator noto'g'ri bo'lardi
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max <= 0) return
            val level = Math.round(am.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / max)
            volume.show(level, am.isStreamMute(AudioManager.STREAM_MUSIC))
        } catch (e: Exception) {
            ErrorLog.log(this, "volume", "Ovoz holatini o'qib bo'lmadi: ${e.message}", e)
        }
    }

    // ------------------------------------------------------------------ amallar

    private fun onCardClick(model: CardModel) {
        if (!model.isAction) prefs.pushRecent(model.entry.key)
        AppLauncher.launch(this, model.entry, this)
        refresh(rescanPackages = false)
    }

    private fun onBannerClick(b: Banner) {
        val entry = b.toEntry()
        when {
            entry.action != null || entry.packages.isNotEmpty() -> AppLauncher.launch(this, entry, this)
            entry.url != null -> AppLauncher.openUrl(this, entry.url, entry.name)
        }
    }

    private fun onCardMenu(model: CardModel) {
        Dialogs.showContextMenu(
            this, theme, model,
            onOpen = { onCardClick(model) },
            onToggleFavorite = {
                val nowFav = prefs.toggleFavorite(model.entry.key)
                Toast.makeText(
                    this,
                    getString(if (nowFav) R.string.toast_added_fav else R.string.toast_removed_fav),
                    Toast.LENGTH_SHORT
                ).show()
                refresh(rescanPackages = false)
            },
            onAbout = { Dialogs.showAppInfo(this, theme, model) },
            onMarket = { AppLauncher.openPlayMarket(this, model.entry) },
            onUninstall = {
                val pkg = model.installedPackage
                if (pkg != null) {
                    if (prefs.confirmUninstall) {
                        Dialogs.showChoice(
                            this, theme, getString(R.string.confirm_uninstall_title, model.entry.name),
                            listOf(getString(R.string.confirm_yes), getString(R.string.confirm_no)), 1
                        ) { i ->
                            if (i == 0) {
                                IconCache.invalidate(pkg)
                                AppLauncher.uninstall(this, pkg)
                            }
                        }
                    } else {
                        IconCache.invalidate(pkg)
                        AppLauncher.uninstall(this, pkg)
                    }
                }
            }
        )
    }

    private fun onOpenAction(action: String) {
        when (action) {
            "search" -> openSearch()
            "all_apps" -> openAllApps()
            "hub_settings" -> openHubSettings()
            "hdmi" -> openHdmiPicker()
            "voice" -> openVoiceSearch()
            "profile" -> openProfiles()
            "wifi" -> AppLauncher.launch(this, AppEntry("Wi-Fi", emptyList(), "#000", action = "wifi"), this)
        }
    }

    override fun openSearch() = openSearchWith("")

    private fun openSearchWith(initialQuery: String) {
        Dialogs.showSearch(
            this, theme,
            allItems = {
                val hdmi = toModels(hdmiEntries())
                val statics = toModels(staticByKey.values.toList())
                (statics + allAppModels() + hdmi).distinctBy { it.entry.key }
            },
            animations = prefs.animations,
            sizeScale = cardSizeScale(),
            onClick = { m -> onCardClick(m) },
            onMenu = { m -> onCardMenu(m) },
            initialQuery = initialQuery
        )
    }

    override fun openAllApps() {
        Dialogs.showAllApps(
            this, theme, allAppModels(), prefs.animations, cardSizeScale(),
            onClick = { onCardClick(it) },
            onMenu = { onCardMenu(it) }
        )
    }

    override fun openHubSettings() {
        Dialogs.showSettings(
            this, theme, prefs, profiles.active(),
            onChanged = {
                applyTheme()
                refresh(rescanPackages = false)
                updateWeather()
                resetIdle()
            },
            onManageFavorites = {
                Dialogs.showManageFavorites(
                    this, theme,
                    favoriteModels = {
                        val lookup = (staticByKey.values + allAppModels().map { it.entry } + hdmiEntries()).associateBy { it.key }
                        toModels(prefs.favorites().mapNotNull { lookup[it] })
                    },
                    animations = prefs.animations,
                    sizeScale = cardSizeScale(),
                    onRemove = { m ->
                        prefs.toggleFavorite(m.entry.key)
                        refresh(rescanPackages = false)
                    }
                )
            },
            onProfiles = { openProfiles() }
        )
    }

    override fun openHdmiPicker() {
        val hdmi = hdmiEntries()
        if (hdmi.isEmpty()) {
            Dialogs.showMessage(this, theme, getString(R.string.section_hdmi), getString(R.string.info_unknown))
            return
        }
        Dialogs.showChoice(this, theme, getString(R.string.section_hdmi), hdmi.map { it.name }, -1) { i ->
            AppLauncher.launch(this, hdmi[i], this)
        }
    }

    // ------------------------------------------------------------------ profillar

    override fun openProfiles() {
        Dialogs.showProfiles(
            this, theme, profiles,
            onSwitch = { id -> switchProfile(id) },
            onChanged = { reloadForProfile() }
        )
    }

    private fun switchProfile(id: String) {
        profiles.setActive(id)
        reloadForProfile()
    }

    /** Boshqa profilga o'tganda (yoki profil o'zgarganda) butun ekran shu profil ma'lumotlari bilan qayta quriladi. */
    private fun reloadForProfile() {
        Dialogs.dismissAll()
        recreate()
    }

    // ------------------------------------------------------------------ ovozli qidiruv

    override fun openVoiceSearch() = startVoiceSearch()

    private fun startVoiceSearch() {
        val tag = VOICE_LANGS[prefs.voiceLang.coerceIn(0, VOICE_LANGS.size - 1)].first
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            if (tag != null) putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_prompt))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            startActivityForResult(intent, REQ_VOICE)
        } catch (e: ActivityNotFoundException) {
            voiceUnavailable(e)
        } catch (e: SecurityException) {
            voiceUnavailable(e)
        }
    }

    private fun voiceUnavailable(e: Exception) {
        ErrorLog.log(this, "voice", "Ovozli qidiruv ochilmadi: ${e.javaClass.simpleName}", e)
        Toast.makeText(this, R.string.toast_voice_unsupported, Toast.LENGTH_LONG).show()
        openSearch()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_VOICE) return
        if (resultCode != RESULT_OK) return
        val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
        if (text.isNullOrEmpty()) {
            Toast.makeText(this, R.string.toast_voice_nothing, Toast.LENGTH_SHORT).show()
        } else {
            openSearchWith(text)
        }
    }

    // ------------------------------------------------------------------ hayot sikli

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        isHome = intent?.hasCategory(Intent.CATEGORY_HOME) == true
        if (isHome && ::scroll.isInitialized) {
            screensaver.hide(immediate = true)
            resetIdle()
            scroll.smoothScrollTo(0, 0)
        }
    }

    override fun onBackPressed() {
        // Launcher sifatida ishlaganda "Orqaga" ilovadan chiqarib yubormasin
        if (!isHome) super.onBackPressed()
    }

    companion object {
        private const val REQ_VOICE = 4711
        private const val WEATHER_TICK_MS = 15 * 60 * 1000L

        /** (til kodi yoki null = tizim tili, nomi). Sozlamalardagi tanlov shu tartibda. */
        val VOICE_LANGS: List<Pair<String?, Int>> = listOf(
            Pair("uz-UZ", R.string.voice_lang_uz),
            Pair("ru-RU", R.string.voice_lang_ru),
            Pair("en-US", R.string.voice_lang_en),
            Pair(null, R.string.voice_lang_system)
        )
    }
}
