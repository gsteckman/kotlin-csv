package com.jsoizo.kotlincsv.client

import com.jsoizo.kotlincsv.dsl.context.CsvReaderContext
import com.jsoizo.kotlincsv.dsl.context.CsvWriterContext
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.Buffer

class CsvReadWriteCompatibilityTest : StringSpec({
    "CSVReader and CSVWriter are compatible" {
        val data = listOf(
            listOf("a", "bb", "ccc"),
            listOf("d", "ee", "fff")
        )
        val buffer = Buffer()
        SinkCsvWriterScope(CsvWriterContext(), buffer).writeRows(data)
        val rctx = CsvReaderContext()
        val actual = SourceCsvReaderScope(rctx, buffer, rctx.logger).readAllAsSequence().toList()
        actual shouldBe data
    }
})
