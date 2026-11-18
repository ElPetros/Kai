package com.inspiredandroid.kai.speechbenchmark

/**
 * Deterministic word-error-rate scoring for offline ASR benchmark transcripts.
 *
 * Normalization policy v1:
 * - Unicode lowercase using Locale.ROOT.
 * - Curly apostrophes are normalized to straight apostrophes.
 * - Tokens contain Unicode letters/digits; an internal apostrophe is retained.
 * - Punctuation and whitespace are separators.
 * - Numbers are not rewritten or expanded.
 *
 * A score with an empty reference has no defined WER denominator and therefore
 * reports [wordErrorRate] as null, while still reporting insertion counts.
 */
internal object WordErrorRate {
    private val tokenPattern = Regex("[\\p{L}\\p{N}]+(?:'[\\p{L}\\p{N}]+)*")

    fun score(reference: String, hypothesis: String): Result {
        val referenceWords = tokenize(reference)
        val hypothesisWords = tokenize(hypothesis)
        val rows = referenceWords.size + 1
        val columns = hypothesisWords.size + 1
        val distances = Array(rows) { IntArray(columns) }
        val operations = Array(rows) { ByteArray(columns) }

        for (i in 1 until rows) {
            distances[i][0] = i
            operations[i][0] = DELETE
        }
        for (j in 1 until columns) {
            distances[0][j] = j
            operations[0][j] = INSERT
        }

        for (i in 1 until rows) {
            for (j in 1 until columns) {
                if (referenceWords[i - 1] == hypothesisWords[j - 1]) {
                    distances[i][j] = distances[i - 1][j - 1]
                    operations[i][j] = MATCH
                } else {
                    val substitution = distances[i - 1][j - 1] + 1
                    val deletion = distances[i - 1][j] + 1
                    val insertion = distances[i][j - 1] + 1
                    // Stable tie-break: substitution, then deletion, then insertion.
                    when (minOf(substitution, deletion, insertion)) {
                        substitution -> {
                            distances[i][j] = substitution
                            operations[i][j] = SUBSTITUTE
                        }
                        deletion -> {
                            distances[i][j] = deletion
                            operations[i][j] = DELETE
                        }
                        else -> {
                            distances[i][j] = insertion
                            operations[i][j] = INSERT
                        }
                    }
                }
            }
        }

        var substitutions = 0
        var deletions = 0
        var insertions = 0
        var i = referenceWords.size
        var j = hypothesisWords.size
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && operations[i][j] == MATCH -> {
                    i--
                    j--
                }
                i > 0 && j > 0 && operations[i][j] == SUBSTITUTE -> {
                    substitutions++
                    i--
                    j--
                }
                i > 0 && (j == 0 || operations[i][j] == DELETE) -> {
                    deletions++
                    i--
                }
                j > 0 -> {
                    insertions++
                    j--
                }
            }
        }

        val referenceCount = referenceWords.size
        val totalErrors = substitutions + deletions + insertions
        return Result(
            substitutions = substitutions,
            deletions = deletions,
            insertions = insertions,
            referenceWordCount = referenceCount,
            hypothesisWordCount = hypothesisWords.size,
            wordErrorRate = if (referenceCount == 0) null else totalErrors.toDouble() / referenceCount,
        )
    }

    fun normalize(text: String): String =
        tokenize(text).joinToString(" ")

    private fun tokenize(text: String): List<String> =
        tokenPattern.findAll(text.lowercase(java.util.Locale.ROOT).replace('’', '\''))
            .map { it.value }
            .toList()

    private const val MATCH: Byte = 0
    private const val SUBSTITUTE: Byte = 1
    private const val DELETE: Byte = 2
    private const val INSERT: Byte = 3

    data class Result(
        val substitutions: Int,
        val deletions: Int,
        val insertions: Int,
        val referenceWordCount: Int,
        val hypothesisWordCount: Int,
        /** Null when the reference contains no scored words. */
        val wordErrorRate: Double?,
    ) {
        val totalErrors: Int get() = substitutions + deletions + insertions
    }
}
