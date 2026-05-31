package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.WaterTrackerScreen
import com.example.ui.WaterViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: WaterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeType by viewModel.appTheme.collectAsState()
            val oledModeEnabled by viewModel.oledModeEnabled.collectAsState()
            val paletteIndex by viewModel.appThemePaletteIndex.collectAsState()
            val wallpaperColors by viewModel.wallpaperThemeColors.collectAsState()
            MyApplicationTheme(
                themeType = themeType,
                oledModeEnabled = oledModeEnabled,
                paletteIndex = paletteIndex,
                wallpaperColors = wallpaperColors
            ) {
                WaterTrackerScreen(viewModel = viewModel)
            }
        }
    }
}
