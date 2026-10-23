# Room and Hilt provide their own consumer rules.
# iCal4j 4.4 explicitly probes this non-public Android class and catches LinkageError,
# then resolves supported TZIDs against platform rules. IcsSupport rejects incompatible
# embedded VTIMEZONE rules instead of silently changing their meaning.
-dontwarn java.time.zone.ZoneRulesProvider
# Optional compile-only formatting annotation in threeten-extra, never invoked by the app.
-dontwarn org.joda.convert.ToString
