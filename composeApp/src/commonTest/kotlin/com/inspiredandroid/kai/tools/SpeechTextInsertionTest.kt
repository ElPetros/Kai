package com.inspiredandroid.kai.tools

import kotlin.test.Test
import kotlin.test.assertEquals

class SpeechTextInsertionTest {
    @Test
    fun appendsToAnEmptyField() {
        assertEquals("Hello Kai", appendSpeechTranscript("", " Hello Kai "))
    }

    @Test
    fun appendsWithOneSeparatorWhenNeeded() {
        assertEquals("Existing text spoken words", appendSpeechTranscript("Existing text", "spoken words"))
    }

    @Test
    fun preservesExistingTrailingWhitespace() {
        assertEquals("Existing text\nspoken words", appendSpeechTranscript("Existing text\n", "spoken words"))
    }

    @Test
    fun ignoresBlankRecognition() {
        assertEquals("Keep this", appendSpeechTranscript("Keep this", "   "))
    }
}
