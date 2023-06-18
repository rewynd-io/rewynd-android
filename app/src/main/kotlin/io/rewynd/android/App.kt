package io.rewynd.android

import android.app.Application
import android.content.Context

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        sApplication = this
    }

    companion object {
        private lateinit var sApplication: Application
        val application: Application
            get() = sApplication
        val context: Context
            get() = application.applicationContext
    }
}