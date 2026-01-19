package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.OutputStream
import java.net.Socket

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoStreamScreen()
        }
    }
}

@Composable
fun AutoStreamScreen() {
    val context = LocalContext.current



    val SERVER_HOST = "bumwl-192-167-110-25.a.free.pinggy.link" // Change this to your ngrok URL
    val SERVER_PORT = 36683            // Change this to your ngrok Port

    var statusText by remember { mutableStateOf("Initializing...") }
    var isStreaming by remember { mutableStateOf(false) }

    fun startAutoConnection() {
        if (isStreaming) return

        Thread {
            try {
                // 1. Permission Check
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    statusText = "Missing Microphone Permission"
                    return@Thread
                }

                statusText = "Connecting to $SERVER_HOST:$SERVER_PORT..."

                // 2. Connect via TCP
                // This blocks until connected or fails
                val socket = Socket(SERVER_HOST, SERVER_PORT)

                statusText = "🔴 LIVE! Transmitting to Laptop..."
                isStreaming = true

                val outputStream: OutputStream = socket.getOutputStream()

                // Audio Configuration (Must match the 'aplay' command on laptop)
                val sampleRate = 44100
                val channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO
                val audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT

                val minBufferSize = android.media.AudioRecord.getMinBufferSize(
                    sampleRate, channelConfig, audioFormat
                )

                val recorder = android.media.AudioRecord(
                    android.media.MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufferSize
                )

                recorder.startRecording()
                val buffer = ByteArray(minBufferSize)

                while (isStreaming) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        try {
                            outputStream.write(buffer, 0, read)
                            outputStream.flush()
                        } catch (e: Exception) {
                            isStreaming = false
                            statusText = "Connection Lost: ${e.message}"
                        }
                    }
                }

                // Cleanup
                recorder.stop()
                recorder.release()
                socket.close()

            } catch (e: Exception) {
                e.printStackTrace()
                statusText = "Connection Failed (${e.message})\nCheck Ngrok address?"
                isStreaming = false
            }
        }.start()
    }

    // Permissions Logic (Only Audio needed now)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) startAutoConnection()
            else statusText = "Mic Permission Denied."
        }
    )

    // AUTOMATIC START TRIGGER
    LaunchedEffect(Unit) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            startAutoConnection()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = statusText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(30.dp))

        if (!isStreaming) {
            Button(onClick = { startAutoConnection() }) {
                Text("Retry Connection")
            }
        } else {
            Button(onClick = { isStreaming = false; statusText = "Stopped" }) {
                Text("Stop Stream")
            }
        }
    }
}