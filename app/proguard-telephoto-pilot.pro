# Controlled renderer pilot only.
# Force Telephoto to remain reachable in release so R8 compatibility and APK cost can be measured
# even though the pilot composable is intentionally not wired into production navigation.
-keep class me.saket.telephoto.** { *; }
-keep class com.veilreader.app.manga.reader.TelephotoRendererPilotKt { *; }
