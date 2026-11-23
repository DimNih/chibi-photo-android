package com.chibi.photo.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.*
import com.chibi.photo.physics.MaterialPreset
import com.chibi.photo.physics.ZoneTemplate

enum class AppScreen {
    ARCHIVE,
    WORKBENCH
}

@Composable
fun MochiMakerApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.ARCHIVE) }
    var activeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var activeTitle by remember { mutableStateOf("Astrid") }
    var activeMaterial by remember { mutableStateOf(MaterialPreset.SOFT_GEL) }
    var activeZone by remember { mutableStateOf(ZoneTemplate.DUAL_CHEEKS) }

    when (currentScreen) {
        AppScreen.ARCHIVE -> {
            InputScreen(
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
                    onBackToStudio = { currentScreen = AppScreen.ARCHIVE }
                )
            }
        }
    }
}
