package com.freqvox.ui.story

import com.freqvox.ui.main.MainViewModel

/**
 * The library of "curated" segments — feature tips (with deep-link CTAs), algorithm explainers
 * that make the dynamic, tag- and habit-driven feed legible, and highlights drawn from the user's
 * own stats. A single one is chosen per drop period so it stays stable for a couple of days.
 */
object StoryContent {

    /** A tip/explainer/stat drop for the given period (changes every few days via [periodIndex]). */
    fun tipDrop(stats: MainViewModel.ListeningStats, periodIndex: Long, epochDay: Long): StoryDrop {
        val pool = segments(stats)
        val i = ((periodIndex % pool.size).toInt() + pool.size) % pool.size
        val seg = pool[i]
        return StoryDrop(id = "tip-$periodIndex", createdDay = epochDay, kind = seg.kind, segments = listOf(seg))
    }

    /** QA-only: a single drop containing one of every card + visual type (synthetic data where the
     *  library is sparse), for eyeballing designs on demand without waiting on the scheduler. */
    fun previewDrop(stats: MainViewModel.ListeningStats, epochDay: Long): StoryDrop {
        val segs = mutableListOf<StorySegment>()
        segs += StorySegment("pv-intro", SegmentKind.RECAP, "Story preview",
            "Every card type, for design QA — this only appears via long-press.", SegmentVisual.Illustration(IllustrationKind.STATS))

        // All evergreen tips/explainers + whatever stat cards the real data supports.
        segs += segments(stats)

        // Force-cover the data visuals with real values or synthetic samples.
        val spark = stats.weekDailyMinutes.ifEmpty { listOf(0.2f, 0.6f, 0.4f, 0.9f, 0.5f, 0.75f, 0.35f) }
        val days = stats.activeDays14.ifEmpty { List(14) { it % 3 != 0 } }
        val bars = stats.topArtists.ifEmpty { listOf("Artist A" to 12, "Artist B" to 9, "Artist C" to 5) }
        segs += StorySegment("pv-spark", SegmentKind.STAT, "Sparkline", "Weekly minutes.", SegmentVisual.Sparkline(spark, "sample"))
        segs += StorySegment("pv-streak", SegmentKind.STAT, "Streak calendar", "Active-day dots.", SegmentVisual.StreakCalendar(days))
        segs += StorySegment("pv-hist", SegmentKind.STAT, "Hour histogram", "Listening by hour.", SegmentVisual.Histogram(List(24) { (it * 7) % 13 }))
        segs += StorySegment("pv-bars", SegmentKind.STAT, "Bars", "Top artists.", SegmentVisual.Bars(bars))
        stats.topArtistTrack?.let { segs += StorySegment("pv-art", SegmentKind.STAT, "Album art", it.title, SegmentVisual.AlbumArt(it)) }
        if (stats.newThisWeek.isNotEmpty()) segs += StorySegment("pv-collage", SegmentKind.STAT, "Collage", "New this week.", SegmentVisual.ArtCollage(stats.newThisWeek))

        // Every illustration kind.
        IllustrationKind.values().forEach { k ->
            segs += StorySegment("pv-ill-$k", SegmentKind.ALGORITHM, "Illustration · $k", "Illustration sample.", SegmentVisual.Illustration(k))
        }

        // The full recap + a milestone sample.
        segs += weeklyRecap(stats, 0, epochDay).segments
        val ms = metMilestones(stats).firstOrNull() ?: MilestoneDef("streak7-sample", "🔥", "7-day streak!", "Sample milestone card.")
        segs += milestoneDrop(ms, epochDay).segments

        return StoryDrop("preview-$epochDay", epochDay, SegmentKind.TIP, segs)
    }

    data class MilestoneDef(val key: String, val emoji: String, val title: String, val body: String)

