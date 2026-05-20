FREQVOX
Personal Android Music Player
App Specification  ·  Version 1.0  ·  2026
Frequency + Voice — your library, alive.
Developer
Patrick  ·  Signiqe / StarfiniteX
Platform
Android — Kotlin + Jetpack Compose
Device
Infinix NOTE Edge  ·  256 GB
Domain
freqvox.com
Type
Hobby  ·  Personal  ·  Offline-first
Table of Contents
1.
App Overview & Vision
2.
UI Design & Aesthetic
3.
Dynamic Palette Theming
4.
Navigation & Tabs
5.
Player Screen
6.
Home / Discover Screen
7.
Library Screen
8.
Search Screen
9.
Radio Screen
10.
Tagging System
11.
Smart Local Features
12.
Audio Engine & Playback
13.
Library Scanning & Metadata
14.
Data Layer
15.
Tech Stack
16.
Development Environment
17.
Development Workflow
18.
Out of Scope
1. App Overview & Vision
Freqvox is a personal, offline-first Android music player built for a single user with a large local library. The philosophy is simple: a beautiful, fast, personal player that feels premium without any backend, subscriptions, or social mechanics. Think Poweramp — but rebuilt from scratch with a distinct identity and exactly the features that matter.
App Name
Freqvox
Meaning
Frequency + Voice — your library given a voice
Domain
freqvox.com
Nature
Hobby project, personal use, solo developer
Target Device
Infinix NOTE Edge, 256 GB local storage
Platform
Android only — iOS not in scope (AI translation if ever needed later)
Architecture
Offline-first, file-based, no cloud, no backend, no accounts
2. UI Design & Aesthetic
The visual identity of Freqvox is dark synthwave — deep space blacks, vivid purples, terracotta accents, and neon greens. The UI feels premium and alive, not generic. Every screen shares a cohesive dark atmosphere that shifts dynamically based on the currently playing song.
Design Language
Theme
Dark — near-black backgrounds, never pure #000000
Primary Color
#6B3FA0 deep purple (static brand color)
Accent 1
#7c4dff violet — interactive elements, progress, active states
Accent 2
#ff4d7c terracotta-pink — likes, live indicators, secondary CTA
Accent 3
#4dffb4 neon green — volume, positive states
Typography
Syne (display/headings) + JetBrains Mono (timestamps, counters)
Surfaces
#0a0a0f base > #12121a surface > #1a1a26 elevated > #22222f borders
Corner Radius
8dp small, 14dp cards, 20dp large panels, 50dp pill tags
Elevation
Shadow + subtle border (1px #ffffff10) instead of heavy drop shadows
Prototype Reference
A full HTML/CSS interactive prototype of Freqvox has been built covering all 5 tabs with animations, mini player, waveform scrubber, and dynamic color system. This prototype is the definitive visual reference for the Kotlin/Compose implementation.
3. Dynamic Palette Theming
Freqvox implements YouTube Music-style dynamic theming — the entire UI shifts color personality per song, making the app feel alive and deeply connected to the music being played. This is the single most impactful visual feature of the app.
How It Works
Album artwork loaded as Bitmap from embedded ID3 tag via MediaMetadataRetriever — fully offline, zero network calls
Bitmap fed to androidx.palette — Palette.from(bitmap).generate()
Dominant or vibrant swatch extracted as the seed color
Raw color HSL-clamped — saturation reduced, lightness darkened ~60-70% — so bright artwork never produces garish backgrounds
Processed color becomes gradient background origin, propagating across: player background, status bar tint, tab bar surface, mini player surface, control icon tints
Gradient is radial or angled — saturated behind the album art, fading to near-black toward the bottom
Song change triggers animated crossfade via animateColorAsState — never a hard color swap
Implementation Details
Library
androidx.palette:palette-ktx
Metadata
MediaMetadataRetriever.getEmbeddedPicture() for artwork
Animation
animateColorAsState with tween(600ms) for smooth crossfade
Color Processing
Convert to HSL, clamp saturation 0.5-0.7, lightness 0.1-0.2
Fallback
Brand purple (#7c4dff) if no artwork or palette extraction fails
Surfaces Affected
Player BG, status bar, tab bar, mini player, card surfaces
4. Navigation & Tabs
Freqvox uses a bottom navigation bar with 5 tabs. A persistent mini player bar floats above the tab bar on all screens except the full player screen.
Player
Now Playing
Full now-playing screen — the heart of the app
Home
Discover
Recently added, featured playlists, quick picks, stats
Search
Browse
Search bar + browse by genre/category grid
Library
Collection
Full local library with sub-tab filtering
Radio
Live Stations
Live internet radio stations with stream URLs
Mini Player
A compact now-playing bar floats above the tab bar on Home, Search, Library, and Radio screens. Shows album art thumbnail, track title, artist, and playback controls (prev / play-pause / next). Tapping it opens the full Player screen. A thin progress bar runs along its bottom edge. The mini player inherits the dynamic palette color of the current song.
5. Player Screen
The full now-playing screen is the centrepiece of Freqvox — it occupies the entire screen and provides complete playback control with rich visual feedback.
Album Artwork
Large square artwork (300x300dp) centered on screen
Soft glow radial gradient behind artwork, color-matched to palette
Animated equalizer bars overlaid while playing — 7 bars, staggered animation speeds via infiniteTransition
Floating abstract shape animations inside artwork panel for additional visual depth
Track Info & Actions
Track title (large, bold) and artist name displayed below artwork
Like/heart button — one tap adds to Liked Songs tag, toggles with pop animation
Three-dot menu for track options: add to playlist, tag, share, file info
Waveform Scrubber
Custom waveform built with Canvas / DrawScope in Compose using drawRect
Played portion renders in accent gradient (purple to pink)
Unplayed portion renders in dark surface color
Tappable — tap to seek to any position
Progress timestamps: current time (left), total duration (right)
Playback Controls
Control row: Shuffle  -  Previous  -  Play/Pause  -  Next  -  Repeat
Play/Pause button: 68dp, gradient-filled, glowing shadow
Shuffle and Repeat show active state in accent color
Volume & Extra Controls
Volume slider: neon green to purple gradient fill, speaker icons at each end
Extra controls row: Lyrics, Queue, EQ (system Intent), Sleep Timer, Share
6. Home / Discover Screen
Surfaces content intelligently using smart local logic — no algorithmic engine required.
Header
Contextual greeting based on time of day (Good morning / afternoon / evening)
User avatar / profile initial top right
Three stat cards: total songs, total hours listened, number of tag-group playlists
Featured Playlists Carousel
Horizontal scrollable row of playlist cards
Each card: gradient artwork, playlist name, track count
Sources: user-created playlists and auto-generated tag-query playlists
Recently Added Feed
Vertical list sorted by date added — newest first
Primary discovery mechanism — surfaces new downloads immediately
Each row: thumbnail, title, artist, duration, three-dot menu
7. Library Screen
Complete access to the local collection with flexible sub-tab filtering.
Sub-Tab Filter Row
Horizontally scrollable pill tabs across the top of the screen
Playlists — user-created and tag-generated dynamic playlists
Albums — tracks grouped by album metadata
Artists — tracks grouped by artist
Songs — full flat list, alphabetical with A-Z fast-scroll index
Downloads — sorted by date added, newest first
Podcasts — placeholder in v1, full support future consideration
Songs View
Right-edge alphabet index for fast scroll in large libraries (thousands of tracks)
Currently playing track highlighted in accent color
Folder View
Groups tracks by parent directory from MediaStore DATA field
Respects download tool folder organization structure
Nested folder support up to 2 levels deep
8. Search Screen
Search Bar
Prominent search input at top
Full-text local search across: title, artist, album, folder name
Results appear instantly as user types — debounced at ~300ms
Results grouped by type: Songs, Artists, Albums, Playlists
Browse Categories Grid
2-column grid of genre/mood category cards
Each card: color gradient, emoji, and category name
Tapping a category filters library by that tag
Initial categories: Electronic, Ambient, Hip-Hop, R&B, Rock, Classical, Afrobeats, Jazz
Categories expand dynamically as user creates more tags
9. Radio Screen
Live internet radio streaming alongside the local library. Uses ExoPlayer's HLS/direct stream support — no additional library required.
Featured Station
Hero card at top showing currently playing or featured station
Station name, genre tags, listener count, pulsing LIVE badge
Station List
Icon/logo, station name, genre, listener count, LIVE indicator per row
Stations stored locally as a curated list: URL, name, genre, icon
User can add custom stream URLs
Dynamic palette applies — station logo used as palette source when radio is active
10. Tagging System
The tagging system is the core intelligence layer of Freqvox. Since there is no ML or recommendation API, the user becomes the algorithm — assigning meaning to tracks through quick, low-friction interactions while listening. Over time this builds a rich personal taxonomy that powers dynamic playlists.
Tag Categories
Mood
hype  ·  chill  ·  melancholy  ·  focus  ·  nostalgic  ·  energetic
Context
gym  ·  commute  ·  background  ·  late night  ·  morning  ·  study
Era
90s  ·  2000s  ·  2010s  ·  recent — auto-suggested from file metadata year
Rating
Fire  ·  Good  ·  Meh — used for library curation and eventual purging
Custom
User can create any tag — no limit, all stored in Room DB
Interaction Gestures on Player Screen
Swipe UP on album art — opens quick tag panel overlay for instant tagging
Swipe LEFT on album art — skip + logs skip to skip history
Long press on album art — opens full tag manager for the track
Like button (heart) — one tap adds/removes the 'liked' tag
All tag interactions are non-disruptive — music never pauses
Tag Storage
Tags stored in Room DB keyed to MediaStore content URI / file path
Persistent across library rescans — keyed to stable ID, not position
Many-to-many: one track can have multiple tags, one tag can have many tracks
Tag assignment timestamps stored — enables 'recently tagged' queries
11. Smart Local Features
These features replace algorithmic recommendation with deterministic, transparent local logic. The user always knows exactly why a song appears in a given context.
Play More Like This
Available from three-dot menu on any track
Queries Room DB for tracks sharing the most tag overlap with the current track
Pure set intersection — no ML, no network
Example: track tagged [chill, late night, electronic] finds others with 2+ shared tags
Dynamic Tag Playlists
Combine tags with AND / NOT logic to generate playlists on the fly
Examples: 'hype + gym',  'chill + late night - melancholy',  'fire + commute'
Playlists saved as queries, not static lists — always reflect current tag state
Playlist regenerated fresh each time it is opened
Skip Tracker
Every skip logged with timestamp
After 3 skips within 30 days, track is soft-hidden from regular queues
Hidden tracks reviewable and recoverable from a 'Skipped' list in Library
Skip rate displayed in track info panel
Play Statistics
Per-track: play count, last played, total play time, skip count, skip rate
Global: total hours listened, most played artists, most played tracks
Stats shown on Home screen stat cards and in track detail view
Recently Added Feed
MediaStore DATE_ADDED descending — new downloads appear at top automatically
Primary discovery mechanism — replaces algorithmic 'new music' logic
12. Audio Engine & Playback
Core Engine
Library
Media3 (ExoPlayer) — androidx.media3
Background
MediaSessionService — lock screen, notification, headphone controls
Session
MediaSession — system integration for Bluetooth, Android Auto, etc.
Playback Features
Gapless Playback
Enabled via ExoPlayer built-in gapless support
Critical for albums and DJ mixes where silence between tracks breaks the experience
Crossfade
Configurable duration: default 3 seconds, user-adjustable 0-10s
Implemented via audio ducking between ExoPlayer instances or Media3 crossfade
ReplayGain / Volume Normalization
YouTube downloads vary significantly in loudness — normalization is essential
Reads ReplayGain tags from ID3 if present
Fallback: target loudness normalization via ExoPlayer audio processor pipeline
Speed Control
Playback speed: 0.5x to 2.0x in 0.25 steps
Useful for long mixes, podcasts, DJ sets
Sleep Timer
Durations: 15 / 30 / 45 / 60 / 90 min or custom
Gradual volume fade over final 30 seconds before stopping
Countdown badge visible on Player screen
Queue Management
Full queue view with drag-to-reorder
Remove individual tracks, clear queue, shuffle from any point
Add to queue: play next or add to end from any track context menu
System Integration
Lock screen controls via MediaSession
Notification with artwork, track info, and controls
Headphone button: single tap = play/pause, double = next, triple = previous
Home screen widgets: 4x1 (art + controls) and 4x2 (art + info + controls + progress)
13. Library Scanning & Metadata
MediaStore Scanning
ContentResolver queries MediaStore.Audio.Media — handles thousands of tracks efficiently
Fields: _ID, TITLE, ARTIST, ALBUM, DURATION, DATE_ADDED, DATA (path), ALBUM_ID
Incremental scan on app start — only new/changed files processed
On-demand rescan available from settings
Permission: READ_MEDIA_AUDIO (Android 13+) or READ_EXTERNAL_STORAGE (older)
Metadata Cleanup
YouTube downloads frequently have polluted title fields. A cleanup pass runs on first scan of each new track:
Strip patterns: (Official Video), (Official Audio), (4K), [HD], (Lyrics), etc.
Trim leading/trailing whitespace and punctuation
If artist field empty, attempt to parse 'Artist - Title' format from filename
Cleaned metadata stored in Room DB — original MediaStore data never modified
User can manually edit any field from the track detail view
Artwork Extraction
Embedded artwork via MediaMetadataRetriever.getEmbeddedPicture()
Decoded to Bitmap, cached to disk via Coil
Used for: player display, mini player thumbnail, list thumbnails, Palette extraction
Fallback: generated placeholder using track title initials on accent-colored background
14. Data Layer
All persistent data stored locally in Room (SQLite). No cloud sync, no backend, no accounts.
tracks
Cleaned metadata cache: id, title, artist, album, duration, date_added, path, artwork_path
tags
Tag definitions: id, name, color, created_at
track_tags
Join table (many-to-many): track_id, tag_id, assigned_at
play_history
Every play event: track_id, played_at, duration_played, completed (bool)
skip_history
Every skip: track_id, skipped_at, skipped_after_seconds
playlists
User-created + tag-query playlists: id, name, type (manual/query), query_json
playlist_tracks
Manual playlist membership: playlist_id, track_id, position
radio_stations
Saved stations: id, name, stream_url, genre, icon_url, is_custom (bool)
settings
User preferences: key-value store for all app configuration
15. Tech Stack
Language
Kotlin
Primary — idiomatic, modern, concise
UI Framework
Jetpack Compose
Declarative UI matching the design's reactive nature
Audio Engine
Media3 / ExoPlayer
androidx.media3 — gapless, crossfade, speed, streaming
Background Audio
MediaSessionService
Lock screen, notification, headphone, Android Auto
Database
Room (SQLite)
Tags, play history, stats, playlists — all local
Image Loading
Coil
Kotlin-first, Compose-native, disk caching for artwork
Palette
androidx.palette
Dominant color extraction from album artwork bitmaps
Metadata
MediaMetadataRetriever
Embedded artwork and tag extraction from audio files
Library Scan
MediaStore API
ContentResolver for efficient large-library indexing
DI
Hilt
Dependency injection — clean ViewModels/repositories
Async
Coroutines + Flow
Reactive streams, background scanning, DB queries
State
ViewModel + StateFlow
Lifecycle-aware state management
Navigation
Compose Navigation
Tab-based navigation with back stack
16. Development Environment
Development on Kubuntu Linux. All testing on physical device — no emulator needed.
Required Installations
Android Studio
Download .tar.gz from developer.android.com. Extract to /opt/android-studio, run bin/studio.sh. Do NOT use the snap version — sandboxing causes SDK path issues.
JDK 17
sudo apt install openjdk-17-jdk — Android Studio bundles its own JDK but system JDK prevents PATH conflicts with Claude Code.
Android SDK
Installed via Android Studio SDK Manager on first launch. Accept all licenses: sdkmanager --licenses
i386 libs
sudo apt install libc6:i386 libncurses5:i386 libstdc++6:i386 lib32z1 libbz2-1.0:i386
adb
Included with SDK platform-tools. Verify device connection with: adb devices
Node.js
Required for Claude Code CLI.
Claude Code
npm install -g @anthropic-ai/claude-code — point at project root to begin.
Environment Variables  (~/.bashrc)
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
Run: source ~/.bashrc after adding
Device Setup — Infinix NOTE Edge
Settings > About Phone > tap Build Number 7 times > Developer Options unlocked
Developer Options > enable USB Debugging
Developer Options > enable Wireless Debugging (wire-free dev after first USB pair)
Verify: adb devices — device should appear with status 'device'
17. Development Workflow
The Build Loop
Claude Code writes and edits Kotlin / Compose code from the project root
Android Studio Live Edit pushes Composable changes to device instantly — no recompile needed
Logic changes trigger full rebuild and redeploy — approximately 10-15 seconds
Logcat streams live device logs in Android Studio — Log.d() for runtime debugging
Layout Inspector shows live Composable tree from the running app on device
Wireless debugging active — phone sits beside laptop, no USB cable after initial pair
Estimated Timeline
Week 1
Player screen working with real local audio via Media3
Week 2
Library scanning, MediaStore indexing, Room DB schema, metadata cleanup
Week 3
Home and Library screens wired to real data
Week 4
Tagging system — Room schema, swipe gestures, quick tag overlay
Week 5
Dynamic palette theming, crossfade, gapless, volume normalization
Week 6
Search screen, skip tracker, play stats, dynamic tag playlists
Week 7
Radio screen, lock screen controls, widgets, sleep timer
Week 8
Polish — animations, edge cases, performance on large library
18. Out of Scope
The following are intentionally excluded to keep the project focused, buildable solo, and true to its offline-first personal nature.
Lyrics fetching
Requires third-party API, adds network complexity and rate limits
Custom Equalizer
Android built-in EQ launched via Intent — sufficient for personal use
Social / Sharing features
Personal player — not a social platform
Cloud sync / backup
No backend, no accounts, no cloud — data lives on device only
Account system
No login, no authentication layer of any kind
iOS / cross-platform
Android-only. AI-assisted translation available if needed in future
Streaming service APIs
No Spotify, Apple Music, or any streaming integration
Music discovery API
No Last.fm, MusicBrainz, or similar — all curation is local and manual
Podcast full features
Tab placeholder only in v1 — full support is a future consideration
Monetization
Hobby project — no commercial mechanics of any kind
Freqvox App Specification  ·  v1.0  ·  2026
Patrick  ·  Signiqe / StarfiniteX  ·  freqvox.com
Frequency + Voice — your library, alive.