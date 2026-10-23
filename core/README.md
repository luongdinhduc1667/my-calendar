# Vietnamese calendar core

The `zodiac` package added in 1.1 provides Vietnamese animal signs, nạp âm,
source-linked cycle reports, hour pillars and standard 24-Mountain / Eight-House
calculations. Its exact conventions and editorial boundaries are documented in
[the 1.1 rule set](../docs/ZODIAC_FENG_SHUI_RULES_1.1.md).

Pure Kotlin/JVM, offline calculation. Android, widgets, recurring reminders and
Compose all use this same module. No network permission, API key, downloaded
calendar table or runtime astronomy dependency is required.

## Scope and time standard

Supported Gregorian dates are **1900-01-01 through 2100-12-31**, inclusive.
`solarToLunar` throws `IllegalArgumentException` outside that range.
`lunarToSolar` returns `null` for unsupported or impossible results, including
a nonexistent leap month or day 30 in a 29-day month. The lunar tail of 1899 is
accepted when its solar result is in January 1900, so the full solar range
roundtrips correctly.

Astronomical days use the **fixed UTC+7 calendar meridian**, independently of the
device's time zone. `zoneId` exposes `Asia/Ho_Chi_Minh` for obtaining the user's
present Vietnamese civil date. We deliberately do not use its historical
offset transitions to construct lunar months. Dates before 1968 therefore apply
the modern UTC+7 rules retrospectively; they are not an archival reconstruction
of the different calendars previously used in Northern and Southern Vietnam.

The ephemeris uses Meeus new-moon periodic terms, low-precision apparent solar
longitude, and estimated TT−UT. This is a practical civil-calendar calculation,
not a high-precision ephemeris service. The historical and future limits on ΔT,
and events extremely close to midnight, should be kept in mind when comparing
independent astronomical tables. Solar terms are the term prevailing at **noon
in UTC+7**, not a prediction of the exact transition time.

## API

Package: `vn.lichviet.core`.

```kotlin
val solar = LocalDate.of(2026, 2, 17)
val lunar = VietnameseCalendar.solarToLunar(solar) // LunarDate(1, 1, 2026)
VietnameseCalendar.lunarToSolar(lunar)             // 2026-02-17
VietnameseCalendar.dayCanChi(solar)
VietnameseCalendar.monthCanChi(lunar)
VietnameseCalendar.yearCanChi(lunar.year)
VietnameseCalendar.solarTerm(solar)
VietnameseCalendar.auspiciousHours(solar)            // six traditional double-hours
VietnameseCalendar.holidays(solar)                 // names and observances

Almanac.purposes // Khai trương, Cưới hỏi, Chuyển nhà, Xuất hành
Almanac.assess(solar, "Khai trương")                // DayAssessment
Almanac.findDates(solar, solar.plusDays(30), "Khai trương")
```

`LunarDate(day, month, year, isLeap = false)` preserves the leap-month flag in
both directions. `shortLabel` and `fullLabel` are display conveniences.
`DayAssessment` has `isRecommended`, `reasons`, and `label` properties.
Holiday names include lunar observances, mùng một, rằm and Giao thừa; they are
not a legal leave schedule and do not encode annually announced substitute days.

