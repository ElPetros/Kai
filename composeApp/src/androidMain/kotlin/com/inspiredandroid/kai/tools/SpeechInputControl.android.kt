package com.inspiredandroid.kai.tools

import android.media.AudioRecord
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.inspiredandroid.kai.permissionQualifier
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.speech_input_added
import kai.composeapp.generated.resources.speech_input_error
import kai.composeapp.generated.resources.speech_input_label
import kai.composeapp.generated.resources.speech_input_model_ready
import kai.composeapp.generated.resources.speech_input_no_speech
import kai.composeapp.generated.resources.speech_input_permission_denied
import kai.composeapp.generated.resources.speech_input_preparing
import kai.composeapp.generated.resources.speech_input_recording
import kai.composeapp.generated.resources.speech_input_start
import kai.composeapp.generated.resources.speech_input_stop
import kai.composeapp.generated.resources.speech_input_transcribing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private enum class SpeechInputState {
    Idle,
    Preparing,
    Ready,
    Recording,
    Transcribing,
    Error,
}

@Composable
actual fun SpeechInputControl(
    onTranscript: (String) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current.applicationContext
    val permissionController = koinInject<PermissionController>(
        permissionQualifier(AppPermission.RECORD_AUDIO),
    )
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val modelStore = remember(context) { MoonshineModelStore(context) }
    val stopRequested = remember { AtomicBoolean(false) }
    val recorderReference = remember { AtomicReference<AudioRecord?>(null) }
    val jobReference = remember { AtomicReference<Job?>(null) }

    var state by remember { mutableStateOf(SpeechInputState.Idle) }
    var status by remember { mutableStateOf("") }

    val label = stringResource(Res.string.speech_input_label)
    val startDescription = stringResource(Res.string.speech_input_start)
    val stopDescription = stringResource(Res.string.speech_input_stop)
    val preparingText = stringResource(Res.string.speech_input_preparing)
    val modelReadyText = stringResource(Res.string.speech_input_model_ready)
    val recordingText = stringResource(Res.string.speech_input_recording)
    val transcribingText = stringResource(Res.string.speech_input_transcribing)
    val addedText = stringResource(Res.string.speech_input_added)
    val noSpeechText = stringResource(Res.string.speech_input_no_speech)
    val permissionDeniedText = stringResource(Res.string.speech_input_permission_denied)
    val errorText = stringResource(Res.string.speech_input_error)

    fun stopRecording() {
        stopRequested.set(true)
        recorderReference.get()?.let { recorder ->
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                runCatching { recorder.stop() }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // Do not leave the microphone active when Kai moves to the background.
                stopRecording()
                jobReference.getAndSet(null)?.cancel()
                state = SpeechInputState.Idle
                status = ""
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopRecording()
            jobReference.getAndSet(null)?.cancel()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            enabled = state != SpeechInputState.Preparing && state != SpeechInputState.Transcribing,
            onClick = {
                if (state == SpeechInputState.Recording) {
                    stopRecording()
                } else {
                    jobReference.set(
                        scope.launch {
                            var capturedSamples: FloatArray? = null
                            try {
                                state = SpeechInputState.Preparing
                                if (!permissionController.hasPermission() &&
                                    !permissionController.requestPermission()
                                ) {
                                    state = SpeechInputState.Error
                                    status = permissionDeniedText
                                    return@launch
                                }

                                if (!modelStore.isInstalled()) {
                                    status = preparingText
                                    modelStore.ensureInstalled()
                                    state = SpeechInputState.Ready
                                    status = modelReadyText
                                    return@launch
                                }

                                stopRequested.set(false)
                                status = recordingText
                                val samples = captureSpeech(
                                    stopRequested = stopRequested,
                                    recorderReference = recorderReference,
                                    onStarted = { state = SpeechInputState.Recording },
                                )
                                capturedSamples = samples

                                state = SpeechInputState.Transcribing
                                status = transcribingText
                                val transcript = withContext(Dispatchers.Default) {
                                    transcribeMoonshineV2(modelStore.modelDirectory, samples)
                                }
                                if (transcript.isBlank()) {
                                    state = SpeechInputState.Ready
                                    status = noSpeechText
                                } else {
                                    onTranscript(transcript)
                                    state = SpeechInputState.Ready
                                    status = addedText
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                state = SpeechInputState.Error
                                status = errorText
                            } finally {
                                // Audio exists only for this operation. Wipe the PCM
                                // supplied to the recognizer on success, failure, or cancel.
                                capturedSamples?.fill(0.0f)
                                stopRequested.set(false)
                                jobReference.set(null)
                            }
                        },
                    )
                }
            },
        ) {
            Icon(
                imageVector = if (state == SpeechInputState.Recording) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (state == SpeechInputState.Recording) stopDescription else startDescription,
                tint = if (state == SpeechInputState.Recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = status.ifBlank { label },
            style = MaterialTheme.typography.bodySmall,
            color = if (state == SpeechInputState.Error) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
