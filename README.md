# easyNFP

Offline-Android-App, die Zyklen nach dem **Sensiplan**-Regelwerk (symptothermale Methode) erfasst und
nachvollziehbar auswertet. Sie ersetzt das Papier-Zyklusblatt: erfassen, Kurve sehen, regelkonforme
Auswertung mit Begründung bekommen. Die morgendliche Temperatureingabe dauert zwei Taps (Widget → Speichern).

> easyNFP ist kein zertifiziertes Medizinprodukt. Jede Auswertung zeigt, welche Regel mit welchen Tagen
> gegriffen hat, damit sie selbst geprüft werden kann. Im Zweifel gilt ein Tag als fruchtbar.

## Funktionen

- **Heute:** Zyklustag, Status in einem Satz, offene Einträge, Schnellzugriff, Periodenprognose (Schätzung)
- **Tageseintrag:** Temperatur per Zahlenrad (vorbelegt), Ausklammern, Zervixschleim (Empfinden + Aussehen → t/ø/f/S/S+),
  Blutung, Periodenbeginn, Störfaktoren, optionale Felder, Notiz
- **Schnelleingabe:** dunkel und gedimmt, vom Widget oder der Morgen-Erinnerung aus
- **Zyklusblatt:** Kurve mit Referenzwerten, Hilfslinie, höheren Messungen, Schleimhöhepunkt 1–2–3,
  Phasenband (Farbe + Muster + Buchstabe), Begründung, manuelle Übersteuerung, PDF-Export
- **Kalender** und **Statistik** (Zykluslängen, 1. höhere Messung, Phasenlängen, Diagramme)
- **Daten:** verschlüsselte Sicherung, automatische Sicherung in einen Ordner, CSV-Export, App-Sperre
- Keine Netzwerk-Berechtigung, keine Tracker, kein Konto

## Projektaufbau

| Modul | Inhalt |
| --- | --- |
| `rules/` | Sensiplan-Regelwerk in reinem Kotlin, vollständig getestet |
| `backup/` | Sicherungsformat (JSON, Version, AES-GCM), CSV |
| `data/` | Room-Datenbank, Einstellungen, Repositories |
| `widget/` | Homescreen-Widget (Glance) |
| `app/` | Oberfläche (Jetpack Compose), Erinnerungen, Export |

## Dokumentation

- [Anforderungen](docs/requirements.md) und [Umsetzungsstand](docs/status.md)
- [Regelwerk mit IDs](docs/rules.md)
- [Architektur](docs/architecture.md)
- [Datenmodell](docs/data-model.md)
- [Build und Release](docs/build-and-release.md)

## Schnellstart

```bash
./gradlew check assembleDebug
```

Installierbare APKs liefert die CI (GitHub Actions → Lauf → Artefakte).
