package com.example.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.detector.DetectorScreen
import com.example.ui.detector.DetectorViewModel
import com.example.ui.onboarding.PermissionScreen
import com.example.ui.splash.SplashScreen

@Composable
fun AppNavigation(viewModel: DetectorViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                viewModel = viewModel,
                onReadyToProceed = {
                    val hasCam = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (hasCam) {
                        navController.navigate("detector") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("permission") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("permission") {
            PermissionScreen(
                onPermissionGranted = {
                    navController.navigate("detector") {
                        popUpTo("permission") { inclusive = true }
                    }
                }
            )
        }

        composable("detector") {
            DetectorScreen(
                viewModel = viewModel,
                onBackToSplash = {
                    navController.navigate("splash") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
