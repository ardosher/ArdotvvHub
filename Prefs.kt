package uz.ardo.tvhub

import android.content.Context
import android.content.SharedPreferences

/**
 * Barcha sozlamalar, sevimlilar va oxirgi ochilganlar shu yerda.
 * Qiymatlar xotirada keshlanadi; SharedPreferences'ga faqat qiymat o'zgargandagina yoziladi.
 * Eski "hub" fayli va "recent" kaliti saqlangan.
 */
class Prefs(context: Context, profileId: String = ProfileManager.DEFAULT_ID) {

    /** Har bir profilning o'z fayli bor; asosiy profil eski "hub" faylida qoladi. */
    private val sp: SharedPreferences = context.applicationContext
        .getSharedPreferences(ProfileManager.prefsFileName(profileId), Context.MODE_PRIVATE)

    var themeMode: Int by intPref("theme", THEME_DARK)
    var background: Int by intPref("background", 0)
    var cardSize: Int by intPref("card_size", 1)
    var animations: Boolean by boolPref("animations", true)
    var showClock: Boolean by boolPref("show_clock", true)
    var showDate: Boolean by boolPref("show_date", true)
    var showWifi: Boolean by boolPref("show_wifi", true)
    var showInternet: Boolean by boolPref("show_internet", true)
    var autoRefresh: Boolean by boolPref("auto_refresh", true)
    var showRecommended: Boolean by boolPref("show_recommended", true)
    var showHdmi: Boolean by boolPref("show_hdmi", true)
    var showQuick: Boolean by boolPref("show_quick", true)
    var confirmUninstall: Boolean by boolPref("confirm_uninstall", true)
    /** Ekran saqlovchi ishga tushishi uchun harakatsizlik daqiqalari; 0 = o'chiq. */
    var screensaverMinutes: Int by intPref("screensaver_min", 5)
    var showBanners: Boolean by boolPref("show_banners", true)
    var showVolume: Boolean by boolPref("show_volume", true)
    var showWeather: Boolean by boolPref("show_weather", true)
    /** Weather.CITIES ro'yxatidagi indeks. */
    var weatherCity: Int by intPref("weather_city", 0)
    /** Ovozli qidiruv tili: MainActivity.VOICE_LANGS ro'yxatidagi indeks. */
    var voiceLang: Int by intPref("voice_lang", 0)

    // ---- Sevimlilar ----

    private var favCache: List<String> = readList("favorites")

    fun favorites(): List<String> = favCache

    fun isFavorite(key: String): Boolean = favCache.contains(key)

    /** @return yangi holat: true = sevimlilarda. */
    fun toggleFavorite(key: String): Boolean {
        val now = if (favCache.contains(key)) {
            favCache = favCache.filter { it != key }
            false
        } else {
            favCache = favCache + key
            true
        }
        writeList("favorites", favCache)
        return now
    }

    fun setFavorites(keys: List<String>) {
        if (keys == favCache) return
        favCache = keys
        writeList("favorites", keys)
    }

    // ---- Oxirgi ochilganlar ----

    private var recentCache: List<String> = readList("recent")

    fun recents(): List<String> = recentCache

    fun pushRecent(key: String) {
        if (recentCache.firstOrNull() == key) return // ortiqcha yozuv yo'q
        recentCache = (listOf(key) + recentCache.filter { it != key }).take(MAX_RECENT)
        writeList("recent", recentCache)
    }

    fun setRecents(keys: List<String>) {
        val trimmed = keys.distinct().take(MAX_RECENT)
        if (trimmed == recentCache) return
        recentCache = trimmed
        writeList("recent", trimmed)
    }

    fun clearRecents() {
        if (recentCache.isEmpty()) return
        recentCache = emptyList()
        writeList("recent", emptyList())
    }

    /** Ko'rinish/xatti-harakat sozlamalarini boshlang'ich holatga qaytaradi. Sevimlilar va oxirgilar saqlanib qoladi. */
    fun resetDisplaySettings() {
        themeMode = THEME_DARK
        background = 0
        cardSize = 1
        animations = true
        showClock = true
        showDate = true
        showWifi = true
        showInternet = true
        autoRefresh = true
        showRecommended = true
        showHdmi = true
        showQuick = true
        confirmUninstall = true
        screensaverMinutes = 5
        showBanners = true
        showVolume = true
        showWeather = true
        weatherCity = 0
        voiceLang = 0
    }

    // ---- ichki yordamchilar ----

    private fun readList(name: String): List<String> =
        (sp.getString(name, "") ?: "").split("\n").filter { it.isNotBlank() }.distinct()

    private fun writeList(name: String, list: List<String>) {
        sp.edit().putString(name, list.joinToString("\n")).apply()
    }

    private fun intPref(name: String, def: Int) = object : kotlin.properties.ReadWriteProperty<Any?, Int> {
        private var cached = sp.getInt(name, def)
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Int = cached
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Int) {
            if (value == cached) return
            cached = value
            sp.edit().putInt(name, value).apply()
        }
    }

    private fun boolPref(name: String, def: Boolean) = object : kotlin.properties.ReadWriteProperty<Any?, Boolean> {
        private var cached = sp.getBoolean(name, def)
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Boolean = cached
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Boolean) {
            if (value == cached) return
            cached = value
            sp.edit().putBoolean(name, value).apply()
        }
    }

    companion object {
        const val MAX_RECENT = 10
        const val THEME_DARK = 0
        const val THEME_LIGHT = 1
        const val THEME_AUTO = 2
    }
}
