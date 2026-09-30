package uz.ardo.tvhub

import android.content.Context

data class Profile(val id: String, val name: String) {
    val avatar: String get() = ProfileManager.avatarFor(name)
}

/**
 * Profillar ro'yxati va faol profil. Har bir profilning o'z sevimlilari, oxirgi ochilganlari va
 * sozlamalari bor (alohida SharedPreferences fayli). Birinchi ("default") profil eski "hub" faylidan
 * foydalanadi, shuning uchun yangilanishdan keyin avvalgi sevimlilar va sozlamalar yo'qolmaydi.
 */
class ProfileManager(context: Context) {

    private val app = context.applicationContext
    private val sp = app.getSharedPreferences("profiles", Context.MODE_PRIVATE)

    private fun extras(): List<Profile> =
        (sp.getString("extras", "") ?: "").split("\n").mapNotNull { line ->
            val i = line.indexOf('\t')
            if (i <= 0 || i == line.length - 1) null
            else Profile(line.substring(0, i), line.substring(i + 1))
        }

    private fun saveExtras(list: List<Profile>) {
        sp.edit().putString("extras", list.joinToString("\n") { "${it.id}\t${it.name}" }).apply()
    }

    private fun defaultName(): String = sp.getString("default_name", DEFAULT_NAME) ?: DEFAULT_NAME

    private fun sanitize(name: String): String {
        val clean = name.replace('\n', ' ').replace('\t', ' ').trim()
        return if (clean.isEmpty()) "Profil" else clean
    }

    fun profiles(): List<Profile> = listOf(Profile(DEFAULT_ID, defaultName())) + extras()

    fun activeId(): String {
        val id = sp.getString("active", DEFAULT_ID) ?: DEFAULT_ID
        return if (profiles().any { it.id == id }) id else DEFAULT_ID
    }

    fun active(): Profile {
        val id = activeId()
        return profiles().firstOrNull { it.id == id } ?: Profile(DEFAULT_ID, defaultName())
    }

    fun setActive(id: String) {
        if (profiles().any { it.id == id }) sp.edit().putString("active", id).apply()
    }

    fun canAdd(): Boolean = profiles().size < MAX_PROFILES

    /** Yangi profil yoki nomni o'zgartirish uchun taklif qilinadigan (band bo'lmagan) nomlar. */
    fun availableNames(forId: String? = null): List<String> {
        val used = profiles().filter { it.id != forId }.map { it.name.lowercase() }.toSet()
        val free = PRESETS.filter { it.lowercase() !in used }
        var n = 2
        while ("profil $n" in used) n++
        return free + "Profil $n"
    }

    fun add(name: String): Profile? {
        if (!canAdd()) return null
        val id = "p" + java.lang.Long.toString(System.currentTimeMillis(), 36)
        val profile = Profile(id, sanitize(name))
        saveExtras(extras() + profile)
        return profile
    }

    fun rename(id: String, name: String) {
        val clean = sanitize(name)
        if (id == DEFAULT_ID) sp.edit().putString("default_name", clean).apply()
        else saveExtras(extras().map { if (it.id == id) it.copy(name = clean) else it })
    }

    /** Asosiy profilni o'chirib bo'lmaydi. Profil sozlamalari ham tozalanadi. */
    fun delete(id: String): Boolean {
        if (id == DEFAULT_ID) return false
        val list = extras()
        if (list.none { it.id == id }) return false
        saveExtras(list.filter { it.id != id })
        app.getSharedPreferences(prefsFileName(id), Context.MODE_PRIVATE).edit().clear().apply()
        if (sp.getString("active", DEFAULT_ID) == id) sp.edit().putString("active", DEFAULT_ID).apply()
        return true
    }

    companion object {
        const val DEFAULT_ID = "default"
        const val DEFAULT_NAME = "Ota-ona"
        const val MAX_PROFILES = 6
        val PRESETS = listOf("Ota-ona", "Ona", "Ota", "Bolalar", "Mehmon")

        /** Eski o'rnatishlar bilan moslik: asosiy profil "hub" faylini ishlatadi. */
        fun prefsFileName(id: String): String = if (id == DEFAULT_ID) "hub" else "hub_$id"

        fun avatarFor(name: String): String = when (name.trim().lowercase()) {
            "ota-ona" -> "\uD83D\uDC6A"
            "ona" -> "\uD83D\uDC69"
            "ota" -> "\uD83D\uDC68"
            "bolalar", "bola" -> "\uD83E\uDDD2"
            "mehmon" -> "\uD83D\uDC64"
            else -> "\uD83D\uDE42"
        }
    }
}