    /** Achievements the user currently qualifies for, highest first. The scheduler shows the first
     *  un-celebrated one; viewing it marks it celebrated so it fires only once. */
    fun metMilestones(stats: MainViewModel.ListeningStats): List<MilestoneDef> {
        val out = mutableListOf<MilestoneDef>()
        when {
            stats.currentStreakDays >= 30 -> out += MilestoneDef("streak30", "🏆", "30-day streak!", "A whole month of daily listening. Legendary.")
            stats.currentStreakDays >= 7 -> out += MilestoneDef("streak7", "🔥", "7-day streak!", "A full week, every single day. You're on fire.")
        }
        when {
            stats.totalPlays >= 1000 -> out += MilestoneDef("plays1000", "💎", "1,000 plays", "A thousand songs played in Freqvox. That's dedication.")
            stats.totalPlays >= 500 -> out += MilestoneDef("plays500", "🌟", "500 plays", "Halfway to a thousand — keep the music going.")
        }
        stats.topTracks.firstOrNull()?.let { (t, c) ->
            if (c >= 100) out += MilestoneDef("track100-${t.id}", "💯", "100 plays of one song", "\"${t.title}\" is a certified favourite.")
        }
        return out
    }

    fun milestoneDrop(def: MilestoneDef, epochDay: Long): StoryDrop = StoryDrop(
        id = "milestone-${def.key}",
        createdDay = epochDay,
        kind = SegmentKind.MILESTONE,
        segments = listOf(
            StorySegment("m-${def.key}", SegmentKind.MILESTONE, def.title, def.body, SegmentVisual.Emoji(def.emoji))
        )
    )

    /** A multi-segment "Your week in Freqvox" recap. Segments are included only when they have data. */
    fun weeklyRecap(stats: MainViewModel.ListeningStats, weekKey: Long, epochDay: Long): StoryDrop {
        val segs = mutableListOf<StorySegment>()
        segs += StorySegment("recap-intro", SegmentKind.RECAP, "Your week in Freqvox",
            "Here's what your week sounded like.", SegmentVisual.Illustration(IllustrationKind.STATS))
        if (stats.weekMinutes > 0 && stats.weekDailyMinutes.isNotEmpty())
            segs += StorySegment("recap-minutes", SegmentKind.RECAP, "${stats.weekMinutes} minutes",
                "Your listening, day by day.", SegmentVisual.Sparkline(stats.weekDailyMinutes, "last 7 days"))
        stats.topArtistTrack?.let { t ->
            segs += StorySegment("recap-artist", SegmentKind.RECAP, "Top artist: ${t.artist}",
                "On heavy rotation this week.", SegmentVisual.AlbumArt(t))
        }
        stats.topTracks.firstOrNull()?.let { (t, c) ->
            segs += StorySegment("recap-track", SegmentKind.RECAP, "On repeat",
                "${t.title} — $c play${if (c == 1) "" else "s"}, your most-played.", SegmentVisual.AlbumArt(t))
        }
        if (stats.newThisWeek.isNotEmpty())
            segs += StorySegment("recap-new", SegmentKind.RECAP, "${stats.newThisWeek.size} new this week",
                "Fresh additions to your library.", SegmentVisual.ArtCollage(stats.newThisWeek))
        if (stats.currentStreakDays >= 2)
            segs += StorySegment("recap-streak", SegmentKind.RECAP, "${stats.currentStreakDays}-day streak",
                "Consistency pays off.", SegmentVisual.StreakCalendar(stats.activeDays14))
        return StoryDrop("recap-$weekKey", epochDay, SegmentKind.RECAP, segs)
    }

