package com.example.ui.detector

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.SoundManager
import com.example.util.VibeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class DetectorViewModel(application: Application) : AndroidViewModel(application) {
    
    // Core Utilities
    val soundManager = SoundManager(application)
    val vibeManager = VibeManager(application)
    
    // UI preferences states
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()
    
    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()
    
    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()
    
    // Tracking States
    private val _detectedFingers = MutableStateFlow(-1) // -1 means no fingers / no hand detected
    val detectedFingers: StateFlow<Int> = _detectedFingers.asStateFlow()
    
    private val _processingTimeMs = MutableStateFlow(0L)
    val processingTimeMs: StateFlow<Long> = _processingTimeMs.asStateFlow()

    private val _lastDetectedTime = MutableStateFlow(0L)
    val lastDetectedTime: StateFlow<Long> = _lastDetectedTime.asStateFlow()
    
    // Model Download/Asset Readiness
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()
    
    private val _modelState = MutableStateFlow<ModelState>(ModelState.Checking)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private var lastRecordedFingerCount = -1
    private var lastToneTime = 0L

    init {
        checkModelStatus()
    }

    fun toggleSound() {
        _soundEnabled.value = !_soundEnabled.value
        soundManager.setEnabled(_soundEnabled.value)
        soundManager.playClickTone()
    }

    fun toggleVibration() {
        _vibrationEnabled.value = !_vibrationEnabled.value
        vibeManager.setEnabled(_vibrationEnabled.value)
        vibeManager.vibrateTick()
    }

    private fun checkModelStatus() {
        viewModelScope.launch {
            _modelState.value = ModelState.Checking
            
            // Check 1: Check internally in Assets folder (if pre-downloaded by Gradle task)
            val hasAsset = try {
                getApplication<Application>().assets.open("hand_landmarker.task").use { true }
            } catch (e: Exception) {
                false
            }
            
            if (hasAsset) {
                _modelState.value = ModelState.Ready(null) // Path null means load straight from assets
                return@launch
            }
            
            // Check 2: Check in internal app files storage
            val localModelFile = getLocalModelFile()
            if (localModelFile.exists() && localModelFile.length() > 5_000_000) { // Should be around ~5.6MB
                _modelState.value = ModelState.Ready(localModelFile.absolutePath)
                return@launch
            }
            
            // Fallback: Needs download
            _modelState.value = ModelState.NeedsDownload
        }
    }

    fun getLocalModelFile(): File {
        return File(getApplication<Application>().filesDir, "hand_landmarker.task")
    }

    fun startModelDownload() {
        viewModelScope.launch {
            _modelState.value = ModelState.Downloading
            _downloadProgress.value = 0f
            
            try {
                val url = URL("https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task")
                val localFile = getLocalModelFile()
                
                // Keep temp file first, rename on success for atomicity
                val tempFile = File(getApplication<Application>().cacheDir, "temp_hand_landmarker.task")
                
                kotlinx.coroutines.Dispatchers.IO.let { dispatcher ->
                    launch(dispatcher) {
                        url.openConnection().apply {
                            connectTimeout = 15000
                            readTimeout = 15000
                            val totalSize = contentLength
                            
                            url.openStream().use { input ->
                                FileOutputStream(tempFile).use { output ->
                                    val buffer = ByteArray(8192)
                                    var bytesRead: Int
                                    var totalBytesRead = 0L
                                    
                                    while (input.read(buffer).also { bytesRead = it } != -1) {
                                        output.write(buffer, 0, bytesRead)
                                        totalBytesRead += bytesRead
                                        if (totalSize > 0) {
                                            _downloadProgress.value = totalBytesRead.toFloat() / totalSize.toFloat()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.join()
                
                if (tempFile.exists() && tempFile.length() > 1_000_000) {
                    tempFile.copyTo(localFile, overwrite = true)
                    tempFile.delete()
                    soundManager.playSuccessTone()
                    vibeManager.vibrateTriplePulse()
                    _modelState.value = ModelState.Ready(localFile.absolutePath)
                } else {
                    _modelState.value = ModelState.Error("Downloaded file is invalid or empty.")
                }
            } catch (e: Exception) {
                _modelState.value = ModelState.Error("Download failed: ${e.message}")
            }
        }
    }

    private var lastLatencyUpdateTime = 0L

    fun updateResults(fingerCount: Int, latency: Long) {
        _detectedFingers.value = fingerCount
        
        val now = SystemClock.uptimeMillis()
        if (now - lastLatencyUpdateTime > 200) {
            _processingTimeMs.value = latency
            lastLatencyUpdateTime = now
        }
        
        _lastDetectedTime.value = now

        if (fingerCount != lastRecordedFingerCount && fingerCount >= 0) {
            val now = SystemClock.uptimeMillis()
            // Throttle consecutive sound/vibe feedback (min 150ms gap) to prevent feedback loops
            if (now - lastToneTime > 400) {
                soundManager.playGestureTone(fingerCount)
                
                // Custom vibration feedbacks
                when (fingerCount) {
                    4 -> vibeManager.vibrateHeartbeat() // Special pulse for I Love You
                    else -> vibeManager.vibrateTick() // Quick click
                }
                
                lastToneTime = now
            }
            lastRecordedFingerCount = fingerCount
        }
    }

    fun updateFps(fpsVal: Int) {
        _fps.value = fpsVal
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}

sealed interface ModelState {
    object Checking : ModelState
    object NeedsDownload : ModelState
    object Downloading : ModelState
    data class Ready(val absolutePath: String?) : ModelState
    data class Error(val message: String) : ModelState
}
