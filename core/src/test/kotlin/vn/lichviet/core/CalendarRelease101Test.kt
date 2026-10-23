package vn.lichviet.core

import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class CalendarRelease101Test {
    @Test fun cultureDayStartsIn2026AndUsesSolarNovember24() {
        assertFalse(VietnameseCalendar.holidayIds(LocalDate.of(2025,11,24)).any { it.id==HolidayId.VIETNAMESE_CULTURE_DAY })
        for(year in listOf(2026,2027,2030,2100)) {
            val date=LocalDate.of(year,11,24)
            assertTrue(VietnameseCalendar.holidayIds(date).any { it.id==HolidayId.VIETNAMESE_CULTURE_DAY })
            assertTrue("Ngày Văn hóa Việt Nam" in VietnameseCalendar.holidays(date))
            assertFalse(VietnameseCalendar.holidayIds(date.minusDays(1)).any { it.id==HolidayId.VIETNAMESE_CULTURE_DAY })
            assertFalse(VietnameseCalendar.holidayIds(date.plusDays(1)).any { it.id==HolidayId.VIETNAMESE_CULTURE_DAY })
        }
    }

    @Test fun isoWeekUsesTheWeekBasedYearAcrossBothYearBoundaries() {
        assertEquals(IsoCalendarWeek(53,2020),VietnameseCalendar.isoWeekForRow(LocalDate.of(2020,12,28)))
        assertEquals(IsoCalendarWeek(53,2020),VietnameseCalendar.isoWeekForRow(LocalDate.of(2020,12,27)))
        assertEquals(IsoCalendarWeek(1,2021),VietnameseCalendar.isoWeekForRow(LocalDate.of(2021,1,3)))
        assertEquals(IsoCalendarWeek(1,2025),VietnameseCalendar.isoWeekForRow(LocalDate.of(2024,12,30)))
    }

    @Test fun cachedDayDataRemainsCorrectAcrossEvictionAndConcurrentCallers() {
        val date=LocalDate.of(2026,11,24)
        val before=VietnameseCalendar.dayData(date)
        val executor=Executors.newFixedThreadPool(4)
        try {
            val futures=(0..1200).map { offset -> executor.submit<CalendarDayData> {
                VietnameseCalendar.dayData(LocalDate.of(2025,1,1).plusDays(offset.toLong()))
            } }
            futures.forEachIndexed { offset,future ->
                val actual=future.get(10,TimeUnit.SECONDS)
                assertEquals(VietnameseCalendar.solarToLunar(LocalDate.of(2025,1,1).plusDays(offset.toLong())),actual.lunar)
            }
            assertEquals(before,VietnameseCalendar.dayData(date))
        } finally { executor.shutdownNow() }
    }
}
