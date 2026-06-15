package com.example.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
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
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.detector.DetectorViewModel
import com.example.ui.detector.ModelState
import com.example.ui.theme.*

@Composable
fun SplashScreen(
    viewModel: DetectorViewModel,
    onReadyToProceed: () -> Unit
) {
    val modelState by viewModel.modelState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    
    // Aesthetic animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "rotate"
    )

    // Handle auto-routing once model is ready
    LaunchedEffect(modelState) {
        if (modelState is ModelState.Ready) {
            onReadyToProceed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(DeepSpace, CosmicBlack),
                    radius = 1800f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Animated Cyber Logo Marker
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp)
            ) {
                // Outer rotating ring
                Canvas(modifier = Modifier.fillMaxSize().rotate(rotateAngle)) {
                    drawCircle(
                        brush = Brush.sweepGradient(listOf(CyberCyan, ElectricMagenta, CyberCyan)),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Inner pulsing glowing core
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(24.dp, shape = RoundedCornerShape(100.dp), spotColor = CyberCyan, ambientColor = CyberCyan)
                        .background(
                            Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.2f), ElectricPurple.copy(alpha = 0.1f))),
                            shape = RoundedCornerShape(100.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(50.dp * pulseScale)) {
                        drawCircle(
                            color = CyberCyan,
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawCircle(
                            color = CyberCyan.copy(alpha = 0.3f),
                            radius = 15.dp.toPx()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "GESTURE VISION",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Neural-Network Edge Hand Tracking",
                fontSize = 14.sp,
                color = TextSecondary,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Stateful Loader card
            AnimatedContent(
                targetState = modelState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
                label = "loader_state"
            ) { state ->
                when (state) {
                    ModelState.Checking -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = CyberCyan,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Initializing Edge Engine...",
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    ModelState.NeedsDownload -> {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            glowColor = CyberCyan.copy(alpha = 0.15f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Model",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "AI Model Setup Required",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "To count fingers locally in real-time, the app needs Google's Hand Landmarker model (~5.6 MB). No personal data leaves the device.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                GlassButton(
                                    onClick = { viewModel.startModelDownload() },
                                    borderColor = CyberCyan,
                                    glowColor = CyberCyan.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "DOWNLOAD MODEL",
                                            color = CyberCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            letterSpacing = 1.sp
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

                    ModelState.Downloading -> {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            glowColor = ElectricMagenta.copy(alpha = 0.15f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = "INSTALLING CORE MODEL",
                                    color = ElectricMagenta,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Syncing Neural Network weights...",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(20.dp))

                                // Custom Neon Progress Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(Color(0x1AFFFFFF), RoundedCornerShape(4.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = downloadProgress.coerceIn(0f, 1f))
                                            .background(
                                                Brush.horizontalGradient(listOf(CyberCyan, ElectricMagenta)),
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}% COMPLETED",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    is ModelState.Ready -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = NeonGreen,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Engine ready! Calibrating lens...",
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    is ModelState.Error -> {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            glowColor = WarmCoral.copy(alpha = 0.2f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = WarmCoral,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Initialization Fault",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = (state as ModelState.Error).message,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                GlassButton(
                                    onClick = { viewModel.startModelDownload() },
                                    borderColor = WarmCoral,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("RETRY SYNC", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
