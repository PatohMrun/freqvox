# Changelog

All notable changes to this project will be documented in this file.

## [2.57] - 2026-07-05
### Added
- **Share your week**: the weekly recap story has a share button that exports a poster image — minutes, a sparkline of your week, top artist, most-played track and streak — to the Android share sheet.
### Changed
- The header ring now gently pulses while there's a new, unseen story, and sits still once you've viewed it.

## [2.56] - 2026-07-05
### Added
- **Explainer stories + working tips**: the ring now teaches — illustrated cards explain how your feed stays endlessly fresh from your tags and habits ("your feed is alive", "why this song?", "tags do the heavy lifting"). Stat cards use real charts (streak calendar, weekly sparkline, top-artist art, top-artist bars). Tip cards' buttons now deep-link straight to the feature — Crossfade, Playback profiles, Appearance, Stats, Browse, duplicates, or play a Daily Mix.

## [2.55] - 2026-07-05
### Added
- **Milestones**: the ring celebrates achievements as you hit them — a 7- or 30-day streak, 500/1000 total plays, or 100 plays of a single song. Each one appears once and is remembered so it won't nag you again.

## [2.54] - 2026-07-05
### Added
- **"Your week in Freqvox" recap**: once a week the header ring opens a multi-segment story — minutes listened (day-by-day sparkline), your top artist and most-played track (with art), new additions (cover collage) and your streak. It takes priority over tips until you've seen it.

## [2.53] - 2026-07-05
### Added
- **Story graphics**: story segments can now render real drawn visuals — album art, cover collages, a listening sparkline, bar and hour-of-day charts, a streak calendar, and simple illustrations — ready for the weekly recap and milestones. (Internal groundwork; surfaces in the next updates.)

## [2.52] - 2026-07-05
### Changed
- **Stories, upgraded**: the header ring now opens a full-screen, Instagram-style story viewer — segmented progress bars that auto-advance, tap left/right to move, press-and-hold to pause, drag down to dismiss. The single daily "Moment" is now a one-segment story, and the ring's cadence moved to a curated ~2–3×/week. (Foundation for weekly recaps, milestones and richer graphics landing next.)

## [2.51] - 2026-07-05
### Added
- **Moments (header story-ring)**: the logo in the Home header now shows a "moment" each day — a tip or a highlight from your stats. A vivid ring means there's something new; tap it to read, and the ring fades once seen. Settings now live solely on the gear icon (the logo no longer duplicates it).

## [2.50] - 2026-07-05
### Added
- **Edit track info**: long-press a song on Home → "Edit info" to fix its title, artist or album. Changes are written to MediaStore (with the system's one-time permission prompt on Android 11+) and reflected across the app. (Updates the library metadata, not the file's embedded tags.)

## [2.49] - 2026-07-05
### Added
- **Playback profiles (per-tag EQ)** (side panel → Audio → Playback profiles): assign an equalizer preset to any tag, and it's applied automatically whenever a track carrying that tag plays (reverting to your previous preset afterwards). Great for a "podcast" or "bass boost" tag.

## [2.48] - 2026-07-05
### Changed
- **Synced lyrics now auto-scroll**: when a track has a timestamped `.lrc`, the lyrics view keeps the current line centered and moving as the song plays, and you can tap any line to jump to that moment.

## [2.47] - 2026-07-05
### Added
- **Share a track as an image**: the track options menu (long-press a song) now has "Share as image", which renders a poster card — album art over a gradient drawn from the cover, with the title, artist and Freqvox wordmark — and opens the Android share sheet.

## [2.46] - 2026-07-05
### Added
- **Multi-select in the Library Songs list**: long-press a song to enter selection mode, then tap to select more. A bottom action bar shows the count and lets you bulk **tag**, **add to a playlist**, or **delete** the whole selection at once (deletes go through one system confirmation). Back or the ✕ exits selection.

## [2.45] - 2026-07-05
### Added
- **Duplicate finder** (side panel → Library → Find duplicates): groups tracks that share a title and artist (ignoring case/punctuation), shows each copy with its folder, length, album and play count — with the most-played one marked "kept" — so you can delete the extras through the normal delete confirmation.

## [2.44] - 2026-07-04
### Added
- **Per-artist & per-tag stats**: the stats sheet now has "Top Artists" and "Top Tags" sections; tapping any of them opens a focused breakdown for that artist or tag — total time, plays, tracks played, first/last heard, and its most-played tracks.

## [2.43] - 2026-07-04
### Added
- **Daily Mixes**: a row of themed mixes near the top of the Home feed, built from your tags and most-present artists. They're seeded by the date, so they stay the same all day (instead of reshuffling every time the feed regenerates) and refresh to a new set tomorrow. Tap a mix to play it.

