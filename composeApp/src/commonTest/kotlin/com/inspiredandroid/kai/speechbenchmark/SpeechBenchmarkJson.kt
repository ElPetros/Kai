package com.inspiredandroid.kai.speechbenchmark

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class SpeechBenchmarkReport(
    val schemaVersion: Int,
    val run: SpeechBenchmarkRun,
    val aggregate: SpeechBenchmarkAggregate,
)

/** Strict, versioned JSON boundary for local benchmark manifests and reports. */
internal object SpeechBenchmarkJson {
    private const val SUPPORTED_REPORT_SCHEMA_VERSION = 1

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = false
    }

    fun encodeManifest(manifest: SpeechBenchmarkManifest): String {
        requireValidManifest(manifest)
        return json.encodeToString(manifest)
    }

    fun decodeManifest(source: String): SpeechBenchmarkManifest {
        val manifest = json.decodeFromString<SpeechBenchmarkManifest>(source)
        requireValidManifest(manifest)
        return manifest
    }

    fun encodeReport(report: SpeechBenchmarkReport): String {
        requireValidReport(report)
        return json.encodeToString(report)
    }

    fun decodeReport(source: String): SpeechBenchmarkReport {
        val report = json.decodeFromString<SpeechBenchmarkReport>(source)
        requireValidReport(report)
        return report
    }

    private fun requireValidManifest(manifest: SpeechBenchmarkManifest) {
        val errors = SpeechBenchmarkManifestValidator.validate(manifest)
        require(errors.isEmpty()) {
            "Invalid speech benchmark manifest: " + errors.joinToString(" ")
        }
    }

    private fun requireValidReport(report: SpeechBenchmarkReport) {
        require(report.schemaVersion == SUPPORTED_REPORT_SCHEMA_VERSION) {
            "Unsupported speech benchmark report schema version."
        }
        require(report.aggregate == SpeechBenchmarkAggregator.aggregate(report.run)) {
            "Speech benchmark report aggregate does not match its per-clip results."
        }
    }
}
