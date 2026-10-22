/*
 * New-moon and solar-coordinate formulae adapted from Sonia Keys' MIT-licensed
 * meeus implementation. Copyright 2013, 2018 Sonia Keys.
 * See core/THIRD_PARTY_NOTICES.md for the complete MIT permission notice.
 */
package vn.lichviet.core

import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.sin

/** Small, offline ephemeris for the supported 1900–2100 civil-calendar interval. */
internal object Astronomy {
    const val JD_UNIX_EPOCH = 2440587.5
    const val ZONE_DAYS = 7.0 / 24.0
    private const val SYNODIC_MONTH = 29.530588861
    private const val NEW_MOON_EPOCH = 2451550.09766

    fun julianDay(date: LocalDate): Double = date.toEpochDay() + JD_UNIX_EPOCH

    fun localDate(julianUt: Double): LocalDate =
        LocalDate.ofEpochDay(floor(julianUt + ZONE_DAYS - JD_UNIX_EPOCH).toLong())

    fun localMidnight(date: LocalDate): Double = julianDay(date) - ZONE_DAYS

    fun estimateLunation(julianUt: Double): Int =
        floor((julianUt - NEW_MOON_EPOCH) / SYNODIC_MONTH).toInt()

    private fun sinDeg(angle: Double): Double = sin(Math.toRadians(angle))

    /** Meeus chapter 49, true new moon. The returned Julian date uses UT. */
    fun newMoon(k: Int): Double {
        val n = k.toDouble()
        val t = n / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t
        val e = 1 - 0.002516 * t - 0.0000074 * t2
        val m = 2.5534 + 29.1053567 * n - 0.0000014 * t2 - 0.00000011 * t3
        val moon = 201.5643 + 385.81693528 * n + 0.0107582 * t2 +
            0.00001238 * t3 - 0.000000058 * t4
        val f = 160.7108 + 390.67050284 * n - 0.0016118 * t2 -
            0.00000227 * t3 + 0.000000011 * t4
        val omega = 124.7746 - 1.56375588 * n + 0.0020672 * t2 + 0.00000215 * t3
        val mean = NEW_MOON_EPOCH + SYNODIC_MONTH * n + 0.00015437 * t2 -
            0.00000015 * t3 + 0.00000000073 * t4
        val periodic = -0.40720 * sinDeg(moon) + 0.17241 * e * sinDeg(m) +
            0.01608 * sinDeg(2 * moon) + 0.01039 * sinDeg(2 * f) +
            0.00739 * e * sinDeg(moon - m) - 0.00514 * e * sinDeg(moon + m) +
            0.00208 * e * e * sinDeg(2 * m) - 0.00111 * sinDeg(moon - 2 * f) -
            0.00057 * sinDeg(moon + 2 * f) + 0.00056 * e * sinDeg(2 * moon + m) -
            0.00042 * sinDeg(3 * moon) + 0.00042 * e * sinDeg(m + 2 * f) +
            0.00038 * e * sinDeg(m - 2 * f) - 0.00024 * e * sinDeg(2 * moon - m) -
            0.00017 * sinDeg(omega) - 0.00007 * sinDeg(moon + 2 * m) +
            0.00004 * sinDeg(2 * (moon - f)) + 0.00004 * sinDeg(3 * m) +
            0.00003 * sinDeg(moon + m - 2 * f) + 0.00003 * sinDeg(2 * (moon + f)) -
            0.00003 * sinDeg(moon + m + 2 * f) + 0.00003 * sinDeg(moon - m + 2 * f) -
            0.00002 * sinDeg(moon - m - 2 * f) - 0.00002 * sinDeg(3 * moon + m) +
            0.00002 * sinDeg(4 * moon)
        val planetary = 0.000325 * sinDeg(299.77 + 0.107408 * n - 0.009173 * t2) +
            0.000165 * sinDeg(251.88 + 0.016321 * n) +
            0.000164 * sinDeg(251.83 + 26.651886 * n) +
            0.000126 * sinDeg(349.42 + 36.412478 * n) +
            0.000110 * sinDeg(84.66 + 18.206239 * n) +
            0.000062 * sinDeg(141.74 + 53.303771 * n) +
            0.000060 * sinDeg(207.14 + 2.453732 * n) +
            0.000056 * sinDeg(154.84 + 7.306860 * n) +
            0.000047 * sinDeg(34.52 + 27.261239 * n) +
            0.000042 * sinDeg(207.19 + 0.121824 * n) +
            0.000040 * sinDeg(291.34 + 1.844379 * n) +
            0.000037 * sinDeg(161.72 + 24.198154 * n) +
            0.000035 * sinDeg(239.56 + 25.513099 * n) +
            0.000023 * sinDeg(331.55 + 3.592518 * n)
        val jde = mean + periodic + planetary
        val approximateYear = 2000 + n / 12.3685
        return jde - deltaT(approximateYear) / 86400.0
    }

    /** Apparent geocentric ecliptic longitude, degrees normalized into [0,360). */
    fun solarLongitude(julianUt: Double): Double {
        val year = 2000 + (julianUt - 2451545.0) / 365.2425
        val t = (julianUt + deltaT(year) / 86400.0 - 2451545.0) / 36525.0
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
        val mean = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val equation = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sinDeg(m) +
            (0.019993 - 0.000101 * t) * sinDeg(2 * m) + 0.000289 * sinDeg(3 * m)
        return normalize(mean + equation - 0.00569 - 0.00478 * sinDeg(125.04 - 1934.136 * t))
    }

    fun normalize(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0

    fun winterSolstice(year: Int): Double {
        var low = julianDay(LocalDate.of(year, 12, 18))
        var high = julianDay(LocalDate.of(year, 12, 25))
        repeat(48) {
            val middle = (low + high) / 2
            if (solarLongitude(middle) < 270.0) low = middle else high = middle
        }
        return (low + high) / 2
    }

    /** Espenak/Meeus NASA polynomial estimates, seconds TT minus UT. */
    private fun deltaT(year: Double): Double {
        return when {
            year < 1900 -> {
                val t = year - 1860
                7.62 + 0.5737 * t - 0.251754 * t * t + 0.01680668 * t * t * t -
                    0.0004473624 * t * t * t * t + t * t * t * t * t / 233174
            }
            year < 1920 -> {
                val t = year - 1900
                -2.79 + 1.494119 * t - 0.0598939 * t * t + 0.0061966 * t * t * t -
                    0.000197 * t * t * t * t
            }
            year < 1941 -> {
                val t = year - 1920
                21.20 + 0.84493 * t - 0.076100 * t * t + 0.0020936 * t * t * t
            }
            year < 1961 -> {
                val t = year - 1950
                29.07 + 0.407 * t - t * t / 233 + t * t * t / 2547
            }
            year < 1986 -> {
                val t = year - 1975
                45.45 + 1.067 * t - t * t / 260 - t * t * t / 718
            }
            year < 2005 -> {
                val t = year - 2000
                63.86 + 0.3345 * t - 0.060374 * t * t + 0.0017275 * t * t * t +
                    0.000651814 * t * t * t * t + 0.00002373599 * t * t * t * t * t
            }
            year < 2050 -> {
                val t = year - 2000
                62.92 + 0.32217 * t + 0.005589 * t * t
            }
            else -> {
                val t = (year - 1820) / 100
                -20 + 32 * t * t - 0.5628 * (2150 - year)
            }
        }
    }
}
