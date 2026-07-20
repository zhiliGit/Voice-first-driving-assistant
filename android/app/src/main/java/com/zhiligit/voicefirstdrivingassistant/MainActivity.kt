package com.zhiligit.voicefirstdrivingassistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhiligit.voicefirstdrivingassistant.model.PlannedAction
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var speechLauncher: ActivityResultLauncher<Intent>
    private lateinit var microphonePermissionLauncher: ActivityResultLauncher<String>
    private var transcriptCallback: ((String) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val transcript = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                if (transcript.isNullOrBlank()) {
                    errorCallback?.invoke("No speech was recognized. You can still type the request.")
                } else {
                    transcriptCallback?.invoke(transcript)
                }
            } else {
                errorCallback?.invoke("Voice input was cancelled. Typed input remains available.")
            }
        }

        microphonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchSpeechRecognizer()
            else errorCallback?.invoke("Microphone permission is required for voice input.")
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F9FC)) {
                    DrivingAssistantScreen(
                        deviceName = deviceName(),
                        isHuawei = Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true),
                        onStartVoiceInput = { onTranscript, onError ->
                            transcriptCallback = onTranscript
                            errorCallback = onError
                            requestVoiceInput()
                        }
                    )
                }
            }
        }
    }

    private fun requestVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchSpeechRecognizer()
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "What should the driving assistant do?")
        }
        if (intent.resolveActivity(packageManager) == null) {
            errorCallback?.invoke(
                "No speech recognition service is installed. Enable Huawei voice input, or type the request."
            )
            return
        }
        speechLauncher.launch(intent)
    }

    private fun deviceName(): String =
        listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Android device" }
}

@Composable
private fun DrivingAssistantScreen(
    deviceName: String,
    isHuawei: Boolean,
    onStartVoiceInput: (((String) -> Unit), ((String) -> Unit)) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Voice-First Driving Assistant",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (state.isMockMode) "Demo mock mode" else "Connected to agent backend",
            color = if (state.isMockMode) Color(0xFF7A5A00) else Color(0xFF176B3A)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF4EE))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (isHuawei) "Huawei-compatible mode" else "Android compatibility mode",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF176B3A)
                )
                Text(
                    "$deviceName · No Google Play Services required",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        OutlinedButton(
            onClick = { onStartVoiceInput(viewModel::updateRequest, viewModel::reportError) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start voice input")
        }
        OutlinedTextField(
            value = state.request,
            onValueChange = viewModel::updateRequest,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Driver request") },
            supportingText = { Text("Voice recognition depends on the service installed on the device.") },
            minLines = 3,
            enabled = !state.isLoading
        )
        Button(
            onClick = viewModel::createPlan,
            enabled = state.request.isNotBlank() && !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Create action plan")
            }
        }

        state.plan?.let { plan ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Proposed action", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(plan.summary)
                    plan.actions.forEach { action ->
                        Text(
                            when (action) {
                                is PlannedAction.CreateNote -> "Note: ${action.title}\n${action.content}"
                                is PlannedAction.CreateReminder -> "Reminder: ${action.title}\n${action.scheduledAt}"
                                is PlannedAction.NoAction -> "No action: ${action.reason}"
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = viewModel::confirm, modifier = Modifier.weight(1f)) { Text("Confirm") }
                        OutlinedButton(onClick = viewModel::cancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    }
                }
            }
        }

        state.result?.let { Text(it, color = Color(0xFF176B3A), fontWeight = FontWeight.SemiBold) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "State-changing actions are never executed before confirmation.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5F6368)
        )
    }
}
