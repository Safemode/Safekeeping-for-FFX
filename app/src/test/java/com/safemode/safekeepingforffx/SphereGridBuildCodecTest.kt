package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.SphereGridBuild
import com.safemode.safekeepingforffx.data.reference.SphereGridBuildCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The build/route codec is what makes a shared route survive the trip through someone else's
 * clipboard, so it must round-trip every scope exactly - **including order**, which is what makes a
 * build a route - and refuse anything it can't safely apply. Contents are dropped leniently (a stray
 * entry never sinks a whole import), but a broken envelope fails loudly. Older v1 codes still decode.
 */
class SphereGridBuildCodecTest {

    private val edits = listOf(
        "n12" to NodeContent.Attribute(NodeType.STRENGTH, 4),
        "n30" to NodeContent.Ability("Firaga", NodeType.BLACK_MAGIC),
        "n7" to NodeContent.Empty
    )

    private fun roundTrip(build: SphereGridBuild): SphereGridBuild =
        SphereGridBuildCodec.decode(SphereGridBuildCodec.encode(build)).getOrThrow()

    @Test
    fun editsAndCurrentRoundTrips() {
        val build = SphereGridBuild(
            GridType.STANDARD,
            edits = edits,
            paths = mapOf(GridCharacter.TIDUS to listOf("n1", "n2", "n3")),
            name = "Tidus opener"
        )
        assertEquals(build, roundTrip(build))
    }

    @Test
    fun editsAndAllPathsRoundTrips() {
        val build = SphereGridBuild(
            GridType.STANDARD,
            edits = edits,
            paths = mapOf(
                GridCharacter.TIDUS to listOf("n1", "n2"),
                GridCharacter.YUNA to listOf("n40", "n41"),
                GridCharacter.LULU to listOf("n88")
            )
        )
        assertEquals(build, roundTrip(build))
    }

    @Test
    fun currentPathOnlyOmitsEdits() {
        val build = SphereGridBuild(
            GridType.STANDARD,
            edits = null,
            paths = mapOf(GridCharacter.AURON to listOf("n5", "n6"))
        )
        val decoded = roundTrip(build)
        assertNull("Path-only build must not carry edits", decoded.edits)
        assertEquals(build, decoded)
    }

    @Test
    fun editsOnlyOmitsPaths() {
        val build = SphereGridBuild(GridType.STANDARD, edits = edits, paths = null)
        val decoded = roundTrip(build)
        assertNull("Edits-only build must not carry paths", decoded.paths)
        assertEquals(build, decoded)
    }

    @Test
    fun orderIsPreservedNotJustMembership() {
        // A route is defined by order, so the decoded lists must match position for position.
        val forwards = SphereGridBuild(
            GridType.EXPERT,
            edits = null,
            paths = mapOf(GridCharacter.RIKKU to listOf("x1", "x2", "x3", "x4"))
        )
        val backwards = forwards.copy(
            paths = mapOf(GridCharacter.RIKKU to listOf("x4", "x3", "x2", "x1"))
        )
        assertEquals(listOf("x1", "x2", "x3", "x4"), roundTrip(forwards).paths?.get(GridCharacter.RIKKU))
        assertEquals(listOf("x4", "x3", "x2", "x1"), roundTrip(backwards).paths?.get(GridCharacter.RIKKU))
    }

    @Test
    fun garbageIsRejected() {
        assertTrue(SphereGridBuildCodec.decode("not a build code").isFailure)
        assertTrue(SphereGridBuildCodec.decode("").isFailure)
        assertTrue(SphereGridBuildCodec.decode("{}").isFailure)
    }

    @Test
    fun wrongVersionIsRejected() {
        val code = """{"v":999,"grid":"STANDARD","editList":[["n1","E"]]}"""
        assertTrue(SphereGridBuildCodec.decode(code).isFailure)
    }

    @Test
    fun unknownGridIsRejected() {
        val code = """{"v":2,"grid":"NONSENSE","editList":[["n1","E"]]}"""
        assertTrue(SphereGridBuildCodec.decode(code).isFailure)
    }

    @Test
    fun undecodableEntriesAreDroppedNotFatal() {
        // One good edit, one unparseable value; one known character, one unknown key.
        val code = """{"v":2,"grid":"STANDARD","name":"partly broken",""" +
            """"editList":[["n1","A|STRENGTH|2"],["n2","garbage"]],""" +
            """"paths":{"TIDUS":["n1"],"BOOGYMAN":["n9"]}}"""
        val build = SphereGridBuildCodec.decode(code).getOrThrow()
        assertEquals(listOf("n1" to NodeContent.Attribute(NodeType.STRENGTH, 2)), build.edits)
        assertEquals(mapOf(GridCharacter.TIDUS to listOf("n1")), build.paths)
        assertEquals("partly broken", build.name)
    }

    @Test
    fun legacyV1CodeStillDecodes() {
        // v1 carried an unordered edits map and no name; it must still import (order unknown).
        val code = """{"v":1,"grid":"STANDARD","edits":{"n1":"A|STRENGTH|2"},""" +
            """"paths":{"TIDUS":["n1","n2"]}}"""
        val build = SphereGridBuildCodec.decode(code).getOrThrow()
        assertEquals(listOf("n1" to NodeContent.Attribute(NodeType.STRENGTH, 2)), build.edits)
        assertEquals(mapOf(GridCharacter.TIDUS to listOf("n1", "n2")), build.paths)
        assertNull(build.name)
    }
}
