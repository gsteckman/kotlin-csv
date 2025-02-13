package com.jsoizo.kotlincsv.client

import com.jsoizo.kotlincsv.util.Const
import kotlinx.io.EOFException
import kotlinx.io.Source
import kotlinx.io.readCodePointValue

/**
 * reader from a [Source] which can read line with line terminator.
 */
internal class SourceLineReader(private val br: Source) {
    companion object {
        private const val BOM = Const.BOM
    }

    private fun StringBuilder.isEmptyLine(): Boolean =
        this.isEmpty() || this.length == 1 && this[0] == BOM

    fun readLineWithTerminator(): String? {
        val sb = StringBuilder()
        do {
            val c = try{
                br.readCodePointValue()
            } catch(_: EOFException){
                if (sb.isEmptyLine()) {
                    return null
                } else {
                    break
                }
            }
            val ch = c.toChar()
            sb.append(ch)

            if (ch == '\n' || ch == '\u2028' || ch == '\u2029' || ch == '\u0085') {
                break
            }

            if (ch == '\r') {
                val s2 = br.peek()

                val c2 = try {
                    s2.readCodePointValue()
                } catch(_: EOFException){
                    break
                }

                if (c2.toChar() == '\n') {
                    sb.append('\n')
                    br.readCodePointValue()
                }
                break
            }
        } while (true)
        return sb.toString()
    }
}
