package com.chibi.photo.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.*
import com.chibi.photo.physics.MaterialPreset
import com.chibi.photo.physics.ZoneTemplate
import com.chibi.photo.ui.AppLanguage

enum class AppScreen {
    ARCHIVE,
    WORKBENCH
}

@Composable
fun MochiMakerApp(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(AppScreen.ARCHIVE) }
    var activeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var activeTitle by remember { mutableStateOf("Astrid") }
    var activeMaterial by remember { mutableStateOf(MaterialPreset.SOFT_GEL) }
    var activeZone by remember { mutableStateOf(ZoneTemplate.DUAL_CHEEKS) }

    when (currentScreen) {
        AppScreen.ARCHIVE -> {
            InputScreen(
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                currentLanguage = currentLanguage,
                onToggleLanguage = onToggleLanguage,
                onOpenWorkbench = { bmp, title, material, zone ->
                    activeBitmap = bmp
                    activeTitle = title
                    activeMaterial = material
                    activeZone = zone
                    currentScreen = AppScreen.WORKBENCH
                }
            )
        }
        AppScreen.WORKBENCH -> {
            activeBitmap?.let { bmp ->
                MochiPlayScreen(
                    sourceBitmap = bmp,
                    specimenTitle = activeTitle,
                    initialMaterial = activeMaterial,
                    initialZone = activeZone,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                    currentLanguage = currentLanguage,
                    onToggleLanguage = onToggleLanguage,
                    onBackToStudio = { currentScreen = AppScreen.ARCHIVE }
                )
            }
        }
    }
}
