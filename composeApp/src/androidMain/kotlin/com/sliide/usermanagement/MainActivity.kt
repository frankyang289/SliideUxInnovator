package com.sliide.usermanagement

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.sliide.usermanagement.di.androidModule
import com.sliide.usermanagement.di.commonModules
import com.sliide.usermanagement.ui.UserScreen
import com.sliide.usermanagement.ui.theme.UserManagementTheme
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UserManagementTheme {
                val windowSizeClass = calculateWindowSizeClass()
                UserScreen(windowSizeClass = windowSizeClass)
            }
        }
    }
}
