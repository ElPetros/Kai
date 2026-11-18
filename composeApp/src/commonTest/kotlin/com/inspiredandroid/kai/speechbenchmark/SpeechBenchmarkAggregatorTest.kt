package com.inspiredandroid.kai.speechbenchmark

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpeechBenchmarkAggregatorTest {
    @Test
    fun corpusWerIsWeightedByReferenceWordCount() {
        val run = runWith(
            SpeechBenchmarkClipResult.success(
                clipId = "short",
                audioSha256 = hash('a'),
                audioDurationMillis = 1_000,
                inferenceDurationMillis = 100,
                recognizedText = "one wrong",
                referenceTranscript = "one two",
            ),
            SpeechBenchmarkClipResult.success(
                clipId = "long",
                audioSha256 = hash('b'),
                audioDurationMillis = 1_000,
                inferenceDurationMillis = 300,
                recognizedText = "one two three four five six seven eight nine ten",
                referenceTranscript = "one two three four five six seven eight nine ten",
            ),
        )

        val aggregate = SpeechBenchmarkAggregator.aggregate(run)

        assertEquals(1, aggregate.substitutions)
        assertEquals(0, aggregate.deletions)
        assertEquals(0, aggregate.insertions)
        assertEquals(12, aggregate.referenceWordCount)
        assertEquals(1.0 / 12.0, aggregate.corpusWordErrorRate)
    }

    @Test
    fun reportsMedianLatencyAndRealTimeFactor() {
        val run = runWith(
            successfulClip("first", hash('a'), audioMillis = 1_000, inferenceMillis = 100),
            successfulClip("second", hash('b'), audioMillis = 1_000, inferenceMillis = 300),
        )

        val aggregate = SpeechBenchmarkAggregator.aggregate(run)

        assertEquals(200.0, aggregate.medianInferenceMillis)
        assertEquals(0.2, aggregate.medianRealTimeFactor)
        assertEquals(100L, aggregate.minInferenceMillis)
        assertEquals(300L, aggregate.maxInferenceMillis)
    }

    @Test
    fun reportsFailuresSeparatelyAndExcludesThemFromWer() {
        val run = runWith(
            successfulClip("ok", hash('a'), audioMillis = 1_000, inferenceMillis = 100),
            SpeechBenchmarkClipResult.failure(
                clipId = "failed",
                audioSha256 = hash('b'),
                audioDurationMillis = 2_000,
                message = "Recognizer timed out.",
            ),
        )

        val aggregate = SpeechBenchmarkAggregator.aggregate(run)

        assertEquals(2, aggregate.totalClipCount)
        assertEquals(1, aggregate.successfulClipCount)
        assertEquals(1, aggregate.failedClipCount)
        assertEquals(0.5, aggregate.failureRate)
        assertEquals(0.0, aggregate.corpusWordErrorRate)
    }

    @Test
    fun allFailedClipsHaveNoAccuracyOrLatencyEstimate() {
        val run = runWith(
            SpeechBenchmarkClipResult.failure(
                clipId = "failed",
                audioSha256 = hash('c'),
                audioDurationMillis = 500,
                message = "Native recognizer failed to load.",
            ),
        )

        val aggregate = SpeechBenchmarkAggregator.aggregate(run)

        assertNull(aggregate.corpusWordErrorRate)
        assertNull(aggregate.medianInferenceMillis)
        assertNull(aggregate.medianRealTimeFactor)
        assertEquals(1, aggregate.failedClipCount)
    }

    @Test
    fun emptyRunProducesEmptyAggregatesWithoutInventingMeasurements() {
        val aggregate = SpeechBenchmarkAggregator.aggregate(runWith())

        assertEquals(0, aggregate.totalClipCount)
        assertEquals(0.0, aggregate.failureRate)
        assertNull(aggregate.corpusWordErrorRate)
        assertNull(aggregate.medianInferenceMillis)
    }

    private fun successfulClip(
        id: String,
        sha: String,
        audioMillis: Long,
        inferenceMillis: Long,
    ) = SpeechBenchmarkClipResult.success(
        clipId = id,
        audioSha256 = sha,
        audioDurationMillis = audioMillis,
        inferenceDurationMillis = inferenceMillis,
        recognizedText = "one two three",
        referenceTranscript = "one two three",
    )

    private fun runWith(vararg clips: SpeechBenchmarkClipResult) = SpeechBenchmarkRun(
        modelId = "moonshine-tiny-en",
        modelRevision = "sha256:example-model-digest",
        deviceModel = "SM-S366V",
        androidVersion = "Android 16",
        appRevision = "benchmark-test",
        sherpaOnnxVersion = "2.0.1",
        threadCount = 2,
        modelLoadMillis = 750,
        installedModelBytes = 45L * 1024L * 1024L,
        memoryBeforeBytes = 300L * 1024L * 1024L,
        peakMemoryBytes = 420L * 1024L * 1024L,
        clipResults = clips.toList(),
    )

    private fun hash(character: Char) = character.toString().repeat(64)
}
