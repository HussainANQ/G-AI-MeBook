package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MeasurementsViewModel
import com.example.ui.viewmodel.MeasurementsViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MeasurementsApp
        val factory = MeasurementsViewModelFactory(app.repository, this)
        val viewModel: MeasurementsViewModel by viewModels { factory }

        setContent {
            val themeSetting by viewModel.themeSetting.collectAsStateWithLifecycle()
            val darkTheme = when (themeSetting) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = darkTheme) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
