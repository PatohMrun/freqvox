package com.freqvox.ui.radio

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

data class RadioStation(
    val id: String,
    val name: String,
    val url: String,
    val genre: String,
    val description: String,
    val accentColor: Long
)

data class CountryOption(val code: String, val label: String)

// Curated Kenyan stations — every URL below was probe-verified (HTTP 200 + audio/* content
// type) on 2026-07-07. The previous list had rotted: Royal Media (Citizen/Inooro/Ramogi/
// Mulembe/Musyi/Chamgei/Egesa/Wimwaro), Radio Africa (Classic 105/Kiss/Jambo), Standard
// (Maisha/Spice) and KBC pulled their public streams behind their own apps — those return
// 403/404 everywhere, including the radio-browser directory, so they're dropped rather than
// listed as dead play buttons.
val KENYA_CURATED_STATIONS = listOf(
    RadioStation("ke_capital",  "Capital FM",           "https://atunwadigital.streamguys1.com/capitalfm",          "Kenya", "98.4 FM · Hip Hop, R&B, Pop",       0xFF7C3AED),
    RadioStation("ke_kameme",   "Kameme FM",            "https://kamemefm-atunwadigital.streamguys1.com/kamemefm",  "Kenya", "101.1 FM · Kikuyu",                 0xFF713F12),
    RadioStation("ke_nrg",      "NRG Radio",            "https://streamingv2.shoutcast.com/nrg-radio-ke",           "Kenya", "Youth, Urban, Afrobeats",           0xFF1D4ED8),
    RadioStation("ke_nation",   "Nation FM",            "http://stream.radiojar.com/3by7s8eg65quv",                 "Kenya", "96.3 FM · News, Talk",              0xFF1E3A5F),
    RadioStation("ke_easyfm",   "Easy FM",              "https://stream.zeno.fm/wvw02zaqpxquv",                     "Kenya", "101.5 FM · Soul, Easy Listening",   0xFF831843),
    RadioStation("ke_ghetto",   "Ghetto Radio",         "https://stream.zeno.fm/kvudezx1h2zuv",                     "Kenya", "89.5 FM · Reggae, Sheng, Youth",    0xFF064E3B),
    RadioStation("ke_radio47",  "Radio 47",             "https://streaming.shoutcast.com/radio-47",                 "Kenya", "Hapa Ndipo! · Swahili Hits",        0xFF9D174D),
    RadioStation("ke_taach",    "Taach FM",             "https://pegasus.nucast.co.uk:8064/stream",                 "Kenya", "Kalenjin · Music & Talk",           0xFF7C2D12),
    RadioStation("ke_jil",      "Jesus Is Lord Radio",  "https://s3.radio.co/s97f38db97/listen",                    "Kenya", "Gospel · Christian",                0xFF166534),
    RadioStation("ke_kenya1",   "Kenya1 FM",            "https://stream.zeno.fm/cmgkmed5u18uv",                     "Kenya", "News, Comedy, Talk",                0xFF374151),
    RadioStation("ke_jembe",    "Jembe FM",             "https://cast3.asurahosting.com/proxy/jembemed/stream",     "Kenya", "Swahili · Coastal Vibes",           0xFF065F46),
    RadioStation("ke_emmachev", "Emmachev Radio",       "https://radio.emmachev.com/listen/emmachev-radio/radio.mp3", "Kenya", "Urban, Indie",                    0xFF451A03),
    RadioStation("ke_at254",    "AT254 Radio",          "https://s127.radiolize.com/radio/8100/radio.mp3",          "Kenya", "Online · Urban Hits",               0xFF4C1D95),
    RadioStation("ke_campus",   "Campus Radio",         "https://stream.zenolive.com/mayn36v1v8quv.aac",            "Kenya", "Campus · Youth",                    0xFF78350F),
)

// African countries first, then global; null = World (no filter)
val COUNTRY_OPTIONS: List<CountryOption?> = listOf(
    null,
    CountryOption("KE", "Kenya"),
    CountryOption("NG", "Nigeria"),
    CountryOption("ZA", "S. Africa"),
    CountryOption("GH", "Ghana"),
    CountryOption("UG", "Uganda"),
    CountryOption("TZ", "Tanzania"),
    CountryOption("EG", "Egypt"),
    CountryOption("ET", "Ethiopia"),
    CountryOption("GB", "UK"),
    CountryOption("US", "USA"),
    CountryOption("DE", "Germany"),
    CountryOption("FR", "France"),
    CountryOption("BR", "Brazil"),
    CountryOption("IN", "India"),
    CountryOption("AU", "Australia"),
    CountryOption("CA", "Canada"),
    CountryOption("JP", "Japan"),
)

