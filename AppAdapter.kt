package uz.ardo.tvhub

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

/**
 * Kartochkalar adapteri. Yangilanish DiffUtil orqali: faqat o'zgargan kartochkalar qayta chiziladi.
 *
 * @param onClick OK bosilganda
 * @param onMenu uzoq OK yoki MENU bosilganda (kontekst menyu)
 */
class AppAdapter(
    private var items: List<CardModel>,
    private val focusRing: Int,
    private val animations: Boolean,
    private val sizeScale: Float,
    private val onClick: (CardModel) -> Unit,
    private val onMenu: ((CardModel) -> Unit)? = null,
    /** Fokuslanmagan kartaning shaffofligi (Android TV uslubidagi "dim" effekti). */
    private val dimAlpha: Float = 0.62f
) : RecyclerView.Adapter<AppAdapter.VH>() {

    class VH(
        val card: FrameLayout,
        val icon: ImageView,
        val letter: TextView,
        val label: TextView,
        val badge: TextView,
        val star: TextView,
        val newTag: TextView
    ) : RecyclerView.ViewHolder(card) {
        /** Asinxron ikonka yuklanayotganda eski bind'ni tanib olish uchun. */
        var boundPackage: String? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val d = ctx.resources.displayMetrics.density
        fun px(v: Int) = (v * d * sizeScale).toInt()
        fun pxFixed(v: Int) = (v * d).toInt()

        val card = FrameLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(px(210), px(124)).apply {
                setMargins(pxFixed(8), pxFixed(20), pxFixed(8), pxFixed(20))
            }
            isFocusable = true
            isClickable = true
            isFocusableInTouchMode = false
            isLongClickable = onMenu != null
        }

        val icon = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(px(64), px(64)).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins(px(18), px(16), 0, 0)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val letter = TextView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(px(64), px(64)).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins(px(18), px(16), 0, 0)
            }
            gravity = Gravity.CENTER
            textSize = 32f * sizeScale
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(70, 255, 255, 255))
            }
        }

        val label = TextView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
                setMargins(px(18), 0, px(14), px(14))
            }
            textSize = 20f * sizeScale
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setShadowLayer(4f, 0f, 1f, Color.argb(160, 0, 0, 0))
        }

        // Yuqori o'ng burchakdagi belgilar qatori: [Yangi] [O'rnatish] [★]
        val tags = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(0, px(12), px(12), 0)
            }
        }

        val newTag = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = px(6) }
            textSize = 12f * sizeScale
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#1B1B1B"))
            setPadding(px(9), px(3), px(9), px(3))
            text = ctx.getString(R.string.badge_new)
            background = GradientDrawable().apply {
                cornerRadius = px(10).toFloat()
                setColor(Color.parseColor("#FFD166"))
            }
            visibility = View.GONE
        }

        val badge = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textSize = 13f * sizeScale
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(px(10), px(4), px(10), px(4))
            text = ctx.getString(R.string.badge_install)
            background = GradientDrawable().apply {
                cornerRadius = px(12).toFloat()
                setColor(Color.argb(150, 0, 0, 0))
            }
        }

        val star = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textSize = 24f * sizeScale
            setTextColor(Color.parseColor("#FFD166"))
            text = "\u2605"
            setShadowLayer(4f, 0f, 1f, Color.argb(160, 0, 0, 0))
        }

        tags.addView(newTag)
        tags.addView(badge)
        tags.addView(star)

        card.addView(icon)
        card.addView(letter)
        card.addView(label)
        card.addView(tags)
        return VH(card, icon, letter, label, badge, star, newTag)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val model = items[position]
        val entry = model.entry
        val ctx = holder.card.context
        val density = ctx.resources.displayMetrics.density
        val base = try {
            Color.parseColor(entry.color)
        } catch (e: IllegalArgumentException) {
            Color.parseColor("#3B5BDB")
        }
        val dark = ColorUtils.blendARGB(base, Color.BLACK, 0.45f)

        fun makeBg(focused: Boolean) = GradientDrawable(
            GradientDrawable.Orientation.TL_BR, intArrayOf(base, dark)
        ).apply {
            cornerRadius = 10 * density
            if (focused) setStroke((3 * density).toInt(), focusRing)
        }

        holder.card.background = makeBg(false)
        // Android TV uslubi: fokuslanmagan kartalar xiralashtirilgan, fokusdagi karta to'liq yorqin
        holder.card.alpha = dimAlpha
        holder.label.text = entry.name

        // Sevimli yulduzcha va "O'rnatish" belgisi bir joyni egallaydi: o'rnatilmagan bo'lsa belgi ustun
        holder.badge.visibility = if (model.isInstalled) View.GONE else View.VISIBLE
        holder.star.visibility = if (model.favorite && model.isInstalled) View.VISIBLE else View.GONE
        // "Yangi" belgisi: apps.json dagi `added` sanasi oxirgi 7 kun ichida bo'lsa
        holder.newTag.visibility = if (model.isNew) View.VISIBLE else View.GONE

        // Ikonka: keshdan darhol, bo'lmasa fon oqimida yuklanadi; xato bo'lsa harf fallback
        val pkg = model.installedPackage
        holder.boundPackage = pkg
        holder.letter.text = entry.name.take(1).uppercase()
        var iconSet = false
        if (pkg != null) {
            val cached = IconCache.peek(ctx, pkg)
            if (cached != null) {
                holder.icon.setImageDrawable(cached)
                iconSet = true
            } else {
                IconCache.load(ctx, pkg) { drawable ->
                    if (holder.boundPackage == pkg && drawable != null) {
                        holder.icon.setImageDrawable(drawable)
                        holder.icon.visibility = View.VISIBLE
                        holder.letter.visibility = View.GONE
                    }
                }
            }
        }
        holder.icon.visibility = if (iconSet) View.VISIBLE else View.GONE
        holder.letter.visibility = if (iconSet) View.GONE else View.VISIBLE

        holder.card.setOnFocusChangeListener { v, hasFocus ->
            v.background = makeBg(hasFocus)
            v.elevation = if (hasFocus) 28 * density else 2 * density
            // Android TV uslubi: fokusdagi karta kattalashadi va to'liq yorqin, qolganlar xiralashadi
            val s = if (hasFocus) 1.14f else 1f
            val a = if (hasFocus) 1f else dimAlpha
            if (animations) {
                v.animate().scaleX(s).scaleY(s).alpha(a).setDuration(150).start()
            } else {
                v.scaleX = s
                v.scaleY = s
                v.alpha = a
            }
            // Fokusdagi karta qo'shnilarini bosib qolmasligi uchun eng old qatlamga chiqadi
            v.bringToFront()
            (v.parent as? View)?.invalidate()
        }
        holder.card.elevation = 2 * density
        holder.card.setOnClickListener { onClick(model) }

        if (onMenu != null) {
            // Uzoq OK (View o'zi DPAD_CENTER uzoq bosishini longClick ga aylantiradi)
            holder.card.setOnLongClickListener {
                onMenu.invoke(model)
                true
            }
            // MENU tugmasi: OK/uzoq OK bilan aralashmaydi, chunki alohida keycode
            holder.card.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_MENU) {
                    if (event.action == KeyEvent.ACTION_UP) onMenu.invoke(model)
                    true
                } else {
                    false
                }
            }
        }
    }

    override fun onViewRecycled(holder: VH) {
        holder.boundPackage = null
        holder.card.animate().cancel()
        super.onViewRecycled(holder)
    }

    /** Faqat farq bo'lsa yangilaydi. @return o'zgarish bo'ldimi. */
    fun setItems(newItems: List<CardModel>): Boolean {
        val old = items
        if (old == newItems) return false
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(o: Int, n: Int) = old[o].entry.key == newItems[n].entry.key
            override fun areContentsTheSame(o: Int, n: Int) = old[o] == newItems[n]
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
        return true
    }

    fun currentItems(): List<CardModel> = items

    override fun getItemCount(): Int = items.size
}
