package com.freqvox.ui.story

import androidx.compose.ui.graphics.Color
import com.freqvox.data.local.entities.TrackEntity

/** Broad category of a story drop, used for scheduling priority and theming. */
enum class SegmentKind { TIP, ALGORITHM, STAT, RECAP, MILESTONE }

/** What renders in the body of a segment. Renderers live in SegmentVisualView. */
sealed interface SegmentVisual {
    data class Emoji(val emoji: String) : SegmentVisual
    data class AlbumArt(val track: TrackEntity) : SegmentVisual
    data class ArtCollage(val tracks: List<TrackEntity>) : SegmentVisual
    /** Normalised 0..1 points for a smooth line (e.g. daily minutes across the week). */
    data class Sparkline(val points: List<Float>, val caption: String = "") : SegmentVisual
    data class Bars(val items: List<Pair<String, Int>>) : SegmentVisual
    data class Histogram(val buckets: List<Int>) : SegmentVisual
    /** One flag per recent day (oldest→newest); true = listened that day. */
    data class StreakCalendar(val activeDays: List<Boolean>) : SegmentVisual
    data class Illustration(val kind: IllustrationKind) : SegmentVisual
}

enum class IllustrationKind {
    FEED_FLOW, TAGS, CROSSFADE, SHUFFLE, STATS, GENERIC,
    GESTURE, TIMER, LYRICS, PLAYLIST, SHARE, SEARCH
}

/** A deep-link the viewer surfaces as a CTA; resolved up in MainScreen. */
enum class StoryActionType {
    NONE, OPEN_CROSSFADE, OPEN_PROFILES, OPEN_STATS, OPEN_ACCENT, OPEN_TAGS,
    OPEN_ARTWORK, PLAY_DAILY_MIX, OPEN_SEARCH, OPEN_DUPLICATES
}

data class StoryCta(val label: String, val action: StoryActionType)

data class StorySegment(
    val id: String,
    val kind: SegmentKind,
    val title: String,
    val body: String,
    val visual: SegmentVisual,
    val accent: Color? = null,
    val cta: StoryCta? = null
)

/** One tap of the ring: an ordered set of segments. `id` drives the seen/unseen ring state. */
data class StoryDrop(
    val id: String,
    val createdDay: Long,
    val kind: SegmentKind,
    val segments: List<StorySegment>
)
