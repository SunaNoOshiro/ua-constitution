package ua.constitution.data.source

import android.content.Context
import android.util.Log
import ua.constitution.data.model.Chapter
import ua.constitution.data.model.ConstitutionContent
import ua.constitution.utils.Constants
import ua.constitution.utils.LogMessages

/**
 * Loads, parses and integrity-checks the bundled constitution into an immutable [ConstitutionContent].
 *
 * Orchestration only: it owns the resource/asset IO + fallback ([readJsonString]) and composes the
 * single-responsibility [IntegrityChecker] (SHA-256) and [ConstitutionJsonDeserializer] (JSON ->
 * domain). This replaces the former `ConstitutionData.initialize(...)`: the composition root calls
 * [load] once and injects the result, instead of mutating a global singleton. Behavior is preserved
 * verbatim, including the quirk that the integrity hash/verification flag are kept even if the
 * subsequent parse fails (and that verification "passes" on a hash mismatch — that lives in
 * [IntegrityChecker.compute]).
 */
object ConstitutionLoader {

    fun load(context: Context): ConstitutionContent {
        val defaults = defaultChapters(context)
        var computedHash = ""
        var integrityVerificationPass = false
        return try {
            val integrity = IntegrityChecker(context).compute()
            computedHash = integrity.computedHash
            integrityVerificationPass = integrity.verificationPass

            val parsed = ConstitutionJsonDeserializer(context).deserialize(readJsonString(context))
            Log.d(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.initSuccess(parsed.articles.size))
            ConstitutionContent(
                articles = parsed.articles,
                parsedChapters = parsed.chapters,
                defaultChapters = defaults,
                integrityVerificationPass = integrityVerificationPass,
                computedHash = computedHash,
                usedFallback = false,
                initializationError = "",
            )
        } catch (e: Exception) {
            Log.e(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.INIT_ERROR, e)
            ConstitutionContent(
                articles = emptyList(),
                parsedChapters = emptyList(),
                defaultChapters = defaults,
                integrityVerificationPass = integrityVerificationPass,
                computedHash = computedHash,
                usedFallback = true,
                initializationError = "${e.javaClass.simpleName}: ${e.message}\n${e.stackTraceToString()}",
            )
        }
    }

    /** Reads the constitution JSON, preferring the raw resource and falling back to the asset.
     *  Extracted verbatim from the former `ConstitutionJsonParser.loadJsonString()`. */
    private fun readJsonString(context: Context): String {
        var jsonString = ""
        val rawId = context.resources.getIdentifier(Constants.CONSTITUTION_RAW_RESOURCE_NAME, Constants.RAW_DEF_TYPE, context.packageName)
        if (rawId != 0) {
            try {
                jsonString = context.resources.openRawResource(rawId).bufferedReader().use { it.readText() }
                Log.d(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.LOADED_FROM_RAW)
            } catch (rawEx: Exception) {
                Log.e(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.loadFromRawFailed(rawEx.message), rawEx)
            }
        }
        if (jsonString.isEmpty()) {
            jsonString = context.assets.open(Constants.CONSTITUTION_JSON_FILE).bufferedReader().use { it.readText() }
            Log.d(LogMessages.TAG_CONSTITUTION_DATA, LogMessages.LOADED_FROM_ASSETS)
        }
        return jsonString
    }

    /** The localized default chapter list (preamble + 14 chapters; chapter 7 is intentionally
     *  skipped), used when the parsed JSON yields no chapters. */
    fun defaultChapters(context: Context): List<Chapter> = listOf(
        Chapter(0, context.getString(ua.constitution.R.string.preamble), context.getString(ua.constitution.R.string.preamble_info)),
        Chapter(1, context.getString(ua.constitution.R.string.chapter_1_title)),
        Chapter(2, context.getString(ua.constitution.R.string.chapter_2_title)),
        Chapter(3, context.getString(ua.constitution.R.string.chapter_3_title)),
        Chapter(4, context.getString(ua.constitution.R.string.chapter_4_title)),
        Chapter(5, context.getString(ua.constitution.R.string.chapter_5_title)),
        Chapter(6, context.getString(ua.constitution.R.string.chapter_6_title)),
        Chapter(8, context.getString(ua.constitution.R.string.chapter_8_title)),
        Chapter(9, context.getString(ua.constitution.R.string.chapter_9_title)),
        Chapter(10, context.getString(ua.constitution.R.string.chapter_10_title)),
        Chapter(11, context.getString(ua.constitution.R.string.chapter_11_title)),
        Chapter(12, context.getString(ua.constitution.R.string.chapter_12_title)),
        Chapter(13, context.getString(ua.constitution.R.string.chapter_13_title)),
        Chapter(14, context.getString(ua.constitution.R.string.chapter_14_title)),
        Chapter(15, context.getString(ua.constitution.R.string.chapter_15_title))
    )
}