    private fun segments(stats: MainViewModel.ListeningStats): List<StorySegment> {
        val list = mutableListOf<StorySegment>()

        // ── Highlights from the user's own listening (real graphics) ──────────
        if (stats.currentStreakDays >= 2 && stats.activeDays14.isNotEmpty())
            list += StorySegment("stat-streak", SegmentKind.STAT, "${stats.currentStreakDays}-day streak",
                "You've pressed play ${stats.currentStreakDays} days running. Keep the fire lit.",
                SegmentVisual.StreakCalendar(stats.activeDays14))
        if (stats.weekMinutes > 0 && stats.weekDailyMinutes.isNotEmpty())
            list += StorySegment("stat-week", SegmentKind.STAT, "${stats.weekMinutes} minutes this week",
                "Your listening, day by day.", SegmentVisual.Sparkline(stats.weekDailyMinutes, "last 7 days"),
                cta = StoryCta("See stats", StoryActionType.OPEN_STATS))
        stats.topArtistTrack?.let { t ->
            list += StorySegment("stat-artist", SegmentKind.STAT, "Top artist: ${t.artist}",
                "On heavy rotation.", SegmentVisual.AlbumArt(t))
        }
        if (stats.totalPlays >= 20 && stats.topArtists.isNotEmpty())
            list += StorySegment("stat-plays", SegmentKind.STAT, "${stats.totalPlays} plays",
                "Your most-played artists.", SegmentVisual.Bars(stats.topArtists))

        // ── Algorithm explainers — make the feed's intelligence visible ───────
        list += StorySegment("algo-alive", SegmentKind.ALGORITHM, "Your feed is alive",
            "Home isn't a fixed list. Every scroll and refresh re-mixes your library into new sections — it never runs out.",
            SegmentVisual.Illustration(IllustrationKind.FEED_FLOW))
        list += StorySegment("algo-why", SegmentKind.ALGORITHM, "Why this song?",
            "Play counts, skips, when you listen, and how far you get all quietly shape what surfaces — and what's held back.",
            SegmentVisual.Illustration(IllustrationKind.SHUFFLE))
        list += StorySegment("algo-tags", SegmentKind.ALGORITHM, "Tags do the heavy lifting",
            "Tag a few songs and whole new sections appear — mood mixes, smart playlists, per-tag EQ. The more you tag, the smarter it gets.",
            SegmentVisual.Illustration(IllustrationKind.TAGS), cta = StoryCta("Browse tags", StoryActionType.OPEN_SEARCH))
        list += StorySegment("algo-time", SegmentKind.ALGORITHM, "It reads the room",
            "Morning, late night, commute, weekend — the feed leans into the time of day using your own listening patterns.",
            SegmentVisual.Illustration(IllustrationKind.GENERIC))

        // ── Feature tips with deep-link CTAs ──────────────────────────────────
        list += StorySegment("tip-crossfade", SegmentKind.TIP, "Seamless transitions",
            "Turn on Crossfade for overlapping songs — Album mode keeps mastered segues intact.",
            SegmentVisual.Illustration(IllustrationKind.CROSSFADE), cta = StoryCta("Open Crossfade", StoryActionType.OPEN_CROSSFADE))
        list += seg("tip-profiles", SegmentKind.TIP, "🎛️", "EQ per tag",
            "Give a tag its own equalizer preset — it applies automatically when a matching song plays.",
            StoryCta("Playback profiles", StoryActionType.OPEN_PROFILES))
        list += seg("tip-accent", SegmentKind.TIP, "🎨", "Make it yours",
            "Pin a fixed accent colour, or let the gradient follow each album's art.",
            StoryCta("Appearance", StoryActionType.OPEN_ACCENT))
        list += seg("tip-mix", SegmentKind.TIP, "🎧", "Daily Mixes",
            "Fresh themed mixes wait at the top of Home each day, built from your tags and most-played artists.",
            StoryCta("Play a mix", StoryActionType.PLAY_DAILY_MIX))
        list += seg("tip-dupes", SegmentKind.TIP, "🧹", "Tidy up",
            "The duplicate finder groups songs that share a title and artist so you can clear the extras.",
            StoryCta("Find duplicates", StoryActionType.OPEN_DUPLICATES))

        return list
    }

    private fun seg(id: String, kind: SegmentKind, emoji: String, title: String, body: String, cta: StoryCta? = null) =
        StorySegment(id = id, kind = kind, title = title, body = body, visual = SegmentVisual.Emoji(emoji), cta = cta)
}
