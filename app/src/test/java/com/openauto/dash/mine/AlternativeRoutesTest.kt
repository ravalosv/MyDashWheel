package com.openauto.dash.mine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternativeRoutesTest {
    @Test
    fun `the asked number of alternatives goes into the request`() {
        val out = AlternativeRoutes.body("""{"costing":"auto","locations":[]}""", enabled = true)
        assertTrue(out.contains("\"alternates\":${AlternativeRoutes.WANTED}"))
    }

    @Test
    fun `switched off, the request is left exactly as it was`() {
        val json = """{"costing":"auto","locations":[]}"""
        assertEquals(json, AlternativeRoutes.body(json, enabled = false))
    }

    @Test
    fun `a request that already asks is not asked twice`() {
        val json = """{"costing":"auto","alternates":1}"""
        val out = AlternativeRoutes.body(json, enabled = true)
        assertEquals(1, Regex("\"alternates\"").findAll(out).count())
    }

    @Test
    fun `nothing to ask for leaves the request untouched`() {
        assertEquals("not json", AlternativeRoutes.body("not json", enabled = true))
    }

    @Test
    fun `without a choice the server's first route is followed`() {
        assertEquals(0, AlternativeRoutes.indexOf(null, listOf("a", "b", "c")))
    }

    @Test
    fun `the tapped route is the one followed, wherever it sits`() {
        assertEquals(2, AlternativeRoutes.indexOf("c", listOf("a", "b", "c")))
    }

    @Test
    fun `a choice that is no longer offered falls back to the first`() {
        assertEquals(0, AlternativeRoutes.indexOf("gone", listOf("a", "b")))
    }

    @Test
    fun `no routes at all has nothing to follow`() {
        assertEquals(-1, AlternativeRoutes.indexOf("a", emptyList()))
    }

    @Test
    fun `a chosen route is remembered and forgotten`() {
        AlternativeRoutes.chosen("geom")
        assertEquals("geom", AlternativeRoutes.chosenGeometry())
        AlternativeRoutes.forget()
        assertEquals(null, AlternativeRoutes.chosenGeometry())
    }
}
