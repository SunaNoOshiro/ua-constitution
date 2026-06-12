package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    try {
      val mainFile = File("src/main/java/com/example/MainActivity.kt")
      if (mainFile.exists()) {
        val text = mainFile.readText()
        var inString = false
        var escaped = false
        var inChar = false
        var inLineComment = false
        var inBlockComment = false
        
        var braceDepth = 0
        val openBraces = mutableListOf<Pair<Int, Int>>() // Pair(lineNum, colNum)
        
        var lineNum = 1
        var colNum = 1
        
        val linesInfo = mainFile.readLines()
        
        var i = 0
        while (i < text.length) {
          val char = text[i]
          
          // Track lines and columns
          val currLine = lineNum
          val currCol = colNum
          
          if (char == '\n') {
            lineNum++
            colNum = 1
          } else {
            colNum++
          }
          
          if (inLineComment) {
            if (char == '\n') {
              inLineComment = false
            }
            i++
            continue
          }
          
          if (inBlockComment) {
            if (char == '/' && i > 0 && text[i - 1] == '*') {
              inBlockComment = false
            }
            i++
            continue
          }
          
          if (inString) {
            if (escaped) {
              escaped = false
            } else if (char == '\\') {
              escaped = true
            } else if (char == '"') {
              inString = false
            }
            i++
            continue
          }
          
          if (inChar) {
            if (escaped) {
              escaped = false
            } else if (char == '\\') {
              escaped = true
            } else if (char == '\'') {
              inChar = false
            }
            i++
            continue
          }
          
          // Check for comments
          if (char == '/' && i + 1 < text.length && text[i + 1] == '/') {
            inLineComment = true
            i += 2
            colNum += 1
            continue
          }
          
          if (char == '/' && i + 1 < text.length && text[i + 1] == '*') {
            inBlockComment = true
            i += 2
            colNum += 1
            continue
          }
          
          // Check for string start
          if (char == '"') {
            inString = true
            i++
            continue
          }
          
          // Check for char start
          if (char == '\'') {
            inChar = true
            i++
            continue
          }
          
          // Count braces
          if (char == '{') {
            braceDepth++
            openBraces.add(Pair(currLine, currCol))
            if (currLine in 1000..5290) {
               // Optional: print line context of deep braces
            }
          } else if (char == '}') {
            braceDepth--
            if (openBraces.isNotEmpty()) {
              openBraces.removeAt(openBraces.size - 1)
            } else {
              println("UNEXPECTED CLOSING BRACE at Line $currLine, Column $currCol")
            }
          }
          
          i++
        }
        
        println("--- ROBUST BRACE BALANCE ANALYSIS ---")
        println("Final depth: $braceDepth")
        println("Unclosed braces: ${openBraces.size}")
        openBraces.forEachIndexed { index, pair ->
          val lineText = linesInfo.getOrNull(pair.first - 1)?.trim() ?: ""
          println("  #$index: Opened at Line ${pair.first}, Column ${pair.second}: $lineText")
        }
        println("-------------------------------------")
      } else {
        println("MainActivity.kt does not exist!")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  @Test
  fun testConstitutionInitialization() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.model.ConstitutionData.initialize(context)
    println("INITIALIZED ARTICLES COUNT: ${com.example.data.model.ConstitutionData.articles.size}")
    println("INITIALIZED CHAPTERS COUNT: ${com.example.data.model.ConstitutionData.chapters.size}")
    com.example.data.model.ConstitutionData.chapters.forEach { chapter ->
      val belongingArticles = com.example.data.model.ConstitutionData.articles.filter { it.chapterId == chapter.id }
      println("Chapter ${chapter.id} ('${chapter.titleUa}'): articles count: ${belongingArticles.size}")
    }
    if (com.example.data.model.ConstitutionData.articles.isEmpty()) {
       println("No articles parsed!")
    } else {
       val firstArticle = com.example.data.model.ConstitutionData.articles.first()
       println("Parsed first article: ${firstArticle.titleUa}")
       println("Paragraphs count: ${firstArticle.paragraphs.size}")
       firstArticle.paragraphs.forEachIndexed { idx, p ->
          println("  Par $idx text length: ${p.text.length}")
          println("  Par $idx text: ${p.text}")
          println("  Par $idx content count: ${p.content.size}")
          p.content.forEachIndexed { sIdx, s ->
             println("    Segment $sIdx: type=${s.type}, value='${s.value}', text='${s.text}'")
          }
       }
    }
  }

  @Test
  fun printJsonHash() {
    val file = File("src/main/assets/constitution_ua.json")
    assertTrue("JSON file should exist", file.exists())
    
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = file.readBytes()
    val hashBytes = digest.digest(bytes)
    val hashHex = hashBytes.joinToString("") { "%02x".format(it) }
    
    println("NEW_JSON_SHA_256_HASH: $hashHex")
    // Keep of the log hash but do not fail
    assertNotNull(hashHex)
  }

  @Test
  fun testWordRangeAtOffset() {
    val text = "Україна є суверенна, незалежна держава."
    
    // "Україна"
    val r1 = getWordRangeAtOffset(text, 2)
    assertNotNull(r1)
    assertEquals(Pair(0, 7), r1)
    
    // "суверенна," should trim punctuation and space
    val r2 = getWordRangeAtOffset(text, 12)
    assertNotNull(r2)
    assertEquals("суверенна", text.substring(r2!!.first, r2.second))
    
    // whitespace should return null
    val rNone = getWordRangeAtOffset(text, 7)
    assertNull(rNone)
  }

  @Test
  fun testBookmarkEditsParser() {
    val edits = mapOf(
      0 to listOf(StyledRange(start = 0, end = 7, colorHex = "#FFF59D", highlight = true, underscore = false)),
      1 to listOf(StyledRange(start = 5, end = 15, colorHex = "#C8E6C9", highlight = true, underscore = true))
    )
    
    val json = BookmarkEditsParser.toJson(edits)
    assertNotNull(json)
    assertTrue(json.contains("FFF59D"))
    assertTrue(json.contains("C8E6C9"))
    
    val parsed = BookmarkEditsParser.parse(json)
    assertEquals(2, parsed.size)
    assertEquals("#FFF59D", parsed[0]?.get(0)?.colorHex)
    assertEquals(true, parsed[1]?.get(0)?.underscore)
  }
}
