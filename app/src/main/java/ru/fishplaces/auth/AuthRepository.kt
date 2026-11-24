package ru.fishplaces.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Account(val uid: String, val email: String)
interface AuthRepository {
    val accounts: Flow<Account?>
    suspend fun login(email: String, password: String)
    suspend fun register(email: String, password: String)
    fun signOut()
}
class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {
    override val accounts = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { source ->
            trySend(source.currentUser?.let { Account(it.uid, it.email.orEmpty()) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    override suspend fun login(email: String, password: String) { auth.signInWithEmailAndPassword(email, password).await() }
    override suspend fun register(email: String, password: String) { auth.createUserWithEmailAndPassword(email, password).await() }
    override fun signOut() = auth.signOut()
}
