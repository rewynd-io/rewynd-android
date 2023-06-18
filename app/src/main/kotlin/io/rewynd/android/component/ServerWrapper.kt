package io.rewynd.android.component

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.constraintlayout.compose.ConstraintLayout
import io.rewynd.android.LoginState
import io.rewynd.android.MainViewModel
import io.rewynd.android.ServerUrl

@Composable
fun ServerWrapper(state: LoginState.ServerSelect, mainViewModel: MainViewModel) = ConstraintLayout {
    val (serverRef, buttonRef) = createRefs()
    val serverUrlState = mainViewModel.loginState.collectAsState()
    val server = serverUrlState.value.serverUrl.value
    TextField(
        label = { Text(text = "Server URL") },
        value = server,
        onValueChange = { mainViewModel.loginState.value = state.copy(serverUrl = ServerUrl(it)) },
        modifier = Modifier.constrainAs(serverRef) {
            top.linkTo(parent.top)
            bottom.linkTo(buttonRef.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        })
    Button({
        mainViewModel.loginState.value = LoginState.LoggedOut(serverUrlState.value.serverUrl)
    }, content = {
        Text("Connect")
    }, modifier = Modifier.constrainAs(buttonRef) {
        top.linkTo(serverRef.bottom)
        bottom.linkTo(parent.bottom)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    })
}