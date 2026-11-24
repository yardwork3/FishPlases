package ru.fishplaces.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.fishplaces.R

data class AuthState(val checking: Boolean = true, val account: Account? = null,
    val busy: Boolean = false, val error: Int? = null)
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val mutable = MutableStateFlow(AuthState())
    val state = mutable.asStateFlow()
    init { viewModelScope.launch {
        repository.accounts.collect { account -> mutable.update { it.copy(checking = false, account = account, error = null) } }
    } }
    fun submit(email: String, password: String, confirmation: String, register: Boolean) {
        if (state.value.busy) return
        val clean = email.trim()
        val error = when {
            !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(clean) -> R.string.auth_email_error
            password.length < 6 -> R.string.auth_password_error
            register && password != confirmation -> R.string.auth_confirmation_error
            else -> null
        }
        mutable.update { it.copy(error = error) }
        if (error != null) return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { if (register) repository.register(clean, password) else repository.login(clean, password)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { mutable.update { it.copy(error = when {
                e is FirebaseNetworkException -> R.string.auth_network_error
                e is FirebaseAuthException && e.errorCode == "ERROR_EMAIL_ALREADY_IN_USE" -> R.string.auth_email_used
                e is FirebaseAuthException && e.errorCode == "ERROR_TOO_MANY_REQUESTS" -> R.string.auth_too_many
                else -> R.string.auth_failed
            }) } } finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun signOut() { if (!state.value.busy) repository.signOut() }
}