## [2.42] - 2026-07-04
### Added
- **Listening streaks + weekly goal** in the stats sheet: a new "Momentum" section shows your current daily listening streak (consecutive days with a play) and how much you've listened this week against a gentle goal, with a progress bar.

## [2.41] - 2026-07-04
### Added
- **"On This Day" feed section**: the Home feed can now surface tracks you played on this same calendar date in an earlier month or year, ranked by how often you played them that day — a nostalgic pull from your listening history.

## [2.40] - 2026-07-04
### Added
- **Sleep-timer fade-out**: a minute-based sleep timer now fades the volume down gently over the final 20 seconds instead of cutting off abruptly (volume is always restored afterward).
- **"Stop after this album"**: a new sleep-timer mode that pauses when the current album finishes (at the first track of a different album), alongside the existing "end of current track".

## [2.39] - 2026-07-04
### Added
- **Fixed accent option** (side panel → Appearance → Accent color): pin the background gradient to a single colour of your choice instead of deriving it from each track's album art. Off by default (album-derived stays the signature look); pick from eight swatches. The static UI accent is unaffected.

## [2.38] - 2026-07-04
### Added
- **Backup now includes your equalizer (backup format v4)**: the EQ on/off state, selected preset, and custom band levels are exported alongside the other settings, so a restore brings your sound tuning back too. Applied live if playback is already running, otherwise picked up when the equalizer next initializes. Still fully compatible with older backup files.

## [2.37] - 2026-07-04
### Added
- **Backup now covers history + settings (backup format v3)**: exports also include your timestamped **play & skip history** (the raw events behind the listening-stats graphs — peak hour, hourly buckets) and your **app settings** (crossfade length, album mode, volume normalization, theme, album-art network policy). So a restore onto a reinstall or a new device brings back the detailed stats and your configuration, not just tags/playlists. History is de-duplicated on import (re-importing on the same device never double-counts), settings apply only when present, and older v1/v2 backup files still restore fine. Network-consent flags are intentionally not exported — you re-acknowledge those per device.

## [2.36] - 2026-07-04
### Fixed
- **Hidden folders wording**: the sheet said folders are excluded "on the next scan", which was misleading — hiding actually takes effect immediately and non-destructively. Reworded to "disappear from your library right away. Nothing is deleted — un-hide any time."

## [2.35] - 2026-07-04
### Changed
- **Tag sheet**: the "New custom tag" button is now pinned to the top of the sheet (full-width and accent-styled) instead of being the last cell in the grid, so it stays reachable as your tag list grows rather than getting buried at the bottom.

## [2.34] - 2026-07-04
### Added
- **Volume normalization (ReplayGain)** — a new toggle in the side panel's Audio group (off by default). When on, each track's `REPLAYGAIN_TRACK_GAIN` tag (read live from Vorbis comments / ID3) turns loud tracks *down* toward a reference level so shuffle stops jumping in loudness. It's attenuate-only, so it can never boost past unity and therefore can never clip; tracks with no tag play unchanged, and the gain composes cleanly with crossfade and audio-focus ducking.

### Changed
- **Side panel groups** now render as soft cards with a faint fill and a low-contrast "borderless" border instead of flat labels, so each group reads as its own panel.
- **Side panel accordions remember their expand/collapse state** across the drawer closing and app restarts (persisted per group), instead of springing back to defaults every time.

## [2.33] - 2026-07-04
### Added
- **Boolean tag filtering** in Browse (Search): tap a tag to *require* it, tap again to *exclude* it, tap a third time to clear. Songs are filtered live to those carrying **all** required tags and **none** of the excluded ones — e.g. "chill AND workout", "hype NOT throwback". A summary line spells out the active expression, with a Clear shortcut. When no filter is set, the colourful browse-by-tag grid stays exactly as before, and plain text search is unchanged.

## [2.32] - 2026-07-04
### Added
- **Album mode** (in the Crossfade sheet, on by default): when crossfade is on, consecutive tracks from the *same album* now butt-join gaplessly instead of fading into each other — so albums mastered to segue track-to-track (live sets, DJ mixes, concept records) stay seamless. Transitions between *different* albums still crossfade normally. It's a no-op (and shown disabled) when crossfade is Off.

## [2.31] - 2026-07-04
### Fixed
- Crossfade click/scratch: the session player is now muted *before* the internal jump to the next track, not after. That jump forces the audio pipeline to flush and reconfigure the output — previously it did so at full volume, producing an audible pop at the start of every crossfade. Muting first makes the flush silent. (If a faint artifact remains, the next suspect is a sample-rate mismatch between the two tracks forcing a resample — noted for follow-up.)

## [2.30] - 2026-06-28
### Changed
- Home "Spotlight" no longer always shows your single most-played track — it now rotates among your top 15 most-played (using the per-page seed), so it varies across feed pages and pull-to-refresh.