Version 1.0.1 adds Vietnamese Culture Day on solar November 24, from 2026,
following [Government Portal guidance on Resolution 28/2026/QH16](https://xaydungchinhsach.chinhphu.vn/ngay-24-11-hang-nam-la-ngay-van-hoa-viet-nam-nguoi-lao-dong-duoc-nghi-huong-nguyen-luong-119260113152642414.htm).
No retrospective occurrence or annual compensatory leave schedule is inferred.
dayData(date) shares a synchronized 512-entry LRU of lunar dates and holiday IDs;
labels remain outside the cache. warmUp() prepares the astronomy table and is
called on a background dispatcher by the Android host. isoWeekForRow(start)
uses the ISO week of the Thursday inside a Monday/Sunday-first grid row.

## Month construction and sources

The implementation locates astronomical new moons and winter solstices, using
the Vietnamese civil date of each event. The month containing the winter
solstice is month 11. When there are 13 months between successive month-11
starts, the first subsequent month containing no major solar term repeats
the preceding month number as its leap month. Major terms are tested against
civil month boundaries, including a term occurring on the new month's first
day in that new month. This handles the exceptional leap month 11 of 2033.

Primary technical references consulted:

- Jean Meeus, *Astronomical Algorithms*, second edition (1998), chapters 25
  and 49. Formula implementation reference:
  [Sonia Keys, `moonphase.go`](https://github.com/soniakeys/meeus/blob/master/v3/moonphase/moonphase.go)
  and [`solar.go`](https://github.com/soniakeys/meeus/blob/master/v3/solar/solar.go).
  The MIT attribution is retained in `THIRD_PARTY_NOTICES.md` and the source.
- [NASA / Fred Espenak, ΔT polynomial expressions](https://eclipse.gsfc.nasa.gov/SEhelp/deltatpoly2004.html),
  for converting dynamical time to universal time.
- [Hong Kong Observatory, The 24 Solar Terms](https://www.hko.gov.hk/en/gts/time/24solarterms.htm),
  for the astronomical term definitions and the principal-term/leap-month rule.
- [Unicode CLDR Vietnamese locale data](https://www.unicode.org/cldr/charts/45/summary/vi.html),
  for Vietnamese names of cyclical years, branches and solar terms.

## Almanac convention

The almanac is an **explicitly simplified cultural reference**, not a prediction
of results or a complete Thông thư system. It does not use birth data or invent
a percentage score. All reasons are exposed to the UI.

The app's published selection convention is:

1. Mark hoàng đạo branches for lunar month 1 as Tý, Sửu, Tỵ, Mùi, and rotate
   by two branches per lunar month, repeating after six months.
2. Calculate the twelve trực from the day's branch relative to the solar
   month's branch. Solar months change at tiết, starting with Dần at Lập xuân.
3. Prefer Mãn/Thành/Thu/Khai for khai trương, Định/Thành/Khai for cưới hỏi and
   chuyển nhà, and Mãn/Thành/Khai for xuất hành.
4. A candidate meets either the hoàng đạo rule or its purpose's trực rule, and
   avoids lunar days 3, 7, 13, 18, 22, 27 (Tam Nương) and 5, 14, 23 (Nguyệt Kỵ).

These are product conventions based on familiar folk-calendar categories;
other almanacs and schools may select different dates. The calculation API
keeps this convention separate from the astronomical lunar-date conversion.
Range searches are inclusive and limited to 367 dates per query.

## Verification

Run `./build.ps1 :core:test` from the project root. The tests cover all 73,414
supported civil dates in both conversion directions, lunar month lengths,
invalid dates, leap months 2/2023, 6/2025 and 11/2033, time-zone independence,
Can Chi, holidays and range filtering.

Additional external-fixture checks measure actual agreement, rather than only
internal conversion consistency:

| Check | Observed result |
| --- | --- |
| Every date in Gregorian 2026–2027 against published HKO month starts, with UTC+7 moon dates checked separately | All 730 dates agree |
| All 25 new moons in 2026–2027 against NASA's minute-rounded UT phase table | Maximum difference 29.081 seconds; all Vietnamese civil dates agree |
| Winter solstice 1984 against the contemporary RASC handbook | Solar model differs by 5.994 minutes; Vietnamese civil date is correct |
| Month numbering from November 1984 through April 1985 | Preserves Vietnamese Tet on 21 January and leap month 2, rather than Chinese Tet on 20 February |

These are observed comparisons for the stated fixtures, **not** an accuracy
guarantee for every event in 1900–2100. In particular, the approximately six-minute
solar-time difference demonstrates why this compact ephemeris must not be exposed
as an exact solar-term transition-time service. The 1984 solstice still falls
on 21 December in UTC+7 and 22 December in UTC+8; its approximately 37-minute
distance from Vietnamese midnight is comfortably larger than that measured
model difference.

Independent fixtures include:

- [NASA phase table, 1901–2000](https://eclipse.gsfc.nasa.gov/phase/phases1901.html):
  new moon on 29 January 1968 at 16:29 UT. In UTC+7 this is still 29 January;
  in UTC+8 it is 30 January. The test explicitly preserves this Vietnamese/
  Chinese difference rather than treating the Chinese calendar as a universal
  lunar-date table.
- HKO conversion tables for
  [2020](https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/2020e.pdf),
  [2021](https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/2021e.pdf),
  [2022](https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/2022e.pdf),
  [2023](https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/2023e.pdf),
  [2024](https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/2024e.pdf),
  [2025](https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/2025e.pdf),
  [2026](https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/2026e.pdf),
  [2027](https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/2027e.pdf),
  [2033](https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/2033e.pdf),
  and [1968](https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/1968e.pdf).
  The selected modern Tet/leap fixtures are dates on which UTC+7 and UTC+8 agree;
  Chinese tables must not be used unconditionally for Vietnam.
- [NASA phase table, 2001–2100](https://eclipse.gsfc.nasa.gov/phase/phases2001.html),
  for all 25 new moons in 2026–2027. The UT timestamps are converted to fixed
  UTC+7 civil dates before comparison.
- [Royal Astronomical Society of Canada, Observer's Handbook 1984](https://www.rasc.ca/sites/default/files/publications/ObserversHandbook-1984.pdf),
  printed page 46 (PDF page 48), gives winter solstice on 21 December at 16:23 UT.
  Combined with NASA's December 1984 and January–April 1985 new moons, this
  independently explains the different Vietnamese month-11 placement and Tet.
  The Chinese contrast is recorded in HKO's
  [1984](https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/1984e.pdf) and
  [1985](https://www.hko.gov.hk/en/gts/time/calendar/pdf/files/1985e.pdf) tables.
- [Vietnamese Government Portal, Hanoi fireworks schedule for Tet 2026](https://xaydungchinhsach.chinhphu.vn/thoi-gian-dia-diem-ban-phao-hoa-dip-tet-duong-lich-va-tet-nguyen-dan-binh-ngo-cua-ha-noi-11925122318284722.htm)
  confirms 17 February 2026 as the start of Tet Bính Ngọ.
- [Vietnamese Government Portal, 16 February 2026](https://thanglong.chinhphu.vn/bao-dam-cong-tac-y-te-phuc-vu-ban-phao-hoa-tet-nguyen-dan-binh-ngo-103260216151439642.htm)
  confirms that date as 29 tháng Chạp, demonstrating the 29-day final month.
