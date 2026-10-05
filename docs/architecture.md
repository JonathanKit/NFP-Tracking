# Architektur

```
┌──────────────────────────── :app (Android, Compose) ────────────────────────────┐
│  MainActivity ─ NavHost: Heute · Zyklusblatt · Kalender · Statistik · Einstellungen │
│  QuickEntryActivity (Schnelleingabe)   Reminders (AlarmManager)   AutoBackup (WorkManager) │
│  ViewModels  ─  ExplanationFormatter (Texte)  ─  CycleChartRenderer (Bildschirm + PDF)  │
└───────────────┬───────────────────────────────────────────────┬─────────────────┘
                │                                               │
        ┌───────▼────────┐                               ┌──────▼───────┐
        │ :widget (Glance)│                               │              │
        └───────┬────────┘                               │              │
                │                                         │              │
        ┌───────▼──────────────────────────────┐          │              │
        │ :data (Android)                      │          │              │
        │ Room · DataStore · NfpRepository     │          │              │
        │ EvaluationRepository · BackupMapper  │          │              │
        └───────┬───────────────────┬──────────┘          │              │
                │                   │                     │              │
        ┌───────▼──────┐    ┌───────▼──────┐              │              │
        │ :rules (JVM) │    │ :backup (JVM)│◄─────────────┘              │
        │ Sensiplan    │    │ JSON, AES,   │                             │
        │ Statistik    │    │ CSV          │                             │
        └──────────────┘    └──────────────┘                             │
```

| Modul | Art | Aufgabe | Kennt nicht |
| --- | --- | --- | --- |
| `:rules` | reines Kotlin | Regelwerk, Statistik, Prognose, Begründungsschritte | Android, Datenbank, Texte |
| `:backup` | reines Kotlin | Sicherungsformat mit Version und Migration, Verschlüsselung, CSV-Schreiber | Android, Datenbank |
| `:data` | Android-Library | Room-Datenbank, Einstellungen, Repositories, Auswertung als `Flow` | Oberfläche |
| `:widget` | Android-Library | Homescreen-Widget (Glance) | Regeln (nutzt nur Ergebnisse) |
| `:app` | Android-App | Oberfläche, Schnelleingabe, Erinnerungen, Sicherung, Export | Regeln (nur Darstellung der Ergebnisse) |

Damit ist N-06 erfüllt: die Oberfläche kennt keine Regeln, das Regelwerk kennt keine Datenbank.

## Datenfluss

1. Die Oberfläche schreibt über `NfpRepository` (Room, transaktional).
2. `EvaluationRepository.snapshot` kombiniert Zyklen, Tageseinträge und das aktuelle Datum und
   ruft `SensiplanEngine.evaluate()` auf (F-40). Ergebnis: `EvaluationSnapshot`.
3. ViewModels beobachten den Snapshot als `StateFlow`; Composables zeigen ihn an.
4. Texte entstehen erst in der App: `ExplanationFormatter` übersetzt die strukturierten
   `ExplanationStep`s mit `strings.xml` in Sätze (übersetzbar).
5. `EasyNfpApplication` aktualisiert bei Datenänderungen das Widget und plant Erinnerungen und
   automatische Sicherungen nach den Einstellungen neu.

## Abhängigkeiten

Manuelle Dependency Injection über `DataContainer` (ein Objekt pro Prozess, auch vom Widget genutzt).
ViewModels werden über `appViewModelFactory` erzeugt. Kein DI-Framework, damit der Build einfach bleibt.

## Zyklusblatt

`CycleChartRenderer` zeichnet auf `android.graphics.Canvas`. Derselbe Code liefert die Kurve auf dem
Bildschirm (über `drawIntoCanvas`) und im PDF-Export (F-52). Das Modell (`ChartModel`) ist reine
Datenaufbereitung und wird separat getestet.

## Tests

| Ebene | Ort | Läuft |
| --- | --- | --- |
| Regelwerk (jede Regel-ID) | `rules/src/test` | JVM, Sekunden |
| Sicherungsformat | `backup/src/test` | JVM |
| Datenbank, Migrationen, Repository | `data/src/test` | Robolectric |
| Formatierung, Zyklusblatt-Modell, Schnelleingabe- und Tageseintrag-UI | `app/src/test` | Robolectric + Compose-Test |

## Bewusste Entscheidungen

- **AlarmManager (inexakt) statt WorkManager für Erinnerungen:** Uhrzeit-nah, ohne Sonderberechtigung für exakte Alarme.
- **App-Sperre mit Geräte-PIN/Biometrie** (`BiometricPrompt`, `BIOMETRIC_WEAK | DEVICE_CREDENTIAL`) statt eigener PIN: keine eigene Passwortverwaltung, sicherer.
- **Explizites Speichern im Tageseintrag** mit Rückfrage beim Verlassen; die Schnelleingabe speichert mit einem Tipp.
- **Automatische Sicherung unverschlüsselt:** ein gespeichertes Passwort würde die Verschlüsselung entwerten. Manuelle Sicherungen können verschlüsselt werden.
