package com.sliide.usermanagement.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sliide.usermanagement.domain.model.Gender
import com.sliide.usermanagement.domain.model.User
import com.sliide.usermanagement.domain.repository.UserRepository
import com.sliide.usermanagement.domain.usecase.ValidationUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val UNDO_WINDOW_MS = 5000L

data class UserUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val pendingDelete: PendingDelete? = null,
    val addUserSheet: AddUserSheetState = AddUserSheetState(),
    val selectedUser: User? = null,
    val latestAddedUserId: Long? = null
)

data class PendingDelete(
    val user: User,
    val undoAvailable: Boolean = true
)

data class AddUserSheetState(
    val isVisible: Boolean = false,
    val name: String = "",
    val email: String = "",
    val gender: Gender = Gender.MALE,
    val nameError: String? = null,
    val emailError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null
) {
    val isFormValid: Boolean get() = ValidationUseCase.isFormValid(name, email)
}

sealed class UserEvent {
    object Refresh : UserEvent()
    data class SelectUser(val user: User) : UserEvent()
    object ClearSelection : UserEvent()

    // Add user sheet
    object ShowAddUser : UserEvent()
    object HideAddUser : UserEvent()
    data class NameChanged(val value: String) : UserEvent()
    data class EmailChanged(val value: String) : UserEvent()
    data class GenderChanged(val value: Gender) : UserEvent()
    object SubmitAddUser : UserEvent()

    // Delete
    data class RequestDelete(val user: User) : UserEvent()
    object UndoDelete : UserEvent()
    object DismissDeleteSnackbar : UserEvent()

    // Error
    object DismissError : UserEvent()
}

class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState(isLoading = true))
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    private var deleteJob: Job? = null

    init {
        observeUsers()
        refresh()
    }

    private fun observeUsers() {
        viewModelScope.launch {
            repository.getUsers().collect { users ->
                _uiState.update { it.copy(users = users, isLoading = false) }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            val result = repository.refreshUsers()
            _uiState.update { state ->
                state.copy(
                    isRefreshing = false,
                    error = if (result.isFailure) resolveError(result.exceptionOrNull()) else null
                )
            }
        }
    }

    fun onEvent(event: UserEvent) {
        when (event) {
            is UserEvent.Refresh -> refresh()
            is UserEvent.SelectUser -> _uiState.update { it.copy(selectedUser = event.user) }
            is UserEvent.ClearSelection -> _uiState.update { it.copy(selectedUser = null) }

            is UserEvent.ShowAddUser -> _uiState.update {
                it.copy(addUserSheet = AddUserSheetState(isVisible = true))
            }
            is UserEvent.HideAddUser -> _uiState.update {
                it.copy(addUserSheet = AddUserSheetState(isVisible = false))
            }
            is UserEvent.NameChanged -> {
                val error = when (val r = ValidationUseCase.validateName(event.value)) {
                    is ValidationUseCase.ValidationResult.Invalid -> r.reason
                    else -> null
                }
                _uiState.update { it.copy(addUserSheet = it.addUserSheet.copy(name = event.value, nameError = error)) }
            }
            is UserEvent.EmailChanged -> {
                val error = when (val r = ValidationUseCase.validateEmail(event.value)) {
                    is ValidationUseCase.ValidationResult.Invalid -> r.reason
                    else -> null
                }
                _uiState.update { it.copy(addUserSheet = it.addUserSheet.copy(email = event.value, emailError = error)) }
            }
            is UserEvent.GenderChanged -> _uiState.update {
                it.copy(addUserSheet = it.addUserSheet.copy(gender = event.value))
            }
            is UserEvent.SubmitAddUser -> submitAddUser()

            is UserEvent.RequestDelete -> requestDelete(event.user)
            is UserEvent.UndoDelete -> undoDelete()
            is UserEvent.DismissDeleteSnackbar -> commitDelete()

            is UserEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun submitAddUser() {
        val sheet = _uiState.value.addUserSheet
        if (!sheet.isFormValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(addUserSheet = it.addUserSheet.copy(isSubmitting = true, submitError = null)) }
            val result = repository.createUser(
                name = sheet.name.trim(),
                email = sheet.email.trim(),
                gender = sheet.gender
            )
            if (result.isSuccess) {
                val user = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        addUserSheet = AddUserSheetState(isVisible = false),
                        latestAddedUserId = user.id
                    )
                }
                viewModelScope.launch { //clear id after blink animation
                    delay(1500)
                    _uiState.update { it.copy(latestAddedUserId = null) }
                }
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to create user"
                _uiState.update {
                    it.copy(addUserSheet = it.addUserSheet.copy(isSubmitting = false, submitError = msg))
                }
            }
        }
    }

    private fun requestDelete(user: User) {
        // Cancel any in-progress delete
        deleteJob?.cancel()
        commitDeleteIfPending()

        // Optimistic: remove from cache immediately
        viewModelScope.launch {
            repository.deleteUserLocally(user.id)
            _uiState.update { it.copy(pendingDelete = PendingDelete(user = user)) }

            // Auto-commit after undo window
            deleteJob = viewModelScope.launch {
                delay(UNDO_WINDOW_MS)
                commitDelete()
            }
        }
    }

    private fun undoDelete() {
        deleteJob?.cancel()
        deleteJob = null
        val pending = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            repository.restoreUser(pending.user)
            _uiState.update { it.copy(pendingDelete = null) }
        }
    }

    private fun commitDelete() {
        deleteJob?.cancel()
        deleteJob = null
        val pending = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            repository.confirmDeleteUser(pending.user.id)
        }
    }

    private fun commitDeleteIfPending() {
        _uiState.value.pendingDelete?.let {
            viewModelScope.launch { repository.confirmDeleteUser(it.user.id) }
            _uiState.update { state -> state.copy(pendingDelete = null) }
        }
    }

    private fun resolveError(e: Throwable?): String {
        val msg = e?.message?.lowercase() ?: ""
        return when {
            msg.contains("unable to resolve host") ||
            msg.contains("no address") ||
            msg.contains("network") ||
            msg.contains("connect") -> "No internet connection"
            msg.contains("timeout") -> "Request timed out"
            msg.contains("401") || msg.contains("unauthorized") -> "Unauthorized — check API token"
            else -> "Something went wrong. Pull to refresh."
        }
    }

    override fun onCleared() {
        super.onCleared()
        commitDeleteIfPending()
    }
}
