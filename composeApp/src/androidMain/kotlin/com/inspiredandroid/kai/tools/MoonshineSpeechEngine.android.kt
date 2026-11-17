package com.inspiredandroid.kai.tools

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineMoonshineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private const val SPEECH_SAMPLE_RATE = 16_000
private const val MAX_RECORDING_SECONDS = 60
private const val MAX_RECORDING_SAMPLES = SPEECH_SAMPLE_RATE * MAX_RECORDING_SECONDS
private const val MODEL_ARCHIVE_BYTES = 29_858_559L
private const val MODEL_ARCHIVE_SHA256 = "9ec31b342d8fa3240c3b81b8f82e1cf7e3ac467c93ca5a999b741d5887164f8d"
private const val MODEL_ARCHIVE_URL =
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-moonshine-tiny-en-quantized-2026-02-27.tar.bz2"
private const val MODEL_DIRECTORY_NAME = "moonshine-tiny-en-quantized-2026-02-27"

internal class MoonshineModelStore(context: android.content.Context) {
    private val appContext = context.applicationContext
    val modelDirectory = File(appContext.filesDir, "speech-models/$MODEL_DIRECTORY_NAME")

    fun isInstalled(): Boolean {
        val encoder = File(modelDirectory, "encoder_model.ort")
        val decoder = File(modelDirectory, "decoder_model_merged.ort")
        val tokens = File(modelDirectory, "tokens.txt")
        return encoder.isFile && encoder.length() > 10_000_000L &&
            decoder.isFile && decoder.length() > 25_000_000L &&
            tokens.isFile && tokens.length() > 100_000L
    }

    suspend fun ensureInstalled() = withContext(Dispatchers.IO) {
        if (isInstalled()) return@withContext

        val freeBytes = android.os.StatFs(appContext.filesDir.absolutePath).availableBytes
        if (freeBytes < 100L * 1024L * 1024L) {
            throw IOException("At least 100 MB of free space is needed to install offline speech input.")
        }

        val archive = File(appContext.cacheDir, "moonshine-tiny-en-v2.tar.bz2.part")
        val staging = File(modelDirectory.parentFile, "$MODEL_DIRECTORY_NAME.installing")
        try {
            downloadAndVerify(archive)
            staging.deleteRecursively()
            if (!staging.mkdirs() && !staging.isDirectory) {
                throw IOException("Could not create the speech model directory.")
            }
            extractModel(archive, staging)
            val required = listOf(
                File(staging, "encoder_model.ort"),
                File(staging, "decoder_model_merged.ort"),
                File(staging, "tokens.txt"),
                File(staging, "LICENSE"),
            )
            if (required.any { !it.isFile || it.length() == 0L }) {
                throw IOException("The downloaded speech model is incomplete.")
            }
            modelDirectory.deleteRecursively()
            if (!staging.renameTo(modelDirectory)) {
                throw IOException("Could not finish installing the speech model.")
            }
        } finally {
            archive.delete()
            staging.deleteRecursively()
        }
    }

