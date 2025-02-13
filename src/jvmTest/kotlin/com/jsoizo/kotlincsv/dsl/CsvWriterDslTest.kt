package com.jsoizo.kotlincsv.dsl

import com.jsoizo.kotlincsv.client.CsvWriterImpl
import com.jsoizo.kotlincsv.dsl.context.WriteQuoteMode
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

/**
 * @author doyaaaaaken
 */
class CsvWriterDslTest : StringSpec({
    "csvWriter method should work as global method with no argument" {
        val writer = csvWriter()
        writer.shouldBeTypeOf<CsvWriterImpl>()
    }
    "csvWriter method should work as dsl" {
        val writer = csvWriter {
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
        assertSoftly {
            writer.delimiter shouldBe '\t'
            writer.nullCode shouldBe "NULL"
            writer.lineTerminator shouldBe "\n"
            writer.outputLastLineTerminator shouldBe false
            writer.prependBOM shouldBe true
            writer.quote.char shouldBe '\''
            writer.quote.mode = WriteQuoteMode.ALL
        }
    }
})