private val GENRE_COLORS = mapOf(
    "Top"        to 0xFF7C3AED,
    "Favorites"  to 0xFFFF4D7C,
    "Synthwave"  to 0xFF7C3AED,
    "Ambient"    to 0xFF065F46,
    "Chill"      to 0xFF831843,
    "Electronic" to 0xFF1D4ED8,
    "Indie"      to 0xFF166534,
    "Jazz"       to 0xFF78350F,
    "Classical"  to 0xFF1E3A5F,
    "Rock"       to 0xFF7C2D12,
    "Pop"        to 0xFF9D174D,
    "Metal"      to 0xFF1F2937,
    "Hip-Hop"    to 0xFF451A03,
    "Country"    to 0xFF713F12,
    "News"       to 0xFF1E3A5F,
    "Talk"       to 0xFF374151,
    "Sports"     to 0xFF064E3B,
)

private fun colorForGenre(genre: String): Long = GENRE_COLORS[genre] ?: 0xFF4C1D95

private val GENRE_TAGS = mapOf(
    "Top"        to null,
    "Favorites"  to null, // handled locally
    "Electronic" to "electronic",
    "Ambient"    to "ambient",
    "Synthwave"  to "synthwave",
    "Chill"      to "chillout",
    "Indie"      to "indie",
    "Jazz"       to "jazz",
    "Classical"  to "classical",
    "Rock"       to "rock",
    "Pop"        to "pop",
    "Metal"      to "metal",
    "Hip-Hop"    to "hip-hop",
    "Country"    to "country",
    "News"       to "news",
    "Talk"       to "talk",
    "Sports"     to "sports",
)

val ALL_GENRES: List<String> = listOf("Top", "Favorites") + GENRE_TAGS.keys.filter { it != "Top" && it != "Favorites" }

private const val API_BASE = "https://de1.api.radio-browser.info/json/stations"
private const val LIMIT = 50

