package com.jsoizo.kotlincsv.client

import com.jsoizo.kotlincsv.dsl.CsvWriterScope
import com.jsoizo.kotlincsv.dsl.context.CsvWriterContext
import com.jsoizo.kotlincsv.dsl.context.ICsvWriterContext
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * CSVWriter implementation, which decides where to write.
 *
 * @author doyaaaaaken
 * @author gsteckman
 */
internal class CsvWriterImpl(private val ctx: CsvWriterContext = CsvWriterContext())
    : CsvWriter, ICsvWriterContext by ctx {

    override fun open(sink: Sink, append: Boolean, write: CsvWriterScope.() -> Unit) {
        SinkCsvWriterScope(ctx, sink).use {
            it.write()
        }
    }

    override fun open(path: Path, append: Boolean, write: CsvWriterScope.() -> Unit) {
        val sink = SystemFileSystem.sink(path, append).buffered()
        open(sink, append, write)
    }

    override suspend fun openAsync(sink: Sink,
        append: Boolean,
        write: suspend CsvWriterScope.() -> Unit) {
        SinkCsvWriterScope(ctx, sink).useSuspend{
            it.write()
        }
    }

    override suspend fun openAsync(path: Path, append: Boolean, write: suspend CsvWriterScope.() -> Unit) {
        val sink = SystemFileSystem.sink(path, append).buffered()
        openAsync(sink, append, write)
    }

    /**
     * *** ONLY for long-running write case ***
     *
     * Get and use [SinkCsvWriterScope] directly.
     * MUST NOT forget to close [SinkCsvWriterScope] after using it.
     *
     * Use this method If you want to close file writer manually (i.e. streaming scenario).
     */
    @KotlinCsvExperimental
    fun openAndGetRawWriter(targetFileName: String, append: Boolean = false): SinkCsvWriterScope {
        val sink = SystemFileSystem.sink(Path(targetFileName), append).buffered()
        return SinkCsvWriterScope(ctx, sink)
    }

    /**
     * write all rows on assigned target file
     */
    override fun writeAll(rows: List<List<Any?>>, sink: Sink, append: Boolean) {
        open(sink, append) { writeRows(rows) }
    }

    override fun writeAll(rows: List<List<Any?>>, path: Path, append: Boolean) {
        open(path, append){ writeRows(rows) }
    }

    /**
     * write all rows on assigned target file
     */
    override suspend fun writeAllAsync(rows: List<List<Any?>>, sink: Sink, append: Boolean) {
        openAsync(sink, append) { writeRows(rows) }
    }

    override suspend fun writeAllAsync(rows: List<List<Any?>>, path: Path, append: Boolean) {
        openAsync(path, append) { writeRows(rows) }
    }
}
