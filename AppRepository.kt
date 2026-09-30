package uz.ardo.tvhub

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/** apps.json o'qish natijasi. [error] null bo'lmasa fayl buzilgan yoki o'qilmagan. */
data class LoadResult(
    val categories: List<Category>,
    val quick: List<AppEntry>,
    val error: String?,
    val banners: List<Banner> = emptyList()
)

object AppRepository {

    private class Parsed(
        val categories: List<Category>,
        val quick: List<AppEntry>,
        val banners: List<Banner>
    )

    /** Eski API: xato bo'lsa exception tashlaydi. */
    fun load(context: Context): List<Category> {
        val text = context.assets.open("apps.json").bufferedReader().use { it.readText() }
        return parse(JSONObject(text)).categories
    }

    /** Hech qachon exception tashlamaydi. Xatolar "Xatoliklar jurnali"ga yoziladi. */
    fun loadSafe(context: Context): LoadResult {
        return try {
            val text = context.assets.open("apps.json").bufferedReader().use { it.readText() }
            val p = parse(JSONObject(text))
            LoadResult(p.categories, p.quick, null, p.banners)
        } catch (e: JSONException) {
            fail(context, e, "JSON")
        } catch (e: java.io.IOException) {
            fail(context, e, "IO")
        } catch (e: RuntimeException) {
            fail(context, e, "Error")
        }
    }

    private fun fail(context: Context, e: Exception, fallback: String): LoadResult {
        val msg = e.message ?: fallback
        ErrorLog.log(context, "apps.json", msg, e)
        return LoadResult(emptyList(), emptyList(), msg)
    }

    private fun parse(root: JSONObject): Parsed {
        val result = ArrayList<Category>()
        val cats = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until cats.length()) {
            val c = cats.optJSONObject(i) ?: continue
            val title = c.optString("title", "")
            if (title.isBlank()) continue
            val apps = parseApps(c.optJSONArray("apps"))
            result.add(Category(title, apps, c.optString("icon", "").ifBlank { null }))
        }
        val quick = parseApps(root.optJSONArray("quick"))
        val banners = parseBanners(root.optJSONArray("banners"))
        return Parsed(result, quick, banners)
    }

    private fun parseStrings(arr: JSONArray?): List<String> {
        val out = ArrayList<String>()
        if (arr == null) return out
        for (k in 0 until arr.length()) {
            val p = arr.optString(k, "")
            if (p.isNotBlank()) out.add(p)
        }
        return out
    }

    /** "yyyy-MM-dd" -> kun boshi (millisekund). Noto'g'ri format bo'lsa null. */
    private fun parseDate(s: String): Long? {
        if (s.isBlank()) return null
        return try {
            val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            f.isLenient = false
            f.parse(s.trim())?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun parseApps(arr: JSONArray?): List<AppEntry> {
        val apps = ArrayList<AppEntry>()
        if (arr == null) return apps
        for (j in 0 until arr.length()) {
            val a = arr.optJSONObject(j) ?: continue
            val name = a.optString("name", "")
            if (name.isBlank()) continue
            apps.add(
                AppEntry(
                    name = name,
                    packages = parseStrings(a.optJSONArray("packages")),
                    color = a.optString("color", "#3B5BDB"),
                    action = a.optString("action", "").ifBlank { null },
                    url = a.optString("url", "").ifBlank { null },
                    description = a.optString("description", "").ifBlank { null },
                    featured = a.optBoolean("featured", false),
                    hidden = a.optBoolean("hidden", false),
                    addedAt = parseDate(a.optString("added", ""))
                )
            )
        }
        return apps
    }

    private fun parseBanners(arr: JSONArray?): List<Banner> {
        val out = ArrayList<Banner>()
        if (arr == null) return out
        for (j in 0 until arr.length()) {
            val b = arr.optJSONObject(j) ?: continue
            val title = b.optString("title", "")
            if (title.isBlank()) continue
            // `until` kunining oxirigacha ko'rsatiladi
            val until = parseDate(b.optString("until", ""))?.plus(AppEntry.DAY_MS - 1)
            out.add(
                Banner(
                    title = title,
                    subtitle = b.optString("subtitle", "").ifBlank { null },
                    color = b.optString("color", "#3B5BDB"),
                    imageUrl = b.optString("image", "").ifBlank { null },
                    packages = parseStrings(b.optJSONArray("packages")),
                    action = b.optString("action", "").ifBlank { null },
                    url = b.optString("url", "").ifBlank { null },
                    expiresAt = until
                )
            )
        }
        return out
    }
}
