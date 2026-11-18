package com.inspiredandroid.kai.speechbenchmark

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WordErrorRateTest {
    @Test
    fun exactMatchHasZeroErrors() {
        val result = WordErrorRate.score("Kai is listening", "Kai is listening")

        assertEquals(0, result.totalErrors)
        assertEquals(0.0, result.wordErrorRate)
    }

    @Test
    fun countsSubstitutionsDeletionsAndInsertions() {
        val result = WordErrorRate.score(
            reference = "the quick brown fox",
            hypothesis = "the slow fox jumps",
        )

        assertEquals(1, result.substitutions)
        assertEquals(1, result.deletions)
        assertEquals(1, result.insertions)
        assertEquals(3, result.totalErrors)
        assertEquals(0.75, result.wordErrorRate)
    }

    @Test
    fun normalizationIgnoresCaseAndPunctuationButKeepsWordContent() {
        val result = WordErrorRate.score(
            reference = "Hello, KAI! Don't stop.",
            hypothesis = "hello kai don't stop",
        )

        assertEquals(0, result.totalErrors)
        assertEquals("hello kai don't stop", WordErrorRate.normalize("Hello, KAI! Don’t stop."))
    }

    @Test
    fun emptyHypothesisCountsEveryReferenceWordAsDeletion() {
        val result = WordErrorRate.score("one two three", "")

        assertEquals(3, result.deletions)
        assertEquals(0, result.insertions)
        assertEquals(1.0, result.wordErrorRate)
    }

    @Test
    fun emptyReferenceHasUndefinedWerAndCountsInsertions() {
        val result = WordErrorRate.score("", "unexpected words")

        assertEquals(2, result.insertions)
        assertNull(result.wordErrorRate)
    }

    @Test
    fun emptyReferenceAndHypothesisHaveUndefinedWer() {
        val result = WordErrorRate.score("", "   ")

        assertEquals(0, result.totalErrors)
        assertNull(result.wordErrorRate)
    }

    @Test
    fun numbersAreNotSilentlyNormalized() {
        val result = WordErrorRate.score("twenty twenty six", "2026")

        assertEquals(3, result.totalErrors)
        assertEquals(1.0, result.wordErrorRate)
    }
}
