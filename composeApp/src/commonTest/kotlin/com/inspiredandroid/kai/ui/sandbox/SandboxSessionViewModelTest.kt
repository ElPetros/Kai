package com.inspiredandroid.kai.ui.sandbox

import com.inspiredandroid.kai.SandboxSessions
import com.inspiredandroid.kai.testutil.FakeDataRepository
import com.inspiredandroid.kai.testutil.FakeSandboxController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SandboxSessionViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun dictationUpdatesTheOriginatingDraftAfterAnotherSessionIsSelected() = runTest {
        val repository = FakeDataRepository().apply {
            currentConversationId.value = "chat-one"
        }
        val viewModel = SandboxSessionViewModel(FakeSandboxController(), repository)
        advanceUntilIdle()

        viewModel.setInputText("chat draft")
        viewModel.selectSession(SandboxSessions.TERMINAL)
        viewModel.setInputText("scratch draft")

        assertTrue(
            viewModel.setInputTextForSession("chat-one", "chat draft with dictation"),
        )
        assertEquals("scratch draft", viewModel.inputText.value)
        assertEquals("chat draft with dictation", viewModel.inputTextForSession("chat-one"))

        viewModel.selectSession("chat-one")
        assertEquals("chat draft with dictation", viewModel.inputText.value)
    }

    @Test
    fun lateDictationIsRejectedAfterItsSessionIsNoLongerVisible() = runTest {
        val repository = FakeDataRepository().apply {
            currentConversationId.value = "chat-one"
        }
        val viewModel = SandboxSessionViewModel(FakeSandboxController(), repository)
        advanceUntilIdle()

        repository.currentConversationId.value = null
        advanceUntilIdle()

        assertFalse(viewModel.setInputTextForSession("chat-one", "late transcript"))
        assertNull(viewModel.inputTextForSession("chat-one"))
        assertEquals(SandboxSessions.TERMINAL, viewModel.selectedSessionId.value)
    }
}
