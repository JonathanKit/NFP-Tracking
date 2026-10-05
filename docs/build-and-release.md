# Build und Release

## Voraussetzungen

- JDK 17 oder neuer (CI nutzt 21)
- Android SDK mit Plattform 35 (Android Studio installiert es automatisch)
- Internetzugang zu Google Maven und Maven Central für die Abhängigkeiten

## Lokal bauen

```bash
./gradlew check            # ktlint, detekt, Android Lint, alle Tests
./gradlew assembleDebug    # app/build/outputs/apk/debug/app-debug.apk
```

Nur die reinen Kotlin-Module (ohne Android SDK, z. B. in eingeschränkten Umgebungen):

```bash
./gradlew -Peasynfp.jvmOnly=true check
```

Das prüft `:rules` und `:backup` vollständig und führt ktlint und detekt zusätzlich über die Quellen
der Android-Module aus (`gradle/android-sources-lint`), ohne sie zu kompilieren.

## CI (N-09)

`.github/workflows/ci.yml` läuft bei jedem Push:

1. `./gradlew check assembleDebug` – Build, ktlint, detekt, Android Lint, Unit-, Robolectric- und UI-Tests
2. Prüfung, dass das Room-Schema eingecheckt ist
3. Prüfung, dass die APK keine `INTERNET`-Berechtigung anfordert (N-01)
4. Debug-APK als Artefakt `easyNFP-debug-<Nummer>`

Die Debug-APK hat die App-ID `de.easynfp.debug` und ist mit dem eingecheckten Debug-Schlüssel
(`app/debug.keystore`) signiert, damit neuere Test-Builds über ältere installiert werden können.
Sie ist zum Ausprobieren gedacht; für die echte Nutzung die Release-APK verwenden.

`versionCode` ist in CI die Laufnummer, damit jede neue APK als Update installierbar ist.

## Release-APK (N-11)

### Einmalig: Signaturschlüssel erzeugen

```bash
keytool -genkeypair -v -keystore easynfp-release.jks -alias easynfp \
  -keyalg RSA -keysize 4096 -validity 36500
```

**Den Schlüssel und die Passwörter sicher aufbewahren** (z. B. Passwortmanager + Offline-Kopie).
Ohne ihn lassen sich spätere Updates nicht über die installierte App installieren – die Daten müssten
dann per Sicherung umgezogen werden.

### In GitHub hinterlegen

Repository → Settings → Secrets and variables → Actions:

| Secret | Inhalt |
| --- | --- |
| `EASYNFP_KEYSTORE_BASE64` | `base64 -w0 easynfp-release.jks` |
| `EASYNFP_KEYSTORE_PASSWORD` | Keystore-Passwort |
| `EASYNFP_KEY_ALIAS` | `easynfp` |
| `EASYNFP_KEY_PASSWORD` | Schlüssel-Passwort |

Bei jedem Push auf `main` und bei Tags `v*` baut der Job „Signed release APK“ die Release-APK
(Artefakt `easyNFP-release-<Nummer>`).

### Lokal

Datei `keystore.properties` im Projektverzeichnis (nicht einchecken, steht in `.gitignore`):

```
storeFile=easynfp-release.jks
storePassword=…
keyAlias=easynfp
keyPassword=…
```

Dann `./gradlew assembleRelease`.

### Installation auf dem Handy

APK aus dem CI-Artefakt herunterladen (ZIP entpacken), auf dem Handy öffnen und „Installation aus
unbekannten Quellen“ für den Browser/Dateimanager erlauben. Updates werden genauso installiert;
die Daten bleiben erhalten, solange derselbe Schlüssel verwendet wird.

## Versionen anheben

- Abhängigkeiten: `gradle/libs.versions.toml`
- Ziel-API (N-10): `targetSdk`/`compileSdk` in `libs.versions.toml`; danach `./gradlew check` und
  auf dem Gerät testen (Verhaltensänderungen der neuen Android-Version beachten).
- App-Version: `versionName` in `app/build.gradle.kts`.
