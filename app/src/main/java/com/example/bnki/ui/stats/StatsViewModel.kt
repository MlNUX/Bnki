package com.example.bnki.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bnki.data.BnkiRepository
import com.example.bnki.data.StudyLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class DayCount(val label: String, val count: Int)

data class StatsState(
    val totalReviews: Int = 0,
    val correctRate: Int = 0,
    val streak: Int = 0,
    val perDay: List<DayCount> = emptyList(),
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BnkiRepository.from(app)
    private val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)

    val state: StateFlow<StatsState> =
        repo.observeLogsSince(since)
            .map { logs -> computeStats(logs) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsState())

    private fun computeStats(logs: List<StudyLog>): StatsState {
        if (logs.isEmpty()) return StatsState()
        val total = logs.size
        val correct = logs.count { it.quality >= 3 }
        val rate = (correct * 100) / total

        // Tage mit Aktivität (als Tages-Index).
        val activeDays = logs.map { dayIndex(it.reviewedAt) }.toSet()
        val streak = currentStreak(activeDays)

        // Letzte 7 Tage.
        val today = dayIndex(System.currentTimeMillis())
        val perDay = (6 downTo 0).map { offset ->
            val day = today - offset
            val count = logs.count { dayIndex(it.reviewedAt) == day }
            DayCount(label = weekdayLabel(day), count = count)
        }

        return StatsState(total, rate, streak, perDay)
    }

    private fun currentStreak(activeDays: Set<Long>): Int {
        var streak = 0
        var day = dayIndex(System.currentTimeMillis())
        // Wenn heute nichts gelernt wurde, ab gestern zählen.
        if (day !in activeDays) day -= 1
        while (day in activeDays) {
            streak += 1
            day -= 1
        }
        return streak
    }

    private fun dayIndex(millis: Long): Long =
        BnkiRepository.startOfDay(millis) / TimeUnit.DAYS.toMillis(1)

    private fun weekdayLabel(dayIdx: Long): String {
        val millis = dayIdx * TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(12)
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Mo"
            Calendar.TUESDAY -> "Di"
            Calendar.WEDNESDAY -> "Mi"
            Calendar.THURSDAY -> "Do"
            Calendar.FRIDAY -> "Fr"
            Calendar.SATURDAY -> "Sa"
            else -> "So"
        }
    }
}
