package com.sliide.usermanagement

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.window.ComposeUIViewController
import com.sliide.usermanagement.ui.UserScreen
import com.sliide.usermanagement.ui.theme.UserManagementTheme
import platform.UIKit.UIViewController

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun MainViewController(): UIViewController = ComposeUIViewController {
    UserManagementTheme {
        val windowSizeClass = calculateWindowSizeClass()
        UserScreen(windowSizeClass = windowSizeClass)
    }
}
