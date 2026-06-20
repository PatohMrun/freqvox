# Freqvox — Feature Roadmap Scratchpad

Working through recommended features one at a time. **Each feature = one build-number bump**
(`versionCode` +1, `versionName` minor +0.01). Starting point: 1.32 (code 21).

Legend: ⬜ todo · 🔄 in progress · ✅ done

---

## Tier 1 — High value, data already exists

### ✅ 1. Backup / Restore  → build 1.33 (code 22)
Export tags, per-track tag assignments, track stats, and playlists (manual + smart) to a JSON
file the user picks via SAF (no storage permission). Import merges non-destructively.
- Key tracks by **path** (stable across MediaStore id reissue), re-resolve on import.
- Tags matched by **name**; smart-playlist `queryJson` translated tag-id ↔ tag-name so it
  survives a reinstall/new device.
- Files: `data/backup/BackupRepository.kt`, `ui/settings/SettingsSheet.kt`,
  DAO `getAllPlaylistTracks()`, MainViewModel export/import fns, Library header logo → opens Settings.
- Not in v1: full `play_history`/`skip_history` rows (aggregate stats on TrackEntity are captured).

### ✅ 2. Listening stats / "Wrapped" screen  → build 1.34 (code 23)
Surface existing play_history + playCount/skipCount/totalPlayTime: top tracks, total hours,
listening-by-hour histogram, most-skipped, added-this-month. Reached via Settings → Listening stats.
- Files: `MainViewModel.ListeningStats` + `listeningStats` flow, `ui/settings/StatsSheet.kt`,
  Settings "INSIGHTS" row, LibraryScreen `showStatsSheet`.
- Note: confirmed MediaStore `dateAdded` is in **seconds** (FeedGenerator has a latent unit bug
  comparing it to millis thresholds — `freshThisWeek` etc. silently empty). Stats uses seconds.

## Reordered per user: SAFE features first, Android Auto last.

### ✅ 3. Playback speed  → build 1.35 (code 24)
`PlaybackController.playbackSpeed` + `setPlaybackSpeed`; speed picker in the player "more" menu
(0.5×–2×). Gapless is already ExoPlayer default for adjacent items; crossfade deferred.

### ✅ 6. Sleep timer "end of current track"  → build 1.36 (code 25)
`stopAfterCurrentTrack` flag; pauses on the auto-transition after the current song. Mutually
exclusive with the minute timer; surfaced in the sleep sheet + Sleep button active state.

### ✅ 7. Save queue as playlist + Play Next / Add to Queue  → build 1.37 (code 26)
`PlaybackController.playTrackNext` + `buildMediaItem` helper; menu items added to LibraryTrackItem
(wired in Library Songs, Search, SearchSheet). QueueSheet header save action →
`createManualPlaylistWithTracks`.

### ✅ 8. Scan folder exclusions  → build 1.38 (code 27)
`TrackRepository` excluded-folders pref + scan skip; Settings → Hidden folders sheet
(`FoldersSheet`) toggles + re-scans.

### ✅ 5. Tag management  → build 1.39 (code 28)
Rename / recolor / delete (with crossref cleanup). Settings → Manage tags (`TagManagerSheet`).
Merge + bulk-tagging deferred (need multi-select UI).

## Remaining — heavier / riskier

### ✅ Right-side feature panel  → build 1.41 (code 30)
Right-edge navigation drawer (RTL-flip trick on ModalNavigationDrawer) opened by the top-right
logo on Home & Library (+ right-edge swipe). Hosts: library header (logo + tracks/hours),
Listening stats, Equalizer, Sleep timer (new standalone `SleepTimerSheet`), Hidden folders,
Manage tags, Backup/Restore, and quick-jump rows that PLAY a playlist or a tag's tracks.
- Files: `ui/components/AppSidePanel.kt` (+ `RightNavigationDrawer`), `ui/settings/SleepTimerSheet.kt`.
- Consolidation: settings/stats/folders/tags sheets moved from LibraryScreen to MainScreen root;
  `SettingsSheet.kt` deleted (panel supersedes it). Library/Home logo now opens the panel.

### ⬜ 9. Home-screen widget / Quick Settings tile  → next build
### ✅ 4. Android Auto support  → build 1.40 (code 29)
`FreqvoxPlaybackService` migrated `MediaSessionService` → `MediaLibraryService` with a
`MediaLibrarySession` + browse tree (Root → All Songs / Playlists / Tags → items). `onAddMediaItems`
resolves mediaId → playback URI; `onConnect` grants the custom like command. Manifest: browse
intent-filter (`MediaLibraryService` + `MediaBrowserService`) + `com.google.android.gms.car.application`
meta-data → `res/xml/automotive_app_desc.xml`. In-app MediaController path unchanged.
**Verified with full `assembleReleaseDebug` (R8 + manifest merge), not just compile. Needs on-device/Auto test.**

## Tier 4 — Nice to have / consider
### ⬜ 10. Online synced lyrics (opt-in, keep privacy stance)
### ⬜ 11. First-launch onboarding
### ⬜ 12. Fuzzy search + search history

---

## Bugfixes  → build 1.42 (code 31)
- **FeedGenerator "Fresh This Week" never showed** (pre-existing): `dateAdded` is MediaStore
  seconds but was compared to a millisecond threshold → filter always false. Added
  `sevenDaysAgoSec` and used it. Other `dateAdded` uses are sort-only (unit-agnostic) = fine.
- **Regression from the Android Auto migration (1.40), found during review**: service
  `onAddMediaItems` rebuilt every numeric-id item via `toPlayableItem()`, which dropped the
  `isAutoAppended` extra that `cycleRepeatMode` uses to strip autoplay tracks. Fixed to only
  resolve URI-less items (Android Auto browse items) and pass complete in-app items through.

## Embedded album art everywhere  → build 1.43 (code 32)
New `TrackArtwork` composable (HomeScreen): real embedded cover when `artworkPath != null`,
procedural pattern as the base/fallback layer. Used in Home (grid/cards/carousels), Library
(songs/albums/artists), and the full player (cover replaces the visualizer when art exists).
Still no online art fetch — embedded only.

## Audit fixes  → builds 1.44–1.47 (code 33–36)
- **#1 playCount inflation (1.44):** `logPlayTime` now counts a play once per track via a
  `playCounted` flag (reset on new track); later segments use new `TrackDao.addPlayTime`
  (time only, no count). Fixes Most Played / Heavy Rotation / Stats totals.
- **#2 Next ≠ skip (1.45):** new `PlaybackController.nextTrack()` records a skip only if you
  bail < 15s in; both full-player and mini-player Next/swipe use it. `skipNext()` (always-skip)
  retained for the dislike auto-skip.
- **#5 Non-destructive folder exclusion (1.46):** scan no longer deletes excluded folders'
  tracks; MainViewModel filters them out of `allTracks`/`activeTracks` instead, `availableFolders`
  reads the unfiltered library, toggle no longer rescans. Re-including is now lossless.
- **#6 Tag delete cleans smart playlists (1.47):** `PlaylistRepository.removeTagFromQueries`
  strips the deleted tag id from every query playlist; called from `deleteTag`.

## Notes / decisions
- JSON via `org.json` (no new dependency; matches RadioViewModel).
- Settings entry point: the circular logo avatar in the Library header (was inert) → Settings sheet.
- Backup/restore uses SAF (`CreateDocument`/`OpenDocument`) so no storage permission is needed.
