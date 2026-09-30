package uz.ardo.tvhub

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs

/** "Ilova haqida" oynasi uchun qurilma ma'lumotlari. */
object DeviceInfo {

    /** [free] va [total] baytda. */
    data class Usage(val free: Long, val total: Long) {
        val usedFraction: Float
            get() = if (total <= 0) 0f else ((total - free).toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    fun storage(): Usage? = try {
        val st = StatFs(Environment.getDataDirectory().path)
        Usage(st.availableBytes, st.totalBytes)
    } catch (e: Exception) {
        null
    }

    fun memory(context: Context): Usage? = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        Usage(info.availMem, info.totalMem)
    } catch (e: Exception) {
        null
    }

    /** (versiya nomi, versiya kodi) */
    fun appVersion(context: Context): Pair<String, Long> {
        val info = InstalledApps.packageInfo(context, context.packageName)
        return if (info != null) Pair(info.versionName ?: "-", InstalledApps.versionCode(info))
        else Pair("-", 0L)
    }

    fun androidVersion(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    fun deviceName(): String {
        val maker = Build.MANUFACTURER ?: ""
        val model = Build.MODEL ?: ""
        return if (model.startsWith(maker, ignoreCase = true)) model else "$maker $model".trim()
    }
}
