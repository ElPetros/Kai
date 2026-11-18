package com.inspiredandroid.kai.speechbenchmark

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke test proving Android-KMP instrumentation and app-private test storage are available. */
@RunWith(AndroidJUnit4::class)
class SpeechBenchmarkDeviceTest {
    @Test
    fun instrumentationRunsOnTheTargetDevice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext

        assertTrue("Expected an Android API level supported by Kai", Build.VERSION.SDK_INT >= 26)
        assertTrue("Instrumentation target package must be available", targetContext.packageName.isNotBlank())
        assertTrue("App-private test files directory must be available", targetContext.filesDir.isDirectory)
    }

    /**
     * Opt in explicitly with -e runSpeechBenchmark true. Ordinary instrumentation
     * runs stay as smoke tests and do not require a corpus or installed model.
     */
    @Test
    fun runsImportedLocalCorpusWhenExplicitlyRequested() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(
            "Pass -e runSpeechBenchmark true to execute the local corpus benchmark.",
            InstrumentationRegistry.getArguments().getString("runSpeechBenchmark") == "true",
        )

        val report = SpeechBenchmarkAndroidRunner.run(instrumentation.targetContext)
        assertTrue("Benchmark report must be written to app-private storage", report.isFile)
        assertTrue("Benchmark report must contain JSON", report.length() > 0L)
    }
}
