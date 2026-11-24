package com.aad.ai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private val background = Color(0xFF080D1A)
private val cyan = Color(0xFF3DDCFF)
private data class Message(val text: String, val user: Boolean)

class MainActivity : ComponentActivity() {
    private var speaker: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speaker = TextToSpeech(this) { }
        setContent { AAdScreen(onSpeak = { text ->
            speaker?.language = Locale.getDefault()
            speaker?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aad-response")
        }) }
    }
    override fun onDestroy() {
        speaker?.stop()
        speaker?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun AAdScreen(onSpeak: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("aad_settings", 0) }
    var endpoint by remember { mutableStateOf(prefs.getString("endpoint", "https://aad-ai-e96f.ajakurdish438.workers.dev").orEmpty()) }
    var accessToken by remember { mutableStateOf(prefs.getString("token", "").orEmpty()) }
    var settingsVisible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var autoSpeak by remember { mutableStateOf(prefs.getBoolean("speak", false)) }
    val messages = remember { mutableStateListOf(Message("سڵاو! من AAd AI ـم. بۆ وەڵامی AI، ناونیشانی سێرڤەر لە ڕێکخستنەکان دابنێ.", false)) }
    val scope = rememberCoroutineScope()
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            input = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = cyan, background = background, surface = Color(0xFF172238))) {
        Column(
            Modifier.fillMaxSize().background(background).statusBarsPadding().navigationBarsPadding().padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("AAd AI", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { settingsVisible = true }) { Text("⚙ Settings") }
            }
            if (settingsVisible) {
                AlertDialog(
                    onDismissRequest = { settingsVisible = false },
                    title = { Text("Settings") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = endpoint, onValueChange = { endpoint = it },
                                label = { Text("HTTPS backend URL") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = accessToken, onValueChange = { accessToken = it },
                                label = { Text("Backend access token") }, singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth())
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = autoSpeak, onCheckedChange = { autoSpeak = it })
                                Text("Speak AI replies")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(enabled = endpoint.trim().startsWith("https://"), onClick = {
                            endpoint = endpoint.trim().trimEnd('/')
                            prefs.edit().putString("endpoint", endpoint).putString("token", accessToken.trim())
                                .putBoolean("speak", autoSpeak).apply()
                            settingsVisible = false
                        }) { Text("Save") }
                    },
                    dismissButton = { TextButton(onClick = { settingsVisible = false }) { Text("Close") } }
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(110.dp).background(
                        Brush.radialGradient(listOf(Color(0xFFA9F6FF), cyan, Color(0xFF1453A3), background)),
                        CircleShape
                    )
                )
                Text("✦", color = Color.White, fontSize = 36.sp)
            }
            Text("Your AI companion", modifier = Modifier.fillMaxWidth(), color = Color.LightGray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(messages.toList()) { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.user) Arrangement.End else Arrangement.Start) {
                        Text(
                            message.text, color = Color.White,
                            modifier = Modifier.fillMaxWidth(0.9f).background(
                                if (message.user) Color(0xFF135B85) else Color(0xFF1B2840),
                                RoundedCornerShape(16.dp)
                            ).padding(12.dp)
                        )
                    }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    placeholder = { Text("Write a message…") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f), maxLines = 3
                )
                TextButton(onClick = {
                    try {
                        voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                        })
                    } catch (_: Exception) {
                        messages.add(Message("Voice recognition is not available on this device.", false))
                    }
                }) { Text("🎙") }
                Button(enabled = input.isNotBlank() && !busy, onClick = {
                    val prompt = input.trim()
                    input = ""
                    messages.add(Message(prompt, true))
                    if (accessToken.isBlank()) {
                        settingsVisible = true
                        messages.add(Message("تکایە Backend access token لە Settings دابنێ.", false))
                    } else if (!endpoint.startsWith("https://")) {
                        messages.add(Message("Set an HTTPS backend URL in Settings to enable AI replies.", false))
                    } else {
                        busy = true
                        scope.launch {
                            val reply = withContext(Dispatchers.IO) { requestReply(endpoint, accessToken, prompt) }
                            messages.add(Message(reply, false))
                            if (autoSpeak) onSpeak(reply)
                            busy = false
                        }
                    }
                }) { Text("➤") }
            }
        }
    }
}

private fun requestReply(base: String, token: String, prompt: String): String {
    var connection: HttpURLConnection? = null
    return try {
        val url = URL(base.trimEnd('/') + "/chat")
        connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer " + token.trim())
        }
        connection.outputStream.use { it.write(JSONObject().put("message", prompt).toString().toByteArray(Charsets.UTF_8)) }
        if (connection.responseCode == 401) "Token ـەکە هەڵەیە. لە Settings بیپشکنە."
        else if (connection.responseCode !in 200..299) "Server error: HTTP ${connection.responseCode}"
        else JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optString("reply", "No reply received.")
    } catch (e: Exception) {
        "Connection failed: ${e.localizedMessage ?: "unknown error"}"
    } finally {
        connection?.disconnect()
    }
}
