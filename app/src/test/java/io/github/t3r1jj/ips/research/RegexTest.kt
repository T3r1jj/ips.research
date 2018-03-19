package io.github.t3r1jj.ips.research

import org.junit.Assert.*
import org.junit.Test

class RegexTest {
    @Test
    fun oneOfWords() {
        val regex = Regex("(eduroam|dziekanat|pb_guest)", RegexOption.IGNORE_CASE)
        assertTrue(regex.matches("eduroam"))
        assertTrue(regex.matches("dziekanat"))
        assertTrue(regex.matches("Dziekanat"))
    }
    @Test
    fun oneOfWords2() {
        val regex = Regex("(eduroam|dziekanat|pb-guest|.*hotspot.*)", RegexOption.IGNORE_CASE)
        assertTrue(regex.matches("eduroam"))
        assertTrue(regex.matches("dziekanat"))
        assertTrue(regex.matches("Dziekanat"))
        assertTrue(regex.matches("PB-GUEST"))
        assertTrue(regex.matches("www.hotspot.eu"))
        assertTrue(regex.matches("www.fhotspot.eu"))
    }
}