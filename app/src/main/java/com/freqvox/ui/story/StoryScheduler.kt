package com.freqvox.ui.story

import com.freqvox.data.prefs.StoryPreferences
import com.freqvox.ui.main.MainViewModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the active story drop by COMPOSING everything currently due into one session —
 * every uncelebrated milestone, the weekly recap when its Monday arrives, and the current
 * tip — as segments of a single multi-card story (the way a real status works).
 *
 * The old design surfaced one candidate at a time behind a strict priority queue, which had
 * two complaints: content felt distanced (each item waited out its own cooldown) and a drop
 * only ever carried one thing. Composing solves both: an eventful day yields one RICH drop,
 * not a chain of relights, so the ring can afford to be responsive:
 *  - drops containing a fresh milestone light the ring IMMEDIATELY (events are worth it);
 *  - routine content (tips/recap) still respects a short post-view cooldown so the ring
 *    isn't chatty — steady state is a drop every 1–2 days, never several per day.
 */
@Singleton
class StoryScheduler @Inject constructor(
    private val prefs: StoryPreferences
) {
    fun activeDrop(
        stats: MainViewModel.ListeningStats,
        contextualTips: List<StorySegment>,
        epochDay: Long
    ): StoryDrop {
        val segments = mutableListOf<StorySegment>()
        // The drop id is the joined component ids — new content changes the id, which is what
        // relights the ring; markSeen parses the same components to retire each piece.
        val parts = mutableListOf<String>()

        // Milestones — ALL uncelebrated ones ride along, each as its own card.
        val celebrated = prefs.celebratedMilestones()
        StoryContent.metMilestones(stats).filter { it.key !in celebrated }.forEach { def ->
            segments += StoryContent.milestoneSegment(def)
            parts += "m:${def.key}"
        }

        // Weekly recap (Monday-anchored), until it's been seen for this week. Gated on
        // lastRecapWeek — NOT lastSeenDropId — so a later tip view can't resurrect it.
        val dow = java.time.LocalDate.ofEpochDay(epochDay).dayOfWeek.value  // 1=Mon … 7=Sun
        val mondayKey = epochDay - (dow - 1)
        var hasRecap = false
        if (stats.totalPlays > 0 && prefs.lastRecapWeek != mondayKey) {
            segments += StoryContent.weeklyRecap(stats, mondayKey, epochDay).segments
            parts += "r:$mondayKey"
            hasRecap = true
        }

        // A tip always closes the session: the evergreen pool PLUS any contextual (data-aware)
        // tips that currently apply. Chosen at RANDOM per period, avoiding recently-shown ones,
        // then persisted so it's stable for the whole period and different per install.
        val period = Math.floorDiv(epochDay, TIP_INTERVAL_DAYS)
        val pool = StoryContent.educationalPool + contextualTips
        val byId = pool.associateBy { it.id }
        val needNew = prefs.currentTipPeriod != period || prefs.currentTipId == null || prefs.currentTipId !in byId
        val tipId = if (needNew) {
            val recent = prefs.recentTips().toSet()
            val pick = pool.map { it.id }.filter { it !in recent }.ifEmpty { pool.map { it.id } }.random()
            prefs.currentTipId = pick
            prefs.currentTipPeriod = period
            // Don't repeat until every currently-eligible tip (evergreen + contextual) has
            // been shown once. Was capped at half the evergreen pool, so repeats started
            // well before the full rotation had played — the "tips feel finite" complaint.
            // pool.size - 1 keeps at least one always-eligible candidate, guaranteeing a
            // full cycle before any tip can come back around.
            prefs.pushRecentTip(pick, cap = (pool.size - 1).coerceAtLeast(1))
            pick
        } else prefs.currentTipId!!
        segments += byId.getValue(tipId)
        parts += "t:$period:$tipId"

        val kind = when {
            parts.any { it.startsWith("m:") } -> SegmentKind.MILESTONE
            hasRecap -> SegmentKind.RECAP
            else -> SegmentKind.TIP
        }
        return StoryDrop(parts.joinToString("|"), epochDay, kind, segments)
    }

    // ── Per-card view state (WhatsApp-style) ────────────────────────────────────
    // Seen-ness is tracked per CARD, not per drop: the ring segments per card and greys
    // out only what was actually displayed, re-opening resumes at the first unviewed
    // card, and the cooldown starts only once the whole drop has been consumed.
    // Persistence is keyed by the card's drop-id COMPONENT (m:/r:/t: part), so a watched
    // card stays watched when an unrelated component changes the composed drop id — the
    // old full-drop-id scoping wiped everything on any shift (daily tip roll, a milestone
    // retiring mid-view, the recap arriving) and relit the ring with old cards.

    private fun parts(drop: StoryDrop): List<String> =
        drop.id.split('|').filter { it.isNotEmpty() }

    /** The drop-id component a card belongs to. Milestone ids self-describe ("m-<key>");
     *  recap cards belong to the week part, everything else is the closing tip. */
    private fun partFor(drop: StoryDrop, segment: StorySegment): String = when {
        segment.kind == SegmentKind.MILESTONE -> "m:" + segment.id.removePrefix("m-")
        segment.kind == SegmentKind.RECAP -> parts(drop).firstOrNull { it.startsWith("r:") } ?: drop.id
        else -> parts(drop).firstOrNull { it.startsWith("t:") } ?: drop.id
    }

    private fun cardKey(drop: StoryDrop, segment: StorySegment): String =
        partFor(drop, segment) + StoryPreferences.CARD_KEY_SEP + segment.id

    /** One flag per segment (drop order): true = that card has been displayed. */
    fun seenFlags(drop: StoryDrop): List<Boolean> {
        val seen = prefs.seenCardKeys()
        return drop.segments.map { cardKey(drop, it) in seen }
    }

    /** Where the viewer should resume: the first unviewed card (0 if all seen — replay). */
    fun firstUnseenIndex(drop: StoryDrop): Int =
        seenFlags(drop).indexOfFirst { !it }.coerceAtLeast(0)

    fun isUnseen(drop: StoryDrop): Boolean {
        val seen = prefs.seenCardKeys()
        val unseen = drop.segments.filter { cardKey(drop, it) !in seen }
        if (unseen.isEmpty()) return false
        // Event-driven content bypasses the cooldown — an achievement just earned should
        // light the ring now, and composition guarantees it arrives bundled (no chaining).
        if (unseen.any { it.kind == SegmentKind.MILESTONE }) return true
        return java.time.LocalDate.now().toEpochDay() >= prefs.nextDropEarliestDay
    }

    /**
     * Called by the viewer as each card is DISPLAYED. Retirement is piece-wise, so a
     * partial view spends only what was actually watched:
     *  - a displayed milestone card is celebrated immediately (never re-surfaces);
     *  - the weekly recap retires once ALL its cards have been displayed;
     *  - the post-view cooldown starts only when the WHOLE drop has been consumed
     *    (bailing early leaves the remaining cards lit — no silent spending).
     */
    fun markCardSeen(drop: StoryDrop, segment: StorySegment) {
        val seen = prefs.markCardSeen(cardKey(drop, segment), parts(drop))

        // Milestone cards carry ids of the form "m-<key>" (see StoryContent.milestoneSegment).
        if (segment.kind == SegmentKind.MILESTONE && segment.id.startsWith("m-")) {
            prefs.markMilestoneCelebrated(segment.id.removePrefix("m-"))
        }

        // Recap retires for its week once every recap-kind card has been seen — so a
        // recomposed drop tomorrow (new tip) won't resurrect cards already watched.
        val recapCards = drop.segments.filter { it.kind == SegmentKind.RECAP }
        if (recapCards.isNotEmpty() && recapCards.all { cardKey(drop, it) in seen }) {
            drop.id.split('|').firstOrNull { it.startsWith("r:") }
                ?.removePrefix("r:")?.toLongOrNull()
                ?.let { prefs.lastRecapWeek = it }
        }

        // Full consumption → cooldown. Guarded by lastSeenDropId so replaying a finished
        // drop can't keep pushing the next one out. Weighted, not uniform: mostly a 1-day
        // gap (daily feel), occasionally 2 (~1 in 5) to break the monotony.
        if (drop.segments.all { cardKey(drop, it) in seen } && prefs.lastSeenDropId != drop.id) {
            val gap = if ((1..5).random() == 1) 2L else 1L
            prefs.nextDropEarliestDay = java.time.LocalDate.now().toEpochDay() + gap
            prefs.lastSeenDropId = drop.id
        }
    }

    companion object {
        /** The rotating tip refreshes DAILY — the drop id changes every day, so once the
         *  post-view cooldown (1 day, occasionally 2) lapses there is always fresh content
         *  to relight the ring. With a 2-day tip period the effective cadence was locked to
         *  every-2-days even when the cooldown had already expired. */
        private const val TIP_INTERVAL_DAYS = 1L
    }
}
