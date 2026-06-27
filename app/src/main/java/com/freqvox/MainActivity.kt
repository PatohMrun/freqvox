package com.freqvox

import android.Manifest
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freqvox.data.prefs.ThemeMode
import com.freqvox.data.prefs.ThemePreferences
import com.freqvox.playback.PlaybackController
import com.freqvox.ui.main.MainScreen
import com.freqvox.ui.main.MainViewModel
import com.freqvox.ui.theme.FreqvoxTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playbackController: PlaybackController

    @Inject
    lateinit var themePreferences: ThemePreferences

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.scanLibrary()
        }
        // Once the storage permission flow finishes, ask for notifications too
        // so the lock-screen / shade media controls can render on API 33+.
        maybeRequestNotificationPermission()
    }

    private val requestNotificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* user choice is fine either way — we just need to have asked */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        
        setContent {
            val themeMode by themePreferences.mode.collectAsStateWithLifecycle()
            val isLight = when (themeMode) {
                ThemeMode.LIGHT -> true
                ThemeMode.DARK -> false
                ThemeMode.SYSTEM -> !isSystemInDarkTheme()
            }
            FreqvoxTheme(isLight = isLight) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(playbackController = playbackController, isLight = isLight)
                }
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission)
        } else {
            maybeRequestNotificationPermission()
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val notif = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, notif) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationLauncher.launch(notif)
        }
    }

    // ── Auto-recheck library when songs are added/removed externally ────────────
    // While the app is foreground, watch MediaStore for audio changes (a download
    // finishing, a file deleted elsewhere) and trigger an incremental rescan. The
    // rescan only upserts changed rows and the art backfill only fetches covers for
    // genuinely-new tracks, so new songs get their art automatically. Debounced so a
    // burst of changes (a bulk download) coalesces into one scan, and lifecycle-tied
    // so nothing runs while backgrounded.
    private val mediaChangeHandler = Handler(Looper.getMainLooper())
    private var pendingRescan: Runnable? = null
    private var mediaObserver: ContentObserver? = null

    override fun onStart() {
        super.onStart()
        playbackController.reconnectIfNeeded()
        registerMediaObserver()
    }

    override fun onStop() {
        super.onStop()
        unregisterMediaObserver()
    }

    private fun registerMediaObserver() {
        if (mediaObserver != null) return
        val observer = object : ContentObserver(mediaChangeHandler) {
            override fun onChange(selfChange: Boolean) {
                pendingRescan?.let { mediaChangeHandler.removeCallbacks(it) }
                val rescan = Runnable { viewModel.scanLibrary() }
                pendingRescan = rescan
                mediaChangeHandler.postDelayed(rescan, MEDIA_RESCAN_DEBOUNCE_MS)
            }
        }
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer
        )
        mediaObserver = observer
    }

    private fun unregisterMediaObserver() {
        mediaObserver?.let { contentResolver.unregisterContentObserver(it) }
        mediaObserver = null
        pendingRescan?.let { mediaChangeHandler.removeCallbacks(it) }
        pendingRescan = null
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        // Long enough to coalesce a bulk download's MediaStore events into one scan.
        private const val MEDIA_RESCAN_DEBOUNCE_MS = 2500L
    }
}
