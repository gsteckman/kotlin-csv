package com.jsoizo.kotlincsv.dsl

interface CsvReaderScope {
    /**
     * read all csv rows as Sequence
     */
    fun readAllAsSequence(fieldsNum: Int? = null): Sequence<List<String>>

    /**
     * read all csv rows as Sequence with header information
     */
    fun readAllWithHeaderAsSequence(): Sequence<Map<String, String>>
}
