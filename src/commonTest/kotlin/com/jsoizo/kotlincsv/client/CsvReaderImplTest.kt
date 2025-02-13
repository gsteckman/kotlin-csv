package com.jsoizo.kotlincsv.client

import com.jsoizo.kotlincsv.dsl.context.CsvReaderContext
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.io.Buffer

class CsvReaderImplTest : StringSpec({
    "CsvReader class constructor" should {
        "be created with CsvReaderContext argument" {
            val context = CsvReaderContext().apply {
                quoteChar = '\''
                delimiter = '\t'
                escapeChar = '"'
                skipEmptyLine = true
            }
            val reader = CsvReaderImpl(context)
            assertSoftly {
                reader.quoteChar shouldBe '\''
                reader.delimiter shouldBe '\t'
                reader.escapeChar shouldBe '"'
                reader.skipEmptyLine shouldBe true
            }
        }
    }

    "readAll method (with String argument)" should {
        "read simple csv" {
            val result = CsvReaderImpl().readAll(
                """a,b,c
                        |d,e,f
                    """.trimMargin()
            )
            result shouldBe listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
        }
        "read simple csv as receiver" {
            val result = """a,b,c
                        |d,e,f
                    """.trimMargin().csvReadAll()
            result shouldBe listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
        }
        "read simple csv as Source" {
            Buffer().apply {
                write("""a,b,c
                        |d,e,f
                    """.trimMargin().encodeToByteArray())
            }.use {
                CsvReaderImpl().readAll(it) shouldBe listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
            }
        }
        "read simple csv as Source receiver" {
            Buffer().apply {
                write("""a,b,c
                        |d,e,f
                    """.trimMargin().encodeToByteArray())
            }.use {
                it.csvReadAll() shouldBe listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
            }
        }
        "read csv with line separator" {
            val result = CsvReaderImpl().readAll(
                """a,b,c,"x","y
                            | hoge"
                        |d,e,f,g,h
                    """.trimMargin()
            )
            val firstRow = listOf(
                "a", "b", "c", "x", """y
                    | hoge""".trimMargin()
            )
            val secondRow = listOf("d", "e", "f", "g", "h")
            result shouldBe listOf(firstRow, secondRow)
        }
        "get failed rowNum and colIndex when exception happened on parsing CSV" {
            val reader = CsvReaderImpl()
            val ex1 = shouldThrow<com.jsoizo.kotlincsv.util.CSVParseFormatException> {
                reader.readAll("a,\"\"failed")
            }
            val ex2 = shouldThrow<com.jsoizo.kotlincsv.util.CSVParseFormatException> {
                reader.readAll("a,b\nc,\"\"failed")
            }
            val ex3 = shouldThrow<com.jsoizo.kotlincsv.util.CSVParseFormatException> {
                reader.readAll("a,\"b\nb\"\nc,\"\"failed")
            }

            assertSoftly {
                ex1.rowNum shouldBe 1
                ex1.colIndex shouldBe 4
                ex1.char shouldBe 'f'

                ex2.rowNum shouldBe 2
                ex2.colIndex shouldBe 4
                ex2.char shouldBe 'f'

                ex3.rowNum shouldBe 3
                ex3.colIndex shouldBe 4
                ex3.char shouldBe 'f'
            }
        }
    }
})
