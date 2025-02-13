package com.jsoizo.kotlincsv.dsl

interface CsvWriterScope {
    fun writeRow(row: List<Any?>)

    fun writeRow(vararg entry: Any?)

    fun writeRows(rows: List<List<Any?>>)

    fun writeRows(rows: Sequence<List<Any?>>)

    fun flush()

    fun close()
}
