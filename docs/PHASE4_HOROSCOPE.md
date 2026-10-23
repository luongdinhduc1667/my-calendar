# Phase 4 — Horoscope workspace

## Implemented scope

The existing offline `core/horoscope` engine is connected to a native Compose
workspace. It stores multiple optional-name profiles in Room, grouped as self,
family or friends. A profile preserves solar/lunar input, a leap-month flag,
birth hour and minute, the historical sex parameter, and the explicit 23:00
day-boundary convention. Time is fixed UTC+7; location and historical or true
solar time conversion are not inferred.

The traditional 4×4 chart places twelve palaces around an informational 2×2
center. It supports tap selection, pinch zoom, drag pan, zoom controls and reset.
The readable list retains full star names and uses the normal app font scale.
Available widths of at least 900dp use a chart column and a separately scrollable
detail panel. Selection is animated with the existing Material 3 palette, and
all chart cells expose a full descriptive accessibility label.

Details show supported main/auxiliary stars, bureau, major-cycle counted-age
ranges, an explicit counted-age input for minor and major cycle lookup, and the
edition-specific four transformations. Search matches palace/star names,
including Vietnamese queries without accents. A star glossary explains the
documented placement rule and cites its named source section. The app does not
invent psychological, health, financial, relationship or future-event readings.
Profile comparison only juxtaposes calculated Life/Body/bureau/hour facts.

Image and two-page PDF exports use Android Canvas and PdfDocument. Files contain
only the selected profile and its computed chart, not other saved profiles.
Each save or share requires an explicit personal-data confirmation displaying
the selected nickname and birth details. A reviewed immutable selection is used
for generation. PNG/PDF saves use ACTION_CREATE_DOCUMENT; sharing uses the
existing scoped FileProvider and Android Sharesheet. No storage permission or
new network dependency is required. Cached share files expire after 24 hours
when the next export is generated. User-saved or shared copies remain under the
recipient's control.

VI and EN resources include all labels, accessibility descriptions, error
messages, source notes and export text. Traditional Vietnamese star names remain
unchanged in English, with English explanations of the calculation scope.

## Persistence and compatibility

`HoroscopeProfileEntity` stores inputs only; deterministic chart output is
recomputed with `ming-nanyangtang-ziwei-quanshu-vol3-v1`. There is no redundant
chart table or fabricated strength classification. Database version 4 adds
`horoscope_profiles` through `MIGRATION_3_4`, preserving events/reminders.
Backup v4 includes validated profile inputs; old backup versions preserve
profiles that were absent from their format. Delete-all-data removes profiles.
Profile saves/deletes share the event mutation mutex with full-backup restore.
ViewModel operations survive Activity recreation and validate input before
persistence.

## Rule set and known limits

See [the calculation rules](../core/HOROSCOPE_RULES.md) for primary references,
the selected Ming edition, the exact Geng/Ren transformation rows, two documented
editorial table corrections and the supported modern UTC+7 date range.

For leap months, Life/Body use the next numbered month in this edition. Tả Phụ
and Hữu Bật are omitted when their leap-month rule is not established. Strength
tables, other auxiliary stars, true-solar/historical time and predictive prose
remain explicitly unsupported. A reliable full-chart historical golden has not
been independently established; the engine tests include a documented partial
source fixture and explicit independently stated placement rules, not a claim
of complete historical validation.

## Relevant files

- `app/horoscope/HoroscopeProfileEntity.kt`, `HoroscopeProfileDao.kt`,
  `HoroscopeRepository.kt`, `HoroscopeViewModel.kt`: profiles, validation and
  lifecycle-owned operations.
- `HoroscopeWorkspace.kt`, `HoroscopeProfileEditor.kt`, `HoroscopeChartView.kt`,
  `HoroscopeText.kt`: adaptive UI, accessible chart/list and resource-backed
  facts/glossary.
- `ChartExporter.kt`: offline PNG/PDF and scoped temporary files.
- `ui/tools/HoroscopeScreen.kt`: route entry into the workspace.
- `res/values/horoscope_strings.xml`, `res/values-en/horoscope_strings.xml`.
- Shared integration: `AppDatabase.kt`, `BackupRepository.kt`,
  `res/xml/ics_file_paths.xml`.

## Verification and manual checks

Focused automated coverage is in `HoroscopeProfileValidationTest` (5 tests),
`HoroscopeProfilePersistenceTest` (3), `HoroscopeChartUiTest` (2),
`ChartExportTest` (1) and `HoroscopeJourneyTest` (1), alongside the existing core
placement/boundary tests. The profile journey creates, recreates, edits and
deletes a real Room profile through MainActivity, cleaning up only its own
fixture. The export fixture uses a long nickname and English labels and retains
PNG/PDF plus both PDFRenderer page images in the app's external-files
`horoscope-qa` directory for visual inspection.

Focused execution on the API 37 emulator passed 5 profile validation JVM tests,
3 profile persistence/codec/migration instrumentation tests, and 2 chart/editor
Compose tests. XML evidence is in `artifacts/reports/v1-phase4/`. The export and
real-Activity journey subsequently passed in the full integration gate on both
phone and landscape tablet configurations of the API 37 emulator. The PNG and
both rendered PDF pages were visually inspected with the long English fixture;
no clipping was seen in that fixture. Full integration passed 56 JVM and 38
Android tests, with zero Lint errors and successful R8 APK/AAB builds. See
[VALIDATION.md](../VALIDATION.md) for evidence and remaining device QA limits.

1. Add a solar profile for 2 February 1984 at 00:30, UTC+7. The source fixture
   gives Life/Body at Dần, fire-six bureau and Tử Vi at Dậu.
2. Add lunar 1/2 leap/2023 and compare the result with solar 22 March 2023.
   Check that the omitted leap-month assistants are explained.
3. Compare 16 February 2026 at 23:30 under both day-boundary choices: the
   effective chart date/year changes across Tết only with next-day mode.
4. Save a second nickname, switch/edit/delete profiles, rotate, and restart.
   Existing events and reminders should remain intact.
5. Tap all four sides of the chart, pinch and drag, reset, and use list mode at
   200% font size. Search without Vietnamese accents. Repeat in English/dark
   mode and on tablet landscape.
6. Confirm PNG/PDF export, inspect all palaces and source/convention text, then
   share with the Android Sharesheet. Cancel the personal-data dialog and verify
   that no picker or Sharesheet opens.
