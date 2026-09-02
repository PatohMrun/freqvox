package com.freqvox.ui.story

import androidx.compose.ui.graphics.Color
import com.freqvox.data.local.entities.TrackEntity
import com.freqvox.ui.main.MainViewModel

/**
 * The library of story content: the evergreen [educationalPool] (algorithm explainers, feature
 * tips, and how-tos covering menus, gestures and hidden functions), plus data-driven weekly
 * recaps and milestones. The scheduler picks one pool entry at random per drop period and spaces
 * repeats out, so it never feels like a fixed script.
 */
object StoryContent {

    /** Curated hues that read well over the viewer's dark gradient. Each card picks one
     *  deterministically from its id, so every tip gets its own colour world instead of the
     *  whole pool falling back to the theme accent and looking like one repeated card. */
    private val accentPalette = listOf(
        Color(0xFF7C4DFF), // violet
        Color(0xFF00BCD4), // cyan
        Color(0xFFFF6E40), // coral
        Color(0xFF66BB6A), // green
        Color(0xFFEC407A), // pink
        Color(0xFF29B6F6), // sky
        Color(0xFFFFCA28), // amber
        Color(0xFF26A69A)  // teal
    )

    private fun accentFor(id: String): Color =
        accentPalette[((id.hashCode() % accentPalette.size) + accentPalette.size) % accentPalette.size]

    private fun ill(id: String, kind: SegmentKind, title: String, body: String, k: IllustrationKind, cta: StoryCta? = null) =
        StorySegment(id, kind, title, body, SegmentVisual.Illustration(k), accent = accentFor(id), cta = cta)

    private val A = SegmentKind.ALGORITHM
    private val T = SegmentKind.TIP

