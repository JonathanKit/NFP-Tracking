# Hinweise für Agenten-Sitzungen

- Anforderungen: `docs/requirements.md` (IDs F-xx, R-xx, N-xx). Stand: `docs/status.md`. Beides aktuell halten.
- Regelwerk nur in `:rules` ändern; jede Regeländerung braucht einen Test mit der Regel-ID im Testnamen
  und einen Eintrag in `docs/rules.md`. Im Zweifel fruchtbar, keine eigenen Heuristiken.
- Oberfläche kennt keine Regeln: Texte zu Auswertungen entstehen in `app/.../text/ExplanationFormatter.kt`
  aus `ExplanationStep`s. Alle Texte in `app/src/main/res/values/strings.xml` (Deutsch).
- Datenbankänderung: Version erhöhen, Migration + Test, Schema-JSON committen (`docs/data-model.md`).
- Sicherungsformat: nur kompatibel erweitern oder Formatversion + Migration (`backup/`).
- Keine Netzwerk-Berechtigung, keine Analyse-/Absturzdienste hinzufügen.
- Prognose (F-47) ist kein Regelwerk: sie liegt in `rules/.../statistics/CycleForecast.kt`, ändert keine
  Auswertung und wird in der Oberfläche immer als Prognose („?“, blass) vom heutigen Tag getrennt gezeigt.
- Daten müssen jedes Update überstehen (N-07): nie `fallbackToDestructiveMigration`, Codes in `Codes.kt` nie ändern.
  Auf Geräten mit echten Daten nie `adb uninstall`, nur `adb install -r`.
- Neue Tagesfelder: Entity + Migration, `DayEntry`, Mapper, `BackupDay`/`BackupMapper`, CSV-Spalte
  (`csv_header`), `DemoData` falls sinnvoll.

## Prüfen

```bash
./gradlew check assembleDebug                 # vollständig (Android SDK + Google Maven nötig)
./gradlew -Peasynfp.jvmOnly=true check        # ohne Android: :rules, :backup + ktlint/detekt aller Quellen
./gradlew -Peasynfp.jvmOnly=true ktlintFormat # Formatierung korrigieren
```

In Umgebungen ohne Zugriff auf Google Maven (`dl.google.com`) lassen sich die Android-Module nicht
kompilieren; dann die CI (GitHub Actions) als Prüfung nutzen.

Lokaler Windows-Rechner (Einrichtung: `docs/build-and-release.md`): JDK 21 unter
`%LOCALAPPDATA%\Programs\Java\jdk-21*`, Android SDK unter `%LOCALAPPDATA%\Android\Sdk`. In Git Bash vor
Gradle `export JAVA_HOME="$LOCALAPPDATA/Programs/Java/jdk-21.0.12.1+1"` (System-Java ist 8). Für `adb shell`
mit Gerätepfaden `MSYS_NO_PATHCONV=1` setzen, sonst macht Git Bash aus `/sdcard/...` einen Windows-Pfad.

## Auf dem Gerät prüfen

Testgerät per USB: Sony Xperia 5 II (Android 12, 1080 × 2520). `adb install -r …/app-debug.apk`,
Screenshots mit `adb exec-out screencap -p > s.png`, Bedienung mit `adb shell input tap/swipe` und
`uiautomator dump` (Koordinaten aus `bounds`). Mit vielen Daten: Einstellungen → Demo-Modus.
