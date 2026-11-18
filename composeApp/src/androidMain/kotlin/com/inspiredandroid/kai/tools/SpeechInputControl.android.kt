package com.inspiredandroid.kai.tools

import android.media.AudioRecord
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.inspiredandroid.kai.permissionQualifier
import com.inspiredandroid.kai.tools.VoiceSessionCoordinator
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.speech_input_added
import kai.composeapp.generated.resources.speech_input_error
import kai.composeapp.generated.resources.speech_input_model_ready
import kai.composeapp.generated.resources.speech_input_no_speech
import kai.composeapp.generated.resources.speech_input_permission_denied
import kai.composeapp.generated.resources.speech_input_preparing
import kai.composeapp.generated.resources.speech_input_recording
import kai.composeapp.generated.resources.speech_input_start
import kai.composeapp.generated.resources.speech_input_stop
import kai.composeapp.generated.resources.speech_input_transcribing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
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
    target: VoiceInputTarget,
    modifier: Modifier,
) {
    val context = LocalContext.current.applicationContext
    val permissionController = koinInject<PermissionController>(
        permissionQualifier(AppPermission.RECORD_AUDIO),
    )
    val voiceCoordinator = koinInject<VoiceSessionCoordinator>()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val modelStore = remember(context) { MoonshineModelStore(context) }
    val activeStopRequest = remember { AtomicReference<AtomicBoolean?>(null) }
    val recorderReference = remember { AtomicReference<AudioRecord?>(null) }
    val jobReference = remember { AtomicReference<Job?>(null) }

    var state by remember { mutableStateOf(SpeechInputState.Idle) }
    var status by remember { mutableStateOf("") }

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
        activeStopRequest.get()?.set(true)
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
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            enabled = state != SpeechInputState.Preparing && state != SpeechInputState.Transcribing,
            onClick = {
                if (state == SpeechInputState.Recording) {
                    stopRecording()
                } else {
                    val operationStopRequest = AtomicBoolean(false)
                    val speechJob = scope.launch(start = CoroutineStart.LAZY) {
                        var capturedSamples: FloatArray? = null
                        var voiceSession: VoiceSession? = null
                        try {
                            state = SpeechInputState.Preparing
                            if (!permissionController.hasPermission() &&
                                !permissionController.requestPermission()
                            ) {
                                state = SpeechInputState.Error
                                status = permissionDeniedText
                                return@launch
                            }

                            // Acquire global voice ownership before model preparation as well as
                            // recording. Otherwise two controls can race to install into the
                            // same shared staging directory on first use.
                            val acquiredSession = voiceCoordinator.begin(
                                mode = VoiceInteractionMode.DICTATION,
                                target = target,
                            )
                            if (acquiredSession == null) {
                                state = SpeechInputState.Error
                                status = errorText
                                return@launch
                            }
                            voiceSession = acquiredSession

                            if (!modelStore.isInstalled()) {
                                status = preparingText
                                modelStore.ensureInstalled()
                                state = SpeechInputState.Ready
                                status = modelReadyText
                                return@launch
                            }

                            val sessionId = acquiredSession.id
                            status = recordingText
                            val samples = captureSpeech(
                                stopRequested = operationStopRequest,
                                recorderReference = recorderReference,
                                onStarted = {
                                    state = SpeechInputState.Recording
                                    voiceCoordinator.updatePhase(sessionId, VoiceSessionPhase.LISTENING)
                                },
                            )
                            capturedSamples = samples

                            voiceCoordinator.updatePhase(sessionId, VoiceSessionPhase.PROCESSING)
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
                            activeStopRequest.compareAndSet(operationStopRequest, null)
                            jobReference.compareAndSet(currentCoroutineContext()[Job], null)
                            withContext(NonCancellable) {
                                voiceSession?.let { voiceCoordinator.end(it.id) }
                            }
                        }
                    }
                    // Publish the operation-specific cancellation signal and lazy job before
                    // starting it, so an older operation can never clear a newer one.
                    activeStopRequest.set(operationStopRequest)
                    jobReference.set(speechJob)
                    speechJob.start()
                }
            },
        ) {
            Icon(
                imageVector = if (state == SpeechInputState.Recording) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (state == SpeechInputState.Recording) stopDescription else startDescription,
                tint = if (state == SpeechInputState.Recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (status.isNotBlank()) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = if (state == SpeechInputState.Error) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
