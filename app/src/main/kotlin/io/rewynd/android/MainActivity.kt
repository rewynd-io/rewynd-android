package io.rewynd.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.rewynd.android.component.LoginInput
import io.rewynd.android.component.ServerWrapper


class MainActivity : AppCompatActivity() {

    private val viewModel by lazy {
        MainViewModel(this.application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val loginState by viewModel.loginState.collectAsState()
            when (val state = loginState) {
                is LoginState.ServerSelect -> ServerWrapper(state, viewModel)
                is LoginState.LoggedIn -> {
                    startActivity(Intent(this, BrowserActivity::class.java))
                }
                is LoginState.LoggedOut -> {
                    viewModel.verify() // Check if we are already logged in
                }
                is LoginState.LoggedOutVerificationFailed -> {
                    LoginInput(viewModel)
                }
                is LoginState.PendingLogin,
                is LoginState.PendingVerification -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    companion object
}



