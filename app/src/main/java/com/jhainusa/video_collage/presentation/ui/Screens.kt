package com.jhainusa.video_collage.presentation.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jhainusa.video_collage.domain.model.ProcessingState
import com.jhainusa.video_collage.presentation.viewmodel.ProcessingViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun PickerScreen(onVideoSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onVideoSelected(it) }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Video Face Collage", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { launcher.launch("video/*") }) {
            Text("Select Video")
        }
    }
}

@Composable
fun ProcessingScreen(
    videoUri: Uri,
    viewModel: ProcessingViewModel,
    onProcessingComplete: () -> Unit
) {
    val state by viewModel.processingState.collectAsState()

    LaunchedEffect(videoUri) {
        viewModel.processVideo(videoUri)
    }

    LaunchedEffect(state) {
        if (state is ProcessingState.Complete) {
            onProcessingComplete()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            
            val statusText = when (val s = state) {
                is ProcessingState.Idle -> "Preparing..."
                is ProcessingState.ExtractingFrames -> "Extracting frames... ${(s.progress * 100).toInt()}%"
                is ProcessingState.DetectingFaces -> "Detecting faces... ${(s.progress * 100).toInt()}%"
                is ProcessingState.GeneratingEmbeddings -> "Analyzing faces... ${(s.progress * 100).toInt()}%"
                is ProcessingState.ClusteringIdentities -> "Identifying persons..."
                is ProcessingState.BuildingCollage -> "Building collage..."
                is ProcessingState.Complete -> "Finished!"
                is ProcessingState.Error -> "Error: ${s.message}"
            }
            
            Text(statusText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ResultScreen(viewModel: ProcessingViewModel, onRestart: () -> Unit) {
    val state by viewModel.processingState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state is ProcessingState.Complete) {
            val completeState = state as ProcessingState.Complete
            
            Text(
                "Identity Collage", 
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Found ${completeState.persons.size} unique faces", 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Image(
                bitmap = completeState.collage.asImageBitmap(),
                contentDescription = "Face Collage",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New Video")
                }
                
                Button(
                    onClick = { shareCollage(context, completeState.collage) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No result available.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRestart) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}

private fun shareCollage(context: android.content.Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val stream = FileOutputStream("$cachePath/collage.png")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val imageFile = File(cachePath, "collage.png")
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Collage"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
