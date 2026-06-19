package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun findUnbalancedBraces() {
    val file = java.io.File("/app/src/main/java/com/example/ui/screens/AddServiceScreen.kt")
    val content = file.readText()
    val stack = java.util.Stack<Pair<Char, Int>>() // Char, LineNumber
    var lineNum = 1
    var insideString = false
    var insideChar = false
    var insideBlockComment = false
    var insideLineComment = false
    var i = 0
    while (i < content.length) {
        val c = content[i]
        if (insideString) {
            if (c == '"' && content[i-1] != '\\') insideString = false
        } else if (insideChar) {
            if (c == '\'' && content[i-1] != '\\') insideChar = false
        } else if (insideBlockComment) {
            if (c == '*' && i + 1 < content.length && content[i+1] == '/') {
                insideBlockComment = false
                i++
            }
        } else if (insideLineComment) {
            if (c == '\n') insideLineComment = false
        } else {
            if (c == '"') insideString = true
            else if (c == '\'') insideChar = true
            else if (c == '/' && i + 1 < content.length && content[i+1] == '*') {
                insideBlockComment = true
                i++
            }
            else if (c == '/' && i + 1 < content.length && content[i+1] == '/') {
                insideLineComment = true
                i++
            }
            else if (c == '{') {
                stack.push(Pair('{', lineNum))
            } else if (c == '}') {
                if (stack.isEmpty()) {
                    println("ERROR: Unmatched closing brace '}' at line $lineNum")
                } else {
                    stack.pop()
                }
            }
        }
        if (c == '\n') lineNum++
        i++
    }
    while (!stack.isEmpty()) {
        val open = stack.pop()
        println("ERROR: Unclosed opening brace '{' at line ${open.second}")
    }
    assertTrue(true)
  }
}
