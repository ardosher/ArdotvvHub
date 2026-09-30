package uz.ardo.tvhub

/**
 * apps.json dagi bitta yozuv (yoki o'rnatilgan ilovadan yasalgan yozuv).
 * Eski maydonlar (name, packages, color, action, url, key) saqlangan.
 */
data class AppEntry(
    val name: String,
    val packages: List<String>,
    val color: String,
    val action: String? = null,
    val url: String? = null,
    val key: String = name,
    val description: String? = null,
    val featured: Boolean = false,
    val hidden: Boolean = false,
    /** apps.json dagi `added` ("yyyy-MM-dd") sanasi, millisekundda. Yo'q bo'lsa null. */
    val addedAt: Long? = null
) {
    /** `added` sanasi oxirgi [days] kun ichida bo'lsa true ("Yangi" belgisi uchun). */
    fun isNew(nowMillis: Long, days: Int = NEW_DAYS): Boolean {
        val t = addedAt ?: return false
        val age = nowMillis - t
        return age >= 0 && age <= days * DAY_MS
    }

    companion object {
        const val NEW_DAYS = 7
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}

data class Category(
    val title: String,
    val apps: List<AppEntry>,
    /** apps.json dagi ixtiyoriy `icon` (emoji). Bo'lmasa nomiga qarab avtomatik tanlanadi. */
    val icon: String? = null
)

/** Bosh sahifadagi katta reklama banneri (apps.json > "banners"). */
data class Banner(
    val title: String,
    val subtitle: String?,
    val color: String,
    val imageUrl: String?,
    val packages: List<String>,
    val action: String?,
    val url: String?,
    /** `until` sanasining oxiri (millisekund). null = muddatsiz. */
    val expiresAt: Long?
) {
    fun toEntry(): AppEntry = AppEntry(
        name = title,
        packages = packages,
        color = color,
        action = action,
        url = url,
        key = "banner:$title"
    )
}

/** Kartochkaning ekrandagi holati. Faqat shu o'zgarsa RecyclerView yangilanadi. */
data class CardModel(
    val entry: AppEntry,
    /** O'rnatilgan paket nomi (yo'q bo'lsa null). */
    val installedPackage: String?,
    val favorite: Boolean,
    /** "Yangi" belgisi ko'rsatilsinmi. */
    val isNew: Boolean = false
) {
    val isAction: Boolean get() = entry.action != null
    val isInstalled: Boolean get() = installedPackage != null || isAction
}
