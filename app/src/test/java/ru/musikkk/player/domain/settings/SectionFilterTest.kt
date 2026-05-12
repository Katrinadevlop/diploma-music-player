package ru.musikkk.player.domain.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionFilterTest {

    @Test
    fun `All пропускает любую секцию`() {
        for (raw in listOf("albums", "eps", "singles", "collabs", "garbage")) {
            assertTrue("$raw должно пройти через All", SectionFilter.All.matchesRaw(raw))
        }
    }

    @Test
    fun `остальные фильтры пропускают только свою секцию`() {
        val cases = listOf(
            SectionFilter.Albums to "albums",
            SectionFilter.Eps to "eps",
            SectionFilter.Singles to "singles",
            SectionFilter.Collabs to "collabs",
        )

        for ((filter, expectedRaw) in cases) {
            assertTrue("$filter должен принять $expectedRaw", filter.matchesRaw(expectedRaw))
            for ((_, otherRaw) in cases) {
                if (otherRaw == expectedRaw) continue
                assertFalse("$filter не должен принимать $otherRaw", filter.matchesRaw(otherRaw))
            }
        }
    }
}
