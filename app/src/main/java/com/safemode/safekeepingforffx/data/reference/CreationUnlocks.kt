package com.safemode.safekeepingforffx.data.reference

/**
 * Column headers for the fiend CSV that carry meaning beyond a plain detail row. The generic parser
 * keeps every column keyed by its header (see [Monster.details]); naming the ones the unlock logic
 * and the inline UI care about keeps those readers from scattering string literals around.
 */
object MonsterColumns {
    const val MONSTER_TYPE = "Monster Type"
    const val CREATION_UNLOCK = "Creation Unlock"
    const val CREATION_UNLOCK_AMOUNT = "Creation Unlock Amount Needed"
    const val UNLOCK_CONDITION = "Unlock Condition"
    const val UNLOCK_REWARD = "Unlock Reward"
}

/** Which family a creation belongs to, which is what decides how it is unlocked. */
enum class CreationKind { AREA, SPECIES, ORIGINAL }

/**
 * A creation's live unlock state, worked out from the current capture counts.
 *
 * [current] of [required] is the coarse progress the note shows ("5 / 7 done"); [unlocked] is true
 * once the whole condition is met. [requirement] is the sentence straight from the CSV and [reward]
 * the payout, both surfaced on the row so the player never has to expand it to see what they are
 * working towards.
 */
data class CreationProgress(
    val unlocked: Boolean,
    val current: Int,
    val required: Int,
    val requirement: String,
    val reward: String
)

/** The three creation groups, matched against the area column that files them. */
fun Monster.creationKind(): CreationKind? = when {
    area.equals("Area Creations", ignoreCase = true) -> CreationKind.AREA
    area.equals("Species Creations", ignoreCase = true) -> CreationKind.SPECIES
    area.equals("Original Creations", ignoreCase = true) -> CreationKind.ORIGINAL
    else -> null
}

/** Blank cells are dropped at parse time; "N/A" survives as text, so it is filtered out here. */
private fun String?.meaningful(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() && !it.equals("N/A", ignoreCase = true) }

/** The fiend's species, shown as subtext under its name. Null for creations, which have none. */
val Monster.monsterType: String? get() = details[MonsterColumns.MONSTER_TYPE].meaningful()

/** The species creation this fiend feeds, e.g. a Lupine feeds Fenrir. Null when it feeds nothing. */
val Monster.feedsCreation: String? get() = details[MonsterColumns.CREATION_UNLOCK].meaningful()

/** How many of this fiend the creation it feeds needs. Null when the count is unspecified. */
val Monster.feedAmount: Int? get() = details[MonsterColumns.CREATION_UNLOCK_AMOUNT].meaningful()?.toIntOrNull()

/** The plain-language sentence describing what unlocks a creation. Null for capturable fiends. */
val Monster.unlockCondition: String? get() = details[MonsterColumns.UNLOCK_CONDITION].meaningful()

/** What the creation pays out once unlocked. Null for capturable fiends. */
val Monster.unlockReward: String? get() = details[MonsterColumns.UNLOCK_REWARD].meaningful()

/**
 * Fold away spacing and punctuation so a fiend's stated target matches the creation's name even
 * when the CSV spells them differently - a fiend feeds "One Eye" and "Iron Clad" while the creation
 * rows are named "One-Eye" and "Ironclad".
 */
private fun normalize(value: String): String = value.lowercase().filter { it.isLetterOrDigit() }

/** Area Creations name the area in prose that does not always match the area column verbatim. */
private val AREA_ALIASES = mapOf(
    "djose highroad" to "djose",
    "mount gagazet" to "mt. gagazet",
    "sin" to "inside sin",
    "omega dungeon" to "omega ruins"
)

private val FROM_AREA = Regex("""from\s+(.+?)\s*$""", RegexOption.IGNORE_CASE)
private val AREA_CONQUEST = Regex("""(\d+)\s+Area Conquest""", RegexOption.IGNORE_CASE)
private val SPECIES_CONQUEST = Regex("""(\d+)\s+Species Conquest""", RegexOption.IGNORE_CASE)
private val EVERY_FIEND = Regex("""(\d+)\s+of (?:every|each) fiend""", RegexOption.IGNORE_CASE)
private val FIRST_NUMBER = Regex("""\d+""")

private fun resolveArea(phrase: String, areaLabels: Set<String>): String? {
    val cleaned = phrase.trim().lowercase()
    val target = AREA_ALIASES[cleaned] ?: cleaned
    return areaLabels.firstOrNull { it.lowercase() == target }
}

/**
 * Works out the unlock state of every creation from the current [counts], keyed by the creation's
 * monster id.
 *
 * The three groups are resolved in order because Original Creations lean on the others: two of them
 * need a number of Area or Species creations already unlocked, so those are computed first and their
 * totals fed in.
 *
 * - Area Creations want one of every fiend caught in a named area.
 * - Species Creations want a set number of every fiend that feeds them.
 * - Original Creations are mixed: a count of every fiend, of the underwater fiends only, or a tally
 *   of Area / Species creations already conquered.
 */
