package io.rewynd.android.component

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.constraintlayout.compose.ConstraintLayout
import io.rewynd.model.LoginRequest
import io.rewynd.android.MainViewModel

@Composable
fun LoginInput(
    mainViewModel: MainViewModel
) = ConstraintLayout {
    val (usernameRef, passwordRef, buttonRef) = createRefs()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    TextField(
        label = { Text(text = "username") },
        value = username,
        onValueChange = { username = it },
        modifier = Modifier.constrainAs(usernameRef) {
            top.linkTo(parent.top)
            bottom.linkTo(passwordRef.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        })
    TextField(
        label = { Text(text = "password") },
        value = password,
        onValueChange = { password = it },
        modifier = Modifier.constrainAs(passwordRef) {
            top.linkTo(usernameRef.bottom)
            bottom.linkTo(buttonRef.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        })
    Button({
        mainViewModel.login(LoginRequest(username = username, password = password))
    }, content = {
        Text("Connect")
    }, modifier = Modifier.constrainAs(buttonRef) {
        top.linkTo(passwordRef.bottom)
        bottom.linkTo(parent.bottom)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    })
}