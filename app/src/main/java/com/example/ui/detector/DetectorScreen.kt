package com.example.ui.detector

import android.Manifest
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.detector.HandGestureAnalyzer
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.sin

// Simple inline particle class representing futuristic glowing shapes
data class Particle(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    var color: Color,
    var size: Float
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DetectorScreen(
    viewModel: DetectorViewModel,
    onBackToSplash: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val modelState by viewModel.modelState.collectAsState()
    
    // UI reactive states from VM
    val detectedFingers by viewModel.detectedFingers.collectAsState()
    val latencyMs by viewModel.processingTimeMs.collectAsState()
    val fpsVal by viewModel.fps.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()

    // Temporary list holder for real-time skeletal layout to draw on overlay Canvas
    var activeLandmarks by remember { mutableStateOf<List<List<NormalizedLandmark>>?>(null) }
    
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var analyzerRef by remember { mutableStateOf<HandGestureAnalyzer?>(null) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderRef?.unbindAll()
            } catch (e: Exception) {
                Log.e("DetectorScreen", "Failed to unbind camera", e)
            }
            try {
                analyzerRef?.close()
            } catch (e: Exception) {
                Log.e("DetectorScreen", "Failed to close analyzer", e)
            }
            try {
                analysisExecutor.shutdown()
            } catch (e: Exception) {
                Log.e("DetectorScreen", "Failed to shutdown analysis executor", e)
            }
        }
    }

    // Particle active state for gesture "4" (I Love You)
    val particles = remember { mutableStateListOf<Particle>() }
    var particleIdCounter by remember { mutableStateOf(0) }

    // Particle update loop omitted to prevent ConcurrentModificationException and ANRs
    LaunchedEffect(detectedFingers) {
        if (detectedFingers != 4) {
            particles.clear()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBlack)
    ) {
        // 1. Camera View & Overlay Mesh Layout
        if (modelState is ModelState.Ready) {
            val modelAbsolutePath = (modelState as ModelState.Ready).absolutePath
            
            Box(modifier = Modifier.fillMaxSize()) {
                // Embedded Android View for CameraX preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                        
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProviderRef = cameraProvider
                            
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            
                            var fpsFrameCounter = 0
                            var fpsLastTimestamp = SystemClock.uptimeMillis()

                            // Dynamic Analyzer binding
                            val imageAnalyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                .build()
                                .also { analysis ->
                                    val mainExecutor = ContextCompat.getMainExecutor(ctx)
                                    val gestureAnalyzer = HandGestureAnalyzer(ctx, modelAbsolutePath) { count, latency, landmarks ->
                                        mainExecutor.execute {
                                            viewModel.updateResults(count, latency)
                                            activeLandmarks = landmarks
                                            
                                            // Live FPS evaluation
                                            fpsFrameCounter++
                                            val now = SystemClock.uptimeMillis()
                                            if (now - fpsLastTimestamp >= 1000) {
                                                viewModel.updateFps(fpsFrameCounter)
                                                fpsFrameCounter = 0
                                                fpsLastTimestamp = now
                                            }
                                        }
                                    }
                                    analyzerRef = gestureAnalyzer
                                    analysis.setAnalyzer(analysisExecutor, gestureAnalyzer)
                                }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalyzer
                                )
                            } catch (e: Exception) {
                                Log.e("CameraSetup", "Camera binding failure", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Real-time Holographic Mesh Painting Overlay
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.85f)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    activeLandmarks?.forEach { landmarksList ->
                        if (landmarksList.isNotEmpty()) {
                            // Scale landmarks mapping normalized fields to the Canvas surface
                            // Standard connections mapping rules
                            val points = landmarksList.map {
                                // HandLandmarker output is inverted on the horizontal X plane due to frame projection
                                Offset(it.x() * canvasWidth, it.y() * canvasHeight)
                            }

                            // Knuckle skeletal links
                            val jointPaths = listOf(
                                // Thumb links
                                listOf(0, 1, 2, 3, 4),
                                // Index links
                                listOf(0, 5, 6, 7, 8),
                                // Middle links
                                listOf(0, 9, 10, 11, 12),
                                // Ring links
                                listOf(0, 13, 14, 15, 16),
                                // Pinky links
                                listOf(0, 17, 18, 19, 20),
                                // Horizontal Palm link
                                listOf(5, 9, 13, 17)
                            )

                            // Render connective structural paths in cyber neon green or neon cyan
                            jointPaths.forEach { nodes ->
                                for (i in 0 until nodes.size - 1) {
                                    val startNode = nodes[i]
                                    val endNode = nodes[i + 1]
                                    if (startNode < points.size && endNode < points.size) {
                                        drawLine(
                                            color = CyberCyan,
                                            start = points[startNode],
                                            end = points[endNode],
                                            strokeWidth = 3.dp.toPx()
                                        )
                                    }
                                }
                            }

                            // Render nodes as electric glowing magenta points
                            points.forEach { point ->
                                drawCircle(
                                    color = ElectricMagenta,
                                    radius = 6.dp.toPx(),
                                    center = point
                                )
                                drawCircle(
                                    color = CyberCyan.copy(alpha = 0.5f),
                                    radius = 10.dp.toPx(),
                                    center = point,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Loading placeholder if state falls behind
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Preheating ML Models...", color = Color.White)
            }
        }

        // 3. Immersive Interactive Overlay Animations corresponding to detected finger counts
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedContent(
                targetState = detectedFingers,
                transitionSpec = {
                    slideInVertically { height -> -height } + fadeIn() togetherWith
                            slideOutVertically { height -> -height } + fadeOut()
                },
                label = "gesture_animation_panel"
            ) { count ->
                when (count) {
                    1 -> {
                        // "1 Finger" - Pulsing neon scale indicator
                        val infinitePulse = rememberInfiniteTransition(label = "p_one")
                        val scale1 by infinitePulse.animateFloat(
                            initialValue = 0.95f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = EaseInOutBounce),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_scale"
                        )
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            GlassCard(
                                cornerRadius = 24.dp,
                                borderWidth = 2.dp,
                                glowColor = CyberCyan.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .widthIn(max = 280.dp)
                                    .scale(scale1)
                                    .testTag("gesture_card_1")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "1",
                                        fontSize = 80.sp,
                                        fontWeight = FontWeight.Black,
                                        color = CyberCyan,
                                        lineHeight = 80.sp
                                    )
                                    Text(
                                        text = "INDEX ACTIVE",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // "2 Fingers" - Springy bouncy visual
                        val bounceOffset = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            // Infinite mechanical jumpy movement
                            while(true) {
                                bounceOffset.animateTo(
                                    targetValue = -30f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                )
                                bounceOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                )
                                delay(600)
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .offset(y = bounceOffset.value.dp)
                        ) {
                            GlassCard(
                                cornerRadius = 24.dp,
                                borderWidth = 2.dp,
                                glowColor = ElectricMagenta.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .widthIn(max = 280.dp)
                                    .testTag("gesture_card_2")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "2",
                                        fontSize = 80.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ElectricMagenta,
                                        lineHeight = 80.sp
                                    )
                                    Text(
                                        text = "PEACE SIGNAL",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }

                    3 -> {
                        // "3 Fingers" - Glowing spin Zoom-in
                        val infiniteRotate = rememberInfiniteTransition(label = "p_three")
                        val spinAngle by infiniteRotate.animateFloat(
                            initialValue = -10f,
                            targetValue = 10f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1400, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "spin"
                        )
                        val scaleFactor = remember { Animatable(0.2f) }
                        LaunchedEffect(Unit) {
                            scaleFactor.animateTo(
                                targetValue = 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .scale(scaleFactor.value)
                        ) {
                            GlassCard(
                                cornerRadius = 24.dp,
                                borderWidth = 2.dp,
                                glowColor = ElectricPurple.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .widthIn(max = 280.dp)
                                    .scale(scaleX = 1f, scaleY = 1f)
                                    .testTag("gesture_card_3")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "3",
                                        fontSize = 80.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ElectricPurple,
                                        lineHeight = 80.sp
                                    )
                                    Text(
                                        text = "TRI-LEVEL ENERGY",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }

                    4 -> {
                        // "4 Fingers" - Cinematic animated banner with custom vertical particle showers
                        val scalePulse = rememberInfiniteTransition(label = "p_four")
                        val pulseSizeFactor by scalePulse.animateFloat(
                            initialValue = 0.96f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = EaseInOutQuad),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_banner"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .padding(horizontal = 24.dp)
                        ) {
                            // Canvas rendering active particle showers in screen backdrop
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                particles.forEach { p ->
                                    drawCircle(
                                        color = p.color.copy(alpha = p.alpha),
                                        radius = p.size.dp.toPx() / 2f,
                                        center = Offset(p.x, p.y)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .scale(pulseSizeFactor)
                            ) {
                                GlassCard(
                                    cornerRadius = 32.dp,
                                    borderWidth = 3.dp,
                                    glowColor = WarmCoral,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                        .testTag("gesture_card_4")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = "Love",
                                            tint = WarmCoral,
                                            modifier = Modifier
                                                .size(60.dp)
                                                .scale(pulseSizeFactor * 1.1f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "I Love You ❤️",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            letterSpacing = 2.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "UNIVERSAL GESTURE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            letterSpacing = 3.sp,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    5 -> {
                        // "5 Fingers" - Radiant wave High-Five effect
                        val glowTransition = rememberInfiniteTransition(label = "p_five")
                        val glowAlpha by glowTransition.animateFloat(
                            initialValue = 0.15f,
                            targetValue = 0.6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "highfive_radial"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            GlassCard(
                                cornerRadius = 24.dp,
                                borderWidth = 2.dp,
                                glowColor = NeonGreen.copy(alpha = glowAlpha),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .widthIn(max = 280.dp)
                                    .testTag("gesture_card_5")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "🖐️",
                                        fontSize = 68.sp,
                                        lineHeight = 75.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "HIGH FIVE!",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NeonGreen,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // "No active gesture hand" overlay placeholder instructions
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                        ) {
                            GlassCard(
                                cornerRadius = 16.dp,
                                borderWidth = 1.dp,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .widthIn(max = 300.dp)
                                    .testTag("gesture_card_none")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PanTool,
                                        contentDescription = "Hand detected",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "SHOW BACK CAMERA HAND",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Glassmorphic Sci-Fi Bottom Controller Deck
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Secondary informational diagnostics panel (FPS, Latency, Engine Status)
                GlassCard(
                    cornerRadius = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Diagnostic Metrics block
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("FPS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                val currentFps = fpsVal
                                Text("$currentFps", color = if (currentFps > 24) NeonGreen else CyberCyan, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0x33FFFFFF)))
                            Column {
                                Text("LATENCY", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                val currentLatency = latencyMs
                                Text("${currentLatency}ms", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        // Active status signal
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (detectedFingers >= 0) NeonGreen else Color(0xFFFF9800), CircleShape)
                            )
                            Text(
                                text = if (detectedFingers >= 0) "TRACKING" else "SCANNING...",
                                color = if (detectedFingers >= 0) NeonGreen else Color(0xFFFF9800),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Interactive command deck row
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Back arrow button to reset to Welcome
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onBackToSplash() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Toggles group
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Audio synthetic feedback toggle click
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (soundEnabled) CyberCyan.copy(alpha = 0.25f) else Color(0x22FFFFFF))
                                .border(1.dp, if (soundEnabled) CyberCyan else Color(0x33FFFFFF), CircleShape)
                                .clickable { viewModel.toggleSound() }
                                .testTag("btn_sound_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = "Sound Feed",
                                tint = if (soundEnabled) CyberCyan else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Cinematic vibrating haptic feedback toggle
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (vibrationEnabled) ElectricMagenta.copy(alpha = 0.25f) else Color(0x22FFFFFF))
                                .border(1.dp, if (vibrationEnabled) ElectricMagenta else Color(0x33FFFFFF), CircleShape)
                                .clickable { viewModel.toggleVibration() }
                                .testTag("btn_vibe_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (vibrationEnabled) Icons.Default.PhoneAndroid else Icons.Default.Settings,
                                contentDescription = "Haptic Feed",
                                tint = if (vibrationEnabled) ElectricMagenta else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
