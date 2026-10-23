# Horoscope calculation rule set

The pure-Kotlin engine in `vn.lichviet.core.horoscope` implements a deliberately
limited, deterministic **Tử Vi Đẩu Số** chart. Its rule-set identifier is
`ming-nanyangtang-ziwei-quanshu-vol3-v1`. These placements describe a
traditional cultural system; the module does not generate predictions.

## Text and scope

The selected edition is the Ming Nanyangtang / Baohetang
*新鋟希夷陳先生紫微斗數全書*, supplemented by 潘希尹. The
[Japanese National Archives record](https://www.digital.archives.go.jp/file/1078787)
identifies its two volumes as a Ming printed edition; [Shuge's catalog and
facsimile](https://www.shuge.org/view/zi_wei_dou_shu_quan_shu/) gives the
seven-volume contents. The [Ming-edition OCR, volume 3](https://www.shidianguji.com/zh/book/SDZJ0170/chapter/1jvzooy7dkbhh)
contains the exact named rules cited below. A [Wikisource transcription](https://zh.wikisource.org/zh-hant/紫微斗數全書/卷二)
contains the five 30-day 紫微 diagrams in searchable text; it numbers the
corresponding rule section as volume 2. We have **not** visually checked every
diagram cell against the National Archives facsimile. Thus the two corrections
below are documented editorial emendations, not claimed readings of the scan.

| Calculation | Edition section and implemented reading |
| --- | --- |
| Mệnh and Thân | `安身命例`: start 正月 at 寅; count month forward, then birth hour backward for Mệnh and forward for Thân. A leap month counts as the following numbered month **for these placements**. |
| Twelve palace roles | `安十二宮例`: Mệnh, Huynh Đệ, Phu Thê, Tử Tức, Tài Bạch, Tật Ách, Thiên Di, Nô Bộc, Quan Lộc, Điền Trạch, Phúc Đức, Phụ Mẫu, moving backward one branch each time. The UI may list them in another order. |
| Palace stems and bureau | `起五寅例` starts 寅 at 丙/戊/庚/壬/甲 for year-stem groups 甲己/乙庚/丙辛/丁壬/戊癸. Stems then ascend through branches. `六十花甲子納音歌` supplies the 30 consecutive two-year elements; the Mệnh palace's stem–branch pair determines 水2, 木3, 金4, 土5, 火6. The OCR's `屋上王` is corrected to `屋上土`, also attested in Ming [*三命通會*](https://www.shidianguji.com/zh/book/SK1610/chapter/1kf5v6hz705r9). |
| 紫微 and 13 dependent main stars | `定金木水火土局` supplies **direct 5×30 lookup tables** by bureau and lunar day. `安南北斗諸星訣` gives the offsets of the remaining stars; `安天府圖` reflects 紫微 around the 寅–申 axis, meeting in those two palaces. |
| Auxiliary stars | `安文昌文曲星訣`, `安左輔右弼星訣`, `安天馬星訣`, `安祿存星訣`, `安擎羊陀羅二星訣`, and `安天空地劫訣`. This edition names the hour-derived empty star **天空**, so the typed ID is `TIAN_KONG`, not another school's `DI_KONG`. |
| Four transformations | `安祿權科忌四星變化訣`, applied from the birth **year** stem in its published order 祿/權/科/忌. The selected Ming edition explicitly reads `庚日武同相` (太陽/武曲/天同/天相) and `壬梁紫府武` (天梁/紫微/天府/武曲). Later/common texts also have `庚日武陰同` or `庚陽武同陰`; neither is substituted here. All ten rows are asserted in tests. |
| Đại hạn | `安大限訣`: the first decade starts in Mệnh at the bureau's age (2–6). The next decade goes to Phụ Mẫu for yang male/yin female, or Huynh Đệ for yin male/yang female; continue in that direction. |
| Tiểu hạn | `安小限訣`: age-one branch is 辰 for 寅午戌 years, 戌 for 申子辰, 未 for 巳酉丑, 丑 for 亥卯未. The [transcription's adjacent direction note](https://zh.wikisource.org/zh-hant/紫微斗數全書/卷二) says males count forward and females backward independent of year polarity. Both cycle APIs use 1-based traditional counted age; they do not infer age from today's civil date. |

The searchable [diagram transcription](https://zh.wikisource.org/zh-hant/紫微斗數全書/卷二)
duplicates 木三局 day 9 at 寅 while omitting day 5. 金四局 omits day 30.
The [edition commentary and checked tables](https://xingge.tw/zh-hant/quanshu/anming-tu)
identify the intended corrected cells as **木3/day5 → 寅** and
**金4/day30 → 亥**. Each of the five lookup strings in the engine has exactly
30 positions, and its day-one entries are separately asserted in tests.
The correction does not justify treating an entire calculated chart as a
verified historical golden chart.

## Input convention and limits

The engine accepts a Vietnamese solar or lunar birth date and wall-clock time,
plus the binary historical sex parameter required by these particular cycle
rules. It enforces the calendar core's **fixed UTC+7** meridian and solar
range 1900-01-01 through 2100-12-31. It rejects other offsets, impossible
lunar dates, and an early-Zi rollover past the supported end date. There is
no location-dependent or true-solar-time adjustment. Historical regional
calendar and time standards before 1968 are not reconstructed.

Both 23:00–23:59 and 00:00–00:59 are 子 hour. The classic text does not resolve
whether the 23:00 portion changes the lunar **day**. `CIVIL_MIDNIGHT` keeps the
declared civil date; `EARLY_ZI_NEXT_DAY` treats 23:00–23:59 as the next date,
then recomputes lunar date and birth year before charting. This is an explicit
input convention, not a claim that one convention is universally correct.

The source only explicitly assigns leap months to the following month for
Mệnh/Thân. For leap-month charts, the engine does **not** place month-derived
`ZUO_FU` and `YOU_BI`, since their leap-month rule is not established by the
selected text. `LEAP_MONTH_AUXILIARY_PLACEMENT` marks that omission. A
transformation targeting `YOU_BI` on such a chart has a typed target and a
`null` branch rather than a fabricated location.

`TIAN_KONG` is a deliberate source name: this edition's `安天空地劫訣`
places **天空** by counting backward from 亥 at 子 hour while **地劫** counts
forward. The UI should label it Thiên Không; relabeling it Địa Không would
silently import a different naming convention.

`STAR_BRIGHTNESS`, additional auxiliaries (including ambiguous 魁/鉞 and
火/鈴 placements), predictive readings, and historical/true-solar time are
marked unsupported. The engine does not assign fortunes, health outcomes,
wealth outcomes, or life-event dates. Avoid adding these through UI copy when
the chart lacks a sourced calculation.

## Traceable checks

The partial worked case in `安身命例` says 甲-year, first lunar month, first day,
子 hour gives Mệnh/Thân 寅, 丙寅 fire-six bureau, and 紫微 酉. Vietnamese
1984-02-02 00:30 is a modern civil-date instance of that input. The tests
check those explicit source claims, then check the remaining 13 main-star
positions derived from the adjacent placement verse and 天府 diagram. This is
a **partial source fixture**, not an independently verified full-chart golden.

Separate tests cover the two corrected table cells, ten supported auxiliaries,
edition-specific four-transform rows, a 2023 leap-month case, 23:00 rollover,
UTC+7 rejection, and male/female major/minor cycle direction. A complete
full-chart golden must await a reliably dated and legible chart in the chosen
edition; no such fixture is asserted in this package.
