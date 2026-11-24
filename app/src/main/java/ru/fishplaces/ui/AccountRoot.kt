package ru.fishplaces.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.fishplaces.FishingApplication
import ru.fishplaces.R
import ru.fishplaces.auth.AuthViewModel
import ru.fishplaces.data.SpotRepository

private inline fun <reified T : ViewModel> model(owner: ViewModelStoreOwner, crossinline create: () -> T): T =
    ViewModelProvider(owner, object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
    })[T::class.java]

@Composable
fun AccountRoot(app: FishingApplication) {
    MaterialTheme {
        if (!app.firebaseReady) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.firebase_setup))
            }
            return@MaterialTheme
        }
        val owner = androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current!!
        val auth = remember(owner) { model(owner) { AuthViewModel(app.authRepository) } }
        val state by auth.state.collectAsStateWithLifecycle()
        when {
            state.checking -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.account == null -> AuthScreen(auth)
            else -> key(state.account!!.uid) {
                // Destroy the entire account scope on logout, including forms, dialogs and jobs.
                val scope = remember { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
                DisposableEffect(scope) { onDispose { scope.viewModelStore.clear() } }
                val spots = remember(scope) { model(scope) { SpotsViewModel(SpotRepository(app.cloudSpots, state.account!!.uid)) } }
                FishPlacesApp(spots, state.account!!.email, auth::signOut)
            }
        }
    }
}

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(if (register) R.string.auth_register else R.string.auth_login), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.auth_email)) }, enabled = !state.busy, singleLine = true)
        OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.auth_password)) },
            visualTransformation = PasswordVisualTransformation(), enabled = !state.busy, singleLine = true)
        if (register) OutlinedTextField(confirmation, { confirmation = it }, label = { Text(stringResource(R.string.auth_confirmation)) },
            visualTransformation = PasswordVisualTransformation(), enabled = !state.busy, singleLine = true)
        state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        if (state.busy) LinearProgressIndicator()
        Button(enabled = !state.busy, onClick = { viewModel.submit(email, password, confirmation, register) }) {
            Text(stringResource(if (register) R.string.auth_register else R.string.auth_login))
        }
        TextButton(enabled = !state.busy, onClick = { register = !register; password = ""; confirmation = "" }) {
            Text(stringResource(if (register) R.string.auth_login else R.string.auth_register))
        }
    }
}
