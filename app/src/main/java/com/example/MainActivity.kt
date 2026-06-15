package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable Edge-to-Edge full screen bleed rendering
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                // Initialize modern ViewModels in lifecycle-scoped containers
                val detectorViewModel: com.example.ui.detector.DetectorViewModel = viewModel()
                
                // Master visual directory
                AppNavigation(viewModel = detectorViewModel)
            }
        }
    }
}
