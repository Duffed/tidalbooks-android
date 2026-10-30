package de.tidalbooks.prototype

import android.app.Application
import com.instacart.library.truetime.TrueTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ProbeApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val gateway by lazy { OfficialTidalGateway(this, scope) }
    val probe by lazy { PlaybackProbe(this, gateway, scope) }

    override fun onCreate() {
        super.onCreate()
        AppLanguage.initialize(this)
        scope.launch(Dispatchers.IO) {
            // One attempt only: no retry loop, no credentials or raw exceptions in logs.
            runCatching { TrueTime().initialize() }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        AppLanguage.initialize(this)
    }
}
