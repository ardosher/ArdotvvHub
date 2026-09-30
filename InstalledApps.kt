package uz.ardo.tvhub

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/** O'rnatilgan, ishga tushiriladigan ilova (nom + paket). */
data class InstalledApp(val label: String, val pkg: String)

/** Paketlarni skanerlash. Og'ir ish: faqat fon oqimida chaqiring. */
object InstalledApps {

    fun scan(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val seen = HashSet<String>()
        val out = ArrayList<InstalledApp>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(cat)
            val found = try {
                pm.queryIntentActivities(query, 0)
            } catch (e: Exception) {
                emptyList()
            }
            for (ri in found) {
                val pkg = ri.activityInfo?.packageName ?: continue
                if (pkg == context.packageName || !seen.add(pkg)) continue
                val label = try {
                    ri.loadLabel(pm)?.toString()
                } catch (e: Exception) {
                    null
                }
                out.add(InstalledApp(if (label.isNullOrBlank()) pkg else label, pkg))
            }
        }
        return out.sortedBy { it.label.lowercase() }
    }

    /** apps.json dagi paket nomlaridan qaysilari (launch intent orqali) topilishini tekshiradi. */
    fun resolveLaunchable(context: Context, packages: Collection<String>): Set<String> {
        val pm = context.packageManager
        val out = HashSet<String>()
        for (p in packages) {
            try {
                if (pm.getLeanbackLaunchIntentForPackage(p) != null || pm.getLaunchIntentForPackage(p) != null) {
                    out.add(p)
                }
            } catch (e: Exception) {
                // e'tiborsiz
            }
        }
        return out
    }

    fun packageInfo(context: Context, pkg: String): PackageInfo? = try {
        context.packageManager.getPackageInfo(pkg, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    } catch (e: Exception) {
        null
    }

    @Suppress("DEPRECATION")
    fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    /** Yangilanmagan tizim ilovasimi (Android uni o'chirishga ruxsat bermaydi). */
    fun isSystemApp(context: Context, pkg: String): Boolean = try {
        val flags = context.packageManager.getApplicationInfo(pkg, 0).flags
        val system = flags and ApplicationInfo.FLAG_SYSTEM != 0
        val updated = flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
        system && !updated
    } catch (e: Exception) {
        false
    }
}
