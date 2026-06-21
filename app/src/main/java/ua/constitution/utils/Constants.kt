package ua.constitution.utils

object Constants {
    // Database configuration
    const val DATABASE_NAME = "constitution_database"
    const val TABLE_BOOKMARKS = "bookmarks"

    // Asset and Resource names
    const val CONSTITUTION_JSON_FILE = "constitution_ua.json"
    const val CONSTITUTION_RAW_RESOURCE_NAME = "constitution_ua"
    const val RAW_DEF_TYPE = "raw"
    
    // Cryptographic checksum
    const val EXPECTED_JSON_HASH = "65bbb2e3ef7e8bfbf2c7f908d46e229d4ee53434174020bb743a68e6810e112b"
    const val ERROR_HASH_VALUE = "error_calculating"

    // Content types
    const val TYPE_TEXT = "text"
    const val TYPE_LINK = "link"
    const val ANNOTATION_TAG_URL = "URL"

    // Text splitting regexes
    const val SPLIT_TITLE_REGEX_PATTERN = """^(Стаття|Пункт)\s+(\d+(?:[\.\-]\d+)?)\.?\s*(.*)$"""

    // Highlight edit tools
    const val TOOL_NONE = "NONE"
    const val TOOL_MARKER = "MARKER"
    const val TOOL_UNDERLINE = "UNDERLINE"
    const val TOOL_ERASER = "ERASER"

    // Highlighting color defaults and palettes live in ui/theme/HighlightPalette.kt (all color
    // tokens belong in the theme package, alongside the Compose brand palette in Color.kt).

    // JSON parsing keys and system keys
    const val KEY_PREAMBLE = "preamble"
    const val KEY_CHAPTERS = "chapters"
    const val KEY_ARTICLES = "articles"
    const val KEY_PARAGRAPHS = "paragraphs"
    const val KEY_CONTENT = "content"
    const val KEY_NOTES = "notes"
    const val KEY_TYPE = "type"
    const val KEY_VALUE = "value"
    const val KEY_TEXT = "text"
    const val KEY_URL = "url"
    const val KEY_TITLE_UA = "titleUa"
    const val KEY_INFO = "info"
    const val KEY_EXCLUDED = "excluded"
    const val KEY_SOURCE_URL = "sourceUrl"
    const val KEY_EXCLUDED_NOTE = "excludedNote"
    const val KEY_CHAPTER_ID = "chapterId"
    const val KEY_ID = "id"

    // Cryptographic and formatting configurations
    const val ALGORITHM_SHA_256 = "SHA-256"
    const val HEX_FORMAT_BYTE = "%02x"

    // Reference & source URLs
    const val DEFAULT_RADA_URL = "https://zakon.rada.gov.ua/laws/show/254%D0%BA/96-%D0%B2%D1%80"
    const val PREAMBLE_SOURCE_URL = "https://zakon.rada.gov.ua/laws/show/254%D0%BA/96-%D0%B2%D1%80#n4164"
}
