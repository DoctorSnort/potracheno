package kz.chaykin.potracheno.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RuPluralTest {

    @Test
    fun `день, дня, дней — по русским правилам, а не по языку телефона`() {
        assertEquals("1 день", RuPlural.days(1))
        assertEquals("2 дня", RuPlural.days(2))
        assertEquals("4 дня", RuPlural.days(4))
        assertEquals("5 дней", RuPlural.days(5))
        assertEquals("7 дней", RuPlural.days(7))
        assertEquals("11 дней", RuPlural.days(11))
        assertEquals("14 дней", RuPlural.days(14))
        assertEquals("21 день", RuPlural.days(21))
        assertEquals("22 дня", RuPlural.days(22))
        assertEquals("111 дней", RuPlural.days(111))
        assertEquals("0 дней", RuPlural.days(0))
    }
}
