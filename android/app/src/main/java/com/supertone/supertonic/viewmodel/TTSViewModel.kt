package com.supertone.supertonic.viewmodel

import android.app.Application
import android.content.ContentValues
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.supertone.supertonic.tts.Style
import com.supertone.supertonic.tts.TextToSpeech
import com.supertone.supertonic.tts.WavWriter
import ai.onnxruntime.OrtEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UI state for TTS screen
 */
data class TTSUiState(
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val isPlaying: Boolean = false,
    val statusMessage: String = "Loading models...",
    val inputText: String = "Hello, this is a text to speech example.",
    val selectedVoice: String = "M1",
    val speed: Float = 1.05f,
    val denoisingSteps: Int = 5,
    val lastGeneratedFile: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel for TTS functionality
 */
class TTSViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(TTSUiState())
    val uiState: StateFlow<TTSUiState> = _uiState.asStateFlow()
    
    private var textToSpeech: TextToSpeech? = null
    private var currentStyle: Style? = null
    private var mediaPlayer: MediaPlayer? = null
    private var ortEnvironment: OrtEnvironment? = null
    
    companion object {
        private const val TAG = "TTSViewModel"
        private const val ONNX_DIR = "onnx"
        private const val VOICE_STYLES_DIR = "voice_styles"
    }
    
    init {
        loadModels()
    }
    
    private fun loadModels() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    statusMessage = "Loading models..."
                )
                
                withContext(Dispatchers.IO) {
                    textToSpeech = TextToSpeech.loadFromAssets(
                        context = getApplication(),
                        onnxDir = ONNX_DIR,
                        useGpu = false
                    ) { progress ->
                        _uiState.value = _uiState.value.copy(statusMessage = progress)
                    }
                    
                    ortEnvironment = OrtEnvironment.getEnvironment()
                    loadVoiceStyle(_uiState.value.selectedVoice)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Ready"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading models", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load models: ${e.message}",
                    statusMessage = "Error"
                )
            }
        }
    }
    
    private suspend fun loadVoiceStyle(voiceName: String) {
        withContext(Dispatchers.IO) {
            currentStyle?.close()
            val env = ortEnvironment ?: OrtEnvironment.getEnvironment().also { ortEnvironment = it }
            currentStyle = Style.loadFromAssets(
                context = getApplication(),
                voiceStylePaths = listOf("$VOICE_STYLES_DIR/$voiceName.json"),
                env = env
            )
        }
    }
    
    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }
    
    fun updateVoice(voice: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    selectedVoice = voice,
                    statusMessage = "Loading voice..."
                )
                loadVoiceStyle(voice)
                _uiState.value = _uiState.value.copy(statusMessage = "Ready")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading voice style", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load voice: ${e.message}"
                )
            }
        }
    }
    
    fun updateSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(speed = speed)
    }
    
    fun updateDenoisingSteps(steps: Int) {
        _uiState.value = _uiState.value.copy(denoisingSteps = steps)
    }
    
    fun generateSpeech() {
        val tts = textToSpeech ?: return
        val style = currentStyle ?: return
        
        if (_uiState.value.inputText.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter some text")
            return
        }
        
        viewModelScope.launch {
            try {
                stopPlayback()
                
                _uiState.value = _uiState.value.copy(
                    isGenerating = true,
                    statusMessage = "Generating speech...",
                    errorMessage = null
                )
                
                val result = withContext(Dispatchers.IO) {
                    tts.call(
                        text = _uiState.value.inputText,
                        style = style,
                        totalStep = _uiState.value.denoisingSteps,
                        speed = _uiState.value.speed
                    )
                }
                
                // Save to temp file
                val tempFile = withContext(Dispatchers.IO) {
                    val cacheDir = getApplication<Application>().cacheDir
                    val timestamp = System.currentTimeMillis()
                    val file = File(cacheDir, "speech_$timestamp.wav")
                    WavWriter.writeWavFile(file.absolutePath, result.wav, tts.sampleRate)
                    file
                }
                
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    statusMessage = "Playing ${String.format("%.2f", result.duration[0])}s of audio...",
                    lastGeneratedFile = tempFile.absolutePath
                )
                
                // Play audio
                playAudio(tempFile.absolutePath)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating speech", e)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = "Failed to generate speech: ${e.message}",
                    statusMessage = "Error"
                )
            }
        }
    }
    
    private fun playAudio(filePath: String) {
        try {
            stopPlayback()
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _uiState.value = _uiState.value.copy(
                        isPlaying = false,
                        statusMessage = "Ready"
                    )
                }
                start()
            }
            
            _uiState.value = _uiState.value.copy(isPlaying = true)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to play audio: ${e.message}"
            )
        }
    }
    
    fun togglePlayback() {
        val player = mediaPlayer
        if (player != null && player.isPlaying) {
            player.pause()
            _uiState.value = _uiState.value.copy(isPlaying = false)
        } else if (player != null) {
            player.start()
            _uiState.value = _uiState.value.copy(isPlaying = true)
        } else if (_uiState.value.lastGeneratedFile != null) {
            playAudio(_uiState.value.lastGeneratedFile!!)
        }
    }
    
    fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            statusMessage = "Ready"
        )
    }
    
    fun downloadAudio() {
        val filePath = _uiState.value.lastGeneratedFile ?: return
        
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val sourceFile = File(filePath)
                    if (!sourceFile.exists()) {
                        throw Exception("Audio file not found")
                    }
                    
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val fileName = "supertonic_$timestamp.wav"
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Use MediaStore for Android 10+
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        
                        val resolver = getApplication<Application>().contentResolver
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                            ?: throw Exception("Failed to create file")
                        
                        resolver.openOutputStream(uri)?.use { output ->
                            sourceFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                    } else {
                        // Legacy method for older Android versions
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val destFile = File(downloadsDir, fileName)
                        sourceFile.copyTo(destFile, overwrite = true)
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    statusMessage = "File saved to Downloads"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving file", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to save file: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        currentStyle?.close()
        textToSpeech?.close()
    }
}
