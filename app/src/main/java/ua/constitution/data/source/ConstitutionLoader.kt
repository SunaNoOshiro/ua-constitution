package ua.constitution.data.source

import android.content.Context
import android.util.Log
import ua.constitution.data.model.Chapter
import ua.constitution.data.model.ConstitutionContent
import ua.constitution.utils.LogMessages

/**
 * Loads, parses and integrity-checks the bundled constitution into an immutable [ConstitutionContent].
 *
 * This replaces the former `ConstitutionData.initialize(...)`: the composition root calls [load]
 * once and injects the result, instead of mutating a global singleton. Behavior is preserved
 * verbatim, including the quirk that the integrity hash/verification flag are kept even if the
 * subsequent parse fails (and that verification "passes" on a hash mismatch — that lives in
 * [ConstitutionJsonParser.computeIntegrity]).
 */
object ConstitutionLoader {

    fun load(context: Context): ConstitutionContent {
        val defaults = defaultChapters(context)
        val parser = ConstitutionJsonParser(context)
        var computedHash = ""
        var integrityVerificationPass = false
        return try {
            val integrity = parser.computeIntegrity()
            computedHash = integrity.computedHash
            integrityVerificationPass = integrity.verificationPass

            val parsed = parser.parse(parser.loadJsonString())
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
