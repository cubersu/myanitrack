package com.myanitrack.core.data.details

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class MalPersonPageTest {
    private fun fixture(name: String) = checkNotNull(javaClass.getResource("/people/$name.html")).readText()

    @Test fun `reads character biography without voice actor section`() {
        val person = parsePersonPage(fixture("character"), 11, true)
        assertEquals("Edward Elric", person.name)
        assertEquals("エドワード・エルリック", person.nameKanji)
        assertTrue(person.about!!.contains("Age: 15-16"))
        assertFalse(person.about!!.contains("Voice Actors"))
        assertFalse(person.about!!.contains("Exclude voice actors"))
        assertTrue(person.favorites > 0)
        assertTrue(person.images!!.large!!.startsWith("https://cdn.myanimelist.net/"))
    }

    @Test fun `reads staff biography from public page`() {
        val person = parsePersonPage(fixture("staff"), 1, false)
        assertTrue(person.name.contains("Seki"))
        assertTrue(person.about!!.contains("Hometown: Tokyo, Japan"))
        assertTrue(person.favorites > 0)
    }

    @Test fun `rejects challenge and unrelated pages instead of caching them`() {
        assertThrows(IllegalArgumentException::class.java) { parsePersonPage("<html>Checking your browser</html>", 11, true) }
        assertThrows(IllegalArgumentException::class.java) { parsePersonPage(fixture("staff"), 11, true) }
    }
}
