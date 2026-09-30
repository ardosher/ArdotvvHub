package uz.ardo.tvhub

import android.content.Context

/**
 * "Tomosha qilishni davom ettiring" bo'limi uchun model.
 * Hozircha ma'lumot manbai yo'q (demo ma'lumot ishlatilmaydi). Kelajakda kino/serial
 * ilovalari bilan integratsiya qilinganda faqat [ContinueWatchingProvider] qo'shiladi.
 */
data class ContinueItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    /** 0..100 oralig'ida; noma'lum bo'lsa null. */
    val progressPercent: Int? = null,
    /** Ilovaning paket nomi (ikonka va ochish uchun). */
    val packageName: String? = null,
    /** Ilova ichidagi kontentni to'g'ridan-to'g'ri ochadigan havola (ixtiyoriy). */
    val deepLink: String? = null
) {
    fun toEntry(): AppEntry = AppEntry(
        name = title,
        packages = listOfNotNull(packageName),
        color = "#334155",
        url = deepLink,
        key = "continue:$id",
        description = subtitle
    )
}

fun interface ContinueWatchingProvider {
    fun load(context: Context): List<ContinueItem>
}

object ContinueWatchingRepository {
    private val providers = ArrayList<ContinueWatchingProvider>()

    fun register(provider: ContinueWatchingProvider) {
        if (!providers.contains(provider)) providers.add(provider)
    }

    /** Provayder xatosi launcher'ni yiqitmaydi. */
    fun load(context: Context): List<ContinueItem> {
        val out = ArrayList<ContinueItem>()
        for (p in providers) {
            try {
                out.addAll(p.load(context))
            } catch (e: Exception) {
                // e'tiborsiz qoldiriladi
            }
        }
        return out.distinctBy { it.id }
    }
}
