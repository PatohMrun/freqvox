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

        // ── Highlights from the user's own listening ──────────────────────────
        if (stats.currentStreakDays >= 2)
            list += seg("stat-streak", SegmentKind.STAT, "🔥", "${stats.currentStreakDays}-day streak",
                "You've pressed play ${stats.currentStreakDays} days running. Keep the fire lit.")
        if (stats.weekMinutes > 0)
            list += seg("stat-week", SegmentKind.STAT, "⏱️", "${stats.weekMinutes} minutes this week",
                "That's your listening so far this week — track it in Stats.", StoryCta("See stats", StoryActionType.OPEN_STATS))
        stats.topArtists.firstOrNull()?.let {
            list += seg("stat-artist", SegmentKind.STAT, "🎤", "Top artist: ${it.first}",
                "${it.second} play${if (it.second == 1) "" else "s"} and counting.")
        }
        if (stats.totalPlays >= 20)
            list += seg("stat-plays", SegmentKind.STAT, "📈", "${stats.totalPlays} plays",
                "Across ${stats.uniqueTracks} tracks in your library.")

        // ── Algorithm explainers — make the feed's intelligence visible ───────
        list += seg("algo-alive", SegmentKind.ALGORITHM, "♾️", "Your feed is alive",
            "Home isn't a fixed list. Every scroll and refresh re-mixes your library into new sections — it never runs out.")
        list += seg("algo-why", SegmentKind.ALGORITHM, "🧠", "Why this song?",
            "Play counts, skips, when you listen, and how far you get all quietly shape what surfaces — and what's held back.")
        list += seg("algo-tags", SegmentKind.ALGORITHM, "🏷️", "Tags do the heavy lifting",
            "Tag a few songs and whole new sections appear — mood mixes, smart playlists, per-tag EQ. The more you tag, the smarter it gets.",
            StoryCta("Browse tags", StoryActionType.OPEN_SEARCH))
        list += seg("algo-time", SegmentKind.ALGORITHM, "🌗", "It reads the room",
            "Morning, late night, commute, weekend — the feed leans into the time of day using your own listening patterns.")

        // ── Feature tips with deep-link CTAs ──────────────────────────────────
        list += seg("tip-crossfade", SegmentKind.TIP, "🎚️", "Seamless transitions",
            "Turn on Crossfade for overlapping songs — Album mode keeps mastered segues intact.",
            StoryCta("Open Crossfade", StoryActionType.OPEN_CROSSFADE))
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
