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
 * The build codec is what makes a shared build survive the trip through someone else's clipboard, so
 * it must round-trip every scope exactly and refuse anything it can't safely apply. Contents are
 * dropped leniently (a stray entry never sinks a whole import), but a broken envelope fails loudly.
 */
class SphereGridBuildCodecTest {

    private val edits = mapOf(
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
            paths = mapOf(GridCharacter.TIDUS to setOf("n1", "n2", "n3"))
        )
        assertEquals(build, roundTrip(build))
    }

    @Test
    fun editsAndAllPathsRoundTrips() {
        val build = SphereGridBuild(
            GridType.STANDARD,
            edits = edits,
            paths = mapOf(
                GridCharacter.TIDUS to setOf("n1", "n2"),
                GridCharacter.YUNA to setOf("n40", "n41"),
                GridCharacter.LULU to setOf("n88")
            )
        )
        assertEquals(build, roundTrip(build))
    }

    @Test
    fun currentPathOnlyOmitsEdits() {
        val build = SphereGridBuild(
            GridType.STANDARD,
            edits = null,
            paths = mapOf(GridCharacter.AURON to setOf("n5", "n6"))
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
    fun garbageIsRejected() {
        assertTrue(SphereGridBuildCodec.decode("not a build code").isFailure)
        assertTrue(SphereGridBuildCodec.decode("").isFailure)
        assertTrue(SphereGridBuildCodec.decode("{}").isFailure)
    }

    @Test
    fun wrongVersionIsRejected() {
        val code = """{"v":999,"grid":"STANDARD","edits":{"n1":"E"}}"""
        assertTrue(SphereGridBuildCodec.decode(code).isFailure)
    }

    @Test
    fun unknownGridIsRejected() {
        val code = """{"v":1,"grid":"NONSENSE","edits":{"n1":"E"}}"""
        assertTrue(SphereGridBuildCodec.decode(code).isFailure)
    }

    @Test
    fun undecodableEntriesAreDroppedNotFatal() {
        // One good edit, one unparseable value; one known character, one unknown key.
        val code = """{"v":1,"grid":"STANDARD","edits":{"n1":"A|STRENGTH|2","n2":"garbage"},""" +
            """"paths":{"TIDUS":["n1"],"BOOGYMAN":["n9"]}}"""
        val build = SphereGridBuildCodec.decode(code).getOrThrow()
        assertEquals(mapOf("n1" to NodeContent.Attribute(NodeType.STRENGTH, 2)), build.edits)
        assertEquals(mapOf(GridCharacter.TIDUS to setOf("n1")), build.paths)
    }
}
