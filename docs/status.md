# Umsetzungsstand

Stand: nach Version 1.0.0 – Datenbank 2, Sicherungsformat 2. ✅ umgesetzt · ➖ bewusst nicht umgesetzt · ⏳ offen

| ID | Stand | Wo / Anmerkung |
| --- | --- | --- |
| F-01 | ✅ | Zahlenrad auf 34,00–42,00 begrenzt, Werte außerhalb sind nicht wählbar |
| F-02 | ✅ | Messzeit = Zeitpunkt des Speicherns, änderbar |
| F-03 | ✅ | `TemperatureWheel` (Zahlenrad + Stepper), Vorbelegung mit dem letzten Wert |
| F-04 | ✅ | Schalter „Wert ausklammern“ |
| F-05, F-06 | ✅ | 5 Stufen Empfinden, 7 Stufen Aussehen, Kategorie wird angezeigt (R-11 unverändert) |
| F-07 | ✅ | Abschnitt heißt „Periode“ |
| F-08 | ✅ | Vorschlag im Tageseintrag und auf „Heute“, nie automatisch |
| F-09, F-10, F-11 | ✅ | Störfaktoren verwalten; Rückfrage einmal pro Bearbeitung |
| F-12, F-13 | ✅ | |
| F-14 | ✅ | in den Einstellungen abschaltbar |
| F-15 | ➖ | Kann-Anforderung, später |
| F-16 | ✅ | Abschnitt „Blutung und Symptome“, auch in Sicherung und CSV |
| F-20 | ✅ | Glance-Widget 2 × 1 (nur waagerecht veränderbar): Zyklustag + Status, Tap öffnet die App; keine Schnelleingabe mehr aus dem Widget |
| F-21 | ✅ | Tap auf die Morgen-Erinnerung + „Speichern“ = 2 Taps |
| F-22 | ✅ | eigenes dunkles Theme; Helligkeit bleibt unverändert |
| F-23, F-24 | ✅ | AlarmManager, entfällt wenn eingetragen |
| F-30 – F-34 | ✅ | `CycleChartRenderer`: Datum/Zyklustag oben, Kurve 30 % höher, Legende hinter „i“, höhere Messungen eingekreist, Spalte der 1. hM und unfruchtbare Tage hinterlegt |
| F-35, F-36 | ✅ | Kalenderzeilen füllen die Höhe (`BoxWithConstraints`) |
| F-37 | ➖ | Kann-Anforderung, später |
| F-40, F-41 | ✅ | `EvaluationRepository`, `ExplanationFormatter` |
| F-42 | ✅ | Zyklusblatt → „Manuelle Übersteuerung“ |
| F-43, F-44, F-46 | ✅ | Statistik-Seite: typischer Zyklus (Phasenbalken), Schwankung (Spannweiten), Balkendiagramme; Bausteine in `StatisticsCharts.kt` |
| F-45 | ✅ | Spanne aus den letzten 6 Zyklen (mind. 3), ohne Hinweistext |
| F-47 | ✅ | `CycleForecaster` (`:rules`, Statistik), im Zyklusblatt und Kalender (U?/F?, Ring = erwartete Periode) |
| F-48 | ✅ | Einstellungen → Demo-Modus; eigene Datenbank `easynfp-demo.db`, App startet neu |
| F-49 | ✅ | `IntercourseStatistics` (`:app`), Karte auf der Statistik-Seite; ausgeblendet, wenn F-14 abgeschaltet ist |
| F-50 | ✅ | optional AES-256-GCM verschlüsselt |
| F-51 | ✅ | WorkManager, unverschlüsselt (siehe architecture.md); ruht im Demo-Modus |
| F-52 | ✅ | PDF A4 quer: Kurve + Begründung (ohne Prognose) |
| F-53 | ✅ | `;`-getrennt, UTF-8 mit BOM |
| F-54 | ✅ | Geräte-Biometrie/PIN; Widget ohne Werte; Schnelleingabe fragt ebenfalls nach |
| F-55 | ✅ | zwei Bestätigungen |
| R-01 – R-34 | ✅ | siehe `docs/rules.md` |
| R-35 – R-37 | ➖ | Entscheidung in requirements.md, Abschnitt 7 |
| N-01 | ✅ | `INTERNET` per Manifest entfernt, CI und Test prüfen es |
| N-02 | ✅ | `allowBackup=false`, Datenextraktionsregeln schließen alles aus |
| N-03 – N-06 | ✅ | |
| N-07 | ✅ | Schema-Export + MigrationTest (1 → 2); Kopie der alten DB vor Migration; `hasFragileUserData`; Sicherungsformat versioniert. Update 1.0 → 1.1 auf dem Gerät geprüft |
| N-08 | ✅ | Regel-, Migrations- und UI-Tests (Robolectric) |
| N-09 | ✅ | GitHub Actions |
| N-10 | ✅ | minSdk 28, targetSdk 35 – beim Anheben siehe build-and-release.md |
| N-11 | ✅ | Release-Signatur über Secrets, Anleitung in build-and-release.md |
| N-12 | ✅ | `docs/` |
| N-13 | ✅ | `Theme.kt`: Pfirsich/Lavendel/Mint auf violett getöntem Dunkel, runde Ecken, Verlauf in der Statuskarte, Begrüßung; noch nicht auf dem Gerät angesehen |

## Auf dem Gerät geprüft (Oktober 2026)

Sony Xperia 5 II, Android 12, 1080 × 2520 (21:9), Dunkelmodus, per USB/adb:
Heute, Tageseintrag (neue Schleimstufen, Periode, Blutung und Symptome speichern und wieder laden),
Zyklusblatt (Layout, Kreise, Hinterlegungen, Prognose, Legende), Kalender mit Prognose, Schnelleingabe
(keine Helligkeitsänderung), Einstellungen, Demo-Modus ein/aus, Update von 1.0 (DB 1) auf 1.1 (DB 2) mit Daten.

## Veröffentlichung

Öffentliches Hobbyprojekt unter GPL-3.0, APK nur über GitHub-Releases (kein Play Store, keine
Monetarisierung – Begründung: Medizinprodukterecht und Marke Sensiplan®). App-ID
`io.github.jonathankit.easynfp`. Release-Ablauf: `docs/build-and-release.md`, Installationsanleitung:
`docs/installation.md`, Hinweise für Beitragende: `CONTRIBUTING.md`, `SECURITY.md`.

## Offene Punkte

- ⏳ Widget auf dem Homescreen von Hand prüfen (2 × 1, Tap → App); per adb lässt sich kein Widget platzieren.
- ✅ Neue Farbwelt (N-13), Statistik-Grafiken und Kalender auf dem Gerät angesehen (Demo-Modus).
- ⏳ Noch nicht auf dem Gerät durchgeklickt: Erinnerungen, App-Sperre, Sicherung exportieren/importieren,
  automatische Sicherung, PDF- und CSV-Export, Hellmodus, große Schrift, Android 9 (minSdk).
- ⏳ Screenshot-Tests (Roborazzi) für das Zyklusblatt.
