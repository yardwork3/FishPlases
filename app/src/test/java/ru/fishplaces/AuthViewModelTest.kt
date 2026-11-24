package ru.fishplaces

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import ru.fishplaces.auth.*

class FakeAuth : AuthRepository {
    override val accounts = MutableStateFlow<Account?>(null)
    var calls = 0
    var fail = false
    override suspend fun login(email: String, password: String) {
        calls++; delay(100); check(!fail); accounts.value = Account("one", email)
    }
    override suspend fun register(email: String, password: String) = login(email, password)
    override fun signOut() { accounts.value = null }
}
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuth
    private lateinit var vm: AuthViewModel
    @Before fun setup() { Dispatchers.setMain(dispatcher); repository = FakeAuth(); vm = AuthViewModel(repository) }
    @After fun cleanup() { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    @Test fun validationBlocksRequests() = runTest(dispatcher) {
        vm.submit("", "123456", "", false)
        assertEquals(R.string.auth_email_error, vm.state.value.error)
        vm.submit("a@b.ru", "123", "", false)
        assertEquals(R.string.auth_password_error, vm.state.value.error)
        vm.submit("a@b.ru", "123456", "654321", true)
        assertEquals(R.string.auth_confirmation_error, vm.state.value.error)
        advanceUntilIdle(); assertEquals(0, repository.calls)
    }
    @Test fun busyBlocksDoubleClickAndSessionCanEnd() = runTest(dispatcher) {
        repeat(10) { vm.submit("a@b.ru", "123456", "", false) }
        assertTrue(vm.state.value.busy)
        advanceUntilIdle(); assertEquals(1, repository.calls)
        assertEquals("one", vm.state.value.account?.uid)
        vm.signOut(); advanceUntilIdle(); assertNull(vm.state.value.account)
    }
    @Test fun errorAllowsRetry() = runTest(dispatcher) {
        repository.fail = true
        vm.submit("a@b.ru", "123456", "123456", true); advanceUntilIdle()
        assertFalse(vm.state.value.busy); assertNotNull(vm.state.value.error)
        repository.fail = false
        vm.submit("a@b.ru", "123456", "123456", true); advanceUntilIdle()
        assertNotNull(vm.state.value.account)
    }
}