@HiltViewModel
class RadioViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("radio_favorites", Context.MODE_PRIVATE)

    // ── Favorites stored as full station data so they survive genre switches ──
    private val _favoriteStations = MutableStateFlow(loadSavedFavorites())
    val favoriteStations: StateFlow<List<RadioStation>> = _favoriteStations.asStateFlow()

    private val _favorites = MutableStateFlow(_favoriteStations.value.map { it.id }.toSet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _stations = MutableStateFlow<List<RadioStation>>(emptyList())
    val stations: StateFlow<List<RadioStation>> = _stations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedGenre = MutableStateFlow("Top")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    // Default to Kenya — hardcoded preference, no GPS required
    private val _selectedCountryCode = MutableStateFlow<String?>("KE")
    val selectedCountryCode: StateFlow<String?> = _selectedCountryCode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Tracks the last station sent to the player so now-playing card survives genre switches
    private val _playingStation = MutableStateFlow<RadioStation?>(null)
    val playingStation: StateFlow<RadioStation?> = _playingStation.asStateFlow()

    init {
        combine(
            _selectedGenre,
            _selectedCountryCode,
            _searchQuery.debounce(500)
        ) { genre, country, query -> Triple(genre, country, query) }
            .distinctUntilChanged()
            .onEach { (genre, country, query) -> doLoad(genre, country, query) }
            .launchIn(viewModelScope)
    }

    fun selectGenre(genre: String) {
        if (_selectedGenre.value == genre && genre != "Top") {
            _selectedGenre.value = "Top"
        } else {
            _selectedGenre.value = genre
        }
    }

    fun selectCountry(code: String?) {
        if (_selectedCountryCode.value == code && code != null) {
            _selectedCountryCode.value = null
        } else {
            _selectedCountryCode.value = code
        }
    }
    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setPlayingStation(station: RadioStation) { _playingStation.value = station }

    fun toggleFavorite(station: RadioStation) {
        val id = station.id
        val isFav = id in _favorites.value
        val newFavs = if (isFav) _favorites.value - id else _favorites.value + id
        _favorites.value = newFavs
        val newList = if (isFav) {
            _favoriteStations.value.filter { it.id != id }
        } else {
            _favoriteStations.value + station
        }
        _favoriteStations.value = newList
        persistFavorites(newList)
        // The Favorites tab renders _stations, which doLoad only refreshes on genre/country/
        // query changes — so unfavoriting while ON that tab left the row visible (a stale
        // snapshot). Mirror the change into the visible list immediately.
        if (_selectedGenre.value == "Favorites") _stations.value = newList
    }

    private suspend fun doLoad(genre: String, countryCode: String?, query: String) {
        if (genre == "Favorites") {
            _stations.value = _favoriteStations.value
            return
        }
        // Serve the curated Kenya list instantly — no API round-trip needed
        if (genre == "Top" && countryCode == "KE" && query.isBlank()) {
            _stations.value = KENYA_CURATED_STATIONS
            return
        }
        _isLoading.value = true
        _error.value = null
        try {
            val countryParam = if (countryCode != null) "&countrycode=${URLEncoder.encode(countryCode, "UTF-8")}" else ""
            val urlString = when {
                query.isNotBlank() -> {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    "$API_BASE/search?name=$encoded&limit=$LIMIT&hidebroken=true&order=clickcount&reverse=true$countryParam"
                }
                genre == "Top" -> {
                    if (countryCode != null) {
                        "$API_BASE/bycountrycodeexact/${URLEncoder.encode(countryCode, "UTF-8")}?limit=$LIMIT&hidebroken=true&order=clickcount&reverse=true"
                    } else {
                        "$API_BASE/topclick?limit=$LIMIT&hidebroken=true"
                    }
                }
                else -> {
                    val tag = URLEncoder.encode(GENRE_TAGS[genre] ?: genre.lowercase(), "UTF-8")
                    "$API_BASE/bytag/$tag?limit=$LIMIT&hidebroken=true&order=clickcount&reverse=true$countryParam"
                }
            }
            _stations.value = fetchStations(urlString, genre)
        } catch (e: Exception) {
            _error.value = "Couldn't load stations — check your connection."
            _stations.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun fetchStations(urlString: String, genre: String): List<RadioStation> =
        withContext(Dispatchers.IO) {
            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Freqvox/1.0")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            try {
                val body = conn.inputStream.bufferedReader().readText()
                val arr = JSONArray(body)
                val result = mutableListOf<RadioStation>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val resolvedUrl = obj.optString("url_resolved").ifBlank { obj.optString("url") }
                    if (resolvedUrl.isBlank()) continue
                    // HTTPS-only: the network policy blocks cleartext (except the curated radiojar
                    // host), so an http:// station from the API would just be a dead play button.
                    // Filter them out rather than list stations that can't play.
                    if (!resolvedUrl.startsWith("https://", ignoreCase = true)) continue
                    val name = obj.optString("name").trim()
                    if (name.isBlank()) continue
                    val stationId = obj.optString("stationuuid").ifBlank { name }
                    val tags = obj.optString("tags")
                    val country = obj.optString("country")
                    val description = tags.split(",").take(2)
                        .map { it.trim() }.filter { it.isNotBlank() }
                        .joinToString(" · ")
                        .ifBlank { country.ifBlank { genre } }
                    result.add(RadioStation(stationId, name, resolvedUrl, genre, description, colorForGenre(genre)))
                }
                result
            } finally {
                conn.disconnect()
            }
        }

    // ── Favorites persistence (full station data, tab-delimited) ──────────────

    private fun persistFavorites(stations: List<RadioStation>) {
        val entries = stations.map {
            listOf(it.id, it.name, it.url, it.genre, it.description, it.accentColor.toString())
                .joinToString("\t")
        }.toSet()
        prefs.edit().putStringSet("station_data", entries).apply()
    }

    private fun loadSavedFavorites(): List<RadioStation> =
        (prefs.getStringSet("station_data", emptySet()) ?: emptySet()).mapNotNull { entry ->
            val p = entry.split("\t")
            if (p.size >= 6) RadioStation(p[0], p[1], p[2], p[3], p[4], p[5].toLongOrNull() ?: 0xFF7C3AED)
            else null
        }.filter { playableUnderNetworkPolicy(it.url) }

    /** True when the stream survives the HTTPS-only network policy (cleartext is allowed solely
     *  for the curated radiojar host). Drops previously-favorited http stations that can no
     *  longer play, instead of showing dead buttons. */
    private fun playableUnderNetworkPolicy(url: String): Boolean =
        url.startsWith("https://", ignoreCase = true) ||
            (url.startsWith("http://", ignoreCase = true) && url.contains("radiojar.com", ignoreCase = true))
}
