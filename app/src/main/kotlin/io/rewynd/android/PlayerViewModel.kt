package io.rewynd.android

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import io.rewynd.android.PlayerService.Companion.PLAYER_SERVICE_INTENT_BUNDLE_PROPS_KEY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PlayerViewModel(
    private val activity: PlayerActivity,
    private val serverUrl: ServerUrl,
    val client: RewyndClient = RewyndClient(serverUrl.value, httpClientConfig = {
        it.install(ContentNegotiation) {
            json()
        }
        it.install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.v("RewyndClient", message)
                }
            }
            level = LogLevel.ALL
        }
        it.install(HttpCookies) {
            this.storage = PersistentCookiesStorage.INSTANCE
        }
    }),
) : AndroidViewModel(activity.application) {

    fun startPlayerService(serviceProps: PlayerServiceProps) {
        val intent = Intent(activity.application, PlayerService::class.java).apply {
            putExtra(PLAYER_SERVICE_INTENT_BUNDLE_PROPS_KEY, Json.encodeToString(serviceProps))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.application.startForegroundService(intent)
        } else {
            activity.application.startService(intent)
        }
    }

    fun stopPlayerService() {
    }


    val areControlsVisible = MutableStateFlow(false)
    fun setAreControlsVisible(state: Boolean) = runBlocking {
        areControlsVisible.emit(state)
    }

    companion object {
    }
}