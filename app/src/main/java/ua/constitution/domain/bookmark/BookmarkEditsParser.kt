package ua.constitution.domain.bookmark

import ua.constitution.domain.text.StyledRange

/** (De)serializes per-paragraph highlight/underline edits to/from JSON. Extracted from MainActivity. */

object BookmarkEditsParser {
    fun parse(jsonStr: String?): Map<Int, List<StyledRange>> {
        val result = mutableMapOf<Int, List<StyledRange>>()
        if (jsonStr.isNullOrBlank()) return result
        try {
            val root = org.json.JSONObject(jsonStr)
            val paragraphEdits = root.optJSONObject("paragraphEdits") ?: return result
            val keys = paragraphEdits.keys()
            while (keys.hasNext()) {
                putParagraphEdits(result, paragraphEdits, keys.next())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /** Parses one key's ranges into [result], skipping non-numeric keys / non-array values (the
     *  former inline `?: continue` guards). A malformed range still propagates to [parse]'s catch. */
    private fun putParagraphEdits(
        result: MutableMap<Int, List<StyledRange>>,
        paragraphEdits: org.json.JSONObject,
        key: String
    ) {
        val paragraphIndex = key.toIntOrNull() ?: return
        val arr = paragraphEdits.optJSONArray(key) ?: return
        result[paragraphIndex] = parseRanges(arr)
    }

    /** Parses one paragraph's array of styled ranges. A range missing a required field throws,
     *  which (per [parse]'s catch) discards the whole parse — the pinned characterization quirk. */
    private fun parseRanges(arr: org.json.JSONArray): List<StyledRange> {
        val ranges = mutableListOf<StyledRange>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val colorHex = obj.getString("colorHex")
            ranges.add(
                StyledRange(
                    start = obj.getInt("start"),
                    end = obj.getInt("end"),
                    colorHex = colorHex,
                    highlight = obj.getBoolean("highlight"),
                    underscore = obj.getBoolean("underscore"),
                    highlightColorHex = obj.optString("highlightColorHex", colorHex),
                    underscoreColorHex = obj.optString("underscoreColorHex", colorHex)
                )
            )
        }
        return ranges
    }

    fun toJson(edits: Map<Int, List<StyledRange>>): String {
        try {
            val root = org.json.JSONObject()
            val paragraphEdits = org.json.JSONObject()
            for ((paragraphIndex, ranges) in edits) {
                if (ranges.isEmpty()) continue
                val arr = org.json.JSONArray()
                for (range in ranges) {
                    val obj = org.json.JSONObject()
                    obj.put("start", range.start)
                    obj.put("end", range.end)
                    obj.put("colorHex", range.colorHex)
                    obj.put("highlight", range.highlight)
                    obj.put("underscore", range.underscore)
                    obj.put("highlightColorHex", range.highlightColorHex)
                    obj.put("underscoreColorHex", range.underscoreColorHex)
                    arr.put(obj)
                }
                paragraphEdits.put(paragraphIndex.toString(), arr)
            }
            root.put("paragraphEdits", paragraphEdits)
            return root.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}
