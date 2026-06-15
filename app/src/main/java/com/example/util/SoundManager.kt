package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

class SoundManager(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    private var isEnabled = true

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error initializing ToneGenerator", e)
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isEnabled(): Boolean = isEnabled

    fun playGestureTone(fingerCount: Int) {
        if (!isEnabled || toneGenerator == null) return
        
        try {
            when (fingerCount) {
                1 -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                2 -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
                3 -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                4 -> {
                    // Play a premium pleasant futuristic double chord
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                }
                5 -> toneGenerator?.startTone(ToneGenerator.TONE_SUP_PIP, 180)
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Playback error", e)
        }
    }

    fun playSuccessTone() {
        if (!isEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 150)
        } catch (e: Exception) {
            Log.e("SoundManager", "Success tone error", e)
        }
    }

    fun playClickTone() {
        if (!isEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (e: Exception) {
            Log.e("SoundManager", "Click tone error", e)
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
