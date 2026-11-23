package com.chibi.photo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chibi.photo.ui.screens.MochiMakerApp
import com.chibi.photo.ui.theme.ChibiPhotoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChibiPhotoTheme {
                MochiMakerApp()
            }
        }
    }
}
