package com.inspiredandroid.kai.speechbenchmark

import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.SystemClock
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineMoonshineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.Locale

/**
 * Explicit, local-only device benchmark. It reads only files already imported
 * into filesDir/benchmark-corpus and never downloads a model or sends audio out.
 */
internal object SpeechBenchmarkAndroidRunner {
    private const val SAMPLE_RATE = 16_000
    private const val THREAD_COUNT = 2
    private const val MODEL_ID = "moonshine-tiny-en-quantized"
    private const val MODEL_REVISION =
        "9ec31b342d8fa3240c3b81b8f82e1cf7e3ac467c93ca5a999b741d5887164f8d"
    private const val SHERPA_ONNX_VERSION = "v2.0.1"
    private const val MAX_MANIFEST_BYTES = 2L * 1024L * 1024L
    private const val MAX_CLIP_SECONDS = 120L

    private val modelDirectoryName = "speech-models/moonshine-tiny-en-quantized-2026-02-27"

    fun run(context: Context): File {
        val corpusRoot = File(context.filesDir, "benchmark-corpus").canonicalFile
        require(corpusRoot.isDirectory) {
            "No local benchmark corpus found at filesDir/benchmark-corpus."
        }
        val manifestFile = File(corpusRoot, "manifest.json").canonicalFile
        require(manifestFile.parentFile == corpusRoot && manifestFile.isFile) {
            "The corpus must contain manifest.json at its root."
        }
        require(manifestFile.length() in 1..MAX_MANIFEST_BYTES) {
            "The benchmark manifest is empty or larger than 2 MiB."
        }
        val manifest = SpeechBenchmarkJson.decodeManifest(manifestFile.readText(Charsets.UTF_8))

        val modelDirectory = File(context.filesDir, modelDirectoryName).canonicalFile
        val encoder = File(modelDirectory, "encoder_model.ort")
        val decoder = File(modelDirectory, "decoder_model_merged.ort")
        val tokens = File(modelDirectory, "tokens.txt")
        require(modelDirectory.parentFile == File(context.filesDir, "speech-models").canonicalFile) {
            "The installed model path is not inside app-private storage."
        }
        require(encoder.isFile && decoder.isFile && tokens.isFile) {
            "Moonshine Tiny is not installed. Install it through Kai before running the benchmark."
        }

        val modelBytes = modelDirectory.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val memoryBefore = currentPssBytes()
        val modelConfig = OfflineModelConfig(
            moonshine = OfflineMoonshineModelConfig(
                encoder = encoder.absolutePath,
                mergedDecoder = decoder.absolutePath,
            ),
            tokens = tokens.absolutePath,
            numThreads = THREAD_COUNT,
            provider = "cpu",
        )

        val loadStart = SystemClock.elapsedRealtimeNanos()
        val recognizer = OfflineRecognizer(
            config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
                modelConfig = modelConfig,
            ),
        )
        val modelLoadMillis = (SystemClock.elapsedRealtimeNanos() - loadStart) / 1_000_000L

