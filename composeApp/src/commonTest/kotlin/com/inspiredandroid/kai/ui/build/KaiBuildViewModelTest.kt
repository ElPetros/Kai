package com.inspiredandroid.kai.ui.build

import app.cash.turbine.test
import com.inspiredandroid.kai.FileBrowserSource
import com.inspiredandroid.kai.KaiBuildController
import com.inspiredandroid.kai.NoOpFileBrowserSource
import com.inspiredandroid.kai.build.BuildEnvironmentState
import com.inspiredandroid.kai.build.KaiBuildState
import com.inspiredandroid.kai.build.BuildTerminalSession
import com.inspiredandroid.kai.testutil.FakeDataRepository
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class KaiBuildViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDataRepository
    private lateinit var fakeController: FakeKaiBuildController

    private class FakeKaiBuildController : KaiBuildController {
        override val state = MutableStateFlow(
            KaiBuildState(
                environment = BuildEnvironmentState.Ready,
                installedAgents = persistentSetOf("claude-code", "grok"),
            ),
        )
        override val files: FileBrowserSource = NoOpFileBrowserSource

        /** Agent ids passed to [startSession], oldest first. */
        val startedWith = mutableListOf<String?>()

        /** Project names passed to [deleteProject], oldest first. */
        val deleted = mutableListOf<String>()

        /** Old-to-new pairs passed to [renameProject], oldest first. */
        val renamed = mutableListOf<Pair<String, String>>()
        val written = mutableListOf<String>()

        override fun install(agentIds: Set<String>) {}
        override fun cancel() {}
        override fun uninstall() {}
        override fun refresh() {}
        override fun createProject(name: String): String? = name
        override fun deleteProject(name: String) {
            deleted += name
        }

        override fun renameProject(name: String, newName: String): String {
            renamed += name to newName
            return newName
        }

        override fun startSession(project: String, agentId: String?) {
            startedWith += agentId
        }

        override fun selectSession(id: String) { state.value = state.value.copy(activeSessionId = id) }
        override fun closeSession(id: String) {
            val remaining = state.value.sessions.filterNot { it.id == id }.toImmutableList()
            state.value = state.value.copy(sessions = remaining, activeSessionId = remaining.firstOrNull()?.id)
        }
        override fun resumeProject(project: String): Boolean = false
        override fun leaveProject(project: String) {}
        override fun writeToTerminal(text: String) { written += text }
        override fun resizeTerminal(columns: Int, rows: Int) {}
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDataRepository()
        fakeController = FakeKaiBuildController()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setLaunchAgent persists the choice`() = runTest {
        val viewModel = KaiBuildViewModel(fakeController, fakeRepository)

        viewModel.uiState.test {
            assertNull(awaitItem().launchAgentId)

            viewModel.setLaunchAgent("grok")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals("grok", awaitItem().launchAgentId)
            assertEquals("grok", fakeRepository.getKaiBuildLaunchAgent())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `launch agent is restored on the next app start`() = runTest {
        fakeRepository.storedKaiBuildLaunchAgent = "claude-code"

        val viewModel = KaiBuildViewModel(fakeController, fakeRepository)

        viewModel.uiState.test {
            assertEquals("claude-code", awaitItem().launchAgentId)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.openProject("demo")
        assertEquals(listOf<String?>("claude-code"), fakeController.startedWith)
    }

    @Test
    fun `picking the shell again clears the stored agent`() = runTest {
        fakeRepository.storedKaiBuildLaunchAgent = "grok"

        val viewModel = KaiBuildViewModel(fakeController, fakeRepository)
        viewModel.setLaunchAgent(null)

        assertNull(fakeRepository.getKaiBuildLaunchAgent())
        viewModel.openProject("demo")
        assertEquals(listOf<String?>(null), fakeController.startedWith)
    }

    @Test
    fun `deleting and renaming reach the environment`() = runTest {
        val viewModel = KaiBuildViewModel(fakeController, fakeRepository)

        viewModel.deleteProject("old-demo")
        viewModel.renameProject("demo", "demo-2")

        assertEquals(listOf("old-demo"), fakeController.deleted)
        assertEquals(listOf("demo" to "demo-2"), fakeController.renamed)
    }

    @Test
    fun `terminal drafts remain independent when sessions switch`() = runTest {
        val first = BuildTerminalSession(id = "terminal-1", project = "demo", agentId = null, number = 1)
        val second = BuildTerminalSession(id = "terminal-2", project = "demo", agentId = null, number = 2)
        fakeController.state.value = fakeController.state.value.copy(sessions = persistentListOf(first, second), activeSessionId = first.id)
        val viewModel = KaiBuildViewModel(fakeController, fakeRepository)
        viewModel.openProject("demo")
        viewModel.setTerminalDraft(first.id, "first command")
        viewModel.selectSession(second.id)
        viewModel.setTerminalDraft(second.id, "second command")
        assertEquals("first command", viewModel.terminalDrafts.value[first.id])
        assertEquals("second command", viewModel.terminalDrafts.value[second.id])
    }

    @Test
    fun `dictation is appended only to its originating session draft`() = runTest {
        val first = BuildTerminalSession(id = "terminal-1", project = "demo", agentId = null, number = 1)
        val second = BuildTerminalSession(id = "terminal-2", project = "demo", agentId = null, number = 2)
        fakeController.state.value = fakeController.state.value.copy(sessions = persistentListOf(first, second), activeSessionId = second.id)
        val viewModel = KaiBuildViewModel(fakeController, fakeRepository)
        viewModel.openProject("demo")
        viewModel.setTerminalDraft(first.id, "please")
        viewModel.selectSession(second.id)
        assertEquals(true, viewModel.insertTerminalTranscript(first.id, "review this"))
        assertEquals("please review this", viewModel.terminalDrafts.value[first.id])
        assertNull(viewModel.terminalDrafts.value[second.id])
    }

    @Test
    fun `late dictation is rejected after its terminal session closes`() = runTest {
        val first = BuildTerminalSession(id = "terminal-1", project = "demo", agentId = null, number = 1)
        fakeController.state.value = fakeController.state.value.copy(sessions = persistentListOf(first), activeSessionId = first.id)
        val viewModel = KaiBuildViewModel(fakeController, fakeRepository)
        viewModel.openProject("demo")
        viewModel.closeSession(first.id)
        assertEquals(false, viewModel.insertTerminalTranscript(first.id, "must not appear"))
        assertNull(viewModel.terminalDrafts.value[first.id])
    }

    @Test
    fun `terminal line submission requires the originating session to remain active`() = runTest {
        val first = BuildTerminalSession(id = "terminal-1", project = "demo", agentId = null, number = 1)
        val second = BuildTerminalSession(id = "terminal-2", project = "demo", agentId = null, number = 2)
        fakeController.state.value = fakeController.state.value.copy(sessions = persistentListOf(first, second), activeSessionId = second.id)
        val viewModel = KaiBuildViewModel(fakeController, fakeRepository)
        viewModel.openProject("demo")
        viewModel.setTerminalDraft(first.id, "echo first")
        viewModel.submitLine(first.id, "echo first\r")
        assertEquals(emptyList(), fakeController.written)
        assertEquals("echo first", viewModel.terminalDrafts.value[first.id])
        viewModel.selectSession(first.id)
        viewModel.submitLine(first.id, "echo first\r")
        assertEquals(listOf("echo first\r"), fakeController.written)
        assertNull(viewModel.terminalDrafts.value[first.id])
    }

    @Test
    fun `a remembered agent that is no longer installed falls back to a shell`() = runTest {
        fakeRepository.storedKaiBuildLaunchAgent = "opencode"

        val viewModel = KaiBuildViewModel(fakeController, fakeRepository)

        viewModel.uiState.test {
            assertNull(awaitItem().launchAgentId)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.openProject("demo")
        assertEquals(listOf<String?>(null), fakeController.startedWith)
    }
}
