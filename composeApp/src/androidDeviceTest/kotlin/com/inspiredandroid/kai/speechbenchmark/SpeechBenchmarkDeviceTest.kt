package com.inspiredandroid.kai.speechbenchmark

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
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
}
