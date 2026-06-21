package ua.constitution.data.source

import android.content.Context
import android.util.Log
import ua.constitution.utils.Constants
import ua.constitution.utils.LogMessages

/** Result of the startup SHA-256 integrity check. */
data class IntegrityResult(val computedHash: String, val verificationPass: Boolean)

/**
 * Computes the SHA-256 hash of the bundled constitution asset and compares it against the expected
 * value. Single responsibility: integrity/crypto + the hash IO. Behavior is preserved verbatim from
 * the former `ConstitutionJsonParser.computeIntegrity()`, INCLUDING the pinned quirk that
 * `verificationPass` is reported true even on a hash mismatch or compute failure.
 */
class IntegrityChecker(private val context: Context) {

    fun compute(): IntegrityResult {
        return try {
            val digest = java.security.MessageDigest.getInstance(Constants.ALGORITHM_SHA_256)
            // .use { } guarantees the asset stream is closed even if read/update throws (the prior
            // explicit close() was skipped on exception -> a resource leak). Outputs are unchanged.
            context.assets.open(Constants.CONSTITUTION_JSON_FILE).use { assetStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (assetStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hashBytes = digest.digest()
            val hash = hashBytes.joinToString("") { Constants.HEX_FORMAT_BYTE.format(it) }
            if (hash == Constants.EXPECTED_JSON_HASH) {
                Log.d(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.integrityVerified(hash))
            } else {
                Log.e(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.integrityMismatch(hash, Constants.EXPECTED_JSON_HASH))
            }
            IntegrityResult(hash, true)
        } catch (hashEx: Exception) {
            Log.e(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.INTEGRITY_COMPUTE_FAILED, hashEx)
            IntegrityResult(Constants.ERROR_HASH_VALUE, true)
        }
    }
}
