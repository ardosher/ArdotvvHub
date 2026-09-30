package uz.ardo.tvhub

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.ColorUtils
import java.util.Calendar

/** Tema ranglari (dark / light / automatic) va fon gradientlari. */
class HubTheme(val dark: Boolean, backgroundIndex: Int) {

    val ink: Int = if (dark) Color.parseColor("#F4F6FF") else Color.parseColor("#0B1020")
    val inkDim: Int = if (dark) Color.parseColor("#B4BCDD") else Color.parseColor("#3A4468")
    val surface: Int = if (dark) Color.parseColor("#1B2140") else Color.parseColor("#FFFFFF")
    val surfaceAlt: Int = if (dark) Color.parseColor("#2A3260") else Color.parseColor("#E6EAF8")
    /** Android TV uslubidagi fokus ramkasi: qorong'i fonda oq, yorug' fonda deyarli qora. */
    val focusRing: Int = if (dark) Color.parseColor("#FFFFFF") else Color.parseColor("#101018")
    /** Fokuslanmagan kartochkalarning xiralashtirilgan (dimmed) shaffofligi — Android TV effekti. */
    val dimAlpha: Float = if (dark) 0.62f else 0.72f

    private val top: Int
    private val bottom: Int

    init {
        val pair = BACKGROUNDS[backgroundIndex.coerceIn(0, BACKGROUNDS.size - 1)]
        top = if (dark) pair.first else ColorUtils.blendARGB(pair.first, Color.WHITE, 0.9f)
        bottom = if (dark) pair.second else ColorUtils.blendARGB(pair.second, Color.WHITE, 0.82f)
    }

    fun backgroundDrawable(): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(top, bottom))

    companion object {
        /** (yuqori, pastki) ranglar. Tartib strings.xml dagi bg_names bilan mos. */
        val BACKGROUNDS = listOf(
            Pair(Color.parseColor("#0B1020"), Color.parseColor("#1A1F3D")), // standart
            Pair(Color.parseColor("#000000"), Color.parseColor("#14161C")), // qorong'i
            Pair(Color.parseColor("#04122E"), Color.parseColor("#0F3D8C")), // ko'k
            Pair(Color.parseColor("#160A2E"), Color.parseColor("#4A1F8C")), // binafsha
            Pair(Color.parseColor("#04180F"), Color.parseColor("#0F5C3A"))  // yashil
        )

        fun resolve(prefs: Prefs): HubTheme {
            val dark = when (prefs.themeMode) {
                Prefs.THEME_LIGHT -> false
                Prefs.THEME_AUTO -> {
                    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    h < 7 || h >= 19 // kechasi qorong'i, kunduzi yorug'
                }
                else -> true
            }
            return HubTheme(dark, prefs.background)
        }
    }
}
