package uz.ardo.tvhub

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.util.concurrent.Executors

/**
 * Ilova ikonkalari uchun kichik xotira keshi.
 * Faqat ApplicationContext ushlanadi (Activity'ga havola yo'q), shuning uchun xotira oqishi bo'lmaydi.
 * Drawable emas, [Drawable.ConstantState] keshlanadi va har safar yangi drawable yasaladi.
 */
object IconCache {

    private val cache = LruCache<String, Drawable.ConstantState>(96)
    private val io = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())

    /** Keshdan tez olish (UI oqimida xavfsiz). */
    fun peek(context: Context, pkg: String): Drawable? {
        val state = cache.get(pkg) ?: return null
        return try {
            state.newDrawable(context.resources)
        } catch (e: Exception) {
            null
        }
    }

    /** Fon oqimida ikonkani yuklab, natijani asosiy oqimda qaytaradi. Topilmasa null. */
    fun load(context: Context, pkg: String, onLoaded: (Drawable?) -> Unit) {
        val app = context.applicationContext
        try {
            io.execute {
                val drawable: Drawable? = try {
                    val d = app.packageManager.getApplicationIcon(pkg)
                    d.constantState?.let { cache.put(pkg, it) }
                    d
                } catch (e: Exception) {
                    null
                }
                main.post { onLoaded(drawable) }
            }
        } catch (e: java.util.concurrent.RejectedExecutionException) {
            main.post { onLoaded(null) }
        }
    }

    /** Ilova o'chirilsa yoki yangilansa eski ikonkani tashlab yuborish. */
    fun invalidate(pkg: String?) {
        if (pkg == null) cache.evictAll() else cache.remove(pkg)
    }
}
