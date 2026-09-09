package com.ley.wordmemo.data.stats

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.statsStore: DataStore<Preferences> by preferencesDataStore(name = "study_stats")

/** 今日学习统计（跨天自动清零） */
data class TodayStats(
    val date: String = "",        // yyyy-MM-dd
    val reviewedToday: Int = 0,   // 今日复习/标记次数 (标熟 + 标忘记)
    val masteredToday: Int = 0,   // 今日标熟次数
)

/**
 * 每日学习进度（参考网页版学习进度追踪/每日目标）。
 * 数据仅存本地 DataStore，按天自动重置。
 */
@Singleton
class StudyStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val date = stringPreferencesKey("stats_date")
        val reviewed = intPreferencesKey("reviewed_today")
        val mastered = intPreferencesKey("mastered_today")
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val todayStats: Flow<TodayStats> = context.statsStore.data.map { p ->
        val storedDate = p[Keys.date] ?: ""
        if (storedDate != today()) {
            TodayStats(date = today()) // 跨天: 返回清零视图 (惰性重置, 写入发生在下一次记录时)
        } else {
            TodayStats(
                date = storedDate,
                reviewedToday = p[Keys.reviewed] ?: 0,
                masteredToday = p[Keys.mastered] ?: 0,
            )
        }
    }

    /** 记录一次复习 (known=true 表示标熟) */
    suspend fun recordReview(known: Boolean) {
        val d = today()
        context.statsStore.edit { p ->
            if (p[Keys.date] != d) {
                p[Keys.date] = d
                p[Keys.reviewed] = 1
                p[Keys.mastered] = if (known) 1 else 0
            } else {
                p[Keys.reviewed] = (p[Keys.reviewed] ?: 0) + 1
                if (known) p[Keys.mastered] = (p[Keys.mastered] ?: 0) + 1
            }
        }
    }

    /** 手动清零今日进度 (调试/演示用) */
    suspend fun resetToday() {
        context.statsStore.edit { p ->
            p[Keys.date] = today()
            p[Keys.reviewed] = 0
            p[Keys.mastered] = 0
        }
    }
}
