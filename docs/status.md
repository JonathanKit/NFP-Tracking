# Umsetzungsstand

Stand: Version 1.0.0. ✅ umgesetzt · ➖ bewusst nicht umgesetzt · ⏳ offen

| ID | Stand | Wo / Anmerkung |
| --- | --- | --- |
| F-01 | ✅ | Zahlenrad auf 34,00–42,00 begrenzt, Werte außerhalb sind nicht wählbar |
| F-02 | ✅ | Messzeit = Zeitpunkt des Speicherns, änderbar |
| F-03 | ✅ | `TemperatureWheel` (Zahlenrad + Stepper), Vorbelegung mit dem letzten Wert |
| F-04 | ✅ | Schalter „Wert ausklammern“ |
| F-05, F-06 | ✅ | Empfinden/Aussehen als Auswahl, Kategorie wird angezeigt |
| F-07 | ✅ | |
| F-08 | ✅ | Vorschlag im Tageseintrag und auf „Heute“, nie automatisch |
| F-09, F-10, F-11 | ✅ | Störfaktoren verwalten; Rückfrage einmal pro Bearbeitung |
| F-12, F-13 | ✅ | |
| F-14 | ✅ | in den Einstellungen abschaltbar |
| F-15 | ➖ | Kann-Anforderung, später |
| F-20 | ✅ | Glance-Widget, Tap öffnet die Schnelleingabe |
| F-21 | ✅ | Widget-Tap + „Speichern“ = 2 Taps |
| F-22 | ✅ | eigenes dunkles Theme, Bildschirmhelligkeit 15 % |
| F-23, F-24 | ✅ | AlarmManager, entfällt wenn eingetragen |
| F-30 – F-34 | ✅ | `CycleChartRenderer`, Pager zwischen Zyklen, Tap auf Tag |
| F-35, F-36 | ✅ | |
| F-37 | ➖ | Kann-Anforderung, später |
| F-40, F-41 | ✅ | `EvaluationRepository`, `ExplanationFormatter` |
| F-42 | ✅ | Zyklusblatt → „Manuelle Übersteuerung“ |
| F-43, F-44, F-46 | ✅ | Statistik-Seite |
| F-45 | ✅ | Spanne aus den letzten 6 Zyklen (mind. 3) |
| F-50 | ✅ | optional AES-256-GCM verschlüsselt |
| F-51 | ✅ | WorkManager, unverschlüsselt (siehe architecture.md) |
| F-52 | ✅ | PDF A4 quer: Kurve + Begründung |
| F-53 | ✅ | `;`-getrennt, UTF-8 mit BOM |
| F-54 | ✅ | Geräte-Biometrie/PIN; Widget ohne Werte; Schnelleingabe fragt ebenfalls nach |
| F-55 | ✅ | zwei Bestätigungen |
| R-01 – R-34 | ✅ | siehe `docs/rules.md` |
| R-35 – R-37 | ➖ | Entscheidung in requirements.md, Abschnitt 7 |
| N-01 | ✅ | `INTERNET` per Manifest entfernt, CI und Test prüfen es |
| N-02 | ✅ | `allowBackup=false`, Datenextraktionsregeln schließen alles aus |
| N-03 – N-06 | ✅ | |
| N-07 | ✅ | Schema-Export + MigrationTest; Sicherungsformat versioniert |
| N-08 | ✅ | Regel-, Migrations- und UI-Tests (Robolectric) |
| N-09 | ✅ | GitHub Actions |
| N-10 | ✅ | minSdk 28, targetSdk 35 – beim Anheben siehe build-and-release.md |
| N-11 | ✅ | Release-Signatur über Secrets, Anleitung in build-and-release.md |
| N-12 | ✅ | `docs/` |

## Offene Punkte

- ⏳ Referenztests mit den Beispielzyklen aus dem Sensiplan-Übungsheft (lagen nicht vor).
- ⏳ Abgleich der Regeln mit dem Praxisbuch „Natürlich und sicher“.
- ⏳ Screenshot-Tests (Roborazzi) für das Zyklusblatt.
