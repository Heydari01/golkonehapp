package com.example.greenhouse.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class JalaliDate(
    val jy: Int,
    val jm: Int,
    val jd: Int,
    val monthName: String = ""
)

object JalaliCalendar {
    val PERSIAN_MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val ENGLISH_DIGITS = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    private val PERSIAN_DIGITS = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(value: Any): String {
        val str = value.toString()
        val sb = StringBuilder()
        for (ch in str) {
            val idx = ENGLISH_DIGITS.indexOf(ch)
            if (idx != -1) {
                sb.append(PERSIAN_DIGITS[idx])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(value: String): String {
        val sb = StringBuilder()
        for (ch in value) {
            val idx = PERSIAN_DIGITS.indexOf(ch)
            if (idx != -1) {
                sb.append(ENGLISH_DIGITS[idx])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 335)
        var jy = if (gy > 1600) 979 else 0
        val adjustedGy = if (gy > 1600) gy - 1600 else gy
        val gy2 = if (gm > 2) adjustedGy + 1 else adjustedGy
        var days = 365 * adjustedGy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 - 80 + gd + gdm[gm - 1]

        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + (days % 31)
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + ((days - 186) % 30)
        }
        val mName = if (jm in 1..12) PERSIAN_MONTHS[jm - 1] else ""
        return JalaliDate(jy, jm, jd, mName)
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        var gy = if (jy <= 979) 0 else 1600
        val jy2 = jy - (if (jy <= 979) 0 else 979)
        var days = 365 * jy2 + (jy2 / 33) * 8 + (jy2 % 33 + 3) / 4 + 78 + jd + (if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186)
        gy += 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days--
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val salA = intArrayOf(
            0, 31,
            if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 29 else 28,
            31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        )
        var gm = 0
        for (i in 1..12) {
            if (gd <= salA[i]) {
                gm = i
                break
            }
            gd -= salA[i]
        }
        return Triple(gy, gm, gd)
    }

    fun getTodayJalali(): JalaliDate {
        val cal = Calendar.getInstance()
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun getTodayGregorianString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun parseDateToJalali(dateStr: String): JalaliDate {
        try {
            val clean = dateStr.split("T")[0]
            val parts = clean.split("-").map { it.toInt() }
            if (parts.size == 3) {
                return gregorianToJalali(parts[0], parts[1], parts[2])
            }
        } catch (_: Exception) {}
        return getTodayJalali()
    }

    fun formatToJalali(dateStr: String, format: String = "medium"): String {
        val jalali = parseDateToJalali(dateStr)
        var timeStr = ""
        if (dateStr.contains("T")) {
            val timePart = dateStr.split("T").getOrNull(1)
            if (timePart != null) {
                val tParts = timePart.split(":")
                if (tParts.size >= 2) {
                    timeStr = " (ساعت ${tParts[0]}:${tParts[1]})"
                }
            }
        }

        val mStr = if (jalali.jm < 10) "۰${jalali.jm}" else toPersianDigits(jalali.jm)
        val dStr = if (jalali.jd < 10) "۰${jalali.jd}" else toPersianDigits(jalali.jd)
        val yStr = toPersianDigits(jalali.jy)

        return when (format) {
            "short" -> {
                val shortYear = toPersianDigits(jalali.jy.toString().takeLast(2))
                "$shortYear/$mStr/$dStr$timeStr"
            }
            "long" -> "${toPersianDigits(jalali.jd)} ${jalali.monthName} $yStr$timeStr"
            else -> "$yStr/$mStr/$dStr$timeStr"
        }
    }

    fun formatCurrency(amount: Long): String {
        val isNegative = amount < 0
        val absVal = abs(amount)
        val formatter = DecimalFormat("#,###")
        val formattedAbs = formatter.format(absVal)
        val persianStr = toPersianDigits(formattedAbs)
        return if (isNegative) "($persianStr) تومان" else "$persianStr تومان"
    }

    fun numberToPersianWords(num: Long): String {
        if (num == 0L) return "صفر"
        val yekan = listOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه")
        val dahgan = listOf("", "ده", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
        val dahTaBist = listOf("ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده")
        val sadgan = listOf("", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
        val vahed = listOf("", "هزار", "میلیون", "میلیارد", "تریلیون")

        val chunks = mutableListOf<Long>()
        var temp = abs(num)
        while (temp > 0) {
            chunks.add(temp % 1000)
            temp /= 1000
        }

        fun chunkToWords(n: Long): String {
            if (n == 0L) return ""
            val sb = StringBuilder()
            val s = (n / 100).toInt()
            if (s > 0) {
                sb.append(sadgan[s])
            }
            val remaining = (n % 100).toInt()
            if (remaining > 0) {
                if (sb.isNotEmpty()) sb.append(" و ")
                if (remaining in 10..19) {
                    sb.append(dahTaBist[remaining - 10])
                } else {
                    val d = remaining / 10
                    val y = remaining % 10
                    if (d > 0) {
                        sb.append(dahgan[d])
                        if (y > 0) sb.append(" و ").append(yekan[y])
                    } else if (y > 0) {
                        sb.append(yekan[y])
                    }
                }
            }
            return sb.toString()
        }

        val chunkWords = mutableListOf<String>()
        for (i in chunks.indices) {
            val chunk = chunks[i]
            if (chunk > 0) {
                val words = chunkToWords(chunk)
                val suffix = vahed.getOrNull(i) ?: ""
                chunkWords.add(if (suffix.isNotEmpty()) "$words $suffix" else words)
            }
        }

        val result = chunkWords.reversed().joinToString(" و ").trim()
        return if (num < 0) "منفی $result" else result
    }
}
