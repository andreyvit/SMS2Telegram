package com.tigerworkshop.sms2telegram.sms
import org.junit.Assert.*
import org.junit.Test
class MessageChunksTest {
    @Test fun preservesLongUnicodeMessages() {
        val text = "x".repeat(3899) + "😀" + "Привет ".repeat(1500)
        val chunks = messageChunks(text)
        assertEquals(text, chunks.joinToString(""))
        assertTrue(chunks.all { it.length <= 3900 })
        assertTrue(chunks.none { it.lastOrNull()?.isHighSurrogate() == true })
        assertTrue(chunks.none { it.firstOrNull()?.isLowSurrogate() == true })
    }
    @Test fun boundariesAndEmpty() {
        for (size in listOf(0,1,3899,3900,3901,7800,7801)) {
            val text = "a".repeat(size)
            assertEquals(text, messageChunks(text).joinToString(""))
        }
    }
}
