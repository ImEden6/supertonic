package com.supertone.supertonic.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supertone.supertonic.tts.Style
import com.supertone.supertonic.viewmodel.TTSViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TTSScreen(
    viewModel: TTSViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToBatch: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Supertonic",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = onNavigateToBatch) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = "Batch Convert"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            StatusCard(
                isLoading = uiState.isLoading,
                isGenerating = uiState.isGenerating,
                statusMessage = uiState.statusMessage
            )
            
            // Text Input
            TextInputCard(
                text = uiState.inputText,
                onTextChange = viewModel::updateInputText,
                enabled = !uiState.isLoading && !uiState.isGenerating
            )
            
            // Voice Selection
            VoiceSelectionCard(
                selectedVoice = uiState.selectedVoice,
                onVoiceSelected = viewModel::updateVoice,
                enabled = !uiState.isLoading && !uiState.isGenerating
            )
            
            // Parameters
            ParametersCard(
                speed = uiState.speed,
                onSpeedChange = viewModel::updateSpeed,
                denoisingSteps = uiState.denoisingSteps,
                onDenoisingStepsChange = viewModel::updateDenoisingSteps,
                enabled = !uiState.isLoading && !uiState.isGenerating
            )
            
            // Generate Button
            GenerateButton(
                isLoading = uiState.isLoading,
                isGenerating = uiState.isGenerating,
                isPlaying = uiState.isPlaying,
                onGenerate = viewModel::generateSpeech,
                onStop = viewModel::stopPlayback
            )
            
            // Playback Controls
            if (uiState.lastGeneratedFile != null) {
                PlaybackCard(
                    isPlaying = uiState.isPlaying,
                    onTogglePlayback = viewModel::togglePlayback,
                    onDownload = viewModel::downloadAudio
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    isLoading: Boolean,
    isGenerating: Boolean,
    statusMessage: String
) {
    val backgroundColor by animateColorAsState(
        when {
            isLoading || isGenerating -> MaterialTheme.colorScheme.primaryContainer
            statusMessage.startsWith("Error") -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
        label = "statusColor"
    )
    
    val contentColor by animateColorAsState(
        when {
            isLoading || isGenerating -> MaterialTheme.colorScheme.onPrimaryContainer
            statusMessage.startsWith("Error") -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onSecondaryContainer
        },
        label = "statusContentColor"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading || isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    imageVector = if (statusMessage.startsWith("Error")) 
                        Icons.Default.Error else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
        }
    }
}

@Composable
private fun TextInputCard(
    text: String,
    onTextChange: (String) -> Unit,
    enabled: Boolean
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp),
        label = { Text("Text to synthesize") },
        placeholder = { Text("Enter the text you want to convert to speech...") },
        enabled = enabled,
        maxLines = 10,
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceSelectionCard(
    selectedVoice: String,
    onVoiceSelected: (String) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Voice Style",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = it }
            ) {
                OutlinedTextField(
                    value = Style.VOICE_STYLES.find { it.first == selectedVoice }?.second ?: selectedVoice,
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    Style.VOICE_STYLES.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onVoiceSelected(id)
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (id.startsWith("M")) 
                                        Icons.Default.Male else Icons.Default.Female,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParametersCard(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    denoisingSteps: Int,
    onDenoisingStepsChange: (Int) -> Unit,
    enabled: Boolean
) {
    var stepsText by remember(denoisingSteps) { mutableStateOf(denoisingSteps.toString()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Parameters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            // Speed Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Speed", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        String.format("%.2fx", speed),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Slider(
                    value = speed,
                    onValueChange = onSpeedChange,
                    valueRange = 1.0f..3.0f,
                    steps = 39,
                    enabled = enabled
                )
            }
            
            // Denoising Steps with editable text field
            Column {
                Text("Denoising Steps", style = MaterialTheme.typography.bodyMedium)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Slider for quick adjustment (1-50)
                    Slider(
                        value = denoisingSteps.coerceIn(1, 50).toFloat(),
                        onValueChange = { 
                            val newValue = it.toInt()
                            onDenoisingStepsChange(newValue)
                            stepsText = newValue.toString()
                        },
                        valueRange = 1f..50f,
                        steps = 48,
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Editable text field for precise input (1-100)
                    OutlinedTextField(
                        value = stepsText,
                        onValueChange = { newText ->
                            stepsText = newText
                            val parsed = newText.toIntOrNull()
                            if (parsed != null && parsed in 1..100) {
                                onDenoisingStepsChange(parsed)
                            }
                        },
                        modifier = Modifier.width(80.dp),
                        enabled = enabled,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                Text(
                    "Range: 1-100. Higher = better quality, slower. Recommended: 2-10",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GenerateButton(
    isLoading: Boolean,
    isGenerating: Boolean,
    isPlaying: Boolean,
    onGenerate: () -> Unit,
    onStop: () -> Unit
) {
    Button(
        onClick = if (isPlaying) onStop else onGenerate,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isLoading && !isGenerating,
        shape = RoundedCornerShape(16.dp)
    ) {
        if (isGenerating) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Generating...")
        } else if (isPlaying) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Stop Playback")
        } else {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate & Play Speech")
        }
    }
}

@Composable
private fun PlaybackCard(
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onTogglePlayback,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isPlaying) "Pause" else "Play")
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            OutlinedButton(
                onClick = onDownload,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download")
            }
        }
    }
}
