package de.easynfp.data

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** System clock that always uses the current default time zone, e.g. after travelling (F-10). */
object LiveClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)

    override fun instant(): Instant = Instant.now()
}
