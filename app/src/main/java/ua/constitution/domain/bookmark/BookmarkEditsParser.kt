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
                val key = keys.next()
                val paragraphIndex = key.toIntOrNull() ?: continue
                val arr = paragraphEdits.optJSONArray(key) ?: continue
                val ranges = mutableListOf<StyledRange>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val start = obj.getInt("start")
                    val end = obj.getInt("end")
                    val colorHex = obj.getString("colorHex")
                    val highlight = obj.getBoolean("highlight")
                    val underscore = obj.getBoolean("underscore")
                    val highlightColorHex = obj.optString("highlightColorHex", colorHex)
                    val underscoreColorHex = obj.optString("underscoreColorHex", colorHex)
                    ranges.add(
                        StyledRange(
                            start = start,
                            end = end,
                            colorHex = colorHex,
                            highlight = highlight,
                            underscore = underscore,
                            highlightColorHex = highlightColorHex,
                            underscoreColorHex = underscoreColorHex
                        )
                    )
                }
                result[paragraphIndex] = ranges
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
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