## [2.29] - 2026-06-27
### Changed
- Side panel: Playlists and Tags groups now start collapsed (they grow with the library), while the settings groups stay expanded.

## [2.28] - 2026-06-27
### Added
- Side panel groups (Insights, Audio, Library, Backup, Playlists, Tags) are now collapsible accordions — tap a header to fold/unfold with an animated chevron. Expanded by default; collapse state persists.

## [2.27] - 2026-06-27
### Fixed
- The bright pink/red accent (Radio "NOW PLAYING" + live pulse, favorites, error text, cancel-timer, delete actions) vibrated harshly on white in light mode. Added a theme-aware "rose" accent token — bright pink in dark, a deeper rose in light — and applied it to those on-surface spots.

## [2.26] - 2026-06-27
### Fixed
- Buttons on the accent fill (the main player play/pause control, Home back-to-top, Library "+" FAB, the sleep-timer "Set" button) now use white icons/labels instead of dark ones that were invisible on the dark accent in light mode.

## [2.25] - 2026-06-27
### Fixed
- Search: the colorful tag browse cards now use white labels in both themes (were dark text that washed out on the saturated gradient in light mode).

## [2.24] - 2026-06-27
### Fixed (light theme polish)
- Navbar: darkened unselected tab icons/labels (less washed-out gray).
- Now-playing screen: added a subtle album-tinted gradient wash so it isn't a flat white slab.
- Search: selected tag chips now use the solid tag color with a white label.
- Header greeting auto-shrinks to fit instead of truncating — keeps the full "Good evening" wording, no scrolling/motion.

## [2.23] - 2026-06-27
### Fixed (light theme contrast)
- Darkened light-mode grays (pure-black base) and switched to a deeper, legible accent purple; dynamic accents are clamped darker so white text reads on them and they read on white.
- Added an `onAccent` token: text/icons on a colored fill (active Library/Radio pills, etc.) now stay white instead of turning black.
- Themed the surfaces the first pass missed: bottom sheets (equalizer, lyrics, queue, tag), dropdown / sort / context menus, the search bar, and the no-art card.
- Featured / Spotlight / Hero cards (text over album art) now use a fixed dark poster scrim + white text in both themes, fixing washed-out art and the invisible play icon.
- Header: fixed the squashed logo (greeting now yields width to the controls) and added the theme toggle to the Library header too.

