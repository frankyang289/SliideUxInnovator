package com.sliide.usermanagement.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sliide.usermanagement.domain.model.User
import com.sliide.usermanagement.ui.components.AddUserBottomSheet
import com.sliide.usermanagement.ui.components.DeleteConfirmDialog
import com.sliide.usermanagement.ui.components.ErrorState
import com.sliide.usermanagement.ui.components.ShimmerList
import com.sliide.usermanagement.ui.components.UserCard
import com.sliide.usermanagement.ui.components.UserDetailPanel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun UserScreen(
    windowSizeClass: WindowSizeClass,
    viewModel: UserViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var userToDelete by remember { mutableStateOf<User?>(null) }

    val isExpandedWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    // Snackbar for undo-delete
    LaunchedEffect(uiState.pendingDelete) {
        val pending = uiState.pendingDelete ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "${pending.user.name} deleted",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        when (result) {
            SnackbarResult.ActionPerformed -> viewModel.onEvent(UserEvent.UndoDelete)
            SnackbarResult.Dismissed -> viewModel.onEvent(UserEvent.DismissDeleteSnackbar)
        }
    }

    // Error snackbar
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            snackbarHostState.showSnackbar(
                message = uiState.error!!,
                duration = SnackbarDuration.Long
            )
            viewModel.onEvent(UserEvent.DismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Users") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { viewModel.onEvent(UserEvent.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(UserEvent.ShowAddUser) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add User")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        if (isExpandedWidth) {
            // Tablet / Landscape: master-detail layout
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                UserList(
                    uiState = uiState,
                    selectedUser = uiState.selectedUser,
                    onUserClick = { viewModel.onEvent(UserEvent.SelectUser(it)) },
                    onLongPress = { userToDelete = it },
                    onRefresh = { viewModel.onEvent(UserEvent.Refresh) },
                    modifier = Modifier.weight(1f)
                )
                UserDetailPanel(
                    user = uiState.selectedUser,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Portrait: single list
            UserList(
                uiState = uiState,
                selectedUser = null,
                onUserClick = { viewModel.onEvent(UserEvent.SelectUser(it)) },
                onLongPress = { userToDelete = it },
                onRefresh = { viewModel.onEvent(UserEvent.Refresh) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }

    // Delete confirmation dialog
    userToDelete?.let { user ->
        DeleteConfirmDialog(
            user = user,
            onConfirm = {
                viewModel.onEvent(UserEvent.RequestDelete(user))
                userToDelete = null
            },
            onDismiss = { userToDelete = null }
        )
    }

    // Add user bottom sheet
    if (uiState.addUserSheet.isVisible) {
        AddUserBottomSheet(
            state = uiState.addUserSheet,
            sheetState = sheetState,
            onEvent = viewModel::onEvent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserList(
    uiState: UserUiState,
    selectedUser: User?,
    onUserClick: (User) -> Unit,
    onLongPress: (User) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val userCount = uiState.users.size

    LaunchedEffect(userCount) {
        if (userCount > 0) {
            listState.animateScrollToItem(0)
        }
    }
    when {
        uiState.isLoading -> {
            ShimmerList(modifier = modifier)
        }
        uiState.users.isEmpty() && !uiState.isRefreshing -> {
            ErrorState(
                message = "No users found",
                modifier = modifier
            )
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.users,
                        key = { it.id }
                    ) { user ->
                        AnimatedVisibility(
                            visible = true,
                            exit = shrinkVertically(tween(300)) + fadeOut(tween(300))
                        ) {
                            UserCard(
                                user = user,
                                isNew = uiState.latestAddedUserId == user.id,
                                isSelected = selectedUser?.id == user.id,
                                onClick = { onUserClick(user) },
                                onLongClick = { onLongPress(user) }
                            )
                        }
                    }
                }
            }
        }
    }
}
