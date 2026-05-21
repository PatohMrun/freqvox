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

// Curated Kenyan stations — verified stream URLs, loaded without any API call
val KENYA_CURATED_STATIONS = listOf(
    RadioStation("ke_capital",   "Capital FM",         "https://eu10.fastcast4u.com/capitalfm",                    "Kenya", "98.4 FM · Hip Hop, R&B, Pop",          0xFF7C3AED),
    RadioStation("ke_classic",   "Classic 105",        "https://streams.radio.co/s6db01f11e/listen",               "Kenya", "105.2 FM · Classic Hits, Talk",          0xFF1E3A5F),
    RadioStation("ke_kiss",      "Kiss 100 FM",        "https://streams.radio.co/sa35c5cba5/listen",               "Kenya", "100.3 FM · Top 40, Pop",                 0xFF9D174D),
    RadioStation("ke_jambo",     "Radio Jambo",        "https://streams.radio.co/s837130df1/listen",               "Kenya", "97.5 FM · News, Sports, Swahili",        0xFF065F46),
    RadioStation("ke_citizen",   "Radio Citizen",      "https://edge.iono.fm/xice/citizen_fm_live.aac",            "Kenya", "National · News, Local Music, Swahili",  0xFF166534),
    RadioStation("ke_nrg",       "NRG Radio",          "https://streaming.nrg.radio/nrg",                         "Kenya", "97.1 FM · Youth, Urban, Afrobeats",      0xFF1D4ED8),
    RadioStation("ke_maisha",    "Radio Maisha",       "https://edge.iono.fm/xice/standard_rm_live.aac",           "Kenya", "National · Bongo, Swahili Pop",          0xFF831843),
    RadioStation("ke_ghetto",    "Ghetto Radio",       "https://s3.radio.co/sbefab6c14/listen",                   "Kenya", "89.5 FM · Reggae, Sheng, Youth",         0xFF064E3B),
    RadioStation("ke_spice",     "Spice FM",           "https://edge.iono.fm/xice/standard_spice_live.aac",        "Kenya", "94.4 FM · Soul, R&B",                    0xFF78350F),
    RadioStation("ke_nation",    "Nation FM",          "http://stream.radiojar.com/3by7s8eg65quv",                "Kenya", "96.3 FM · News, Talk",                   0xFF1D4ED8),
    RadioStation("ke_kbc",       "KBC English",        "https://kbc-ice.stream.co.ke/english.mp3",                "Kenya", "National · News, English",               0xFF1E3A5F),
    RadioStation("ke_inooro",    "Inooro FM",          "https://edge.iono.fm/xice/inooro_fm_live.aac",             "Kenya", "98.9 FM · Kikuyu",                       0xFF713F12),
    RadioStation("ke_kameme",    "Kameme FM",          "https://kamemefm-atunwadigital.streamguys1.com/kamemefm",  "Kenya", "101.1 FM · Kikuyu",                      0xFF713F12),
    RadioStation("ke_ramogi",    "Ramogi FM",          "https://edge.iono.fm/xice/ramogi_fm_live.aac",             "Kenya", "107.1 FM · Luo",                         0xFF064E3B),
    RadioStation("ke_kass",      "Kass FM",            "https://s3.radio.co/s9c647b0bc/listen",                   "Kenya", "89.1 FM · Kalenjin",                     0xFF7C2D12),
    RadioStation("ke_mulembe",   "Mulembe FM",         "https://edge.iono.fm/xice/mulembe_fm_live.aac",            "Kenya", "97.9 FM · Luhya",                        0xFF065F46),
    RadioStation("ke_musyi",     "Musyi FM",           "https://edge.iono.fm/xice/musyi_fm_live.aac",              "Kenya", "102.2 FM · Kamba",                       0xFF451A03),
    RadioStation("ke_chamgei",   "Chamgei FM",         "https://edge.iono.fm/xice/chamgei_fm_live.aac",            "Kenya", "90.4 FM · Kalenjin",                     0xFF4C1D95),
    RadioStation("ke_egesa",     "Egesa FM",           "https://edge.iono.fm/xice/egesa_fm_live.aac",              "Kenya", "94.6 FM · Kisii",                        0xFF1F2937),
    RadioStation("ke_wimwaro",   "Wimwaro FM",         "https://edge.iono.fm/xice/wimwaro_fm_live.aac",            "Kenya", "93.0 FM · Embu",                         0xFF713F12),
    RadioStation("ke_easyfm",    "Easy FM",            "https://stream.zeno.fm/wvw02zaqpxquv",                    "Kenya", "101.5 FM · Soul, Easy Listening",        0xFF831843),
    RadioStation("ke_coro",      "KBC Coro FM",        "https://kbc-ice.stream.co.ke/coro.mp3",                   "Kenya", "99.5 FM · Kikuyu",                       0xFF374151),
    RadioStation("ke_mayienga",  "KBC Mayienga FM",    "https://kbc-ice.stream.co.ke/mayienga.mp3",               "Kenya", "93.5 FM · Luo",                          0xFF164E63),
    RadioStation("ke_mwatu",     "KBC Mwatu FM",       "https://kbc-ice.stream.co.ke/mwatu.mp3",                  "Kenya", "104.3 FM · Kamba",                       0xFF064E3B),
    RadioStation("ke_pwani",     "KBC Pwani FM",       "https://kbc-ice.stream.co.ke/pwani.mp3",                  "Kenya", "104.7 FM · Swahili, Coastal",             0xFF065F46),
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

    fun selectGenre(genre: String) { _selectedGenre.value = genre }
    fun selectCountry(code: String?) { _selectedCountryCode.value = code }
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
                genre == "Top" ->
                    "$API_BASE/topclick?limit=$LIMIT&hidebroken=true$countryParam"
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
        }
}
