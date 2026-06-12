package com.example.utils

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

    // Highlighting color defaults and palettes
    const val COLOR_DEFAULT_MARKER = "#FFF59D"
    const val COLOR_DEFAULT_UNDERLINE = "#F57F17"

    val MARKER_COLORS = listOf(
        "#FFF59D", // Soft Yellow
        "#FFE0B2", // Soft Orange
        "#C8E6C9", // Soft Green
        "#B2DFDB", // Soft Teal
        "#BBDEFB", // Soft Blue
        "#E1BEE7", // Soft Purple
        "#FFCDD2"  // Soft Red
    )

    val UNDERLINE_COLORS = listOf(
        "#F57F17", // Deep Gold
        "#E65100", // Deep Orange
        "#1B5E20", // Deep Green
        "#004D40", // Deep Teal
        "#0D47A1", // Deep Blue
        "#4A148C", // Deep Purple
        "#B71C1C"  // Deep Red
    )

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

    // Fallback chapter titles and info
    const val FALLBACK_PREAMBLE_TITLE = "Преамбула"
    const val FALLBACK_PREAMBLE_INFO = "Вступна частина Конституції України, що визначає історичні, правові підвалини та засади суверенітету нашої держави."
    const val FALLBACK_CHAPTER_1_TITLE = "Загальні засади"
    const val FALLBACK_CHAPTER_2_TITLE = "Права, свободи та обов'язки людини і громадянина"
    const val FALLBACK_CHAPTER_3_TITLE = "Вибори. Референдум"
    const val FALLBACK_CHAPTER_4_TITLE = "Верховна Рада України"
    const val FALLBACK_CHAPTER_5_TITLE = "Президент України"
    const val FALLBACK_CHAPTER_6_TITLE = "Кабінет Міністрів України. Інші органи виконавчої влади"
    const val FALLBACK_CHAPTER_8_TITLE = "Правосуддя"
    const val FALLBACK_CHAPTER_9_TITLE = "Територіальний устрій України"
    const val FALLBACK_CHAPTER_10_TITLE = "Автономна Республіка Крим"
    const val FALLBACK_CHAPTER_11_TITLE = "Місцеве самоврядування"
    const val FALLBACK_CHAPTER_12_TITLE = "Конституційний Суд України"
    const val FALLBACK_CHAPTER_13_TITLE = "Внесення змін до Конституції України"
    const val FALLBACK_CHAPTER_14_TITLE = "Прикінцеві положення"
    const val FALLBACK_CHAPTER_15_TITLE = "Перехідні положення"

    // Reference & source URLs
    const val DEFAULT_RADA_URL = "https://zakon.rada.gov.ua/laws/show/254%D0%BA/96-%D0%B2%D1%80"
    const val PREAMBLE_SOURCE_URL = "https://zakon.rada.gov.ua/laws/show/254%D0%BA/96-%D0%B2%D1%80#n4164"

    // Link checking and normalization constants
    const val LINK_PUNKT_FULL = "пункт"
    const val LINK_P_DOT = "п."
    const val LINK_P_SPACE_START = "п "
    const val LINK_P_SPACE_MID = " п "
}
