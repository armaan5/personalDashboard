package dev.armaan.pd

import android.app.Application
import dev.armaan.pd.widget.RefreshWorker

class PdApplication : Application() {
    /** Set once in [onCreate]; instrumented tests replace it before the activity starts. */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        RefreshWorker.enqueue(this)
    }
}
