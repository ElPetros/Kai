package com.inspiredandroid.kai.speechbenchmark

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeechBenchmarkManifestValidatorTest {
    @Test
    fun acceptsAValidMonoSixteenKhzManifest() {
        assertTrue(SpeechBenchmarkManifestValidator.validate(validManifest()).isEmpty())
    }

    @Test
    fun rejectsUnsupportedSchemaAndNormalizationVersions() {
        val errors = SpeechBenchmarkManifestValidator.validate(
            validManifest().copy(schemaVersion = 2, normalizationPolicyVersion = "v2"),
        )

        assertTrue(errors.any { it.contains("schemaVersion") })
        assertTrue(errors.any { it.contains("normalizationPolicyVersion") })
    }

    @Test
    fun rejectsEmptyManifestAndDuplicateClipIds() {
        val emptyErrors = SpeechBenchmarkManifestValidator.validate(validManifest().copy(clips = emptyList()))
        val duplicate = validManifest().clips.single()
        val duplicateErrors = SpeechBenchmarkManifestValidator.validate(
            validManifest().copy(clips = listOf(duplicate, duplicate)),
        )

        assertTrue(emptyErrors.any { it.contains("at least one") })
        assertTrue(duplicateErrors.any { it.contains("must be unique") })
    }

    @Test
    fun rejectsUnsafePathsAndMalformedHashes() {
        val clip = validManifest().clips.single()
        val errors = SpeechBenchmarkManifestValidator.validate(
            validManifest().copy(
                clips = listOf(
                    clip.copy(audioPath = "../outside.wav", audioSha256 = "not-a-hash"),
                ),
            ),
        )

        assertTrue(errors.any { it.contains("safe relative path") })
        assertTrue(errors.any { it.contains("64 hexadecimal") })
    }

    @Test
    fun rejectsWrongAudioFormatAndMissingReference() {
        val clip = validManifest().clips.single()
        val errors = SpeechBenchmarkManifestValidator.validate(
            validManifest().copy(
                clips = listOf(
                    clip.copy(sampleRateHz = 48_000, channels = 2, referenceTranscript = "  "),
                ),
            ),
        )

        assertTrue(errors.any { it.contains("sampleRateHz") })
        assertTrue(errors.any { it.contains("channels") })
        assertTrue(errors.any { it.contains("referenceTranscript") })
    }

    @Test
    fun rejectsZeroOrNegativeDuration() {
        val clip = validManifest().clips.single()
        val errors = SpeechBenchmarkManifestValidator.validate(
            validManifest().copy(clips = listOf(clip.copy(durationMillis = 0L))),
        )

        assertFalse(errors.isEmpty())
        assertTrue(errors.any { it.contains("durationMillis") })
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
