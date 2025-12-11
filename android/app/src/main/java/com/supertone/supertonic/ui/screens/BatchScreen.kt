package com.supertone.supertonic.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class BatchItem(
    val name: String,
    val uri: Uri,
    val status: BatchItemStatus = BatchItemStatus.PENDING
)

enum class BatchItemStatus {
    PENDING, PROCESSING, COMPLETED, ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(
    onNavigateBack: () -> Unit
) {
    var inputFolderUri by remember { mutableStateOf<Uri?>(null) }
    var outputFolderUri by remember { mutableStateOf<Uri?>(null) }
    var batchItems by remember { mutableStateOf<List<BatchItem>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    
    val inputFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            inputFolderUri = it
            // In a real implementation, we would scan for .txt files here
            batchItems = listOf(
                BatchItem("example1.txt", it),
                BatchItem("example2.txt", it),
                BatchItem("example3.txt", it)
            )
        }
    }
    
    val outputFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { outputFolderUri = it }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Convert", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Folder Selection
            FolderSelectionCard(
                title = "Input Folder",
                description = "Select folder containing .txt files",
                selectedUri = inputFolderUri,
                onSelectFolder = { inputFolderPicker.launch(null) },
                icon = Icons.Default.FolderOpen
            )
            
            // Output Folder Selection
            FolderSelectionCard(
                title = "Output Folder",
                description = "Select folder for audio output",
                selectedUri = outputFolderUri,
                onSelectFolder = { outputFolderPicker.launch(null) },
                icon = Icons.Default.Folder
            )
            
            // File List
            if (batchItems.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Files to Convert",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${batchItems.size} files",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(batchItems) { item ->
                                BatchItemRow(item = item)
                            }
                        }
                    }
                }
            } else {
                // Empty State
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Select an input folder to begin",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Convert Button
            Button(
                onClick = { isProcessing = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = inputFolderUri != null && outputFolderUri != null && !isProcessing,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Processing...")
                } else {
                    Icon(Icons.Default.Transform, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Convert All Files")
                }
            }
        }
    }
}

@Composable
private fun FolderSelectionCard(
    title: String,
    description: String,
    selectedUri: Uri?,
    onSelectFolder: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        selectedUri?.lastPathSegment ?: description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            
            FilledTonalButton(onClick = onSelectFolder) {
                Text("Browse")
            }
        }
    }
}

@Composable
private fun BatchItemRow(item: BatchItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                item.name,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        when (item.status) {
            BatchItemStatus.PENDING -> {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Pending",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BatchItemStatus.PROCESSING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
            BatchItemStatus.COMPLETED -> {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            BatchItemStatus.ERROR -> {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
