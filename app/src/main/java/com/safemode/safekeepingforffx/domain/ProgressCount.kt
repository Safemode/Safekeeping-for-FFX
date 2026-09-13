package com.safemode.safekeepingforffx.domain

/** How much of something is done: [found] out of [total]. Adds up across lists. */
data class ProgressCount(val found: Int, val total: Int) {
    operator fun plus(other: ProgressCount) = ProgressCount(found + other.found, total + other.total)
}
