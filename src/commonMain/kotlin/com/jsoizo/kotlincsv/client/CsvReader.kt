package com.jsoizo.kotlincsv.client

import com.jsoizo.kotlincsv.dsl.CsvReaderScope
import com.jsoizo.kotlincsv.dsl.context.ICsvReaderContext
import kotlinx.io.Source
import kotlinx.io.files.Path

interface CsvReader : ICsvReaderContext {
    /**
     * read csv data as String, and convert into List<List<String>>
     */
    fun readAll(data: String): List<List<String>>

    /**
     * read csv data from a Source, and convert into List<List<String>>.
     */
    fun readAll(data: Source): List<List<String>>

    /**
     * read csv data from a Path, and convert into List<List<String>>
     *
     * No need to close the [path] when calling this method.
     */
    fun readAll(path: Path): List<List<String>>

    /**
     * read csv data with header, and convert into List<Map<String, String>>
     */
    fun readAllWithHeader(data: String): List<Map<String, String>>

    /**
     * read csv data with a header from a Source, and convert into List<List<String>>.
     */
    fun readAllWithHeader(data: Source): List<Map<String, String>>

    /**
     * read csv data with header, and convert into List<Map<String, String>>
     *
     * No need to close [path] when calling this method.
     */
    fun readAllWithHeader(path: Path): List<Map<String, String>>

    /**
     * open [source] and execute reading process.
     *
     * If you want to control read flow precisely, use this method.
     * Otherwise, use utility method (e.g. CsvReader.readAll ).
     *
     * Usage example:
     * <pre>
     *   val data: Sequence<List<String?>> = csvReader().open(source) {
     *       readAllAsSequence()
     *           .map { fields -> fields.map { it.trim() } }
     *           .map { fields -> fields.map { if(it.isBlank()) null else it } }
     *   }
     * </pre>
     */
    fun <T> open(source: Source, read: CsvReaderScope.() -> T): T

    /**
     * open [path] and execute reading process.
     *
     * If you want to control read flow precisely, use this method.
     * Otherwise, use utility method (e.g. CsvReader.readAll ).
     *
     * Usage example:
     * <pre>
     *   val data: Sequence<List<String?>> = csvReader().open("test.csv") {
     *       readAllAsSequence()
     *           .map { fields -> fields.map { it.trim() } }
     *           .map { fields -> fields.map { if(it.isBlank()) null else it } }
     *   }
     * </pre>
     */
    fun <T> open(path: Path, read: CsvReaderScope.() -> T): T

    /**
     * open [source] and execute reading process on a **suspending** function.
     *
     * If you want to control read flow precisely, use this method.
     * Otherwise, use utility method (e.g. CsvReader.readAll ).
     *
     * Usage example:
     * <pre>
     *   val data: Sequence<List<String?>> = csvReader().open(source) {
     *       readAllAsSequence()
     *           .map { fields -> fields.map { it.trim() } }
     *           .map { fields -> fields.map { if(it.isBlank()) null else it } }
     *   }
     * </pre>
     */
    suspend fun <T> openAsync(source: Source, read: suspend CsvReaderScope.() -> T): T

    /**
     * open [path] and execute reading process on a **suspending** function.
     *
     * If you want to control read flow precisely, use this method.
     * Otherwise, use utility method (e.g. CsvReader.readAll ).
     *
     * Usage example:
     * <pre>
     *   val data: Sequence<List<String?>> = csvReader().openAsync("test.csv") {
     *       readAllAsSequence()
     *           .map { fields -> fields.map { it.trim() } }
     *           .map { fields -> fields.map { if(it.isBlank()) null else it } }
     *   }
     * </pre>
     */
    suspend fun <T> openAsync(path: Path, read: suspend CsvReaderScope.() -> T): T
}
