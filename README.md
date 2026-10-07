# easyNFP

Offline-Android-App als digitales Zyklusblatt für die symptothermale Methode: Temperatur, Zervixschleim
und Blutung erfassen, die Kurve sehen und nachvollziehen, welche Regel mit welchen Tagen greift.
Die morgendliche Temperatureingabe dauert zwei Taps (Widget → Speichern).

## Installation

**[➜ Schritt-für-Schritt-Anleitung zur Installation](docs/installation.md)** – auch ohne Technikkenntnisse.

**[➜ easyNFP.apk direkt herunterladen (neueste Version)](https://github.com/JonathanKit/NFP-Tracking/releases/latest/download/easyNFP.apk)**
· [alle Versionen](https://github.com/JonathanKit/NFP-Tracking/releases)

Die App gibt es nicht im Play Store. Android fragt deshalb bei der Installation nach, ob du der Quelle
vertraust – die Anleitung zeigt, was du wo antippen musst. Voraussetzung: Android 9 oder neuer.

## Wichtiger Hinweis

> **easyNFP ist ein privates, nicht kommerzielles Hobbyprojekt. Es ist kein Medizinprodukt und nicht zur
> Verhütung bestimmt.**
>
> Zweckbestimmung: easyNFP dokumentiert eigene Zyklusbeobachtungen und zeigt zum Lernen und Nachprüfen,
> wie die veröffentlichten Regeln der symptothermalen Methode auf diese Daten angewendet werden. Die
> Anzeige ersetzt weder die eigene Auswertung nach einer qualifizierten Einführung in die Methode noch
> ärztlichen Rat. Die App kann Fehler enthalten. Im Zweifel gilt ein Tag als fruchtbar.
>
> Die Nutzung erfolgt auf eigene Verantwortung und ohne jede Gewährleistung oder Haftung, soweit gesetzlich
> zulässig (siehe [Lizenz](LICENSE), Abschnitte 15 und 16).

Wer natürliche Familienplanung zur Verhütung nutzen möchte, lernt die Methode am besten mit dem
Originalbuch und einer ausgebildeten Beraterin (siehe [Quellen](#regelwerk-quellen-und-marken)).

## Funktionen

- **Heute:** Zyklustag, Status in einem Satz, offene Einträge, Schnellzugriff, Periodenprognose (Schätzung)
- **Tageseintrag:** Temperatur per Zahlenrad (vorbelegt), Ausklammern, Zervixschleim in feinen Stufen
  (Empfinden + Aussehen → t/ø/f/S/S+), Periode, Periodenbeginn, Blutung und Symptome, Störfaktoren,
  optionale Felder, Notiz
- **Schnelleingabe:** dunkel, vom Widget oder der Morgen-Erinnerung aus; die Helligkeit bleibt, wie sie ist
- **Zyklusblatt:** Kurve mit Referenzwerten, Hilfslinie, eingekreisten höheren Messungen, Schleimhöhepunkt 1–2–3,
  Phasenband (Farbe + Muster + Buchstabe), Begründung, manuelle Übersteuerung, PDF-Export
- **Prognose:** fruchtbare/unfruchtbare Tage und nächste Periode im Zyklusblatt und Kalender, klar als
  Schätzung („?“) vom ausgewerteten Teil getrennt
- **Kalender** und **Statistik** (Zykluslängen, 1. höhere Messung, Phasenlängen, Diagramme)
- **Demo-Modus** zum Ausprobieren mit vielen erfundenen Zyklen, ohne die echten Daten anzufassen
- **Daten:** verschlüsselte Sicherung, automatische Sicherung in einen Ordner, CSV-Export, App-Sperre
- **Datenschutz:** keine Netzwerk-Berechtigung, keine Tracker, keine Werbung, kein Konto.
  Die Daten verlassen das Handy nur, wenn du selbst eine Sicherung oder einen Export speicherst.

## Regelwerk, Quellen und Marken

Die Auswertung setzt die veröffentlichten Regeln der symptothermalen Methode (Doppelkontrolle aus
Temperatur und Zervixschleim) um, wie sie die Malteser Arbeitsgruppe NFP unter dem Namen Sensiplan®
beschreibt. Umsetzung und Regel-IDs: [docs/rules.md](docs/rules.md).

- Malteser Arbeitsgruppe NFP: „Natürlich und sicher – Das Praxisbuch“ – das maßgebliche Werk
- [Sensiplan-Übungsheft, Malteser Arbeitsgruppe NFP](https://sensiplan.profilschmiede.de/storage/media/documents/45/sensiplan-uebungsheft-web.pdf)
- [sensiplan.de](https://www.sensiplan.de) – Methode, Beratung und Kurse

Sensiplan® ist eine Marke der Malteser Arbeitsgruppe NFP. easyNFP ist ein unabhängiges Projekt, steht in
keiner Verbindung zur Arbeitsgruppe NFP oder zu den Maltesern und wird von ihnen weder geprüft noch
empfohlen.

## Für Entwickler

| Modul | Inhalt |
| --- | --- |
| `rules/` | Regelwerk in reinem Kotlin, vollständig getestet |
| `backup/` | Sicherungsformat (JSON, Version, AES-GCM), CSV |
| `data/` | Room-Datenbank, Einstellungen, Repositories |
| `widget/` | Homescreen-Widget (Glance) |
| `app/` | Oberfläche (Jetpack Compose), Erinnerungen, Export |

```bash
./gradlew check assembleDebug
```

- [Anforderungen](docs/requirements.md) und [Umsetzungsstand](docs/status.md)
- [Regelwerk mit IDs](docs/rules.md)
- [Architektur](docs/architecture.md)
- [Datenmodell](docs/data-model.md)
- [Build und Release](docs/build-and-release.md) – Releases entstehen automatisch aus Git-Tags `v*`

## Lizenz

[GNU General Public License v3.0](LICENSE). Du darfst den Code nutzen, verändern und weitergeben, solange
abgeleitete Versionen ebenfalls unter der GPL-3.0 stehen. Die Software wird ohne Gewährleistung
bereitgestellt.
