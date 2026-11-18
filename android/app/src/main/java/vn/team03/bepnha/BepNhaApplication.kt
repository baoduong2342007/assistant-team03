package vn.team03.bepnha

import android.app.Application

class BepNhaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.FIREBASE_CONFIGURED) DemoAppCheck.initialize(this)
    }
}
