package uz.ardo.tvhub

import android.app.Application

/** Kutilmagan (crash) xatolarni ham jurnalga yozib qo'yadi, keyin odatdagidek davom etadi. */
class HubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            ErrorLog.log(this, "CRASH", "${error.javaClass.simpleName}: ${error.message}", error)
            previous?.uncaughtException(thread, error)
        }
    }
}
