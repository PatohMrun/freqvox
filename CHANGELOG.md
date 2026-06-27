# Changelog

All notable changes to this project will be documented in this file.

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
