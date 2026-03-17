package com.sliide.usermanagement.ui

import app.cash.turbine.test
import com.sliide.usermanagement.domain.model.Gender
import com.sliide.usermanagement.domain.model.User
import com.sliide.usermanagement.domain.model.UserStatus
import com.sliide.usermanagement.fake.FakeUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeUserRepository
    private lateinit var viewModel: UserViewModel

    private val sampleUser = User(
        id = 42L,
        name = "Test User",
        email = "test@example.com",
        gender = Gender.MALE,
        status = UserStatus.ACTIVE,
        createdAt = Clock.System.now().toString()
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeUserRepository()
        viewModel = UserViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Loading state ──────────────────────────────────────────────────────────

    @Test
    fun `initial state is loading`() = runTest {
        val fresh = UserViewModel(FakeUserRepository())
        assertTrue(fresh.uiState.value.isLoading)
    }

    @Test
    fun `after refresh loading becomes false and users are populated`() = runTest {
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()

            // Skip intermediate states until we reach the final loaded state
            var state = awaitItem()
            while (state.isLoading || state.users.isEmpty()) {
                state = awaitItem()
            }

            assertFalse(state.isLoading)
            assertTrue(state.users.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Add user ───────────────────────────────────────────────────────────────

    @Test
    fun `ShowAddUser event opens bottom sheet`() = runTest {
        viewModel.onEvent(UserEvent.ShowAddUser)
        assertTrue(viewModel.uiState.value.addUserSheet.isVisible)
    }

    @Test
    fun `HideAddUser event closes bottom sheet`() = runTest {
        viewModel.onEvent(UserEvent.ShowAddUser)
        viewModel.onEvent(UserEvent.HideAddUser)
        assertFalse(viewModel.uiState.value.addUserSheet.isVisible)
    }

    @Test
    fun `NameChanged updates name and validates`() = runTest {
        viewModel.onEvent(UserEvent.NameChanged("A"))  // too short
        val sheet = viewModel.uiState.value.addUserSheet
        assertEquals("A", sheet.name)
        assertNotNull(sheet.nameError)
    }

    @Test
    fun `valid name clears nameError`() = runTest {
        viewModel.onEvent(UserEvent.NameChanged("Alice Smith"))
        assertNull(viewModel.uiState.value.addUserSheet.nameError)
    }

    @Test
    fun `EmailChanged validates email format`() = runTest {
        viewModel.onEvent(UserEvent.EmailChanged("not-an-email"))
        assertNotNull(viewModel.uiState.value.addUserSheet.emailError)
    }

    @Test
    fun `valid email clears emailError`() = runTest {
        viewModel.onEvent(UserEvent.EmailChanged("user@example.com"))
        assertNull(viewModel.uiState.value.addUserSheet.emailError)
    }

    @Test
    fun `submit with valid data closes sheet and adds user`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val initialCount = viewModel.uiState.value.users.size

        viewModel.onEvent(UserEvent.ShowAddUser)
        viewModel.onEvent(UserEvent.NameChanged("New Person"))
        viewModel.onEvent(UserEvent.EmailChanged("new@example.com"))
        viewModel.onEvent(UserEvent.SubmitAddUser)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.addUserSheet.isVisible)
        assertEquals(initialCount + 1, viewModel.uiState.value.users.size)
    }

    @Test
    fun `submit failure shows error in sheet`() = runTest {
        repository.createResult = Result.failure(Exception("Email already taken"))
        viewModel.onEvent(UserEvent.ShowAddUser)
        viewModel.onEvent(UserEvent.NameChanged("New Person"))
        viewModel.onEvent(UserEvent.EmailChanged("new@example.com"))
        viewModel.onEvent(UserEvent.SubmitAddUser)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.addUserSheet.isVisible)
        assertNotNull(viewModel.uiState.value.addUserSheet.submitError)
    }

    // ── Delete & Undo ──────────────────────────────────────────────────────────

    @Test
    fun `RequestDelete removes user immediately and sets pendingDelete`() = runTest {
        repository.seedUsers(listOf(sampleUser))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(UserEvent.RequestDelete(sampleUser))
        testDispatcher.scheduler.advanceTimeBy(100)

        assertFalse(viewModel.uiState.value.users.any { it.id == sampleUser.id })
        assertNotNull(viewModel.uiState.value.pendingDelete)
    }

    @Test
    fun `UndoDelete restores user and clears pendingDelete`() = runTest {
        repository.seedUsers(listOf(sampleUser))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(UserEvent.RequestDelete(sampleUser))
        testDispatcher.scheduler.advanceTimeBy(100)
        viewModel.onEvent(UserEvent.UndoDelete)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.users.any { it.id == sampleUser.id })
        assertNull(viewModel.uiState.value.pendingDelete)
    }

    @Test
    fun `DismissDeleteSnackbar commits delete to API`() = runTest {
        repository.seedUsers(listOf(sampleUser))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(UserEvent.RequestDelete(sampleUser))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(UserEvent.DismissDeleteSnackbar)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.deleteConfirmCallCount)
        assertNull(viewModel.uiState.value.pendingDelete)
    }

    @Test
    fun `undo does not call API delete`() = runTest {
        repository.seedUsers(listOf(sampleUser))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(UserEvent.RequestDelete(sampleUser))
        testDispatcher.scheduler.advanceTimeBy(100) // advance enough for delete to process but not past undo window

        viewModel.onEvent(UserEvent.UndoDelete)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, repository.deleteConfirmCallCount)
    }

    // ── Error handling ─────────────────────────────────────────────────────────

    @Test
    fun `network error on refresh sets error message`() = runTest {
        repository.refreshResult = Result.failure(Exception("unable to resolve host"))
        val vm = UserViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.error!!.contains("internet", ignoreCase = true))
    }

    @Test
    fun `DismissError clears error`() = runTest {
        repository.refreshResult = Result.failure(Exception("network error"))
        val vm = UserViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onEvent(UserEvent.DismissError)
        assertNull(vm.uiState.value.error)
    }

    // ── Selection ──────────────────────────────────────────────────────────────

    @Test
    fun `SelectUser updates selectedUser`() = runTest {
        viewModel.onEvent(UserEvent.SelectUser(sampleUser))
        assertEquals(sampleUser, viewModel.uiState.value.selectedUser)
    }

    @Test
    fun `ClearSelection clears selectedUser`() = runTest {
        viewModel.onEvent(UserEvent.SelectUser(sampleUser))
        viewModel.onEvent(UserEvent.ClearSelection)
        assertNull(viewModel.uiState.value.selectedUser)
    }
}
