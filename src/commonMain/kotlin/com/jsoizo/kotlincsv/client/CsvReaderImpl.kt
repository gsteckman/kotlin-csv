package com.jsoizo.kotlincsv.client

import com.jsoizo.kotlincsv.dsl.CsvReaderScope
import com.jsoizo.kotlincsv.dsl.context.CsvReaderContext
import com.jsoizo.kotlincsv.dsl.context.ICsvReaderContext
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * CSVReader implementation.
 *
 * @author gsteckman
 */
internal class CsvReaderImpl(
    private val ctx: CsvReaderContext = CsvReaderContext()
) : CsvReader, ICsvReaderContext by ctx {
    override fun readAll(data: String): List<List<String>> = data.csvReadAll(ctx)

    override fun readAll(data: Source): List<List<String>> = data.csvReadAll(ctx)

    override fun readAll(path: Path): List<List<String>> = path.csvReadAll(ctx)

    override fun readAllWithHeader(data: String): List<Map<String, String>> =
        data.csvReadAllWithHeader(ctx)

    override fun readAllWithHeader(data: Source): List<Map<String, String>> = data.csvReadAllWithHeader(ctx)

    override fun readAllWithHeader(path: Path): List<Map<String, String>> = path.csvReadAllWithHeader(ctx)

    override fun <T> open(source: Source, read: CsvReaderScope.() -> T): T =
        SourceCsvReaderScope(ctx, source, ctx.logger).read()

    override fun <T> open(path: Path, read: CsvReaderScope.() -> T): T =
        SystemFileSystem.source(path).buffered().use {
            return open(it, read)
        }

    override suspend fun <T> openAsync(source: Source, read: suspend CsvReaderScope.() -> T): T =
        SourceCsvReaderScope(ctx, source, ctx.logger).read()

    override suspend fun <T> openAsync(path: Path, read: suspend CsvReaderScope.() -> T): T =
        SystemFileSystem.source(path).buffered().use {
            return openAsync(it, read)
        }
}

/**
 * read csv data as String, and convert into List<List<String>>
 */
fun String.csvReadAll(ctx: CsvReaderContext = CsvReaderContext()): List<List<String>> =
    Buffer().apply{ write(encodeToByteArray()) }.use {
        it.csvReadAll(ctx)
    }

/**
 * read csv data with header, and convert into List<Map<String, String>>
 */
fun String.csvReadAllWithHeader(ctx: CsvReaderContext = CsvReaderContext()): List<Map<String, String>> =
    Buffer().apply{ write(encodeToByteArray()) }.use {
        it.csvReadAllWithHeader(ctx)
    }

/**
 * read csv data from a Source, and convert into List<List<String>>.
 */
fun Source.csvReadAll(ctx: CsvReaderContext = CsvReaderContext()): List<List<String>> =
    SourceCsvReaderScope(ctx, this, ctx.logger).readAllAsSequence().toList()

/**
 * read csv data from a Path, and convert into List<List<String>>.
 *
 * No need to close the Path when calling this method.
 */
fun Path.csvReadAll(ctx: CsvReaderContext = CsvReaderContext()): List<List<String>> =
    SystemFileSystem.source(this).buffered().use {
        return it.csvReadAll(ctx)
    }

/**
 * read csv data with a header from a Source, and convert into List<List<String>>.
 */
fun Source.csvReadAllWithHeader(ctx: CsvReaderContext = CsvReaderContext()): List<Map<String, String>> =
    SourceCsvReaderScope(ctx, this, ctx.logger).readAllWithHeaderAsSequence().toList()

/**
 * read csv data with header, and convert into List<Map<String, String>>
 *
 * No need to close Path when calling this method.
 */
fun Path.csvReadAllWithHeader(ctx: CsvReaderContext = CsvReaderContext()): List<Map<String, String>> =
    SystemFileSystem.source(this).buffered().use {
        return it.csvReadAllWithHeader(ctx)
    }

/**
 * read all csv rows as Sequence
 */
fun Source.csvReadAllAsSequence(fieldsNum: Int? = null, ctx: CsvReaderContext = CsvReaderContext())
    : Sequence<List<String>> = SourceCsvReaderScope(ctx, this, ctx.logger).readAllAsSequence(fieldsNum)

/**
 * read all csv rows as Sequence with header information
 */
fun Source.csvReadAllWithHeaderAsSequence(ctx: CsvReaderContext = CsvReaderContext())
    : Sequence<Map<String, String>> = SourceCsvReaderScope(ctx, this, ctx.logger).readAllWithHeaderAsSequence()
