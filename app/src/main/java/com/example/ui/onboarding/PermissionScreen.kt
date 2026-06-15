package com.example.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    
    // Permission state tracker
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Launch permission request
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            onPermissionGranted()
        }
    }

    if (hasCameraPermission) {
        LaunchedEffect(Unit) {
            onPermissionGranted()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(DeepSpace, CosmicBlack),
                    radius = 2000f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .navigationBarsPadding()
                .statusBarsPadding()
        ) {
            // Top Section (Title)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(
                    text = "CAMERA ACCESS REQUIRED",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Grant credentials to initialize real-time visual tracking",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Middle Section (Floating interactive graphic and points)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = CyberCyan.copy(alpha = 0.15f),
                            radius = (70.dp * pulseSize).toPx()
                        )
                        drawCircle(
                            color = CyberCyan.copy(alpha = 0.05f),
                            radius = (80.dp * pulseSize).toPx()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .shadow(24.dp, shape = RoundedCornerShape(100.dp), spotColor = CyberCyan, ambientColor = CyberCyan)
                            .background(
                                Brush.linearGradient(listOf(CyberCyan, ElectricPurple)),
                                shape = RoundedCornerShape(100.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Camera",
                            tint = CosmicBlack,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Explanations
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowColor = ElectricPurple.copy(alpha = 0.1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Privacy",
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp).padding(2.dp)
                            )
                            Column {
                                Text("Offline Computation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Video frames are processed in-memory locally. No footage is stored or transmitted.", color = TextSecondary, fontSize = 12.sp)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Speed",
                                tint = ElectricMagenta,
                                modifier = Modifier.size(24.dp).padding(2.dp)
                            )
                            Column {
                                Text("Ultra-low Latency Pipeline", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Optimized using MediaPipe Tasks Vision architecture for maximum battery and frame rate efficiency.", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Bottom Section (Button)
            GlassButton(
                onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                borderColor = CyberCyan,
                glowColor = CyberCyan.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "GRANT PERMISSION",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = CyberCyan
                    )
                }
            }
        }
    }
}