fun computeCreationProgress(
    monsters: List<Monster>,
    counts: Map<String, Int>
): Map<String, CreationProgress> {
    val capturable = monsters.filter { it.isCapturable }
    val areaLabels = capturable.map { it.area }.toSet()
    fun countOf(monster: Monster): Int = counts[monster.id] ?: 0

    val result = LinkedHashMap<String, CreationProgress>()

    val areaCreations = monsters.filter { it.creationKind() == CreationKind.AREA }
    for (creation in areaCreations) {
        val condition = creation.unlockCondition.orEmpty()
        val areaName = FROM_AREA.find(condition)?.groupValues?.get(1)
            ?.let { resolveArea(it, areaLabels) }
        val feeders = if (areaName != null) capturable.filter { it.area == areaName } else emptyList()
        val required = feeders.size
        val current = feeders.count { countOf(it) >= 1 }
        result[creation.id] = CreationProgress(
            unlocked = required > 0 && current >= required,
            current = current,
            required = required,
            requirement = condition,
            reward = creation.unlockReward.orEmpty()
        )
    }

    val speciesCreations = monsters.filter { it.creationKind() == CreationKind.SPECIES }
    for (creation in speciesCreations) {
        val target = normalize(creation.name)
        val feeders = capturable.filter { it.feedsCreation?.let { f -> normalize(f) == target } == true }
        val required = feeders.size
        val current = feeders.count { countOf(it) >= (it.feedAmount ?: 1) }
        result[creation.id] = CreationProgress(
            unlocked = required > 0 && current >= required,
            current = current,
            required = required,
            requirement = creation.unlockCondition.orEmpty(),
            reward = creation.unlockReward.orEmpty()
        )
    }

    val unlockedAreas = areaCreations.count { result[it.id]?.unlocked == true }
    val unlockedSpecies = speciesCreations.count { result[it.id]?.unlocked == true }

    for (creation in monsters.filter { it.creationKind() == CreationKind.ORIGINAL }) {
        val condition = creation.unlockCondition.orEmpty()
        val progress = evaluateOriginal(
            condition = condition,
            capturable = capturable,
            countOf = ::countOf,
            unlockedAreas = unlockedAreas,
            unlockedSpecies = unlockedSpecies
        )
        result[creation.id] = CreationProgress(
            unlocked = progress.third,
            current = progress.first,
            required = progress.second,
            requirement = condition,
            reward = creation.unlockReward.orEmpty()
        )
    }

    return result
}

/** Returns current, required, unlocked for one Original Creation's condition. */
private fun evaluateOriginal(
    condition: String,
    capturable: List<Monster>,
    countOf: (Monster) -> Int,
    unlockedAreas: Int,
    unlockedSpecies: Int
): Triple<Int, Int, Boolean> {
    AREA_CONQUEST.find(condition)?.let {
        val needed = it.groupValues[1].toInt()
        return Triple(unlockedAreas.coerceAtMost(needed), needed, unlockedAreas >= needed)
    }
    SPECIES_CONQUEST.find(condition)?.let {
        val needed = it.groupValues[1].toInt()
        return Triple(unlockedSpecies.coerceAtMost(needed), needed, unlockedSpecies >= needed)
    }
    if (condition.contains("underwater", ignoreCase = true)) {
        val needed = FIRST_NUMBER.find(condition)?.value?.toIntOrNull() ?: 1
        val feeders = capturable.filter { it.feedsCreation?.let { f -> normalize(f) == "shinryu" } == true }
        val current = feeders.count { countOf(it) >= needed }
        return Triple(current, feeders.size, feeders.isNotEmpty() && current >= feeders.size)
    }
    EVERY_FIEND.find(condition)?.let {
        val needed = it.groupValues[1].toInt()
        val current = capturable.count { countOf(it) >= needed }
        return Triple(current, capturable.size, capturable.isNotEmpty() && current >= capturable.size)
    }
    // An unrecognised condition stays locked rather than guessing it open.
    return Triple(0, 0, false)
}

/**
 * The fiends, and the count each needs, that would satisfy a creation's condition by capture alone,
 * keyed by fiend id.
 *
 * Empty when the creation is not won by capturing: the conquest-gated Original Creations (Earth
 * Eater, Greater Sphere, Catastrophe, Th'uban), whose "N Area / Species Conquest" conditions turn
 * on other creations rather than fiends, and any condition we do not recognise. That emptiness is
 * exactly what leaves those creations out of the long-press auto-capture.
 */
fun creationCaptureTargets(creation: Monster, monsters: List<Monster>): Map<String, Int> {
    val capturable = monsters.filter { it.isCapturable }
    return when (creation.creationKind()) {
        CreationKind.AREA -> {
            val condition = creation.unlockCondition.orEmpty()
            val areaName = FROM_AREA.find(condition)?.groupValues?.get(1)
                ?.let { resolveArea(it, capturable.map { monster -> monster.area }.toSet()) }
            if (areaName == null) {
                emptyMap()
            } else {
                capturable.filter { it.area == areaName }.associate { it.id to 1 }
            }
        }

        CreationKind.SPECIES -> {
            val target = normalize(creation.name)
            capturable
                .filter { it.feedsCreation?.let { feed -> normalize(feed) == target } == true }
                .associate { it.id to (it.feedAmount ?: 1) }
        }

        CreationKind.ORIGINAL ->
            originalCaptureTargets(creation.unlockCondition.orEmpty(), capturable)

        null -> emptyMap()
    }
}

/** The capture side of [evaluateOriginal] - the same rules, expressed as fiends to raise. */
private fun originalCaptureTargets(
    condition: String,
    capturable: List<Monster>
): Map<String, Int> {
    // Conquest-gated originals turn on other creations, not fiends: nothing to capture.
    if (AREA_CONQUEST.containsMatchIn(condition) || SPECIES_CONQUEST.containsMatchIn(condition)) {
        return emptyMap()
    }
    if (condition.contains("underwater", ignoreCase = true)) {
        val needed = FIRST_NUMBER.find(condition)?.value?.toIntOrNull() ?: 1
        return capturable
            .filter { it.feedsCreation?.let { feed -> normalize(feed) == "shinryu" } == true }
            .associate { it.id to needed }
    }
    EVERY_FIEND.find(condition)?.let {
        val needed = it.groupValues[1].toInt()
        return capturable.associate { monster -> monster.id to needed }
    }
    return emptyMap()
}
