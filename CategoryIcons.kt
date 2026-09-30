package uz.ardo.tvhub

/** Kategoriya nomiga mos kichik belgi (emoji). apps.json da `icon` berilsa o'sha ishlatiladi. */
object CategoryIcons {

    const val RECOMMENDED = "\u2B50"
    const val FAVORITES = "\u2764\uFE0F"
    const val CONTINUE = "\u25B6\uFE0F"
    const val RECENT = "\uD83D\uDD58"
    const val HDMI = "\uD83D\uDD0C"
    const val QUICK = "\u26A1"

    fun forCategory(cat: Category): String = cat.icon ?: forTitle(cat.title)

    fun forTitle(title: String): String {
        val t = title.lowercase()
        return when {
            t.contains("sport") -> "\u26BD"
            t.contains("musiqa") || t.contains("music") || t.contains("qo'shiq") -> "\uD83C\uDFB5"
            t.contains("bolalar") || t.contains("kids") -> "\uD83E\uDDF8"
            t.contains("kino") || t.contains("serial") || t.contains("film") -> "\uD83C\uDFAC"
            t.contains("tv") || t.contains("kanal") || t.contains("telekanal") -> "\uD83D\uDCFA"
            t.contains("o'yin") || t.contains("game") -> "\uD83C\uDFAE"
            t.contains("yangilik") || t.contains("news") -> "\uD83D\uDCF0"
            t.contains("ta'lim") || t.contains("o'qish") || t.contains("edu") -> "\uD83C\uDF93"
            t.contains("vosita") || t.contains("tools") || t.contains("sozlama") -> "\uD83D\uDEE0\uFE0F"
            t.contains("asosiy") || t.contains("main") -> "\uD83C\uDFE0"
            else -> "\uD83D\uDCC1"
        }
    }
}
