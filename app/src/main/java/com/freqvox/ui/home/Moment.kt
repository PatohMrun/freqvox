package com.freqvox.ui.home

import com.freqvox.ui.main.MainViewModel

/** A single "moment" surfaced behind the header ring — a tip or a highlight from your stats. */
data class Moment(val emoji: String, val title: String, val body: String)

private val TIPS = listOf(
    Moment("💡", "Quick actions", "Long-press any song for tag, playlist, share, edit and more."),
    Moment("🎚️", "Smooth transitions", "Turn on Crossfade in Audio settings — album mode keeps segues intact."),
    Moment("🏷️", "Smart playlists", "Tag songs, then filter Browse with AND / NOT for instant mixes."),
    Moment("🌙", "Drift off", "The sleep timer now fades out gently — or stop at the end of an album."),
    Moment("🎨", "Make it yours", "Pin a fixed accent colour in Appearance, or follow album art."),
    Moment("🔊", "Even it out", "Volume normalization (Audio) tames loudness jumps on shuffle."),
    Moment("🎛️", "Per-tag EQ", "Give a tag its own EQ preset under Playback profiles."),
    Moment("🖼️", "Share a track", "Long-press → Share as image for a poster card of any song.")
)

/**
 * Builds the day's moment deterministically from the epoch-day, mixing tips with highlights drawn
 * from the user's stats, so the same one shows all day and rolls over tomorrow.
 */
fun generateMoment(stats: MainViewModel.ListeningStats, epochDay: Long): Moment {
    val pool = mutableListOf<Moment>()
    if (stats.currentStreakDays >= 2)
        pool += Moment("🔥", "${stats.currentStreakDays}-day streak", "You've listened ${stats.currentStreakDays} days in a row. Keep it alive!")
    if (stats.weekMinutes > 0)
        pool += Moment("⏱️", "${stats.weekMinutes} min this week", "That's your listening so far this week.")
    stats.topArtists.firstOrNull()?.let {
        pool += Moment("🎤", "Top artist: ${it.first}", "${it.second} play${if (it.second == 1) "" else "s"} and counting.")
    }
    if (stats.totalPlays > 0)
        pool += Moment("📈", "${stats.totalPlays} plays", "Across ${stats.uniqueTracks} tracks in your library.")
    pool += TIPS

    val idx = ((epochDay % pool.size).toInt() + pool.size) % pool.size
    return pool[idx]
}
