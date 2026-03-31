package com.aaryaharkare.voicekeyboard

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaryaharkare.voicekeyboard.ui.theme.VoiceKeyboardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceKeyboardTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    // Handle permission result if needed
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Voice Keyboard Setup")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                            },
                            modifier = Modifier.padding(bottom = 40.dp)
                        ) {
                            Text(text = "Enable Keyboard in Settings")
                        }
                    }
                }
            }
        }
    }
}
