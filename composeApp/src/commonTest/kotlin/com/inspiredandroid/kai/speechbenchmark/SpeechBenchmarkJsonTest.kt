package com.inspiredandroid.kai.speechbenchmark

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpeechBenchmarkJsonTest {
    @Test
    fun manifestRoundTripsThroughStrictJson() {
        val manifest = SpeechBenchmarkManifest(
            schemaVersion = 1,
            benchmarkId = "tedlium3-smoke-v1",
            normalizationPolicyVersion = "v1",
            clips = listOf(
                SpeechBenchmarkClip(
                    id = "talk-0001",
                    dataset = "TED-LIUM 3",
                    datasetRelease = "release-3",
                    split = "test",
                    audioPath = "tedlium3/talk-0001.wav",
                    audioSha256 = "b".repeat(64),
                    sampleRateHz = 16_000,
                    channels = 1,
                    durationMillis = 42_000,
                    referenceTranscript = "A sample reference transcript.",
                ),
            ),
        )

        assertEquals(manifest, SpeechBenchmarkJson.decodeManifest(SpeechBenchmarkJson.encodeManifest(manifest)))
    }

    @Test
    fun manifestEncodingRejectsInvalidPaths() {
        val manifest = validManifest().copy(
            clips = listOf(validManifest().clips.single().copy(audioPath = "../../private.wav")),
        )

        assertFailsWith<IllegalArgumentException> {
            SpeechBenchmarkJson.encodeManifest(manifest)
        }
    }

    @Test
    fun reportRoundTripsAndRecomputesAggregate() {
        val clip = SpeechBenchmarkClipResult.success(
            clipId = "clip-1",
            audioSha256 = "c".repeat(64),
            audioDurationMillis = 2_000,
            inferenceDurationMillis = 400,
            recognizedText = "Kai can hear me",
            referenceTranscript = "Kai can hear me",
        )
        val run = SpeechBenchmarkRun(
            modelId = "moonshine-tiny-en",
            modelRevision = "sha256:verified-model-digest",
            deviceModel = "SM-S366V",
            androidVersion = "Android 16",
            appRevision = "test-revision",
            sherpaOnnxVersion = "2.0.1",
            threadCount = 2,
            modelLoadMillis = 700,
            installedModelBytes = 45L * 1024L * 1024L,
            memoryBeforeBytes = null,
            peakMemoryBytes = null,
            clipResults = listOf(clip),
        )
        val report = SpeechBenchmarkReport(
            schemaVersion = 1,
            run = run,
            aggregate = SpeechBenchmarkAggregator.aggregate(run),
        )

        assertEquals(report, SpeechBenchmarkJson.decodeReport(SpeechBenchmarkJson.encodeReport(report)))
    }

    @Test
    fun reportEncodingRejectsAnAggregateThatDoesNotMatchClipResults() {
        val run = SpeechBenchmarkRun(
            modelId = "moonshine-tiny-en",
            modelRevision = "test",
            deviceModel = "SM-S366V",
            androidVersion = "Android 16",
            appRevision = "test",
            sherpaOnnxVersion = "2.0.1",
            threadCount = 2,
            modelLoadMillis = 0,
            installedModelBytes = null,
            memoryBeforeBytes = null,
            peakMemoryBytes = null,
            clipResults = emptyList(),
        )
        val report = SpeechBenchmarkReport(
            schemaVersion = 1,
            run = run,
            aggregate = SpeechBenchmarkAggregator.aggregate(run).copy(totalClipCount = 1),
        )

        assertFailsWith<IllegalArgumentException> {
            SpeechBenchmarkJson.encodeReport(report)
        }
    }

    private fun validManifest() = SpeechBenchmarkManifest(
        schemaVersion = 1,
        benchmarkId = "earnings22-smoke-v1",
        normalizationPolicyVersion = "v1",
        clips = listOf(
            SpeechBenchmarkClip(
                id = "clip-0001",
                dataset = "Earnings-22",
                datasetRelease = "published-test-release",
                split = "test",
                audioPath = "earnings22/clip-0001.wav",
                audioSha256 = "a".repeat(64),
                sampleRateHz = 16_000,
                channels = 1,
                durationMillis = 12_500L,
                referenceTranscript = "A documented reference sentence.",
            ),
        ),
    )
}
