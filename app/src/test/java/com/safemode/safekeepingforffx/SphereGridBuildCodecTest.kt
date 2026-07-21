package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.GridType
import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType
import com.safemode.safekeepingforffx.data.reference.RouteEvent
import com.safemode.safekeepingforffx.data.reference.SphereGridBuild
import com.safemode.safekeepingforffx.data.reference.SphereGridBuildCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The build/route codec is what makes a shared route survive the trip through someone else's
 * clipboard, so it must round-trip the whole timeline exactly - **order and interleaving included**,
 * which is what makes a build a route - and refuse anything it can't safely apply. Individual events
 * are dropped leniently (a stray one never sinks a whole import), but a broken envelope fails loudly.
 * Older v1 codes still decode, folded into the timeline.
 */
class SphereGridBuildCodecTest {

    private fun roundTrip(build: SphereGridBuild): SphereGridBuild =
        SphereGridBuildCodec.decode(SphereGridBuildCodec.encode(build)).getOrThrow()

    @Test
    fun interleavedTimelineRoundTripsInOrder() {
        // An edit, an activation, another edit, another activation - the interleaving must survive.
        val build = SphereGridBuild(
            GridType.STANDARD,
            events = listOf(
                RouteEvent.Activate(GridCharacter.TIDUS, "n1"),
                RouteEvent.Edit("n7", NodeContent.Attribute(NodeType.MAGIC, 4)),
                RouteEvent.Activate(GridCharacter.TIDUS, "n7"),
                RouteEvent.Edit("n30", NodeContent.Ability("Firaga", NodeType.BLACK_MAGIC)),
                RouteEvent.Activate(GridCharacter.YUNA, "n40")
            ),
            name = "Tidus opener"
        )
        assertEquals(build, roundTrip(build))
    }

    @Test
    fun orderIsPreservedNotJustMembership() {
        val forwards = SphereGridBuild(
            GridType.EXPERT,
            events = listOf(
                RouteEvent.Activate(GridCharacter.RIKKU, "x1"),
                RouteEvent.Activate(GridCharacter.RIKKU, "x2"),
                RouteEvent.Activate(GridCharacter.RIKKU, "x3")
            )
        )
        val backwards = forwards.copy(events = forwards.events.reversed())
        assertEquals(forwards.events, roundTrip(forwards).events)
        assertEquals(backwards.events, roundTrip(backwards).events)
    }

    @Test
    fun garbageIsRejected() {
        assertTrue(SphereGridBuildCodec.decode("not a build code").isFailure)
        assertTrue(SphereGridBuildCodec.decode("").isFailure)
        assertTrue(SphereGridBuildCodec.decode("{}").isFailure)
    }

    @Test
    fun emptyTimelineIsRejected() {
        val code = """{"v":2,"grid":"STANDARD","events":[]}"""
        assertTrue(SphereGridBuildCodec.decode(code).isFailure)
    }

    @Test
    fun wrongVersionIsRejected() {
        val code = """{"v":999,"grid":"STANDARD","events":[["A","TIDUS","n1"]]}"""
        assertTrue(SphereGridBuildCodec.decode(code).isFailure)
    }

    @Test
    fun unknownGridIsRejected() {
        val code = """{"v":2,"grid":"NONSENSE","events":[["A","TIDUS","n1"]]}"""
        assertTrue(SphereGridBuildCodec.decode(code).isFailure)
    }

    @Test
    fun undecodableEventsAreDroppedNotFatal() {
        // A good edit, an unparseable edit, a good activation, and an unknown-character activation.
        val code = """{"v":2,"grid":"STANDARD","name":"partly broken","events":[""" +
            """["E","n1","A|STRENGTH|2"],["E","n2","garbage"],""" +
            """["A","TIDUS","n5"],["A","BOOGYMAN","n9"]]}"""
        val build = SphereGridBuildCodec.decode(code).getOrThrow()
        assertEquals(
            listOf(
                RouteEvent.Edit("n1", NodeContent.Attribute(NodeType.STRENGTH, 2)),
                RouteEvent.Activate(GridCharacter.TIDUS, "n5")
            ),
            build.events
        )
        assertEquals("partly broken", build.name)
    }

    @Test
    fun legacyV1CodeFoldsIntoTimeline() {
        // v1 had no timeline: its edits then its paths become events, order unknown but usable.
        val code = """{"v":1,"grid":"STANDARD","edits":{"n1":"A|STRENGTH|2"},""" +
            """"paths":{"TIDUS":["n1","n2"]}}"""
        val build = SphereGridBuildCodec.decode(code).getOrThrow()
        assertEquals(
            listOf(
                RouteEvent.Edit("n1", NodeContent.Attribute(NodeType.STRENGTH, 2)),
                RouteEvent.Activate(GridCharacter.TIDUS, "n1"),
                RouteEvent.Activate(GridCharacter.TIDUS, "n2")
            ),
            build.events
        )
    }
}
