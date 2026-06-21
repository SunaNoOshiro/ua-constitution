package ua.constitution.domain.text

import ua.constitution.data.model.ContentSegment
import ua.constitution.data.model.Paragraph
import ua.constitution.utils.Constants

/**
 * Pure coordinate math mapping per-paragraph [StyledRange]s to/from a single "combined" text
 * (the article's paragraphs joined by a "\n\n" separator), plus flattening paragraphs into one
 * segment list.
 *
 * Extracted verbatim from ArticleCard so the off-by-one-sensitive arithmetic — the +2 "\n\n"
 * joiner and the half-open [start, end) intersection — is unit-testable without Compose.
 */
object ParagraphRangeMapping {

    /** Flattens all paragraphs into one segment list, inserting a "\n\n" text segment between
     *  paragraphs (but not after the last). */
    fun flattenToSegments(paragraphs: List<Paragraph>): List<ContentSegment> {
        val result = mutableListOf<ContentSegment>()
        paragraphs.forEachIndexed { index, paragraph ->
            result.addAll(paragraph.content)
            if (index < paragraphs.lastIndex) {
                result.add(ContentSegment(type = Constants.TYPE_TEXT, value = "\n\n"))
            }
        }
        return result
    }

    /** Start offset of each paragraph within the combined text. Every paragraph advances the
     *  cursor by its text length plus 2 for the "\n\n" joiner (applied uniformly). */
    fun paragraphOffsets(paragraphs: List<Paragraph>): IntArray {
        val offsets = IntArray(paragraphs.size)
        var currentOffset = 0
        paragraphs.forEachIndexed { index, paragraph ->
            offsets[index] = currentOffset
            currentOffset += paragraph.text.length + 2 // 2 for "\n\n"
        }
        return offsets
    }

    /** Shifts per-paragraph ranges into combined-text coordinates by each paragraph's offset. */
    fun toCombined(perParagraph: Map<Int, List<StyledRange>>, offsets: IntArray): List<StyledRange> {
        val result = mutableListOf<StyledRange>()
        perParagraph.forEach { (pIdx, ranges) ->
            val offset = offsets.getOrNull(pIdx) ?: 0
            ranges.forEach { range ->
                result.add(range.copy(start = range.start + offset, end = range.end + offset))
            }
        }
        return result
    }

    /** Splits combined-text ranges back into per-paragraph coordinates, clipping each to its
     *  paragraph's half-open [offset, offset + length) span and dropping paragraphs that end up
     *  with no range (e.g. a range landing entirely in a "\n\n" gap). */
    fun toPerParagraph(
        combinedRanges: List<StyledRange>,
        paragraphs: List<Paragraph>,
        offsets: IntArray
    ): Map<Int, List<StyledRange>> {
        val newMap = mutableMapOf<Int, List<StyledRange>>()
        paragraphs.forEachIndexed { pIdx, paragraph ->
            val offset = offsets[pIdx]
            val pLen = paragraph.text.length
            val pStartInCombined = offset
            val pEndInCombined = offset + pLen

            val pRanges = mutableListOf<StyledRange>()
            combinedRanges.forEach { range ->
                val intersectStart = maxOf(range.start, pStartInCombined)
                val intersectEnd = minOf(range.end, pEndInCombined)
                if (intersectStart < intersectEnd) {
                    pRanges.add(
                        range.copy(start = intersectStart - offset, end = intersectEnd - offset)
                    )
                }
            }
            if (pRanges.isNotEmpty()) {
                newMap[pIdx] = pRanges
            }
        }
        return newMap
    }
}
