package com.jsoizo.kotlincsv.client

import com.jsoizo.kotlincsv.dsl.CsvWriterScope
import com.jsoizo.kotlincsv.dsl.context.CsvWriterContext
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.io.Buffer
import kotlinx.io.readString

class SinkCsvWriterScopeTest : StringSpec({
    fun writeToBuffer(write: CsvWriterScope.() -> Unit) : Buffer {
        val ctx = CsvWriterContext()
        val buffer = Buffer()
        SinkCsvWriterScope(ctx, buffer).apply { write() }.close()
        return buffer
    }

    "writeRow method should write any primitive types" {
        val row = listOf("String", 'C', 1, 2L, 3.45, true, null)
        val expected = "String,C,1,2,3.45,true,\r\n"

        writeToBuffer { writeRow(row) }.readString() shouldBe expected
    }
    "writeRow method should write kotlinx.datetime.LocalDate and kotlinx.datetime.LocalDateTime types" {
        val row = listOf(
            LocalDate(2019, 8, 19),
            LocalDateTime(2020, 9, 20, 14, 32, 21)
        )
        val expected = "2019-08-19,2020-09-20T14:32:21\r\n"
        writeToBuffer { writeRow(row) }.readString() shouldBe expected
    }
    "writeRow method should write row from variable arguments" {
        val date1 = LocalDate(2019, 8, 19)
        val date2 = LocalDateTime(2020, 9, 20, 14, 32, 21)

        val expected = "a,b,c\r\n" +
                "d,e,f\r\n" +
                "1,2,3\r\n" +
                "2019-08-19,2020-09-20T14:32:21\r\n"
        writeToBuffer {
            writeRow("a", "b", "c")
            writeRow("d", "e", "f")
            writeRow(1, 2, 3)
            writeRow(date1, date2)
        }.readString() shouldBe expected
    }
    "writeAll method should write Sequence data" {
        val rows = listOf(listOf("a", "b", "c"), listOf("d", "e", "f")).asSequence()
        val expected = "a,b,c\r\nd,e,f\r\n"
        writeToBuffer {
            writeRows(rows)
        }.readString() shouldBe expected
    }
    "writeAll method should write escaped field when a field contains quoteChar in it" {
        val rows = listOf(listOf("a", "\"b", "c"), listOf("d", "e", "f\""))
        val expected = "a,\"\"\"b\",c\r\nd,e,\"f\"\"\"\r\n"
        writeToBuffer{
            writeRows(rows)
        }.readString() shouldBe expected
    }
    "writeAll method should write escaped field when a field contains delimiter in it" {
        val rows = listOf(listOf("a", ",b", "c"), listOf("d", "e", "f,"))
        val expected = "a,\",b\",c\r\nd,e,\"f,\"\r\n"
        writeToBuffer{
            writeRows(rows)
        }.readString() shouldBe expected
    }
    "writeAll method should write quoted field when a field contains cr or lf in it" {
        val rows = listOf(listOf("a", "\nb", "c"), listOf("d", "e", "f\r\n"))
        val expected = "a,\"\nb\",c\r\nd,e,\"f\r\n\"\r\n"
        writeToBuffer{
            writeRows(rows)
        }.readString() shouldBe expected
    }
    "writeAll method should write no line terminator when row is empty for rows from list" {
        val rows = listOf(listOf("a", "b", "c"), listOf(), listOf("d", "e", "f"))
        val expected = "a,b,c\r\nd,e,f\r\n"
        writeToBuffer{
            writeRows(rows)
        }.readString() shouldBe expected
    }
    "writeAll method should write no line terminator when row is empty for rows from sequence" {
        val rows = listOf(listOf("a", "b", "c"), listOf(), listOf("d", "e", "f")).asSequence()
        val expected = "a,b,c\r\nd,e,f\r\n"
        writeToBuffer{
            writeRows(rows)
        }.readString() shouldBe expected
    }

    "flush method should flush stream" {
        val row = listOf("a", "b")
        val ctx = CsvWriterContext()
        val buffer = Buffer()
        SinkCsvWriterScope(ctx, buffer).apply {
            writeRow(row)
            flush()
            buffer.readString() shouldBe "a,b\r\n"
        }
    }
})
