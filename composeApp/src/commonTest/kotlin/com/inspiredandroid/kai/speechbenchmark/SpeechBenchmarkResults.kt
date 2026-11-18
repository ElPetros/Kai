package com.inspiredandroid.kai.speechbenchmark

/** Metadata needed to interpret and reproduce one model's device benchmark. */
internal data class SpeechBenchmarkRun(
    val modelId: String,
    val modelRevision: String,
    val deviceModel: String,
    val androidVersion: String,
    val appRevision: String,
    val sherpaOnnxVersion: String,
    val threadCount: Int,
    val modelLoadMillis: Long,
    val installedModelBytes: Long?,
    val memoryBeforeBytes: Long?,
    val peakMemoryBytes: Long?,
    val clipResults: List<SpeechBenchmarkClipResult>,
) {
    init {
        require(modelId.isNotBlank())
        require(modelRevision.isNotBlank())
        require(deviceModel.isNotBlank())
        require(androidVersion.isNotBlank())
        require(appRevision.isNotBlank())
        require(sherpaOnnxVersion.isNotBlank())
        require(threadCount > 0)
        require(modelLoadMillis >= 0L)
        require(installedModelBytes == null || installedModelBytes >= 0L)
        require(memoryBeforeBytes == null || memoryBeforeBytes >= 0L)
        require(peakMemoryBytes == null || peakMemoryBytes >= 0L)
        require(clipResults.map { it.clipId }.distinct().size == clipResults.size) {
            "A benchmark run cannot contain duplicate clip IDs."
        }
    }
}

/**
 * One clip outcome. Failed clips remain visible in failure-rate reporting but
 * are excluded from WER and latency aggregates.
 */
internal data class SpeechBenchmarkClipResult(
    val clipId: String,
    val audioSha256: String,
    val audioDurationMillis: Long,
    val inferenceDurationMillis: Long?,
    val recognizedText: String?,
    val wer: WordErrorRate.Result?,
    val failureMessage: String?,
) {
    init {
        require(clipId.isNotBlank())
        require(Regex("[0-9a-fA-F]{64}").matches(audioSha256))
        require(audioDurationMillis > 0L)

        if (failureMessage == null) {
            require(inferenceDurationMillis != null && inferenceDurationMillis >= 0L)
            require(recognizedText != null)
            require(wer != null)
        } else {
            require(failureMessage.isNotBlank())
            require(inferenceDurationMillis == null)
            require(recognizedText == null)
            require(wer == null)
        }
    }

    companion object {
        fun success(
            clipId: String,
            audioSha256: String,
            audioDurationMillis: Long,
            inferenceDurationMillis: Long,
            recognizedText: String,
            referenceTranscript: String,
        ) = SpeechBenchmarkClipResult(
            clipId = clipId,
            audioSha256 = audioSha256,
            audioDurationMillis = audioDurationMillis,
            inferenceDurationMillis = inferenceDurationMillis,
            recognizedText = recognizedText,
            wer = WordErrorRate.score(referenceTranscript, recognizedText),
            failureMessage = null,
        )

        fun failure(
            clipId: String,
            audioSha256: String,
            audioDurationMillis: Long,
            message: String,
        ) = SpeechBenchmarkClipResult(
            clipId = clipId,
            audioSha256 = audioSha256,
            audioDurationMillis = audioDurationMillis,
            inferenceDurationMillis = null,
            recognizedText = null,
            wer = null,
            failureMessage = message,
        )
    }
}

internal data class SpeechBenchmarkAggregate(
    val totalClipCount: Int,
    val successfulClipCount: Int,
    val failedClipCount: Int,
    val failureRate: Double,
    val substitutions: Int,
    val deletions: Int,
    val insertions: Int,
    val referenceWordCount: Int,
    val corpusWordErrorRate: Double?,
    val medianInferenceMillis: Double?,
    val minInferenceMillis: Long?,
    val maxInferenceMillis: Long?,
    val medianRealTimeFactor: Double?,
    val modelLoadMillis: Long,
    val installedModelBytes: Long?,
    val memoryBeforeBytes: Long?,
    val peakMemoryBytes: Long?,
)

/** Aggregates only successful clips and weights corpus WER by reference words. */
internal object SpeechBenchmarkAggregator {
    fun aggregate(run: SpeechBenchmarkRun): SpeechBenchmarkAggregate {
        val successful = run.clipResults.filter { it.failureMessage == null }
        val failedCount = run.clipResults.size - successful.size

        val substitutions = successful.sumOf { it.wer!!.substitutions }
        val deletions = successful.sumOf { it.wer!!.deletions }
        val insertions = successful.sumOf { it.wer!!.insertions }
        val referenceWordCount = successful.sumOf { it.wer!!.referenceWordCount }
        val totalErrors = substitutions + deletions + insertions

        val inferenceTimes = successful.map { it.inferenceDurationMillis!! }.sorted()
        val realTimeFactors = successful.map {
            it.inferenceDurationMillis!!.toDouble() / it.audioDurationMillis.toDouble()
        }.sorted()

        return SpeechBenchmarkAggregate(
            totalClipCount = run.clipResults.size,
            successfulClipCount = successful.size,
            failedClipCount = failedCount,
            failureRate = if (run.clipResults.isEmpty()) 0.0 else failedCount.toDouble() / run.clipResults.size,
            substitutions = substitutions,
            deletions = deletions,
            insertions = insertions,
            referenceWordCount = referenceWordCount,
            corpusWordErrorRate = if (referenceWordCount == 0) null else totalErrors.toDouble() / referenceWordCount,
            medianInferenceMillis = median(inferenceTimes),
            minInferenceMillis = inferenceTimes.firstOrNull(),
            maxInferenceMillis = inferenceTimes.lastOrNull(),
            medianRealTimeFactor = median(realTimeFactors),
            modelLoadMillis = run.modelLoadMillis,
            installedModelBytes = run.installedModelBytes,
            memoryBeforeBytes = run.memoryBeforeBytes,
            peakMemoryBytes = run.peakMemoryBytes,
        )
    }

    private fun median(sortedValues: List<Long>): Double? {
        if (sortedValues.isEmpty()) return null
        val middle = sortedValues.size / 2
        return if (sortedValues.size % 2 == 1) {
            sortedValues[middle].toDouble()
        } else {
            (sortedValues[middle - 1].toDouble() + sortedValues[middle].toDouble()) / 2.0
        }
    }

    private fun median(sortedValues: List<Double>): Double? {
        if (sortedValues.isEmpty()) return null
        val middle = sortedValues.size / 2
        return if (sortedValues.size % 2 == 1) {
            sortedValues[middle]
        } else {
            (sortedValues[middle - 1] + sortedValues[middle]) / 2.0
        }
    }
}