    private suspend fun downloadAndVerify(destination: File) {
        val connection = (URL(MODEL_ARCHIVE_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Kai-offline-speech-input")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("The offline speech model could not be downloaded.")
            }
            if (connection.contentLengthLong > 0 && connection.contentLengthLong != MODEL_ARCHIVE_BYTES) {
                throw IOException("The speech model download has an unexpected size.")
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        downloaded += count
                        if (downloaded > MODEL_ARCHIVE_BYTES) {
                            throw IOException("The speech model download is larger than expected.")
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            }
            val actualHash = digest.digest().joinToString("") {
                (it.toInt() and 0xff).toString(16).padStart(2, '0')
            }
            if (downloaded != MODEL_ARCHIVE_BYTES || actualHash != MODEL_ARCHIVE_SHA256) {
                throw IOException("The speech model download failed its integrity check.")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun extractModel(archive: File, destination: File) {
        val allowedFiles = setOf(
            "encoder_model.ort",
            "decoder_model_merged.ort",
            "tokens.txt",
            "LICENSE",
        )
        var extractedBytes = 0L
        TarArchiveInputStream(
            BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archive))),
        ).use { tar ->
            while (true) {
                val entry = tar.nextTarEntry ?: break
                if (entry.isDirectory) continue
                val fileName = entry.name.substringAfterLast('/')
                if (fileName !in allowedFiles) continue

                val outputFile = File(destination, fileName)
                FileOutputStream(outputFile).buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = tar.read(buffer)
                        if (count < 0) break
                        extractedBytes += count
                        if (extractedBytes > 50L * 1024L * 1024L) {
                            throw IOException("The extracted speech model is larger than expected.")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
        }
    }
}

internal suspend fun captureSpeech(
    stopRequested: AtomicBoolean,
    recorderReference: AtomicReference<AudioRecord?>,
    onStarted: () -> Unit,
): FloatArray = withContext(Dispatchers.IO) {
    val minimumBufferBytes = AudioRecord.getMinBufferSize(
        SPEECH_SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    )
    if (minimumBufferBytes <= 0) {
        throw IOException("The microphone does not support the required audio format.")
    }

    val recorder = AudioRecord(
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        SPEECH_SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        maxOf(minimumBufferBytes * 2, SPEECH_SAMPLE_RATE * 2),
    )
    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
        recorder.release()
        throw IOException("The microphone could not be initialized.")
    }

    val captured = ShortArray(MAX_RECORDING_SAMPLES)
    val chunk = ShortArray(3_200)
    var capturedSamples = 0
    recorderReference.set(recorder)
    try {
        recorder.startRecording()
        withContext(Dispatchers.Main.immediate) {
            onStarted()
        }
        while (!stopRequested.get() && capturedSamples < captured.size) {
            val read = recorder.read(
                chunk,
                0,
                minOf(chunk.size, captured.size - capturedSamples),
                AudioRecord.READ_BLOCKING,
            )
            when {
                read > 0 -> {
                    chunk.copyInto(captured, capturedSamples, 0, read)
                    capturedSamples += read
                }

                read == AudioRecord.ERROR_INVALID_OPERATION && stopRequested.get() -> break

                read == AudioRecord.ERROR_DEAD_OBJECT -> throw IOException("The microphone connection was lost.")

                read < 0 -> throw IOException("Audio capture stopped unexpectedly.")
            }
        }
    } finally {
        recorderReference.compareAndSet(recorder, null)
        if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            runCatching { recorder.stop() }
        }
        recorder.release()
    }

    if (capturedSamples < SPEECH_SAMPLE_RATE / 3) {
        throw IOException("The recording was too short. Try speaking for a little longer.")
    }
    FloatArray(capturedSamples) { captured[it] / 32768.0f }
}

internal fun transcribeMoonshineV2(modelDirectory: File, samples: FloatArray): String {
    require(samples.isNotEmpty()) { "No audio was captured." }

    val modelConfig = OfflineModelConfig(
        moonshine = OfflineMoonshineModelConfig(
            encoder = File(modelDirectory, "encoder_model.ort").absolutePath,
            mergedDecoder = File(modelDirectory, "decoder_model_merged.ort").absolutePath,
        ),
        tokens = File(modelDirectory, "tokens.txt").absolutePath,
        numThreads = 2,
        provider = "cpu",
    )
    val recognizer = OfflineRecognizer(
        config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = SPEECH_SAMPLE_RATE, featureDim = 80),
            modelConfig = modelConfig,
        ),
    )
    try {
        val stream = recognizer.createStream()
        try {
            stream.acceptWaveform(samples, sampleRate = SPEECH_SAMPLE_RATE)
            recognizer.decode(stream)
            return recognizer.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    } finally {
        recognizer.release()
    }
}
