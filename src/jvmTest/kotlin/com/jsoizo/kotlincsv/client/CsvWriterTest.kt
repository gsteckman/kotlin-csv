package com.jsoizo.kotlincsv.client

import com.jsoizo.kotlincsv.dsl.context.CsvWriterContext
import com.jsoizo.kotlincsv.dsl.context.WriteQuoteMode
import com.jsoizo.kotlincsv.dsl.csvWriter
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import java.io.File
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime

class CsvWriterTest : WordSpec({

    val testFileName = "test.csv"

    afterTest {
        // afterTest getting called more than once and kotlinx-io throws exception if trying to
        // delete non-existent file
        Path(testFileName).also {
            if(SystemFileSystem.exists(it)) {
                SystemFileSystem.delete(it)
            }
        }
    }

    fun readTestFile(charset: Charset = Charsets.UTF_8): String {
        return File(testFileName).readText(charset)
    }

    "CsvWriter class constructor" should {
        "be created with CsvWriterContext argument" {
            val context = CsvWriterContext().apply {
                delimiter = '\t'
                nullCode = "NULL"
                lineTerminator = "\n"
                outputLastLineTerminator = false
                prependBOM = true
                quote {
                    char = '\''
                    mode = WriteQuoteMode.ALL
                }
            }
            val writer = CsvWriterImpl(context)
            assertSoftly {
                writer.delimiter shouldBe '\t'
                writer.nullCode shouldBe "NULL"
                writer.lineTerminator shouldBe "\n"
                writer.outputLastLineTerminator shouldBe false
                writer.prependBOM shouldBe true
                writer.quote.char = '\''
                writer.quote.mode = WriteQuoteMode.ALL
            }
        }
    }

    "open method" should {
        val row1 = listOf("a", "b", null)
        val row2 = listOf("d", "2", "1.0")
        val expected = "a,b,\r\nd,2,1.0\r\n"

        "write simple csv data into file with writing each rows" {
            val buffer = Buffer()
            csvWriter().open(buffer) {
                writeRow(row1)
                writeRow(row2)
            }
            val actual = buffer.readString()
            actual shouldBe expected
        }

        "write simple csv data into file with writing all at one time" {
            csvWriter().open(Path(testFileName)) { writeRows(listOf(row1, row2)) }
            val actual = readTestFile()
            actual shouldBe expected
        }

        "write simple csv data to the tail of existing file with append = true" {
            val writer = csvWriter()
            writer.open(Path(testFileName), true) {
                writeRows(listOf(row1, row2))
            }
            writer.open(Path(testFileName), true) {
                writeRows(listOf(row1, row2))
            }
            val actual = readTestFile()
            actual shouldBe expected + expected
        }

        "overwrite simple csv data with append = false" {
            val writer = csvWriter()
            writer.open(Path(testFileName), false) {
                writeRows(listOf(row2, row2, row2))
            }
            writer.open(Path(testFileName), false) {
                writeRows(listOf(row1, row2))
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
    }

    "writeAll method without calling `open` method" should {
        val rows = listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
        val expected = "a,b,c\r\nd,e,f\r\n"

        "write data with target file name" {
            csvWriter().writeAll(rows, Path(testFileName))
            val actual = readTestFile()
            actual shouldBe expected
        }

        "write data to Sink" {
            val buffer = Buffer()
            csvWriter().writeAll(rows, buffer)
            val actual = buffer.readString()
            actual shouldBe expected
        }
    }

    "writeAllAsync method without calling `open` method" should {
        val rows = listOf(listOf("a", "b", "c"), listOf("d", "e", "f"))
        val expected = "a,b,c\r\nd,e,f\r\n"

        "write data with target file name" {
            csvWriter().writeAllAsync(rows, Path(testFileName))
            val actual = readTestFile()
            actual shouldBe expected
        }

        "write data to Sink" {
            val buffer = Buffer()
            csvWriter().writeAllAsync(rows, buffer)
            val actual = buffer.readString()
            actual shouldBe expected
        }
    }

    "Customized CsvWriter" should {
        "write csv with '|' delimiter" {
            val row1 = listOf("a", "b")
            val row2 = listOf("c", "d")
            val expected = "a|b\r\nc|d\r\n"
            csvWriter {
                delimiter = '|'
            }.open(Path(testFileName)) {
                writeRows(listOf(row1, row2))
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
        "write null with customized null code" {
            val row = listOf(null, null)
            csvWriter {
                nullCode = "NULL"
            }.open(Path(testFileName)) {
                writeRow(row)
            }
            val actual = readTestFile()
            actual shouldBe "NULL,NULL\r\n"
        }
        "write csv with \n line terminator" {
            val row1 = listOf("a", "b")
            val row2 = listOf("c", "d")
            val expected = "a,b\nc,d\n"
            csvWriter {
                lineTerminator = "\n"
            }.open(Path(testFileName)) {
                writeRows(listOf(row1, row2))
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
        "write csv with WriteQuoteMode.ALL mode" {
            val row1 = listOf("a", "b")
            val row2 = listOf("c", "d")
            val expected = "\"a\",\"b\"\r\n\"c\",\"d\"\r\n"
            csvWriter {
                quote {
                    mode = WriteQuoteMode.ALL
                }
            }.open(Path(testFileName)) {
                writeRows(listOf(row1, row2))
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
        "write csv with WriteQuoteMode.NON_NUMERIC mode" {
            val row1 = listOf("a", "b", 1)
            val row2 = listOf(2.0, "03.0", "4.0.0")
            val expected = "\"a\",\"b\",1\r\n2.0,03.0,\"4.0.0\"\r\n"
            csvWriter {
                quote {
                    mode = WriteQuoteMode.NON_NUMERIC
                }
            }.open(Path(testFileName)) {
                writeRows(listOf(row1, row2))
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
        "write csv with custom quote character" {
            val row1 = listOf("a'", "b")
            val row2 = listOf("'c", "d")
            val expected = "'a''',b\r\n'''c',d\r\n"
            csvWriter {
                quote {
                    char = '\''
                }
            }.open(Path(testFileName)) {
                writeRows(listOf(row1, row2))
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
        "write csv with custom quote character on WriteQuoteMode.ALL mode" {
            val rows = listOf(listOf("a1", "b1"), listOf("a2", "b2"))
            val expected = "_a1_,_b1_\r\n_a2_,_b2_\r\n"
            csvWriter {
                quote {
                    mode = WriteQuoteMode.ALL
                    char = '_'
                }
            }.writeAll(rows, Path(testFileName))
            val actual = readTestFile()
            actual shouldBe expected
        }
        "write simple csv with disabled last line terminator with custom terminator" {
            val row1 = listOf("a", "b")
            val row2 = listOf("c", "d")
            val expected = "a,b\nc,d"
            csvWriter {
                lineTerminator = "\n"
                outputLastLineTerminator = false
            }.open(Path(testFileName)) {
                writeRows(listOf(row1, row2))
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
        "write simple csv with enabled last line and custom terminator" {
            val row1 = listOf("a", "b")
            val row2 = listOf("c", "d")
            val expected = "a,b\nc,d\n"
            csvWriter {
                lineTerminator = "\n"
                outputLastLineTerminator = true
            }.open(Path(testFileName)) {
                writeRows(listOf(row1, row2))
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
        "write simple csv with disabled last line terminator" {
            val row1 = listOf("a", "b")
            val row2 = listOf("c", "d")
            val expected = "a,b\r\nc,d"
            csvWriter {
                outputLastLineTerminator = false
            }.open(Path(testFileName)) {
                writeRows(listOf(row1, row2))
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
        "write simple csv with prepending BOM" {
            val row1 = listOf("a", "b")
            val row2 = listOf("c", "d")
            val expected = "\uFEFFa,b\r\nc,d\r\n"
            csvWriter {
                prependBOM = true
            }.open(Path(testFileName)) {
                writeRows(listOf(row1, row2))
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
        "write simple csv with disabled last line terminator multiple writes" {
            val row1 = listOf("a", "b")
            val row2 = listOf("c", "d")
            val row3 = listOf("e", "f")
            val row4 = listOf("g", "h")
            val row5 = listOf("1", "2")
            val row6 = listOf("3", "4")
            val expected = "a,b\r\nc,d\r\ne,f\r\ng,h\r\n1,2\r\n3,4"
            csvWriter {
                outputLastLineTerminator = false
            }.open(Path(testFileName)) {
                writeRow(row1)
                writeRows(listOf(row2, row3))
                writeRow(row4)
                writeRows(listOf(row5, row6))
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
    }

    "openAndGetRawWriter method" should {
        val row1 = listOf("a", "b", null)
        val row2 = listOf("d", "2", "1.0")
        val expected = "a,b,\r\nd,2,1.0\r\n"

        "get raw writer from fileName string and can use it" {
            @OptIn(KotlinCsvExperimental::class)
            val writer = CsvWriterImpl().openAndGetRawWriter(testFileName)
            writer.writeRow(row1)
            writer.writeRow(row2)
            writer.close()

            val actual = readTestFile()
            actual shouldBe expected
        }
    }

    "suspend writeRow method" should {
        "suspend write any primitive types to Path" {
            val row = listOf("String", 'C', 1, 2L, 3.45, true, null)
            val expected = "String,C,1,2,3.45,true,\r\n"
            csvWriter().openAsync(Path(testFileName)) {
                writeRow(row)
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
        "suspend write any primitive types to Sink" {
            val row = listOf("String", 'C', 1, 2L, 3.45, true, null)
            val expected = "String,C,1,2,3.45,true,\r\n"
            val buffer = Buffer()
            csvWriter().openAsync(buffer) {
                writeRow(row)
            }
            val actual = buffer.readString()
            actual shouldBe expected
        }
        "suspend write row from variable arguments" {
            val date1 = LocalDate.of(2019, 8, 19)
            val date2 = LocalDateTime.of(2020, 9, 20, 14, 32, 21)

            val expected = "a,b,c\r\n" +
                    "d,e,f\r\n" +
                    "1,2,3\r\n" +
                    "2019-08-19,2020-09-20T14:32:21\r\n"
            csvWriter().openAsync(Path(testFileName)) {
                writeRow("a", "b", "c")
                writeRow("d", "e", "f")
                writeRow(1, 2, 3)
                writeRow(date1, date2)
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
        "suspend write all Sequence data" {
            val rows = listOf(listOf("a", "b", "c"), listOf("d", "e", "f")).asSequence()
            val expected = "a,b,c\r\nd,e,f\r\n"
            csvWriter().openAsync(Path(testFileName)) {
                writeRows(rows)
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
    }
    "suspend flush method" should {
        "flush stream" {
            val row = listOf("a", "b")
            csvWriter().openAsync(Path(testFileName)) {
                writeRow(row)
                flush()
                val actual = readTestFile()
                actual shouldBe "a,b\r\n"
            }
        }
    }
    "validate suspend test as flow" should {
        "execute line" {
            val rows = listOf(listOf("a", "b", "c"), listOf("d", "e", "f")).asSequence()
            val expected = "a,b,c\r\nd,e,f\r\n"
            csvWriter().openAsync(Path(testFileName)) {
                delay(100)
                rows.forEach {
                    delay(100)
                    writeRow(it)
                }
            }
            val actual = readTestFile()
            actual shouldBe expected
        }
    }
})
