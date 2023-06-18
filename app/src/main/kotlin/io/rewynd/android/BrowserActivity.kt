package io.rewynd.android

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import io.rewynd.android.component.browser.BrowserRouter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BrowserActivity : AppCompatActivity() {
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(application) }
    private val viewModel by lazy {
        BrowserViewModel(
            application, ServerUrl(requireNotNull(
                prefs.getString(
                    MainViewModel.SERVER_URL, null
                )
            ) { "Missing Server Url preference!" })
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(
            BROWSER_SAVED_INSTANCE_STATE,
            Json.encodeToString(viewModel.browserState.value)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback {
            viewModel.popBrowserState()
        }

        val stateStr = savedInstanceState?.getString(BROWSER_SAVED_INSTANCE_STATE)

        if (stateStr != null) {
            viewModel.initBrowserState(Json.decodeFromString(stateStr))
        }

        if (viewModel.browserState.value.isEmpty()) {
            viewModel.putBrowserState(BrowserState.HomeState)
        }

        setContent {
            BrowserRouter(viewModel)
        }
    }


    companion object {
        const val BROWSER_SAVED_INSTANCE_STATE = "BrowserSavedInstanceState"
    }
}



