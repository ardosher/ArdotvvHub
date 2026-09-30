package uz.ardo.tvhub

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlin.math.roundToInt

data class WeatherData(
    val tempC: Int,
    /** WMO ob-havo kodi (Open-Meteo `weather_code`). */
    val code: Int,
    val isDay: Boolean,
    val city: String,
    val fetchedAt: Long
)

/**
 * Sarlavhadagi ob-havo vidjeti. Ma'lumot manbai: Open-Meteo (API kalit kerak emas, internet kerak).
 * Oxirgi natija xotirada saqlanadi, shuning uchun internet vaqtincha yo'q bo'lsa ham eski qiymat ko'rinadi.
 */
object Weather {

    data class City(val name: String, val lat: Double, val lon: Double)

    val CITIES = listOf(
        City("Toshkent", 41.2995, 69.2401),
        City("Samarqand", 39.6542, 66.9597),
        City("Buxoro", 39.7747, 64.4286),
        City("Andijon", 40.7821, 72.3442),
        City("Namangan", 40.9983, 71.6726),
        City("Farg'ona", 40.3842, 71.7843),
        City("Nukus", 42.4531, 59.6103),
        City("Qarshi", 38.8606, 65.7891),
        City("Termiz", 37.2242, 67.2783),
        City("Urganch", 41.5506, 60.6317),
        City("Jizzax", 40.1158, 67.8422),
        City("Guliston", 40.4897, 68.7842),
        City("Navoiy", 40.0844, 65.3792)
    )

    private const val TTL_MS = 30 * 60 * 1000L
    private const val MAX_AGE_MS = 6 * 60 * 60 * 1000L

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var inFlight = false

    fun city(index: Int): City = CITIES[index.coerceIn(0, CITIES.size - 1)]

    private fun store(context: Context) =
        context.applicationContext.getSharedPreferences("device", Context.MODE_PRIVATE)

    /** Saqlangan (juda eski bo'lmagan) qiymat. Boshqa shahar uchun saqlangan bo'lsa null. */
    fun cached(context: Context, city: City): WeatherData? {
        val sp = store(context)
        if (sp.getString("w_city", null) != city.name) return null
        val at = sp.getLong("w_time", 0L)
        if (at == 0L || System.currentTimeMillis() - at > MAX_AGE_MS) return null
        return WeatherData(
            sp.getInt("w_temp", 0), sp.getInt("w_code", 0),
            sp.getBoolean("w_day", true), city.name, at
        )
    }

    /**
     * Qiymat eskirgan bo'lsagina internetdan yangilaydi. Muvaffaqiyatli bo'lsa [onFresh] asosiy oqimda chaqiriladi.
     * Xatolar jurnalga yoziladi va ilovani yiqitmaydi.
     */
    fun refresh(context: Context, city: City, force: Boolean, onFresh: (WeatherData) -> Unit) {
        val app = context.applicationContext
        val c = cached(app, city)
        if (!force && c != null && System.currentTimeMillis() - c.fetchedAt < TTL_MS) return
        if (inFlight) return
        inFlight = true
        try {
            io.execute {
                var result: WeatherData? = null
                try {
                    result = fetch(city)
                    save(app, result)
                } catch (e: Exception) {
                    ErrorLog.log(app, "weather", "${e.javaClass.simpleName}: ${e.message}", e)
                } finally {
                    inFlight = false
                }
                val fresh = result
                if (fresh != null) main.post { onFresh(fresh) }
            }
        } catch (e: RejectedExecutionException) {
            inFlight = false
        }
    }

    private fun save(context: Context, w: WeatherData) {
        store(context).edit()
            .putString("w_city", w.city)
            .putInt("w_temp", w.tempC)
            .putInt("w_code", w.code)
            .putBoolean("w_day", w.isDay)
            .putLong("w_time", w.fetchedAt)
            .apply()
    }

    private fun fetch(city: City): WeatherData {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=${city.lat}&longitude=${city.lon}" +
                "&current=temperature_2m,weather_code,is_day&timezone=auto"
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${conn.responseCode}")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val cur = JSONObject(text).getJSONObject("current")
            return WeatherData(
                tempC = cur.getDouble("temperature_2m").roundToInt(),
                code = cur.getInt("weather_code"),
                isDay = cur.optInt("is_day", 1) == 1,
                city = city.name,
                fetchedAt = System.currentTimeMillis()
            )
        } finally {
            conn.disconnect()
        }
    }

    /** WMO kodi -> emoji. */
    fun emoji(code: Int, isDay: Boolean): String = when (code) {
        0 -> if (isDay) "\u2600\uFE0F" else "\uD83C\uDF19"
        1, 2 -> "\uD83C\uDF24\uFE0F"
        3 -> "\u2601\uFE0F"
        45, 48 -> "\uD83C\uDF2B\uFE0F"
        in 51..57 -> "\uD83C\uDF26\uFE0F"
        in 61..67, in 80..82 -> "\uD83C\uDF27\uFE0F"
        in 71..77 -> "\u2744\uFE0F"
        85, 86 -> "\uD83C\uDF28\uFE0F"
        in 95..99 -> "\u26C8\uFE0F"
        else -> "\u2601\uFE0F"
    }
}
