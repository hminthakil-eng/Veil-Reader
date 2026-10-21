package com.veilreader.app.ui.reader.search

import org.junit.Assert.assertEquals
import org.junit.Test

class PersianFtsQueryNormalizerTest {
    @Test fun normalizesArabicVariantsAndStripsSearchPunctuation() {
        assertEquals(
            "کتاب رازهای مه یک ازمون",
            PersianFtsQueryNormalizer.normalize("كتابِ رازهایِ مه؛ يک آزمون!")
        )
    }

    @Test fun keepsHamzaOnYaInPersianWords() {
        assertEquals("مسئولیت", PersianFtsQueryNormalizer.normalize("مسئولیت"))
    }

    @Test fun treatsFtsOperatorsAsQuotedTokens() {
        assertEquals(
            "\"رازهای\"* \"مه\"* \"OR\"* \"ستاره\"*",
            PersianFtsQueryNormalizer.toSafeFtsMatchQuery("رازهای: (مه) OR \"ستاره\"*")
        )
    }

    @Test fun emptyAndPunctuationOnlyQueriesDoNotProduceMatchOperators() {
        assertEquals("", PersianFtsQueryNormalizer.toSafeFtsMatchQuery(" -- : * "))
    }
}