    /** Evergreen, stats-independent teaching cards — the rotation the ring draws from. */
    val educationalPool: List<StorySegment> = listOf(
        // ── How the feed thinks ───────────────────────────────────────────────
        ill("algo-alive", A, "Your feed is alive",
            "Home isn't a fixed list. Every scroll and refresh re-mixes your library into new sections — it never runs out.", IllustrationKind.FEED_FLOW),
        ill("algo-why", A, "Why this song?",
            "Play counts, skips, when you listen and how far you get all quietly shape what surfaces — and what's held back.", IllustrationKind.SHUFFLE),
        ill("algo-tags", A, "Tags do the heavy lifting",
            "Tag a few songs and whole new sections appear — mood mixes, smart playlists, per-tag EQ. The more you tag, the smarter it gets.", IllustrationKind.TAGS, StoryCta("Browse tags", StoryActionType.OPEN_SEARCH)),
        ill("algo-time", A, "It reads the room",
            "Morning, late night, commute, weekend — the feed leans into the time of day from your own patterns.", IllustrationKind.GENERIC),
        ill("algo-spotlight", A, "Spotlight keeps you guessing",
            "The Spotlight rotates among your top 15 most-played, so it isn't locked to a single song.", IllustrationKind.STATS),
        ill("algo-dailymix", A, "Daily Mixes, once a day",
            "Daily Mixes are seeded by the date — they stay put all day, then refresh to a new set tomorrow.", IllustrationKind.GENERIC),
        ill("algo-onthisday", A, "On This Day",
            "The feed occasionally resurfaces tracks you played on this same date in an earlier month or year.", IllustrationKind.GENERIC),

        // ── Feature tips (deep-linked where it helps) ─────────────────────────
        ill("tip-crossfade", T, "Seamless transitions",
            "Turn on Crossfade to overlap songs. Album mode keeps mastered segues intact.", IllustrationKind.CROSSFADE, StoryCta("Open Crossfade", StoryActionType.OPEN_CROSSFADE)),
        ill("tip-albummode", T, "Albums, as intended",
            "With Album mode on, consecutive tracks from the same album butt-join gaplessly instead of crossfading.", IllustrationKind.CROSSFADE, StoryCta("Open Crossfade", StoryActionType.OPEN_CROSSFADE)),
        ill("tip-profiles", T, "EQ per tag", "Give a tag its own equalizer preset — it's applied automatically when a matching song plays.", IllustrationKind.EQUALIZER, StoryCta("Playback profiles", StoryActionType.OPEN_PROFILES)),
        ill("tip-volnorm", T, "Even it out", "Volume normalization tames loudness jumps between tracks on shuffle.", IllustrationKind.VOLUME, StoryCta("Open Crossfade", StoryActionType.OPEN_CROSSFADE)),
        ill("tip-accent", T, "Make it yours", "Pick from 16 swatches to colour the buttons and switches — and pin the background gradient too, or let it keep following each album's art.", IllustrationKind.PALETTE, StoryCta("Appearance", StoryActionType.OPEN_ACCENT)),
        ill("tip-theme", T, "Light or dark", "Tap the sun/moon in the header to cycle System, Light and Dark.", IllustrationKind.THEME),
        ill("tip-sleep", T, "Drift off", "The sleep timer fades out gently before it stops — or you can stop at the end of the current album.", IllustrationKind.TIMER),
        ill("tip-eq", T, "Shape the sound", "A full equalizer lives in the side panel's Audio section.", IllustrationKind.STATS),
        ill("tip-dupes", T, "Tidy up", "The duplicate finder groups songs that share a title and artist so you can clear the extras.", IllustrationKind.CLEANUP, StoryCta("Find duplicates", StoryActionType.OPEN_DUPLICATES)),
        ill("tip-backup", T, "Never lose your work", "Back up tags, playlists, stats, settings and your EQ to one file — and restore it anytime.", IllustrationKind.SAVE),
        ill("tip-hiddenfolders", T, "Hide the noise", "Hide folders like ringtones or voice notes instantly — nothing is deleted, and you can un-hide any time.", IllustrationKind.FOLDER),
        ill("tip-art", T, "Cover art, filled in", "Missing covers are fetched online, or recovered from your VidMate downloads.", IllustrationKind.ARTWORK, StoryCta("Album art", StoryActionType.OPEN_ARTWORK)),
        ill("tip-radio", T, "Go live", "The Radio tab streams live stations when you want something outside your library.", IllustrationKind.RADIO),

        // ── How-tos, hidden menus & gestures ──────────────────────────────────
        ill("how-longpress", T, "Long-press a song",
            "Long-press any track for the full menu: tag, add to playlist, play next, share, edit info, or delete.", IllustrationKind.GESTURE),
        ill("how-multiselect", T, "Select many at once",
            "Long-press a song in your Library to enter select mode, then bulk-tag, add to a playlist, or delete the lot.", IllustrationKind.GESTURE),
        ill("how-reorderpills", T, "Reorder your tabs",
            "Drag the Library tabs left or right to reorder them — the arrangement sticks.", IllustrationKind.GESTURE),
        ill("how-queue-reorder", T, "Reshape the queue",
            "Drag items in the play queue to change what comes next.", IllustrationKind.GESTURE),
        ill("how-lyrics-seek", T, "Tap a lyric to jump",
            "With synced .lrc lyrics, the current line is highlighted — tap any line to jump straight to that moment.", IllustrationKind.LYRICS),
        ill("how-boolean-tags", T, "Filter with logic",
            "In Search, tap a tag to require it, tap again to exclude it: \"chill AND workout, NOT throwback\".", IllustrationKind.TAGS, StoryCta("Try it", StoryActionType.OPEN_SEARCH)),
        ill("how-pull-refresh", T, "Pull for more",
            "Pull down on Home for a completely fresh remix of your library.", IllustrationKind.GENERIC),
        ill("how-edit-info", T, "Fix the details", "Wrong title, artist or album? Long-press the song → Edit info. Your fix sticks — even after library rescans.", IllustrationKind.EDIT),
        ill("how-share-card", T, "Share a track",
            "Long-press → Share as image turns any song into a poster card you can send anywhere.", IllustrationKind.SHARE),
        ill("how-custom-tag", T, "Make your own tags",
            "Create custom tags — the '+ New custom tag' button is pinned to the top of the tag sheet.", IllustrationKind.TAGS),
        ill("how-smart-playlist", T, "Playlists that update themselves",
            "Tag songs, then create a smart playlist from those tags — it stays in sync as you tag more.", IllustrationKind.PLAYLIST),
        ill("how-playlist-edit", T, "Edit inside a playlist",
            "Open a playlist to rename it, reorder tracks, and add or remove songs on the fly.", IllustrationKind.PLAYLIST),
        ill("how-play-next", T, "Jump the queue", "Use Play next to slot a song in right after the one that's playing.", IllustrationKind.PLAYLIST),
        ill("how-stats", T, "Dig into your stats",
            "In Stats, tap any top artist or tag to open its own breakdown — time, plays and top tracks.", IllustrationKind.STATS, StoryCta("Open stats", StoryActionType.OPEN_STATS)),
        ill("how-settings", T, "One place for everything", "The gear opens it all: audio, library tools, backup and tag management.", IllustrationKind.SETTINGS),
        ill("how-search", T, "Find anything",
            "Search by title, artist or album — or browse your whole library by tag.", IllustrationKind.SEARCH, StoryCta("Search", StoryActionType.OPEN_SEARCH)),
        ill("how-addqueue", T, "Line it up", "Add to queue drops a song at the end of what's already playing.", IllustrationKind.PLAYLIST),

        // ── Newer features (2.78–2.81) ────────────────────────────────────────
        ill("how-search-anywhere", T, "Search from anywhere",
            "The magnifier on Home and Library drops you straight into Search, keyboard ready.", IllustrationKind.SEARCH, StoryCta("Try it", StoryActionType.OPEN_SEARCH)),
        ill("how-see-disliked", T, "See what you've hidden",
            "Disliked songs leave your feed, but they're not gone — in Search, require the Disliked tag to review them.", IllustrationKind.TAGS, StoryCta("Open Search", StoryActionType.OPEN_SEARCH)),
        ill("how-back-layers", T, "Back, one layer at a time",
            "In Search, Back clears the keyboard first, then your typed search, then tag filters — then it leaves.", IllustrationKind.GESTURE),
        ill("tip-pin-gradient", T, "Two looks, one colour",
            "A custom accent colours the controls while the backdrop keeps dancing with your music — pin the gradient only if you want both constant.", IllustrationKind.PALETTE, StoryCta("Appearance", StoryActionType.OPEN_ACCENT)),

        // ── Newer features (2.82–2.90) ────────────────────────────────────────
        ill("tip-speed", T, "Set the pace",
            "Playback speed lives in the player — slow things down to 0.8× or push a mix to 1.5×.", IllustrationKind.SPEED),
        ill("how-save-queue", T, "Keep that queue",
            "Built the perfect run of songs? Save the whole queue as a playlist, straight from the queue sheet.", IllustrationKind.SAVE),
        ill("tip-stop-after", T, "Just one more song",
            "Stop after current track finishes what's playing, then goes quiet — no fumbling for pause.", IllustrationKind.STOP),
        ill("tip-auto", T, "Take it on the road",
            "Freqvox works with Android Auto — your songs, playlists and tags on the car screen.", IllustrationKind.CAR),
        ill("tip-radio-fav", T, "Pin your stations",
            "Heart any radio station to keep it at the top of the Radio tab — favourites even ride along in backups.", IllustrationKind.RADIO),
        ill("how-jump-back", T, "Pick up where you left off",
            "Jump Back In on Home remembers the song you were last playing — one tap resumes it.", IllustrationKind.REWIND)
    )

