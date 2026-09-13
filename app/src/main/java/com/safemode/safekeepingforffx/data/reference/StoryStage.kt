package com.safemode.safekeepingforffx.data.reference

/**
 * Points in the story, declared in the order you reach them.
 *
 * This is what lets a list be sorted by *when* rather than by what things belong to. Declaration
 * order is the whole contract - entries sort by it - so reordering these constants reorders every
 * list that uses them.
 *
 * A stage marks the earliest point an item is reachable, not the point you would sensibly go and
 * get it. Several of the endgame rewards are technically available long before a story-level party
 * can survive the fight or the side quest attached to them.
 */
enum class StoryStage(val label: String) {
    BESAID("Besaid"),
    LUCA("Luca"),
    MIIHEN_HIGHROAD("Mi'ihen Highroad"),
    MUSHROOM_ROCK_ROAD("Mushroom Rock Road"),
    GUADOSALAM("Guadosalam"),
    THUNDER_PLAINS("Thunder Plains"),
    MACALANIA_WOODS("Macalania Woods"),
    // The lake and its temple, after the woods. Kept apart from the woods because the temple's
    // Cloister is here, and so is Rin's agency.
    LAKE_MACALANIA("Lake Macalania"),
    BIKANEL_ISLAND("Bikanel Island"),
    // Reached across the Sanubia Desert and destroyed before you leave, so it gets its own stage
    // rather than folding into Bikanel Island - what is here is gone once the airship lifts off.
    HOME("Home"),
    // The wedding and the Priest's Passage. There is no coming back once the trial is over.
    BEVELLE("Bevelle"),
    CALM_LANDS("Calm Lands"),
    MT_GAGAZET("Mt. Gagazet"),
    ZANARKAND_RUINS("Zanarkand Ruins"),
    AIRSHIP("After the airship")
}
