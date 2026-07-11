# Freqvox — What's new since 2.14

**2.14 (build 52) → 2.95 (build 133)** · 81 releases · July 2026

---

## 🎨 Make it yours — themes & accents
- **Full light theme** with a header sun/moon toggle (System / Light / Dark), followed by several contrast-polish passes so every surface, tag card and accent button reads correctly in both themes *(2.22–2.27)*
- **Custom accent colour** — pick from **16 swatches** that recolour buttons, switches and controls across the whole app, not just highlights *(2.39, 2.74, 2.81)*
- **Gradient pinning is your choice**: keep the background gradient dancing with each album's art, or pin it to your accent — two independent toggles *(2.81)*

## 📖 Stories — your listening, as a status ring
A tappable ring in the header, Instagram-style:
- **Story engine + full-screen viewer**: tap to skip, hold to pause, drag down to dismiss *(2.51–2.53)*
- **Weekly "Your week in Freqvox" recap** — minutes listened, day-by-day sparkline, top artist, on-repeat track, streak *(2.54)*
- **Milestone celebrations** — 7/30-day streaks, 500/1,000 plays, 100 plays of one song *(2.55)*
- **40+ rotating tip & explainer cards** covering every feature, with deep-link buttons straight into the thing being explained *(2.56, 2.63, 2.82)*
- **Contextual tips** that only appear while they apply to *you* ("42 songs untagged", "duplicates found") *(2.64)*
- **Composed multi-card drops** — an eventful day yields one rich story, not a queue of pings *(2.89)*
- **Fresh content daily** (occasional 2-day breather), WhatsApp-style hold-to-pause, and **share any card** as a poster image *(2.91)*
- Track-specific cards show real album art *(2.72)*

## 🎤 Lyrics
- **Synced lyrics** (.lrc) with auto-scrolling karaoke view and **tap a line to jump there** *(2.48)*
- **Lyrics are remembered** — import once and they stick across restarts and rescans; `.lrc` files next to your songs are picked up automatically *(2.92)*
- **Search online** — three sources deep (LRCLIB → NetEase → Lyrics.ovh), synced preferred, fetched once and kept forever *(2.92–2.93)*
- **Sync offset** — nudge lyrics ±0.5s to match your rip, remembered per song *(2.95)*

## 📻 Radio
- Live station streaming with genre and country browsing, **HTTPS-only** for safety *(2.61, 2.69)*
- **Kenya done right**: 29 probe-verified major stations, curated and instant *(2.85, 2.87, 2.88)*
- **Favourites** that survive genre switches — and ride along in backups *(2.86)*
- **Reliable loading**: station directory requests fail over across multiple servers, and error messages only blame your connection when you actually have none *(2.93)*

## 🔀 Playback & audio
- **Equal-power crossfade** rebuilt: no flicker, no volume fights, EQ applied to the crossfade tail *(2.15, 2.19, 2.31)*
- **Album mode** — tracks from the same album butt-join gaplessly instead of crossfading *(2.32)*
- **Volume normalization** — evens out loudness jumps on shuffle *(2.34)*
- **Sleep timer with gentle fade-out** + stop-after-album and stop-after-current-track *(2.40)*
- **Per-tag playback profiles** — a tag can carry its own EQ preset, applied automatically *(2.49)*
- **Playback speed control** in the player
- Smarter play counting: a play registers after 30 seconds of real listening — skimming doesn't inflate your stats *(2.91)*

## 🗂️ Library
- **Edit info** (title / artist / album) on any song, from any page — and your fixes **survive library rescans** *(2.50, 2.80, 2.90)*
- **Multi-select** — long-press to bulk-tag, bulk-add to playlist, or bulk-delete *(2.46)*
- **Duplicate finder** — spots songs sharing a title and artist, clean up in a tap *(2.45)*
- **Hidden folders** — hide ringtones or voice notes instantly, nothing deleted, un-hide anytime *(2.36)*
- **Rescan you can see**: songs stream into the UI while the scan runs, rescans are far faster, and a summary tells you exactly what changed *(2.91–2.92)*
- **Boolean tag search** — require *and* exclude tags ("chill AND workout, NOT throwback") in one unified Search with layered back navigation *(2.33, 2.78)*

## 🏠 Home feed
- **Spotlight** rotates through your top 15 most-played *(2.30)*
- **On This Day** — resurfaces what you played on this date before *(2.41)*
- **Daily Mixes** stay put all day, refresh tomorrow *(2.43)*
- **Jump Back In** offers the song you were last playing *(2.89)*

## 📊 Stats & insights
- **Listening streaks** and a weekly goal with progress *(2.42)*
- **Tap any top artist or tag** for its own breakdown *(2.44)*
- Clear timeframes, a day-by-day week chart, and listening-by-hour histogram *(2.91)*

## 💾 Backup — one file, everything
Backup grew from tags + playlists to a complete snapshot: **timestamped listening history, all app settings, equalizer state, accent, tag profiles, edit-info fixes, radio favourites, hidden folders, tab order and story progress** — restored non-destructively, so restoring never erases newer listening *(2.37, 2.38, 2.86, 2.91)*

## 📤 Sharing
- **Share any song as a poster image** *(2.47)*
- **Share your weekly recap** as a stats card *(2.57)*
- **Share the story card you're looking at** — every card renders as a themed poster *(2.91–2.92)*

## ⚙️ Quality, speed & polish
- Heavy work moved off the main thread — feed generation, library flows, artwork handling *(2.58, 2.76)*
- Leak, battery and hot-path fixes from a dedicated audit *(2.84)*
- HTTPS-only network policy app-wide *(2.61)*
- Launcher icon fits the adaptive-icon safe zone *(2.83)*
- Cover art from three online sources plus local recovery — with granular per-network controls *(2.20)*
- Settings panel reorganized into collapsible groups with a pinned identity header *(2.28, 2.94)*
- Dozens of fixes: playlist preservation across rescans, config-change-safe dialogs, accurate play-time accounting, and more

---

*Freqvox 2.95 (build 133) · © 2026 Signiqe*