    /** A snapshot of the user's live state, used to generate contextual (personalized) tips that
     *  only appear while their condition holds — making the teaching pool effectively unbounded. */
    data class StoryState(
        val crossfadeOff: Boolean = false,
        val eqUnused: Boolean = false,
        val untaggedCount: Int = 0,
        val hasCustomTags: Boolean = true,
        val duplicateGroups: Int = 0,
        val mostSkippedTrack: TrackEntity? = null,
        val mostSkippedCount: Int = 0,
        val fixedAccentDefault: Boolean = false,
        val hasProfiles: Boolean = true
    )

    /** Contextual tips eligible for the current state. Each has a stable id (so the anti-repeat
     *  window applies) and drops out of the rotation the moment its condition is resolved. */
    fun contextualTips(s: StoryState): List<StorySegment> {
        val out = mutableListOf<StorySegment>()
        if (s.crossfadeOff)
            out += ill("ctx-crossfade", T, "Smooth out your shuffle",
                "You haven't turned on Crossfade yet — it overlaps songs so there's no hard cut between them.", IllustrationKind.CROSSFADE, StoryCta("Turn it on", StoryActionType.OPEN_CROSSFADE))
        if (s.eqUnused)
            out += ill("ctx-eq", T, "Untapped: the equalizer",
                "You haven't shaped your sound yet — the equalizer can add warmth or bite in seconds.", IllustrationKind.STATS)
        if (s.untaggedCount >= 10)
            out += ill("ctx-untagged", T, "${s.untaggedCount} songs, no tags",
                "Tagging even a few unlocks mood mixes and smart playlists tailored to you.", IllustrationKind.TAGS, StoryCta("Start tagging", StoryActionType.OPEN_SEARCH))
        if (!s.hasCustomTags)
            out += ill("ctx-firsttag", T, "Make it yours with tags",
                "You haven't created a custom tag yet — try 'gym', 'focus', or 'late night'.", IllustrationKind.TAGS, StoryCta("Browse tags", StoryActionType.OPEN_SEARCH))
        if (s.duplicateGroups > 0)
            out += ill("ctx-dupes", T, "${s.duplicateGroups} possible duplicate${if (s.duplicateGroups == 1) "" else "s"}",
                "Freqvox spotted songs that share a title and artist — clean them up in a tap.", IllustrationKind.CLEANUP, StoryCta("Review", StoryActionType.OPEN_DUPLICATES))
        if (s.mostSkippedTrack != null && s.mostSkippedCount >= 3)
            out += StorySegment("ctx-skip", T, "Always skipping this?",
                "\"${s.mostSkippedTrack.title}\" gets skipped a lot (${s.mostSkippedCount}×). Dislike it to keep it out of your feed.",
                SegmentVisual.AlbumArt(s.mostSkippedTrack), accent = accentFor("ctx-skip"))
        if (s.fixedAccentDefault)
            out += ill("ctx-accent", T, "Lock in a look",
                "Everything follows each cover's colours right now — pick a custom accent for the controls, and optionally pin the gradient too.", IllustrationKind.PALETTE, StoryCta("Appearance", StoryActionType.OPEN_ACCENT))
        if (!s.hasProfiles && s.hasCustomTags)
            out += ill("ctx-profiles", T, "Auto-EQ per tag",
                "Give a tag its own EQ preset — perfect for a 'podcast' or 'bass' tag.", IllustrationKind.EQUALIZER, StoryCta("Playback profiles", StoryActionType.OPEN_PROFILES))
        return out
    }

