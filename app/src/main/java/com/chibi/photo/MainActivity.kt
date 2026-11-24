package com.chibi.photo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.chibi.photo.ui.AppLanguage
import com.chibi.photo.ui.screens.MochiMakerApp
import com.chibi.photo.ui.theme.ChibiPhotoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            var currentLanguage by remember { mutableStateOf(AppLanguage.ID) }

            ChibiPhotoTheme(darkTheme = isDarkMode) {
                MochiMakerApp(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode },
                    currentLanguage = currentLanguage,
                    onToggleLanguage = {
                        currentLanguage = if (currentLanguage == AppLanguage.ID) AppLanguage.EN else AppLanguage.ID
                    }
                )
            }
        }
    }
}
