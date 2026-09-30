package uz.ardo.tvhub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * Bosh sahifadagi katta reklama bannerlari qatori (apps.json > "banners").
 * Rasm (image) berilmasa, `color` asosidagi gradient ishlatiladi.
 */
class BannerAdapter(
    private var items: List<Banner>,
    private val focusRing: Int,
    private val animations: Boolean,
    private val dimAlpha: Float,
    private val sizeScale: Float,
    private val onClick: (Banner) -> Unit
) : RecyclerView.Adapter<BannerAdapter.VH>() {

    class VH(
        val card: FrameLayout,
        val image: ImageView,
        val title: TextView,
        val subtitle: TextView
    ) : RecyclerView.ViewHolder(card) {
        var boundUrl: String? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val d = ctx.resources.displayMetrics.density
        fun px(v: Int) = (v * d * sizeScale).toInt()
        fun pxFixed(v: Int) = (v * d).toInt()

        val card = FrameLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(px(540), px(168)).apply {
                setMargins(pxFixed(8), pxFixed(16), pxFixed(8), pxFixed(16))
            }
            isFocusable = true
            isClickable = true
            isFocusableInTouchMode = false
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }

        val image = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        // Matn o'qilishi uchun pastki qorayish
        val scrim = View(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, px(110), Gravity.BOTTOM
            )
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(190, 0, 0, 0))
            )
        }

        val texts = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
            ).apply { setMargins(px(22), 0, px(22), px(16)) }
        }
        val title = TextView(ctx).apply {
            textSize = 26f * sizeScale
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setShadowLayer(4f, 0f, 1f, Color.argb(160, 0, 0, 0))
        }
        val subtitle = TextView(ctx).apply {
            textSize = 16f * sizeScale
            setTextColor(Color.argb(230, 255, 255, 255))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setShadowLayer(4f, 0f, 1f, Color.argb(160, 0, 0, 0))
        }
        texts.addView(title)
        texts.addView(subtitle)

        card.addView(image)
        card.addView(scrim)
        card.addView(texts)
        return VH(card, image, title, subtitle)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val banner = items[position]
        val ctx = holder.card.context
        val density = ctx.resources.displayMetrics.density
        val base = try {
            Color.parseColor(banner.color)
        } catch (e: IllegalArgumentException) {
            Color.parseColor("#3B5BDB")
        }
        val dark = ColorUtils.blendARGB(base, Color.BLACK, 0.55f)

        holder.card.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR, intArrayOf(base, dark)
        ).apply { cornerRadius = 14 * density }
        holder.card.foreground = null
        holder.card.alpha = dimAlpha

        holder.title.text = banner.title
        holder.subtitle.text = banner.subtitle ?: ""
        holder.subtitle.visibility = if (banner.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE

        val url = banner.imageUrl
        holder.boundUrl = url
        holder.image.setImageDrawable(null)
        if (url != null) {
            BannerImages.load(ctx, url) { bmp ->
                if (holder.boundUrl == url && bmp != null) holder.image.setImageBitmap(bmp)
            }
        }

        holder.card.setOnFocusChangeListener { v, hasFocus ->
            (v as FrameLayout).foreground = if (hasFocus) GradientDrawable().apply {
                cornerRadius = 14 * density
                setStroke((4 * density).toInt(), focusRing)
            } else null
            v.elevation = if (hasFocus) 28 * density else 2 * density
            val s = if (hasFocus) 1.06f else 1f
            val a = if (hasFocus) 1f else dimAlpha
            if (animations) {
                v.animate().scaleX(s).scaleY(s).alpha(a).setDuration(150).start()
            } else {
                v.scaleX = s
                v.scaleY = s
                v.alpha = a
            }
            v.bringToFront()
            (v.parent as? View)?.invalidate()
        }
        holder.card.elevation = 2 * density
        holder.card.setOnClickListener { onClick(banner) }
    }

    override fun onViewRecycled(holder: VH) {
        holder.boundUrl = null
        holder.card.animate().cancel()
        super.onViewRecycled(holder)
    }

    /** Faqat farq bo'lsa yangilaydi. */
    fun setItems(newItems: List<Banner>): Boolean {
        if (items == newItems) return false
        items = newItems
        notifyDataSetChanged()
        return true
    }

    override fun getItemCount(): Int = items.size
}

/** Banner rasmlarini yuklash (kichik xotira keshi bilan). Xatolar jurnalga yoziladi. */
internal object BannerImages {

    private const val MAX_BYTES = 6 * 1024 * 1024
    private const val MAX_WIDTH = 1280

    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val io = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())

    fun load(context: Context, url: String, onLoaded: (Bitmap?) -> Unit) {
        val hit = cache.get(url)
        if (hit != null) {
            onLoaded(hit)
            return
        }
        val app = context.applicationContext
        try {
            io.execute {
                val bmp: Bitmap? = try {
                    download(url)
                } catch (e: Exception) {
                    ErrorLog.log(app, "banner", "$url: ${e.javaClass.simpleName}: ${e.message}", e)
                    null
                }
                if (bmp != null) cache.put(url, bmp)
                main.post { onLoaded(bmp) }
            }
        } catch (e: RejectedExecutionException) {
            main.post { onLoaded(null) }
        }
    }

    private fun download(url: String): Bitmap? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 10000
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${conn.responseCode}")
            val bytes = conn.inputStream.use { it.readBytes() }
            if (bytes.size > MAX_BYTES) throw IOException("rasm juda katta (${bytes.size / 1024} KB)")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= MAX_WIDTH) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                ?: throw IOException("rasm o'qilmadi")
        } finally {
            conn.disconnect()
        }
    }
}
