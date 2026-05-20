package com.freqvox.playback

import android.os.CountDownTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepTimerManager @Inject constructor() {
    private var timer: CountDownTimer? = null
    
    private val _timeLeft = MutableStateFlow(0L)
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    fun startTimer(minutes: Int, onTimerFinished: () -> Unit) {
        timer?.cancel()
        val durationMs = minutes * 60 * 1000L
        
        _isTimerRunning.value = true
        timer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeft.value = millisUntilFinished
            }

            override fun onFinish() {
                _isTimerRunning.value = false
                _timeLeft.value = 0
                onTimerFinished()
            }
        }.start()
    }

    fun stopTimer() {
        timer?.cancel()
        _isTimerRunning.value = false
        _timeLeft.value = 0
    }
}