    /** QA-only: a single drop containing one of every card + visual type (synthetic data where the
     *  library is sparse), for eyeballing designs on demand without waiting on the scheduler. */
    fun previewDrop(stats: MainViewModel.ListeningStats, epochDay: Long): StoryDrop {
        val segs = mutableListOf<StorySegment>()
        segs += StorySegment("pv-intro", SegmentKind.RECAP, "Story preview",
            "Every card type, for design QA — this only appears via long-press.", SegmentVisual.Illustration(IllustrationKind.STATS))

        segs += educationalPool
        // Contextual tips, forced-eligible so QA can see them all.
        segs += contextualTips(StoryState(
            crossfadeOff = true, eqUnused = true, untaggedCount = 42, hasCustomTags = true,
            duplicateGroups = 3, mostSkippedTrack = stats.topTracks.firstOrNull()?.first,
            mostSkippedCount = 7, fixedAccentDefault = true, hasProfiles = false
        ))

        val spark = stats.weekDailyMinutes.ifEmpty { listOf(0.2f, 0.6f, 0.4f, 0.9f, 0.5f, 0.75f, 0.35f) }
        val days = stats.activeDays14.ifEmpty { List(14) { it % 3 != 0 } }
        val bars = stats.topArtists.ifEmpty { listOf("Artist A" to 12, "Artist B" to 9, "Artist C" to 5) }
        segs += StorySegment("pv-spark", SegmentKind.STAT, "Sparkline", "Weekly minutes.", SegmentVisual.Sparkline(spark, "sample"))
        segs += StorySegment("pv-streak", SegmentKind.STAT, "Streak calendar", "Active-day dots.", SegmentVisual.StreakCalendar(days))
        segs += StorySegment("pv-hist", SegmentKind.STAT, "Hour histogram", "Listening by hour.", SegmentVisual.Histogram(List(24) { (it * 7) % 13 }))
        segs += StorySegment("pv-bars", SegmentKind.STAT, "Bars", "Top artists.", SegmentVisual.Bars(bars))
        stats.topArtistTrack?.let { segs += StorySegment("pv-art", SegmentKind.STAT, "Album art", it.title, SegmentVisual.AlbumArt(it)) }
        if (stats.newThisWeek.isNotEmpty()) segs += StorySegment("pv-collage", SegmentKind.STAT, "Collage", "New this week.", SegmentVisual.ArtCollage(stats.newThisWeek))

        IllustrationKind.values().forEach { k ->
            segs += StorySegment("pv-ill-$k", SegmentKind.ALGORITHM, "Illustration · $k", "Illustration sample.", SegmentVisual.Illustration(k))
        }

        segs += weeklyRecap(stats, 0, epochDay).segments
        val ms = metMilestones(stats).firstOrNull() ?: MilestoneDef("streak7-sample", IllustrationKind.FLAME, "7-day streak!", "Sample milestone card.")
        segs += milestoneDrop(ms, epochDay).segments

        return StoryDrop("preview-$epochDay", epochDay, SegmentKind.TIP, segs)
    }

