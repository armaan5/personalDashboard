package dev.armaan.pd.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** SPEC §4.1: a day is the local calendar date in the phone's current zone (D13). */
fun today(now: Instant, zone: ZoneId): LocalDate = now.atZone(zone).toLocalDate()
