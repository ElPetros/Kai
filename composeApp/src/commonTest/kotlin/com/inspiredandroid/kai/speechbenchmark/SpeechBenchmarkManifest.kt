package com.inspiredandroid.kai.speechbenchmark

/**
 * Runtime description of one fixed, preprocessed benchmark corpus.
 *
 * Audio files are local inputs and are not bundled in the repository. Paths
 * are relative to the benchmark corpus root and identify mono 16 kHz audio.
 */
internal data class SpeechBenchmarkManifest(
    val schemaVersion: Int,
    val benchmarkId: String,
    val normalizationPolicyVersion: String,
    val clips: List<SpeechBenchmarkClip>,
)

internal data class SpeechBenchmarkClip(
    val id: String,
    val dataset: String,
    val datasetRelease: String,
    val split: String,
    val audioPath: String,
    val audioSha256: String,
    val sampleRateHz: Int,
    val channels: Int,
    val durationMillis: Long,
    val referenceTranscript: String,
)

internal object SpeechBenchmarkManifestValidator {
    private const val SUPPORTED_SCHEMA_VERSION = 1
    private const val SUPPORTED_NORMALIZATION_POLICY = "v1"
    private const val BENCHMARK_SAMPLE_RATE_HZ = 16_000
    private const val BENCHMARK_CHANNELS = 1
    private val sha256Pattern = Regex("[0-9a-fA-F]{64}")

    /**
     * Returns stable, human-readable validation errors. An empty list means
     * the manifest is structurally valid; it does not verify file bytes.
     */
    fun validate(manifest: SpeechBenchmarkManifest): List<String> {
        val errors = mutableListOf<String>()

        if (manifest.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            errors += "schemaVersion must be $SUPPORTED_SCHEMA_VERSION."
        }
        if (manifest.benchmarkId.isBlank()) {
            errors += "benchmarkId must not be blank."
        }
        if (manifest.normalizationPolicyVersion != SUPPORTED_NORMALIZATION_POLICY) {
            errors += "normalizationPolicyVersion must be $SUPPORTED_NORMALIZATION_POLICY."
        }
        if (manifest.clips.isEmpty()) {
            errors += "clips must contain at least one entry."
        }

        val duplicateIds = manifest.clips
            .groupingBy { it.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        duplicateIds.forEach { errors += "clip id must be unique: $it." }

        manifest.clips.forEachIndexed { index, clip ->
            val prefix = "clips[$index]"
            if (clip.id.isBlank()) errors += "$prefix.id must not be blank."
            if (clip.dataset.isBlank()) errors += "$prefix.dataset must not be blank."
            if (clip.datasetRelease.isBlank()) errors += "$prefix.datasetRelease must not be blank."
            if (clip.split.isBlank()) errors += "$prefix.split must not be blank."
            if (!isSafeRelativeAudioPath(clip.audioPath)) {
                errors += "$prefix.audioPath must be a safe relative path."
            }
            if (!sha256Pattern.matches(clip.audioSha256)) {
                errors += "$prefix.audioSha256 must contain exactly 64 hexadecimal characters."
            }
            if (clip.sampleRateHz != BENCHMARK_SAMPLE_RATE_HZ) {
                errors += "$prefix.sampleRateHz must be $BENCHMARK_SAMPLE_RATE_HZ."
            }
            if (clip.channels != BENCHMARK_CHANNELS) {
                errors += "$prefix.channels must be $BENCHMARK_CHANNELS."
            }
            if (clip.durationMillis <= 0L) {
                errors += "$prefix.durationMillis must be greater than zero."
            }
            if (clip.referenceTranscript.isBlank()) {
                errors += "$prefix.referenceTranscript must not be blank."
            }
        }

        return errors
    }

    private fun isSafeRelativeAudioPath(path: String): Boolean {
        if (path.isBlank() || path.startsWith("/") || path.startsWith("\\")) return false
        if (path.contains('\\') || Regex("^[A-Za-z]:").containsMatchIn(path)) return false
        val segments = path.split('/')
        return segments.none { it.isBlank() || it == "." || it == ".." }
    }
}
