package de.tidalbooks.prototype

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    private val playback
        get() = (application as ProbeApp).probe

    private var sharedInput by mutableStateOf("")
    private var shareNonce by mutableIntStateOf(0)
    private var openPlayerNonce by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntent(intent)
        playback.refreshLogin()
        setContent {
            val state by playback.state.collectAsStateWithLifecycle()

            AudiobookTheme { LibraryApp(state, playback, sharedInput, shareNonce, openPlayerNonce) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    override fun onStop() {
        playback.saveCurrentPosition()
        super.onStop()
    }

    private fun consumeIntent(intent: Intent) {
        intent.data?.let { uri ->
            if (dispatchLoginRedirect(uri.toString()) { playback.finishLogin(it) })
                intent.data = null
        }
        if (intent.getBooleanExtra("openPlayer", false)) {
            openPlayerNonce++
            intent.removeExtra("openPlayer")
        }
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()

            if (text.isNotBlank()) {
                sharedInput =
                    Regex("https://(?:www\\.|listen\\.)?tidal\\.com/[^\\s]+").find(text)?.value
                        ?: text
                shareNonce++
            }

            intent.removeExtra(Intent.EXTRA_TEXT)
        }
    }
}
