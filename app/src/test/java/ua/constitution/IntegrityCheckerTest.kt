package ua.constitution

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ua.constitution.data.source.IntegrityChecker

/**
 * Smoke-characterizes the extracted IntegrityChecker on the bundled asset. Needs Robolectric for
 * Context / assets / SHA-256. Split out of the former ConstitutionJsonParserTest (assertion
 * preserved verbatim, including the pinned quirk that verificationPass is true even on mismatch).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IntegrityCheckerTest {

    @Test
    fun `compute returns a hash and always reports pass`() {
        val result = IntegrityChecker(ApplicationProvider.getApplicationContext<Context>()).compute()
        assertTrue(result.verificationPass) // set true even on mismatch (preserved quirk)
        assertTrue(result.computedHash.isNotEmpty())
    }
}
