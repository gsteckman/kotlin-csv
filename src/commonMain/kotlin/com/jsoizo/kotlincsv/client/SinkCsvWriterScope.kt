package com.jsoizo.kotlincsv.client

import com.jsoizo.kotlincsv.dsl.CsvWriterScope
import com.jsoizo.kotlincsv.dsl.context.CsvWriterContext
import com.jsoizo.kotlincsv.dsl.context.WriteQuoteMode
import com.jsoizo.kotlincsv.util.Const
import kotlinx.io.Sink
import kotlinx.io.writeString

/**
 * An implementation of [CsvWriterScope] which writes to a kotlinx.io.Sink. This implementation is not
 * thread safe. If [SinkCsvWriterScope] needs to be accessed from multiple threads,
 * an additional synchronization is required.
 *
 * @author doyaaaaaken
 * @author gsteckman
 */
internal class SinkCsvWriterScope internal constructor(private val ctx: CsvWriterContext,
    private val writer: Sink) : CsvWriterScope {

    private val quoteNeededChars = setOf('\r',
        '\n',
        ctx.quote.char,
        ctx.delimiter)

    private var hasWroteInitialChar: Boolean = false

    /**
     * write one row
     */
    override fun writeRow(row: List<Any?>) {
        willWritePreTerminator()
        writeNext(row)
        willWriteEndTerminator()
        writer.flush()
    }

    /**
     * write one row
     */
    override fun writeRow(vararg entry: Any?) {
        writeRow(entry.toList())
    }

    /**
     * write rows
     */
    override fun writeRows(rows: List<List<Any?>>) {
        willWritePreTerminator()
        rows.forEachIndexed { index, list ->
            writeNext(list)
            if (index < rows.size - 1 && list.isNotEmpty()) {
                writeTerminator()
            }
        }
        willWriteEndTerminator()
        writer.flush()
    }

    /**
     * write rows from Sequence
     */
    override fun writeRows(rows: Sequence<List<Any?>>) {
        willWritePreTerminator()

        val itr = rows.iterator()
        while (itr.hasNext()) {
            val row = itr.next()
            writeNext(row)
            if (itr.hasNext() && row.isNotEmpty()) writeTerminator()
        }

        willWriteEndTerminator()
        writer.flush()
    }

    override fun flush() {
        writer.flush()
    }

    override fun close() {
        writer.close()
    }

    private fun writeNext(row: List<Any?>) {
        if (!hasWroteInitialChar && ctx.prependBOM) {
            writer.writeString(Const.BOM.toString())
        }

        val rowStr = row.joinToString(ctx.delimiter.toString()) { field ->
            if (field == null) {
                ctx.nullCode
            } else {
                attachQuote(field.toString())
            }
        }
        writer.writeString(rowStr)
        hasWroteInitialChar = true
    }

    /**
     * Will write terminator if writer has not wrote last line terminator on previous line.
     */
    private fun willWritePreTerminator() {
        if (!ctx.outputLastLineTerminator && hasWroteInitialChar) {
            writeTerminator()
        }
    }

    /**
     * write terminator for next line
     */
    private fun writeTerminator() {
        writer.writeString(ctx.lineTerminator)
    }

    private fun willWriteEndTerminator() {
        if (ctx.outputLastLineTerminator) {
            writeTerminator()
        }
    }

    private fun attachQuote(field: String): String {
        val shouldQuote = when (ctx.quote.mode) {
            WriteQuoteMode.ALL -> true
            WriteQuoteMode.CANONICAL -> field.any { ch -> quoteNeededChars.contains(ch) }
            WriteQuoteMode.NON_NUMERIC -> {
                var foundDot = false
                field.any { ch ->
                    if (ch == '.') {
                        if (foundDot) {
                            true
                        } else {
                            foundDot = true
                            false
                        }
                    } else {
                        ch < '0' || ch > '9'
                    }
                }
            }
        }

        return buildString {
            if (shouldQuote) append(ctx.quote.char)
            field.forEach { ch ->
                if (ch == ctx.quote.char) {
                    append(ctx.quote.char)
                }
                append(ch)
            }
            if (shouldQuote) append(ctx.quote.char)
        }
    }

    fun use(block: (SinkCsvWriterScope)->Unit) {
        writer.use {
            block(this)
        }
    }

    suspend fun useSuspend(block: suspend (SinkCsvWriterScope)->Unit) {
        writer.use {
            block(this)
        }
    }
}
