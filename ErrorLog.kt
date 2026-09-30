package uz.ardo.tvhub

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Oddiy xatoliklar jurnali (fayl: filesDir/errors.log). Hech qachon exception tashlamaydi.
 * Sozlamalar > Ilova haqida > "Xatoliklar jurnali"da ko'rinadi.
 */
object ErrorLog {

    data class Entry(val time: Long, val tag: String, val message: String, val detail: String?)

    private const val FILE = "errors.log"
    private const val MAX_CHARS = 30_000
    private const val MAX_ENTRIES = 60
    private const val DEDUP_MS = 10 * 60 * 1000L
    private const val RS = '\u001E'
    private const val US = '\u001F'

    private var lastSig: String? = null
    private var lastAt = 0L

    private fun clean(s: String): String = s.replace(RS, ' ').replace(US, ' ')

    @Synchronized
    fun log(context: Context, tag: String, message: String, t: Throwable? = null) {
        try {
            val now = System.currentTimeMillis()
            val sig = "$tag|$message"
            // Bir xil xato ketma-ket takrorlansa (masalan, internet yo'q) qayta yozilmaydi
            if (sig == lastSig && now - lastAt < DEDUP_MS) return
            lastSig = sig
            lastAt = now
            Log.w("ArdoTvHub", "[$tag] $message", t)

            val detail = if (t != null) Log.getStackTraceString(t).take(2500) else ""
            val record = listOf(now.toString(), clean(tag), clean(message.take(400)), clean(detail))
                .joinToString(US.toString())

            val file = File(context.applicationContext.filesDir, FILE)
            val old = if (file.exists()) file.readText() else ""
            val records = (old.split(RS).filter { it.isNotBlank() } + record).takeLast(MAX_ENTRIES).toMutableList()
            var text = records.joinToString(RS.toString())
            while (text.length > MAX_CHARS && records.size > 1) {
                records.removeAt(0)
                text = records.joinToString(RS.toString())
            }
            file.writeText(text)
        } catch (e: Exception) {
            // jurnal yozishning o'zi ilovani yiqitmasligi kerak
        }
    }

    /** Eng yangisi birinchi. */
    @Synchronized
    fun entries(context: Context): List<Entry> {
        return try {
            val file = File(context.applicationContext.filesDir, FILE)
            if (!file.exists()) return emptyList()
            file.readText().split(RS).filter { it.isNotBlank() }.mapNotNull { rec ->
                val f = rec.split(US)
                if (f.size < 3) null
                else Entry(f[0].toLongOrNull() ?: 0L, f[1], f[2], f.getOrNull(3)?.ifBlank { null })
            }.reversed()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun count(context: Context): Int = entries(context).size

    @Synchronized
    fun clear(context: Context) {
        try {
            File(context.applicationContext.filesDir, FILE).delete()
            lastSig = null
        } catch (e: Exception) {
            // e'tiborsiz
        }
    }
}
