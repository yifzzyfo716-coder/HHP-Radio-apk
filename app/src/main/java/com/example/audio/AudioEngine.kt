package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import com.example.model.AudioRoute
import com.example.model.AudioSettingsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioEngine(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Default)

    // Capture & Playback params
    val sampleRate = 16000
    val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var gainControl: AutomaticGainControl? = null

    private var recordingJob: Job? = null
    private var isRecording = false
    private var isTestingMic = false

    private val _txAudioLevel = MutableStateFlow(0f)
    val txAudioLevel: StateFlow<Float> = _txAudioLevel.asStateFlow()

    private val _rxAudioLevel = MutableStateFlow(0f)
    val rxAudioLevel: StateFlow<Float> = _rxAudioLevel.asStateFlow()

    private val _currentRoute = MutableStateFlow(AudioRoute.SPEAKER)
    val currentRoute: StateFlow<AudioRoute> = _currentRoute.asStateFlow()

    private var currentSettings = AudioSettingsData()
    var onAudioPacketCaptured: ((ByteArray, Int) -> Unit)? = null

    init {
        initAudioTrack()
    }

    fun updateSettings(settings: AudioSettingsData) {
        currentSettings = settings
        applyAudioRouting(settings.audioRoute)
    }

    private fun initAudioTrack() {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(audioFormat)
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfigOut)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startCapture(hasPermission: Boolean, isTestMode: Boolean = false): Boolean {
        if (!hasPermission) {
            Log.w(TAG, "Cannot start capture without RECORD_AUDIO permission")
            return false
        }
        if (isRecording) return true

        try {
            requestAudioFocus()

            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size for AudioRecord")
                return false
            }
            val bufferSize = (minBufferSize * 2).coerceAtLeast(2048)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfigIn,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            val sessionId = audioRecord?.audioSessionId ?: 0
            if (sessionId != 0) {
                setupAudioEffects(sessionId)
            }

            audioRecord?.startRecording()
            isRecording = true
            isTestingMic = isTestMode

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(640) // 20ms of 16kHz 16-bit mono PCM = 320 samples = 640 bytes
                while (isActive && isRecording) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readBytes > 0) {
                        // Apply input gain
                        val gain = currentSettings.inputGain
                        var sumSquares = 0.0
                        val sampleCount = readBytes / 2

                        for (i in 0 until readBytes step 2) {
                            var sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                            if (sample > 32767) sample -= 65536
                            
                            val boosted = (sample * gain).toInt().coerceIn(-32768, 32767)
                            buffer[i] = (boosted and 0xFF).toByte()
                            buffer[i + 1] = ((boosted shr 8) and 0xFF).toByte()

                            sumSquares += boosted * boosted
                        }

                        val rms = sqrt(sumSquares / sampleCount)
                        val level = (rms / 32767.0).toFloat().coerceIn(0f, 1f)
                        _txAudioLevel.value = level

                        if (isTestingMic) {
                            // In test mode, play back to speaker so user can calibrate
                            playIncomingAudio(buffer.copyOf(readBytes))
                        } else {
                            // Transmit to network
                            onAudioPacketCaptured?.invoke(buffer, readBytes)
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioRecord", e)
            stopCapture()
            return false
        }
    }

    fun stopCapture() {
        isRecording = false
        isTestingMic = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            releaseAudioEffects()
            _txAudioLevel.value = 0f
            abandonAudioFocus()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
    }

    fun playIncomingAudio(pcmData: ByteArray) {
        try {
            if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                initAudioTrack()
            }

            // Calculate RX level
            var sumSquares = 0.0
            val sampleCount = pcmData.size / 2
            val vol = currentSettings.outputVolume

            for (i in 0 until pcmData.size step 2) {
                var sample = (pcmData[i].toInt() and 0xFF) or (pcmData[i + 1].toInt() shl 8)
                if (sample > 32767) sample -= 65536

                val amplified = (sample * vol).toInt().coerceIn(-32768, 32767)
                pcmData[i] = (amplified and 0xFF).toByte()
                pcmData[i + 1] = ((amplified shr 8) and 0xFF).toByte()

                sumSquares += amplified * amplified
            }

            val rms = sqrt(sumSquares / (sampleCount.coerceAtLeast(1)))
            val level = (rms / 32767.0).toFloat().coerceIn(0f, 1f)
            _rxAudioLevel.value = level

            audioTrack?.write(pcmData, 0, pcmData.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio chunk", e)
        }
    }

    fun resetRxLevel() {
        _rxAudioLevel.value = 0f
    }

    private fun setupAudioEffects(sessionId: Int) {
        try {
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
                    enabled = currentSettings.echoCancellation
                }
            }
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                    enabled = currentSettings.noiseSuppression
                }
            }
            if (AutomaticGainControl.isAvailable()) {
                gainControl = AutomaticGainControl.create(sessionId)?.apply {
                    enabled = currentSettings.autoGainControl
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach audio effects", e)
        }
    }

    private fun releaseAudioEffects() {
        try {
            echoCanceler?.release()
            echoCanceler = null
            noiseSuppressor?.release()
            noiseSuppressor = null
            gainControl?.release()
            gainControl = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio effects", e)
        }
    }

    fun applyAudioRouting(route: AudioRoute) {
        _currentRoute.value = route
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            when (route) {
                AudioRoute.SPEAKER -> {
                    audioManager.isSpeakerphoneOn = true
                    if (audioManager.isBluetoothScoOn) {
                        audioManager.stopBluetoothSco()
                        audioManager.isBluetoothScoOn = false
                    }
                }
                AudioRoute.EARPIECE -> {
                    audioManager.isSpeakerphoneOn = false
                    if (audioManager.isBluetoothScoOn) {
                        audioManager.stopBluetoothSco()
                        audioManager.isBluetoothScoOn = false
                    }
                }
                AudioRoute.BLUETOOTH -> {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                }
                AudioRoute.WIRED_HEADSET -> {
                    audioManager.isSpeakerphoneOn = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying audio route: $route", e)
        }
    }

    private fun requestAudioFocus() {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        } catch (e: Exception) {
            Log.e(TAG, "Audio focus request error", e)
        }
    }

    private fun abandonAudioFocus() {
        try {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.e(TAG, "Audio focus abandon error", e)
        }
    }

    fun release() {
        stopCapture()
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio track", e)
        }
    }

    companion object {
        private const val TAG = "HhpAudioEngine"
    }
}