        val clipResults = try {
            manifest.clips.map { clip ->
                var samples: FloatArray? = null
                try {
                    val audioFile = resolveCorpusFile(corpusRoot, clip.audioPath)
                    val actualHash = sha256(audioFile)
                    require(actualHash.equals(clip.audioSha256, ignoreCase = true)) {
                        "Audio SHA-256 does not match the manifest."
                    }
                    val wav = readPcm16MonoWav(audioFile)
                    require(wav.durationMillis == clip.durationMillis) {
                        "WAV duration does not match the manifest."
                    }
                    val audioSamples = wav.samples
                    samples = audioSamples
                    val stream = recognizer.createStream()
                    val inferenceStart = SystemClock.elapsedRealtimeNanos()
                    val recognizedText = try {
                        stream.acceptWaveform(audioSamples, sampleRate = SAMPLE_RATE)
                        recognizer.decode(stream)
                        recognizer.getResult(stream).text.trim()
                    } finally {
                        stream.release()
                    }
                    val inferenceMillis =
                        (SystemClock.elapsedRealtimeNanos() - inferenceStart) / 1_000_000L
                    SpeechBenchmarkClipResult.success(
                        clipId = clip.id,
                        audioSha256 = actualHash,
                        audioDurationMillis = wav.durationMillis,
                        inferenceDurationMillis = inferenceMillis,
                        recognizedText = recognizedText,
                        referenceTranscript = clip.referenceTranscript,
                    )
                } catch (failure: Exception) {
                    SpeechBenchmarkClipResult.failure(
                        clipId = clip.id,
                        audioSha256 = clip.audioSha256.lowercase(Locale.ROOT),
                        audioDurationMillis = clip.durationMillis,
                        message = failure.message?.take(240) ?: failure.javaClass.simpleName,
                    )
                } finally {
                    samples?.fill(0f)
                }
            }
        } finally {
            recognizer.release()
        }

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName?.takeIf { it.isNotBlank() } ?: "unknown"
        val run = SpeechBenchmarkRun(
            modelId = MODEL_ID,
            modelRevision = MODEL_REVISION,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appRevision = "versionName=$versionName",
            sherpaOnnxVersion = SHERPA_ONNX_VERSION,
            threadCount = THREAD_COUNT,
            modelLoadMillis = modelLoadMillis,
            installedModelBytes = modelBytes,
            memoryBeforeBytes = memoryBefore,
            // A true peak sampler is not installed yet; do not mislabel an end-of-run sample as peak PSS.
            peakMemoryBytes = null,
            clipResults = clipResults,
        )
        val report = SpeechBenchmarkReport(
            schemaVersion = 1,
            run = run,
            aggregate = SpeechBenchmarkAggregator.aggregate(run),
        )
        val reportDirectory = File(context.filesDir, "benchmark-reports")
        check(reportDirectory.exists() || reportDirectory.mkdirs()) {
            "Could not create the private benchmark report directory."
        }
        val reportFile = File(reportDirectory, "run-${System.currentTimeMillis()}.json")
        reportFile.writeText(SpeechBenchmarkJson.encodeReport(report), Charsets.UTF_8)
        return reportFile
    }

    private fun resolveCorpusFile(root: File, relativePath: String): File {
        require(
            relativePath.isNotBlank() && !relativePath.startsWith("/") &&
                !relativePath.startsWith("\\") && !relativePath.contains('\\'),
        ) {
            "Audio path must be a safe relative path."
        }
        val candidate = File(root, relativePath).canonicalFile
        val rootPrefix = root.path + File.separator
        require(candidate.path.startsWith(rootPrefix) && candidate.isFile) {
            "Audio path is missing or escapes the corpus directory."
        }
        return candidate
    }

    private data class WavData(val samples: FloatArray, val durationMillis: Long)

    private fun readPcm16MonoWav(file: File): WavData {
        RandomAccessFile(file, "r").use { input ->
            require(input.length() >= 44L) { "WAV file is too short." }
            require(readFourCc(input) == "RIFF") { "WAV must use a RIFF container." }
            val riffSize = readUInt32(input)
            require(readFourCc(input) == "WAVE") { "RIFF file is not WAVE audio." }
            val riffEnd = 8L + riffSize
            require(riffEnd == input.length()) { "RIFF size does not match the file length." }

            var formatFound = false
            var dataOffset = -1L
            var dataSize = -1L
            var channels = 0
            var sampleRate = 0L
            var bitsPerSample = 0
            var blockAlign = 0
            while (input.filePointer + 8L <= riffEnd) {
                val chunkId = readFourCc(input)
                val chunkSize = readUInt32(input)
                val chunkStart = input.filePointer
                val chunkEnd = chunkStart + chunkSize
                val paddedEnd = chunkEnd + (chunkSize and 1L)
                require(chunkEnd >= chunkStart && paddedEnd <= riffEnd) {
                    "WAV contains an invalid chunk length."
                }

                when (chunkId) {
                    "fmt " -> {
                        require(chunkSize >= 16L) { "WAV format chunk is incomplete." }
                        val format = readUInt16(input)
                        channels = readUInt16(input)
                        sampleRate = readUInt32(input)
                        readUInt32(input) // byte rate
                        blockAlign = readUInt16(input)
                        bitsPerSample = readUInt16(input)
                        require(format == 1) { "Only uncompressed PCM WAV is supported." }
                        formatFound = true
                    }

                    "data" -> {
                        require(dataOffset == -1L) { "Multiple WAV data chunks are not supported." }
                        dataOffset = chunkStart
                        dataSize = chunkSize
                    }
                }
                input.seek(paddedEnd)
            }

            require(formatFound && dataOffset >= 0L) { "WAV is missing fmt or data chunk." }
            require(
                channels == 1 && sampleRate == SAMPLE_RATE.toLong() &&
                    bitsPerSample == 16 && blockAlign == 2,
            ) {
                "WAV must be mono, 16 kHz, signed 16-bit PCM."
            }
            require(dataSize > 0L && dataSize % 2L == 0L) {
                "WAV PCM payload must contain complete 16-bit samples."
            }
            val sampleCount = dataSize / 2L
            require(sampleCount <= SAMPLE_RATE.toLong() * MAX_CLIP_SECONDS) {
                "WAV clip exceeds the 120-second benchmark limit."
            }
            val durationMillis = sampleCount * 1000L / SAMPLE_RATE
            input.seek(dataOffset)
            val samples = FloatArray(sampleCount.toInt()) {
                val low = input.read()
                val high = input.read()
                require(low >= 0 && high >= 0) { "WAV PCM payload is truncated." }
                (((high shl 8) or low).toShort()).toInt() / 32768.0f
            }
            return WavData(samples, durationMillis)
        }
    }

    private fun readFourCc(input: RandomAccessFile): String {
        val bytes = ByteArray(4)
        input.readFully(bytes)
        return bytes.toString(Charsets.US_ASCII)
    }

    private fun readUInt16(input: RandomAccessFile): Int {
        val low = input.readUnsignedByte()
        val high = input.readUnsignedByte()
        return low or (high shl 8)
    }

    private fun readUInt32(input: RandomAccessFile): Long {
        val b0 = input.readUnsignedByte().toLong()
        val b1 = input.readUnsignedByte().toLong()
        val b2 = input.readUnsignedByte().toLong()
        val b3 = input.readUnsignedByte().toLong()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") {
            (it.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }

    private fun currentPssBytes(): Long {
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        return memoryInfo.totalPss.toLong() * 1024L
    }
}
