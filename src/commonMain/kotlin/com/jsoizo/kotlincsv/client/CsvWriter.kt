package com.jsoizo.kotlincsv.client

import com.jsoizo.kotlincsv.dsl.CsvWriterScope
import com.jsoizo.kotlincsv.dsl.context.ICsvWriterContext
import kotlinx.io.Sink
import kotlinx.io.files.Path

interface CsvWriter : ICsvWriterContext {
    fun writeAll(rows: List<List<Any?>>, sink: Sink, append: Boolean = false)

    fun writeAll(rows: List<List<Any?>>, path: Path, append: Boolean = false)

    suspend fun writeAllAsync(rows: List<List<Any?>>, sink: Sink, append: Boolean = false)

    suspend fun writeAllAsync(rows: List<List<Any?>>, path: Path, append: Boolean = false)

    fun open(sink: Sink, append: Boolean = false, write: CsvWriterScope.() -> Unit)

    fun open(path: Path, append: Boolean = false, write: CsvWriterScope.() -> Unit)

    suspend fun openAsync(sink: Sink, append: Boolean = false, write: suspend CsvWriterScope.() -> Unit)

    suspend fun openAsync(path: Path, append: Boolean = false, write: suspend CsvWriterScope.() -> Unit)
}
