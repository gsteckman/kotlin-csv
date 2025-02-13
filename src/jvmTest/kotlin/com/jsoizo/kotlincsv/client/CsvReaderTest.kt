package com.jsoizo.kotlincsv.client

import com.jsoizo.kotlincsv.dsl.context.ExcessFieldsRowBehaviour
import com.jsoizo.kotlincsv.dsl.context.InsufficientFieldsRowBehaviour
import com.jsoizo.kotlincsv.dsl.csvReader
import com.jsoizo.kotlincsv.util.CSVFieldNumDifferentException
import com.jsoizo.kotlincsv.util.CSVParseFormatException
import com.jsoizo.kotlincsv.util.MalformedCSVException
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.asFlow
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

/**
 * This class is not in commonTest because reading Path(s) from files is not supported on all
 * platforms.
 */
class CsvReaderTest : StringSpec({
    "readAll method (with String argument)" should {
        "read simple csv" {
            val result = csvReader().readAll(
                """a,b,c
                        |d,e,f
                    """.trimMargin()
            )
            result shouldBe listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
        }
        "read csv with line separator" {
            val result = csvReader().readAll(
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
            val reader = csvReader()
            val ex1 = shouldThrow<CSVParseFormatException> {
                reader.readAll("a,\"\"failed")
            }
            val ex2 = shouldThrow<CSVParseFormatException> {
                reader.readAll("a,b\nc,\"\"failed")
            }
            val ex3 = shouldThrow<CSVParseFormatException> {
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
    "readAll method (with Path argument)" should {
        "read simple csv" {
            val result = csvReader().readAll(readTestDataFile("simple.csv"))
            result shouldBe listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
        }
        "read simple csv as receiver" {
            val result = readTestDataFile("simple.csv").csvReadAll()
            result shouldBe listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
        }
        "read tsv file" {
            val result = csvReader {
                delimiter = '\t'
            }.readAll(readTestDataFile("simple.tsv"))
            result shouldBe listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
        }
        "read csv with empty field" {
            val result = csvReader().readAll(readTestDataFile("empty-fields.csv"))
            result shouldBe listOf(listOf("a", "", "b", "", "c", ""), listOf("d", "", "e", "", "f", ""))
        }
        "read csv with escaped field" {
            val result = csvReader().readAll(readTestDataFile("escape.csv"))
            result shouldBe listOf(listOf("a", "b", "c"), listOf("d", "\"e", "f"))
        }
        "read csv with line breaks enclosed in double quotes" {
            val result = csvReader().readAll(readTestDataFile("line-breaks.csv"))
            result shouldBe listOf(listOf("a", "b\nb", "c"), listOf("\nd", "e", "f"))
        }
        "read csv with custom quoteChar and delimiter" {
            val result = csvReader {
                delimiter = '#'
                quoteChar = '$'
            }.readAll(readTestDataFile("hash-separated-dollar-quote.csv"))
            result shouldBe listOf(listOf("Foo ", "Bar ", "Baz "), listOf("a", "b", "c"))
        }
        "read csv with custom escape character" {
            val result = csvReader {
                escapeChar = '\\'
            }.readAll(readTestDataFile("backslash-escape.csv"))
            result shouldBe listOf(listOf("\"a\"", "\"This is a test\""), listOf("\"b\"", "This is a \"second\" test"))
        }
        "read csv with BOM" {
            val result = csvReader {
                escapeChar = '\\'
            }.readAll(readTestDataFile("bom.csv"))
            result shouldBe listOf(listOf("a", "b", "c"))
        }
        "read empty csv with BOM" {
            val result = csvReader {
                escapeChar = '\\'
            }.readAll(readTestDataFile("empty-bom.csv"))
            result shouldBe listOf()
        }
        //refs https://github.com/tototoshi/scala-csv/issues/22
        "read csv with \u2028 field" {
            val result = csvReader().readAll(readTestDataFile("unicode2028.csv"))
            result shouldBe listOf(listOf("\u2028"))
        }
        "read csv with empty lines" {
            val result = csvReader {
                skipEmptyLine = true
            }.readAll(readTestDataFile("empty-line.csv"))
            result shouldBe listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
        }
        "read csv with quoted empty line field" {
            val result = csvReader {
                skipEmptyLine = true
            }.readAll(readTestDataFile("quoted-empty-line.csv"))
            result shouldBe listOf(listOf("a", "b", "c\n\nc"), listOf("d", "e", "f"))
        }
        "throw exception when reading malformed csv" {
            shouldThrow<MalformedCSVException> {
                csvReader().readAll(readTestDataFile("malformed.csv"))
            }
        }
        "throw exception when reading csv with different fields num on each row" {
            val ex = shouldThrow<CSVFieldNumDifferentException> {
                csvReader().readAll(readTestDataFile("different-fields-num.csv"))
            }

            assertSoftly {
                ex.fieldNum shouldBe 3
                ex.fieldNumOnFailedRow shouldBe 2
                ex.csvRowNum shouldBe 2
            }
        }
        "Trim row when reading csv with greater num of fields on a subsequent row" {
            val expected = listOf(listOf("a", "b"), listOf("c", "d"))
            val actual =
                csvReader {
                    excessFieldsRowBehaviour = ExcessFieldsRowBehaviour.TRIM
                }.readAll(readTestDataFile("different-fields-num2.csv"))

            assertSoftly {
                actual shouldBe expected
                actual.size shouldBe 2
            }
        }
        "it should be be possible to skip rows with both excess and insufficient fields" {
            val expected = listOf(listOf("a", "b"))
            val actual =
                csvReader {
                    excessFieldsRowBehaviour = ExcessFieldsRowBehaviour.IGNORE
                    insufficientFieldsRowBehaviour = InsufficientFieldsRowBehaviour.IGNORE
                }.readAll(readTestDataFile("varying-column-lengths.csv"))

            assertSoftly {
                actual shouldBe expected
                actual.size shouldBe 1
            }
        }
        "it should be be possible to replace insufficient fields with strings and skip rows with excess fields" {
            val expected = listOf(listOf("a", "b"), listOf("c", ""))
            val actual =
                csvReader {
                    excessFieldsRowBehaviour = ExcessFieldsRowBehaviour.IGNORE
                    insufficientFieldsRowBehaviour = InsufficientFieldsRowBehaviour.EMPTY_STRING
                }.readAll(readTestDataFile("varying-column-lengths.csv"))

            assertSoftly {
                actual shouldBe expected
                actual.size shouldBe 2
            }
        }
        "it should be be possible to replace insufficient fields with strings and trim rows with excess fields" {
            val expected = listOf(listOf("a", "b"), listOf("c", ""), listOf("d", "e"))
            val actual =
                csvReader {
                    excessFieldsRowBehaviour = ExcessFieldsRowBehaviour.TRIM
                    insufficientFieldsRowBehaviour = InsufficientFieldsRowBehaviour.EMPTY_STRING
                }.readAll(readTestDataFile("varying-column-lengths.csv"))

            assertSoftly {
                actual shouldBe expected
                actual.size shouldBe 3
            }
        }
        "it should be be possible to trim excess columns and skip insufficient row columns" {
            val expected = listOf(listOf("a", "b"), listOf("d", "e"))
            val actual =
                csvReader {
                    excessFieldsRowBehaviour = ExcessFieldsRowBehaviour.TRIM
                    insufficientFieldsRowBehaviour = InsufficientFieldsRowBehaviour.IGNORE
                }.readAll(readTestDataFile("varying-column-lengths.csv"))

            assertSoftly {
                actual shouldBe expected
                actual.size shouldBe 2
            }
        }
        "If the excess fields behaviour is ERROR and the insufficient behaviour is IGNORE then an error should be thrown if there are excess columns" {
            val ex = shouldThrow<CSVFieldNumDifferentException> {
                csvReader {
                    insufficientFieldsRowBehaviour = InsufficientFieldsRowBehaviour.IGNORE
                }.readAll(readTestDataFile("varying-column-lengths.csv"))
            }

            assertSoftly {
                ex.fieldNum shouldBe 2
                ex.fieldNumOnFailedRow shouldBe 3
                ex.csvRowNum shouldBe 3
            }
        }
        "If the excess fields behaviour is IGNORE or TRIM and the insufficient behaviour is ERROR then an error should be thrown if there are columns with insufficient rows" {
            val ex1 = shouldThrow<CSVFieldNumDifferentException> {
                csvReader {
                    excessFieldsRowBehaviour = ExcessFieldsRowBehaviour.IGNORE
                }.readAll(readTestDataFile("varying-column-lengths.csv"))
            }
            val ex2 = shouldThrow<CSVFieldNumDifferentException> {
                csvReader {
                    excessFieldsRowBehaviour = ExcessFieldsRowBehaviour.TRIM
                }.readAll(readTestDataFile("varying-column-lengths.csv"))
            }
            assertSoftly {
                ex1.fieldNum shouldBe 2
                ex1.fieldNumOnFailedRow shouldBe 1
                ex1.csvRowNum shouldBe 2

                ex2.fieldNum shouldBe 2
                ex2.fieldNumOnFailedRow shouldBe 1
                ex2.csvRowNum shouldBe 2
            }
        }
        "should not throw exception when reading csv with different fields num on each row with expected number of columns" {
            val expected = listOf(listOf("a", "b", "c"))
            val actual = csvReader {
                skipMissMatchedRow = true
            }.readAll(readTestDataFile("different-fields-num.csv"))

            val expected2 = listOf(listOf("a", "b"))
            val actual2 = csvReader {
                skipMissMatchedRow = true
            }.readAll(readTestDataFile("different-fields-num2.csv"))

            assertSoftly {
                actual shouldBe expected
                actual.size shouldBe 1

                actual2 shouldBe expected2
                actual2.size shouldBe 1
            }
        }
        "should not throw exception when reading csv with header and different fields num on each row" {
            val expected = listOf(
                mapOf("h1" to "a", "h2" to "b", "h3" to "c"),
                mapOf("h1" to "g", "h2" to "h", "h3" to "i")
            )
            val actual = csvReader {
                skipMissMatchedRow = true
            }.readAllWithHeader(readTestDataFile("with-header-different-size-row.csv"))

            assertSoftly {
                actual.size shouldBe 2
                expected shouldBe actual
            }
        }
    }

    "readAllWithHeader method" should {
        val expected = listOf(
            mapOf("h1" to "a", "h2" to "b", "h3" to "c"),
            mapOf("h1" to "d", "h2" to "e", "h3" to "f")
        )

        "read simple csv file" {
            val file = readTestDataFile("with-header.csv")
            val result = csvReader().readAllWithHeader(file)
            result shouldBe expected
        }
        "read simple csv file as receiver" {
            val file = readTestDataFile("with-header.csv")
            val result = file.csvReadAllWithHeader()
            result shouldBe expected
        }
        "read simple csv as Source" {
            val src = SystemFileSystem.source(readTestDataFile("with-header.csv")).buffered()
            src.use {
                CsvReaderImpl().readAllWithHeader(it) shouldBe expected
            }
        }
        "read simple csv as String receiver" {
            val src = SystemFileSystem.source(readTestDataFile("with-header.csv")).buffered()
            src.use {
                it.readString().csvReadAllWithHeader() shouldBe expected
            }
        }
        "read simple csv as Source receiver" {
            val src = SystemFileSystem.source(readTestDataFile("with-header.csv")).buffered()
            src.use {
                it.csvReadAllWithHeader() shouldBe expected
            }
        }
        "throw on duplicated headers" {
            val file = readTestDataFile("with-duplicate-header.csv")
            shouldThrow<MalformedCSVException> { csvReader().readAllWithHeader(file) }
        }

        "auto rename duplicated headers" {
            val deduplicateExpected = listOf(
                mapOf("a" to "1", "b" to "2", "b_2" to "3", "b_3" to "4", "c" to "5", "c_2" to "6"),
            )
            val file = readTestDataFile("with-duplicate-header.csv")
            val result = csvReader {
                autoRenameDuplicateHeaders = true
            }.readAllWithHeader(file)
            result shouldBe deduplicateExpected
        }

        "auto rename failed" {
            val file = readTestDataFile("with-duplicate-header-auto-rename-failed.csv")
            shouldThrow<MalformedCSVException> { csvReader().readAllWithHeader(file) }
        }

        "read from String" {
            val data = """h1,h2,h3
                    |a,b,c
                    |d,e,f
                """.trimMargin()
            val result = csvReader().readAllWithHeader(data)
            result shouldBe expected
        }

        "read from String containing line break" {
            val data = """h1,"h
                    |2",h3
                    |a,b,c
                """.trimMargin()
            val result = csvReader().readAllWithHeader(data)
            val h2 = """h
                    |2""".trimMargin()
            result shouldBe listOf(mapOf("h1" to "a", h2 to "b", "h3" to "c"))
        }
        "number of fields in a row has to be based on the header #82" {
            val data = "1,2,3\na,b\nx,y,z"

            val exception = shouldThrow<CSVFieldNumDifferentException> {
                csvReader().readAllWithHeader(data)
            }
            exception.fieldNum shouldBe 3
        }
    }

    "open method (with Path argument)" should {
        val rows = csvReader().open(readTestDataFile("simple.csv")) {
            readAllAsSequence().toList()
        }
        rows shouldBe listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
    }

    "execute as suspending function" should {
        "open suspending method (with Path argument)" {
            val rows = csvReader().openAsync(readTestDataFile("simple.csv")) {
                readAllAsSequence().toList()
            }
            rows shouldBe listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
        }
        "validate test as flow" {
            val fileStream = readTestDataFile("simple.csv")
            val rows = mutableListOf<List<String>>()
            csvReader().openAsync(fileStream) {
                readAllAsSequence().asFlow().collect {
                    rows.add(it)
                }
            }
            rows shouldBe listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
        }
    }

    "csvReadAllAsSequence method (with Source receiver)" should {
        val rows = SystemFileSystem.source(readTestDataFile("simple.csv")).buffered().use{
            it.csvReadAllAsSequence().toList()
        }
        rows shouldBe listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
    }

    "csvReadAllAsSequenceWithHeader (with Source receiver)" should {
        val expected = listOf(
            mapOf("h1" to "a", "h2" to "b", "h3" to "c"),
            mapOf("h1" to "d", "h2" to "e", "h3" to "f")
        )
        val result = SystemFileSystem.source(readTestDataFile("with-header.csv")).buffered().use{
            it.csvReadAllWithHeaderAsSequence().toList()
        }
        result shouldBe expected
    }
})

private fun readTestDataFile(fileName: String): Path {
    val path = Path("src", "jvmTest", "resources", "testdata", "csv", fileName)
    println("Made path: $path")
    return path
}