    /** [track] set on track-specific milestones: its cover then replaces the illustration badge. */
    data class MilestoneDef(
        val key: String, val illustration: IllustrationKind, val title: String, val body: String,
        val track: TrackEntity? = null
    )

    /** Achievements the user currently qualifies for, highest first. The scheduler shows the first
     *  un-celebrated one; viewing it marks it celebrated so it fires only once. */
    fun metMilestones(stats: MainViewModel.ListeningStats): List<MilestoneDef> {
        val out = mutableListOf<MilestoneDef>()
        when {
            stats.currentStreakDays >= 30 -> out += MilestoneDef("streak30", IllustrationKind.TROPHY, "30-day streak!", "A whole month of daily listening. Legendary.")
            stats.currentStreakDays >= 7 -> out += MilestoneDef("streak7", IllustrationKind.FLAME, "7-day streak!", "A full week, every single day. You're on fire.")
        }
        when {
            stats.totalPlays >= 1000 -> out += MilestoneDef("plays1000", IllustrationKind.GEM, "1,000 plays", "A thousand songs played in Freqvox. That's dedication.")
            stats.totalPlays >= 500 -> out += MilestoneDef("plays500", IllustrationKind.STAR, "500 plays", "Halfway to a thousand — keep the music going.")
        }
        stats.topTracks.firstOrNull()?.let { (t, c) ->
            if (c >= 100) out += MilestoneDef("track100-${t.id}", IllustrationKind.STAR, "100 plays of one song", "\"${t.title}\" is a certified favourite.", track = t)
        }
        return out
    }

    /** One milestone as a single story card — the scheduler composes these into the active drop. */
    fun milestoneSegment(def: MilestoneDef): StorySegment = StorySegment(
        "m-${def.key}", SegmentKind.MILESTONE, def.title, def.body,
        def.track?.let { SegmentVisual.AlbumArt(it) } ?: SegmentVisual.Illustration(def.illustration),
        accent = accentFor(def.key)
    )

    fun milestoneDrop(def: MilestoneDef, epochDay: Long): StoryDrop = StoryDrop(
        id = "milestone-${def.key}",
        createdDay = epochDay,
        kind = SegmentKind.MILESTONE,
        segments = listOf(milestoneSegment(def))
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
}
