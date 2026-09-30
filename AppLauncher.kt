package uz.ardo.tvhub

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/** Launcher ichki oynalarini ochadigan action'lar (MainActivity amalga oshiradi). */
interface HubActions {
    fun openSearch()
    fun openAllApps()
    fun openHubSettings()
    fun openHdmiPicker()
    fun openVoiceSearch()
    fun openProfiles()
}

object AppLauncher {

    /** action nomi -> Android sozlamalar oynasi. Yangi action qo'shish uchun shu yerga bitta qator qo'shing. */
    private val settingsActions: Map<String, String> = mapOf(
        "settings" to Settings.ACTION_SETTINGS,
        "wifi" to Settings.ACTION_WIFI_SETTINGS,
        "bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
        "display" to Settings.ACTION_DISPLAY_SETTINGS,
        "sound" to Settings.ACTION_SOUND_SETTINGS,
        "network" to Settings.ACTION_WIRELESS_SETTINGS,
        "system" to Settings.ACTION_DEVICE_INFO_SETTINGS
    )

    /** O'rnatilgan birinchi paketni qaytaradi (yo'q bo'lsa null). */
    fun installedPackage(context: Context, entry: AppEntry): String? {
        val pm = context.packageManager
        for (p in entry.packages) {
            val intent = try {
                pm.getLeanbackLaunchIntentForPackage(p) ?: pm.getLaunchIntentForPackage(p)
            } catch (e: Exception) {
                null
            }
            if (intent != null) return p
        }
        return null
    }

    fun launch(context: Context, entry: AppEntry, hub: HubActions? = null) {
        val action = entry.action
        if (action != null) {
            launchAction(context, entry, action, hub)
            return
        }

        val pm = context.packageManager
        for (p in entry.packages) {
            val intent = try {
                pm.getLeanbackLaunchIntentForPackage(p) ?: pm.getLaunchIntentForPackage(p)
            } catch (e: Exception) {
                null
            }
            if (intent != null) {
                if (start(context, intent)) return
            }
        }
        openNotInstalled(context, entry)
    }

    /** Ilova ochilmagan bo'lsa: avval url, keyin Play Market, keyin web. */
    fun openNotInstalled(context: Context, entry: AppEntry) {
        if (!entry.url.isNullOrBlank()) {
            if (start(context, Intent(Intent.ACTION_VIEW, Uri.parse(entry.url)))) return
        }
        Toast.makeText(
            context,
            context.getString(R.string.toast_not_installed_market, entry.name),
            Toast.LENGTH_LONG
        ).show()
        openPlayMarket(context, entry)
    }

    /** Faqat havola bo'lgan banner uchun: brauzerda ochadi, ochilmasa xabar beradi. */
    fun openUrl(context: Context, url: String, name: String) {
        if (!start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))) {
            Toast.makeText(context, context.getString(R.string.toast_open_failed, name), Toast.LENGTH_LONG).show()
        }
    }

    /** Play Market (package bo'lsa package orqali, bo'lmasa nom bo'yicha qidiruv). Topilmasa web. */
    fun openPlayMarket(context: Context, entry: AppEntry) {
        val pkg = entry.packages.firstOrNull()
        val marketUri = if (pkg != null)
            Uri.parse("market://details?id=$pkg")
        else
            Uri.parse("market://search?q=${Uri.encode(entry.name)}&c=apps")

        if (!start(context, Intent(Intent.ACTION_VIEW, marketUri))) {
            val web = if (pkg != null)
                "https://play.google.com/store/apps/details?id=$pkg"
            else
                "https://play.google.com/store/search?q=${Uri.encode(entry.name)}&c=apps"
            if (!start(context, Intent(Intent.ACTION_VIEW, Uri.parse(web)))) {
                Toast.makeText(context, R.string.toast_market_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Android'ning rasmiy o'chirish oynasi. Tizim ilovalari uchun cheklovni hurmat qiladi. */
    fun uninstall(context: Context, pkg: String) {
        if (InstalledApps.isSystemApp(context, pkg)) {
            Toast.makeText(context, R.string.toast_system_app, Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
        if (!start(context, intent)) {
            Toast.makeText(context, R.string.toast_uninstall_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun launchAction(context: Context, entry: AppEntry, action: String, hub: HubActions?) {
        when {
            action == "search" -> hub?.openSearch()
            action == "all_apps" -> hub?.openAllApps()
            action == "hub_settings" -> hub?.openHubSettings()
            action == "hdmi" -> hub?.openHdmiPicker()
            action == "voice" -> hub?.openVoiceSearch()
            action == "profile" -> hub?.openProfiles()
            action.startsWith("hdmi:") -> {
                val id = action.removePrefix("hdmi:")
                val ok = try {
                    val uri = TvContract.buildChannelUriForPassthroughInput(id)
                    start(context, Intent(Intent.ACTION_VIEW, uri))
                } catch (e: Exception) {
                    false
                }
                if (!ok) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_open_failed, entry.name),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            settingsActions.containsKey(action) -> {
                val primary = settingsActions.getValue(action)
                if (!start(context, Intent(primary))) {
                    if (!start(context, Intent(Settings.ACTION_SETTINGS))) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_open_failed, entry.name),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            else -> Toast.makeText(
                context,
                context.getString(R.string.toast_open_failed, entry.name),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun start(context: Context, intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            ErrorLog.log(context, "launch", "Topilmadi: ${intent.dataString ?: intent.action}", e)
            false
        } catch (e: SecurityException) {
            ErrorLog.log(context, "launch", "Ruxsat yo'q: ${intent.dataString ?: intent.action}", e)
            false
        } catch (e: RuntimeException) {
            ErrorLog.log(context, "launch", "${e.javaClass.simpleName}: ${e.message}", e)
            false
        }
    }
}
