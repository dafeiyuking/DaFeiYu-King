package com.sukisu.ultra.term

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * 把 shell 打印的内容变成带样式的文本：脚本用转义码要求的颜色会显示成颜色，而不是 `[1;36m`；
 * “清屏”码会把内容清掉。
 *
 * 只支持命令行工具真正会用到的那部分终端协议：颜色、加粗、清屏和回车。转义序列里其他东西都丢掉，
 * 终端遇到不认识的码也是这么做的。
 */
object Ansi {

    private const val ESC = '\u001B'
    private const val BELL = '\u0007'

    /** 脚本写出来的文本，带上它的颜色。 */
    fun render(raw: String): AnnotatedString {
        val pieces = mutableListOf<Piece>()
        var color: Color? = null
        var bold = false
        var buffer = StringBuilder()

        fun flush() {
            if (buffer.isNotEmpty()) {
                pieces += Piece(buffer.toString(), color, bold)
                buffer = StringBuilder()
            }
        }

        var index = 0
        while (index < raw.length) {
            val c = raw[index]
            if (c == BELL) {
                index++
                continue
            }
            if (c == '\r') {
                // 单独的回车意思是“这一行重写”，进度条就是这么动的。
                val lineStart = raw.lastIndexOf('\n', index - 1) + 1
                val written = buffer.length - (index - lineStart).coerceAtLeast(0)
                if (written >= 0 && written <= buffer.length) {
                    buffer.setLength(written)
                }
                index++
                continue
            }
            if (c == ESC && index + 1 < raw.length && raw[index + 1] == '[') {
                var end = index + 2
                while (end < raw.length && raw[end] !in '@'..'~') {
                    end++
                }
                if (end >= raw.length) {
                    break
                }
                val final = raw[end]
                val params = raw.substring(index + 2, end)
                when (final) {
                    'm' -> {
                        flush()
                        val applied = applySgr(params, color, bold)
                        color = applied.first
                        bold = applied.second
                    }
                    'J' -> if (params == "2" || params == "3") {
                        flush()
                        pieces.clear()
                    }
                    'H', 'f' -> if (params.isEmpty() || params == "1;1") {
                        // 清屏之后回到光标原位：在打印式的日志里没有可移动的东西。
                        flush()
                    }
                    'K' -> if (params == "2") {
                        buffer = StringBuilder()
                    }
                    else -> Unit
                }
                index = end + 1
                continue
            }
            buffer.append(c)
            index++
        }
        flush()

        val effective = if (pieces.isEmpty()) listOf(Piece(" ", null, false)) else pieces
        return buildAnnotatedString {
            effective.forEach { piece ->
                val style = SpanStyle(
                    color = piece.color ?: DEFAULT_TEXT,
                    fontWeight = if (piece.bold) FontWeight.Bold else FontWeight.Normal,
                )
                withStyle(style) { append(piece.text) }
            }
        }
    }

    /** 处理一个 `...m` 码：颜色、加粗和复位。返回它留下的颜色和字重。 */
    private fun applySgr(params: String, color: Color?, bold: Boolean): Pair<Color?, Boolean> {
        var newColor = color
        var newBold = bold
        val codes = params.split(';').filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
        if (codes.isEmpty()) {
            return null to false
        }
        for (code in codes) {
            when {
                code == 0 -> {
                    newColor = null
                    newBold = false
                }
                code == 1 -> newBold = true
                code == 22 -> newBold = false
                code == 39 -> newColor = null
                code in 30..37 -> newColor = STANDARD[code - 30]
                code in 90..97 -> newColor = BRIGHT[code - 90]
            }
        }
        return newColor to newBold
    }

    private data class Piece(val text: String, val color: Color?, val bold: Boolean)

    /** `30`..`37` 要的八种颜色，调成在深色控制台上看得清的版本。 */
    private val STANDARD = listOf(
        Color(0xFF8B93A7), // 黑：灰一点才看得见
        Color(0xFFF2777A), // 红
        Color(0xFF99CC99), // 绿
        Color(0xFFFFCC66), // 黄
        Color(0xFF6699CC), // 蓝
        Color(0xFFCC99CC), // 洋红
        Color(0xFF66CCCC), // 青
        Color(0xFFD3D6DE), // 白
    )

    /** 对应的亮色版，`90`..`97`。 */
    private val BRIGHT = listOf(
        Color(0xFFAAB2C6),
        Color(0xFFFF8F8F),
        Color(0xFFB5E8B5),
        Color(0xFFFFE08A),
        Color(0xFF87B8F0),
        Color(0xFFE3B2E3),
        Color(0xFF8FE3E3),
        Color(0xFFFFFFFF),
    )

    /** 没有颜色码的文字用什么颜色画。 */
    private val DEFAULT_TEXT = Color(0xFFC8F7E4)
}
