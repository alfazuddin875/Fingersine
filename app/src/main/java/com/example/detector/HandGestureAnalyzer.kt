package com.example.detector

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.io.File

class HandGestureAnalyzer(
    private val context: Context,
    private val modelPath: String?,
    private val onResults: (Int, Long, List<List<NormalizedLandmark>>?) -> Unit
) : ImageAnalysis.Analyzer {

    private var handLandmarker: HandLandmarker? = null
    private var lastTimestampMs = -1L
    @Volatile private var isProcessing = false

    init {
        try {
            val baseOptionsBuilder = BaseOptions.builder()
            baseOptionsBuilder.setModelAssetPath("hand_landmarker.task")

            val options = HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setMinHandDetectionConfidence(0.55f)
                .setMinHandPresenceConfidence(0.55f)
                .setMinTrackingConfidence(0.55f)
                .setNumHands(2) // Track up to 2 hands for reliability
                .setResultListener { result: HandLandmarkerResult, mpImage: com.google.mediapipe.framework.image.MPImage ->
                    val timestamp = result.timestampMs()
                    val latency = SystemClock.uptimeMillis() - timestamp
                    
                    val handLandmarks = result.landmarks()
                    if (handLandmarks != null && handLandmarks.isNotEmpty()) {
                        val firstHand = handLandmarks[0]
                        val fingerCount = countFingers(firstHand)
                        onResults(fingerCount, latency, handLandmarks)
                    } else {
                        onResults(-1, latency, null)
                    }
                    isProcessing = false
                }
                .setErrorListener { error: RuntimeException ->
                    Log.e("HandGestureAnalyzer", "MediaPipe Core Error: ${error.message}", error)
                    isProcessing = false
                }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            Log.d("HandGestureAnalyzer", "HandLandmarker initialized successfully.")
        } catch (e: Exception) {
            Log.e("HandGestureAnalyzer", "Failed to initialize HandLandmarker", e)
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
        synchronized(this) {
            if (isProcessing) {
                imageProxy.close()
                return
            }
            
            val frameTime = SystemClock.uptimeMillis()
            if (frameTime <= lastTimestampMs) {
                imageProxy.close()
                return
            }
            lastTimestampMs = frameTime

            val landmarkerHost = handLandmarker
            if (landmarkerHost == null) {
                imageProxy.close()
                return
            }

            try {
                isProcessing = true
                // Converts ImageProxy to a standard bitmap natively (CameraX 1.3.0+)
                val bitmap = imageProxy.toBitmap()
                val mpImage = BitmapImageBuilder(bitmap).build()
                
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val processingOptions = ImageProcessingOptions.builder()
                    .setRotationDegrees(rotationDegrees)
                    .build()

                landmarkerHost.detectAsync(mpImage, processingOptions, frameTime)
            } catch (e: Exception) {
                Log.e("HandGestureAnalyzer", "Frame analysis exception", e)
                isProcessing = false
            } finally {
                imageProxy.close()
            }
        }
    }

    private fun countFingers(landmarks: List<NormalizedLandmark>): Int {
        if (landmarks.size < 21) return 0
        var count = 0
        
        // Index finger: Tip (8) above PIP joint (6)
        if (landmarks[8].y() < landmarks[6].y()) count++
        
        // Middle finger: Tip (12) above PIP joint (10)
        if (landmarks[12].y() < landmarks[10].y()) count++
        
        // Ring finger: Tip (16) above PIP joint (14)
        if (landmarks[16].y() < landmarks[14].y()) count++
        
        // Pinky finger: Tip (20) above PIP joint (18)
        if (landmarks[20].y() < landmarks[18].y()) count++
        
        // Thumb: orientation-invariant distance metrics
        // Compute distance from thumb tip (4) to middle finger base (9)
        val tTip = landmarks[4]
        val tJoint = landmarks[3]
        val centerPoint = landmarks[9]
        
        val distTipToCenter = Math.hypot(
            (tTip.x() - centerPoint.x()).toDouble(),
            (tTip.y() - centerPoint.y()).toDouble()
        )
        val distJointToCenter = Math.hypot(
            (tJoint.x() - centerPoint.x()).toDouble(),
            (tJoint.y() - centerPoint.y()).toDouble()
        )
        
        // When stretched, distance from Tip to palm-center is significantly larger than Joint to palm-center
        if (distTipToCenter > distJointToCenter * 1.15) {
            count++
        }
        
        return count
    }

    fun close() {
        synchronized(this) {
            handLandmarker?.close()
            handLandmarker = null
        }
    }
}
