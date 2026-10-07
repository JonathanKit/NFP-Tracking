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

### Minimal-Einrichtung unter Windows (ohne Android Studio)

So ist der Entwicklungsrechner eingerichtet; alles liegt im Benutzerprofil, Admin-Rechte sind nicht nötig:

| Was | Wo |
| --- | --- |
| Temurin JDK 21 (ZIP von adoptium.net) | `%LOCALAPPDATA%\Programs\Java\jdk-21…` |
| Android Command-line Tools | `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest` |
| per `sdkmanager`: `platform-tools`, `platforms;android-35`, `build-tools;34.0.0` | `%LOCALAPPDATA%\Android\Sdk` |

`local.properties` (nicht eingecheckt) enthält `sdk.dir=C:/Users/<name>/AppData/Local/Android/Sdk`.
Lizenzen einmalig in Git Bash annehmen: `yes | sdkmanager.bat --licenses`. Vor jedem Gradle-Aufruf
`JAVA_HOME` auf das JDK setzen (die systemweite Java-8-Installation reicht nicht).

### Auf ein Handy installieren (USB-Debugging)

```bash
adb devices                                   # Gerät muss "device" zeigen, nicht "unauthorized"
adb install -r app/build/outputs/apk/debug/app-debug.apk   # -r = Update, Daten bleiben erhalten
```

Nie `adb uninstall` auf einem Handy mit echten Daten. Eine lokal gebaute APK hat `versionCode` 1; ist auf
dem Handy schon eine CI-APK mit höherer Nummer, lehnt Android die Installation als Downgrade ab
(Daten bleiben unberührt). Dann eine CI-APK verwenden oder `EASYNFP_VERSION_CODE` passend setzen.
Debug- (`io.github.jonathankit.easynfp.debug`) und Release-APK (`io.github.jonathankit.easynfp`) sind getrennte Apps mit getrennten Daten.

Zum Ausprobieren der Oberfläche mit vielen Daten: Einstellungen → Demo-Modus (eigene Datenbank).

## CI (N-09)

`.github/workflows/ci.yml` läuft bei Pushes auf `main`, bei Tags `v*`, bei Pull Requests und bei
manuellem Start (Actions → CI → „Run workflow“). Andere Branches laufen über ihren Pull Request, damit
nichts doppelt gebaut wird.

1. `./gradlew check assembleDebug` – Build, ktlint, detekt, Android Lint, Unit-, Robolectric- und UI-Tests;
   bei Tags und manuellem Start im selben Gradle-Aufruf zusätzlich `assembleRelease` (signiert)
2. Prüfung, dass das Room-Schema eingecheckt ist
3. Prüfung, dass keine APK die `INTERNET`-Berechtigung anfordert (N-01)
4. Debug-APK als Artefakt `easyNFP-debug-<Nummer>`, ggf. Release-APK als `easyNFP-release-<Nummer>`
5. Nur bei Tags: der kleine Job „Publish GitHub release“ (ohne Build, als einziger mit Schreibrecht)
   legt das GitHub-Release an

Test- und Lint-Berichte werden nur bei Fehlern als Artefakt `reports-<Nummer>` hochgeladen.

Die Debug-APK hat die App-ID `io.github.jonathankit.easynfp.debug` und ist mit dem eingecheckten Debug-Schlüssel
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
| `EASYNFP_KEYSTORE_BASE64` | `base64 -w0 easynfp-release.jks` (Git Bash) bzw. PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("easynfp-release.jks"))` |
| `EASYNFP_KEYSTORE_PASSWORD` | Keystore-Passwort |
| `EASYNFP_KEY_ALIAS` | `easynfp` |
| `EASYNFP_KEY_PASSWORD` | Schlüssel-Passwort |

Bei Tags `v*` und bei manuellem Start (Actions → CI → „Run workflow“) baut die CI zusätzlich die
signierte Release-APK (Artefakt `easyNFP-release-<Nummer>`). Der manuelle Start eignet sich, um den
Release-Build (R8/Minify) vor einem Tag zu prüfen; ohne Secrets entsteht dabei nur die Debug-APK.

### Release veröffentlichen

```bash
git tag v1.0.0
git push origin v1.0.0
```

Der Tag löst die CI aus. Nach erfolgreichem Build und Tests legt der Job „Publish GitHub release“ ein GitHub-Release
`easyNFP 1.0.0` an mit:

- `easyNFP.apk` – signierte Release-APK, immer unter diesem festen Namen, damit
  `https://github.com/JonathanKit/NFP-Tracking/releases/latest/download/easyNFP.apk` stets auf die
  neueste Version zeigt (verlinkt im README und in [installation.md](installation.md))
- `easyNFP.apk.sha256` – Prüfsumme
- Release-Notes: Installationshinweis, Hinweis „kein Medizinprodukt“, automatisch erzeugte Änderungsliste

`versionName` kommt aus dem Tag (`v1.0.0` → `1.0.0`, Umgebungsvariable `EASYNFP_VERSION_NAME`),
`versionCode` aus der Laufnummer der CI. Fehlen die Signatur-Secrets, schlägt ein Tag-Build fehl, statt
ein unsigniertes Release zu veröffentlichen.

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

Für Nutzerinnen: [installation.md](installation.md) (Download aus den GitHub-Releases). Updates werden
genauso installiert; die Daten bleiben erhalten, solange derselbe Schlüssel verwendet wird.

## Versionen anheben

- Abhängigkeiten: `gradle/libs.versions.toml`
- Ziel-API (N-10): `targetSdk`/`compileSdk` in `libs.versions.toml`; danach `./gradlew check` und
  auf dem Gerät testen (Verhaltensänderungen der neuen Android-Version beachten).
- App-Version: über den Git-Tag (siehe „Release veröffentlichen“); `versionName` in
  `app/build.gradle.kts` ist nur der Standardwert für lokale und Test-Builds.
