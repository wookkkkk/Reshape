package com.woo.kuiklycomposetemplate.core.utils

/**
 * 年月数据结构
 */
data class YearMonth(
    val year: Int,
    val month: Int // 1 ~ 12
) {
    fun previous(): YearMonth {
        return if (month == 1) YearMonth(year - 1, 12)
        else YearMonth(year, month - 1)
    }

    fun next(): YearMonth {
        return if (month == 12) YearMonth(year + 1, 1)
        else YearMonth(year, month + 1)
    }

    val displayTitle: String get() = "${year} 年 ${month} 月"
}

/**
 * 跨平台纯 Kotlin 真实公历日历与日期推算算法
 */
object CalendarUtils {

    /**
     * 判断是否是闰年
     */
    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    /**
     * 获取指定年月的真实总天数 (28 ~ 31)
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 31
        }
    }

    /**
     * 计算指定年月日是星期几 (基姆拉尔森公式)
     * 返回值：1 = 周一, 2 = 周二, ..., 7 = 周日
     */
    fun getDayOfWeek(year: Int, month: Int, day: Int): Int {
        var y = year
        var m = month
        if (m == 1 || m == 2) {
            m += 12
            y--
        }
        // 基姆拉尔森公式: 0=周日, 1=周一, 2=周二, ..., 6=周六
        val w = (day + 2 * m + 3 * (m + 1) / 5 + y + y / 4 - y / 100 + y / 400 + 1) % 7
        return if (w == 0) 7 else w
    }

    /**
     * 计算指定年月 1 号是星期几 (1 = 周一 .. 7 = 周日)
     */
    fun getFirstDayOfWeekInMonth(year: Int, month: Int): Int {
        return getDayOfWeek(year, month, 1)
    }

    /**
     * 从时间戳 (毫秒) 精确解析年月日 (默认东八区北京时间 UTC+8)
     */
    fun parseTimestamp(timestampMillis: Long, timeZoneOffsetHours: Int = 8): Triple<Int, Int, Int> {
        val adjustedMillis = timestampMillis + timeZoneOffsetHours * 3600 * 1000L
        val daysSinceEpoch = adjustedMillis / (1000L * 60 * 60 * 24)

        val z = daysSinceEpoch + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1024 + doe / 1461 - doe / 14244) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val m = (mp + (if (mp < 10) 3 else -9)).toInt()
        val finalYear = (y + (if (m <= 2) 1 else 0)).toInt()

        return Triple(finalYear, m, d)
    }

    /**
     * 获取今天所在的真实年月日
     */
    fun getToday(): Triple<Int, Int, Int> {
        val now = platformCurrentTimeMillis()
        return parseTimestamp(now)
    }

    /**
     * 获取当前真实年月
     */
    fun getCurrentYearMonth(): YearMonth {
        val (y, m, _) = getToday()
        return YearMonth(y, m)
    }

    /**
     * 将指定的年月日 (UTC+8) 转换为毫秒时间戳
     */
    fun toTimestampMillis(year: Int, month: Int, day: Int, hour: Int = 18, minute: Int = 0): Long {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val era = if (y >= 0) y / 400 else (y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (m - 3) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        val daysSinceEpoch = era * 146097 + doe - 719468
        return (daysSinceEpoch * 86400000L) + (hour - 8) * 3600000L + minute * 60000L
    }

    /**
     * 动态生成指定年月的 7 列日历网格 (周一至周日)
     * 例如第一行 1 号是周三，则 [null, null, 1, 2, 3, 4, 5]
     */
    fun generateMonthGrid(year: Int, month: Int): List<List<Int?>> {
        val totalDays = getDaysInMonth(year, month)
        val firstDayOfWeek = getFirstDayOfWeekInMonth(year, month) // 1=周一, 7=周日

        val result = mutableListOf<List<Int?>>()
        var currentWeek = mutableListOf<Int?>()

        // 填充第一周 1 号之前的空位 (1=周一 补 0 个，2=周二 补 1 个...)
        val leadingNulls = firstDayOfWeek - 1
        repeat(leadingNulls) {
            currentWeek.add(null)
        }

        for (day in 1..totalDays) {
            currentWeek.add(day)
            if (currentWeek.size == 7) {
                result.add(currentWeek)
                currentWeek = mutableListOf()
            }
        }

        // 填充最后一周剩余的空位
        if (currentWeek.isNotEmpty()) {
            while (currentWeek.size < 7) {
                currentWeek.add(null)
            }
            result.add(currentWeek)
        }

        return result
    }
}