## [2.22] - 2026-06-27
### Added
- **Light theme**: A full light mode with a System / Light / Dark toggle in the Home header (System follows the device setting). The album-art gradient signature is preserved in both modes — the same contrast principle that caps accent brightness in dark mode is mirrored for light (background goes near-white with a faint tint of the cover's hue; accents stay dark enough to read on white). System bars follow the mode.
- Theme choice is persisted across launches.

### Changed
- The UI now paints from semantic color tokens (`FreqvoxColors`) instead of hardcoded colors, so every surface flips with the theme. Dark mode is visually unchanged. (Decorative monogram artwork and on-artwork scrims intentionally stay fixed, since they sit on the colorful album gradient, not a theme surface.)

## [2.21] - 2026-06-27
### Fixed
- **Radio header**: The "Radio" title now stays fixed at the top instead of scrolling away with the station list, matching the locked-header behavior on other pages.

## [2.20] - 2026-06-27
### Added
- **Cover Art Archive (MusicBrainz) lookup**: A third online cover source, tried after iTunes and Deezer. Keyless and open-data, it fills real albums the other two miss (older / non-US / catalogue-gap releases). Rate-limited to respect MusicBrainz (≤1 req/s) and bounded so a single match never fans out into many requests.

## [2.19] - 2026-06-27
### Fixed
- **Equalizer during crossfade**: The main and tail players now share one audio session, so the equalizer is applied to the outgoing track during a crossfade instead of it briefly playing flat. Also makes equalizer initialization deterministic.

## [2.18] - 2026-06-27
### Performance
- **VidMate re-link query**: Matching tracks to VidMate thumbnails now scopes the MediaStore query with `_ID IN (…)` instead of scanning the entire audio table and filtering in memory — faster, and no longer scales with total library size.

## [2.17] - 2026-06-27
### Performance
- **Artwork memory**: Decoded cover bitmaps are now recycled immediately after being cached as JPEG, reducing memory/GC pressure during a full-library art backfill.

## [2.16] - 2026-06-27
### Performance
- **Idle crossfade polling**: When crossfade is off (the default), the controller's poll loop now coasts at 5s instead of waking every second for the entire playback session — fewer needless wakeups, lighter on battery.

## [2.15] - 2026-06-27
### Fixed
- **Audio-focus duck vs. crossfade**: A duckable interruption (notification / nav prompt) landing during a crossfade could leave playback permanently quiet. Duck gain and the crossfade ramp now compose (`volume = ramp × duck`) so both tracks duck cleanly and volume always returns to full afterwards.

## [2.14] - 2026-06-26
First public release since 1.48. Internal versions 2.2–2.13 were never shipped on their own; all of their work is consolidated here.

### Added — Album Artwork
- **Online Album-Art Lookup**: Missing cover art is now fetched automatically from the network (iTunes / Deezer) for tracks with no embedded artwork.
- **VidMate Artwork Source**: Recovers the exact original thumbnail for tracks downloaded via VidMate by reading its folder database. Used as the final fallback, after online lookup.
- **Network Policy & Consent**: An explicit consent gate before any network fetch, with granular control (Wi-Fi only / any connection), surfaced in a new **Album Art** settings sheet.
- **Retry Failed Lookups**: Manually re-check missing art, with a live spinner while it runs.
- **Zoom-Fill Artwork**: Covers fill the card with no black letterbox bars, handling non-square aspect ratios.

### Added — Playback
- **Crossfade Engine**: Configurable overlap (Off / 3 / 5 / 8 / 10 / 12s), off by default. Uses a two-player design with pre-arming to avoid stutter, an equal-power volume curve, and pauses the outgoing track on calls / interruptions.
- **Audio-Focus Ducking**: Lowers volume for transient interruptions (notifications) instead of going silent, while still fully pausing for phone calls.
- **Volume Auto-Pause / Resume**: Pauses at volume 0 and resumes when raised — but only when volume was the cause of the pause.
- **Auto-Rescan Library**: A debounced observer rescans automatically when songs are added or removed on the device.

### Added — Playlists & Library
- **In-Place Playlist Editing**: Rename, remove individual tracks, and add songs from within a playlist (manual playlists), plus inline creation of a new playlist on the fly.
- **Sidebar Playlist Tap**: Opens, expands, and scrolls to the selected playlist.
- **Home Long-Press Menu**: A rich track-options sheet (Tag / Add to Playlist / Play Next / Delete).
- **Persisted Library Pills**: Remembers your last-selected tab and supports drag-to-reorder.
- **Locked Headers**: Page headers stay fixed instead of scrolling away.

### Added — Branding
- New brand assets: full launcher icon set (all densities), brand icon, splash logo, and transparent SVG logo variants; updated brand color.
- Dynamic copyright line fixed at the bottom of the settings drawer (auto-updating year, proper © / • glyphs).

### Fixed — Equalizer
- Restored EQ bands, which failed because ExoPlayer creates the audio session lazily; bands are now read after the session exists.
- Added the missing `MODIFY_AUDIO_SETTINGS` permission.

### Fixed — Crossfade
- Eliminated the split-second silence / pop on handoff (tail AudioTrack warm-up).
- Fixed the mid-fade volume dip via the equal-power curve; reverted the warmup-stagger hack that caused echo.

### Fixed — Playback & Notifications
- Bumped **Media3 1.5.1 → 1.7.1** to fix notifications not updating on track change.
- Fixed the **Play-Next crash** (duplicate Compose keys → unique queue IDs).
- Deleted songs are now purged by id *and* path, so they no longer appear in search until restart.

### Performance & Stability
- **Batched Artwork Backfill**: No per-row DB re-emit, scoped VidMate re-queue, and batched negative-cache writes. Fixes the app becoming very slow when linking VidMate.
- Dynamic crossfade polling intervals to cut CPU wakeups / battery drain.
- Bitmap downsampling (`inSampleSize`) in artwork to prevent RAM bloat / OOM crashes.
- Hoisted Canvas brushes in the visualizer & waveform scrubber to stop GC thrashing / frame drops.

### Library / Backup
- Prune dead hidden folders on rescan.
- Backup now includes excluded (hidden) folders.

## [1.48] - 2026-06-20
### Added
- **Right Side Navigation Panel (`AppSidePanel`)**: A new centralized slide-out menu that houses all application settings and library management tools.
- **Tag Manager**: Edit, organize, and manage custom track tags directly from the app.
- **Hidden Folders Management**: Control which directories are excluded from your audio library.
- **Sleep Timer**: Automatically pause playback after a set duration.
- **Listening Stats**: A new insights dashboard to track your listening habits.
- **Backup & Restore**: Secure your library by exporting playlists, tags, and metadata to a `.json` backup file, with full restore capabilities.
- **Android Auto Support**: Initial configurations added to support playback via Android Auto car interfaces.
- **Quick Jump Navigation**: Playlists and tags are now easily accessible directly from the new side panel.
- **Settings Menu Indicator**: Added a highly visible Settings icon on the Home Screen header. This provides a clear, intuitive entry point for users to discover and access the side panel.
