# Datenmodell

Datenbank: Room (SQLite), Datei `easynfp.db` im privaten App-Speicher. Aktuelle Version: **2**.
Der Demo-Modus (F-48) nutzt eine eigene Datei `easynfp-demo.db` mit demselben Schema.

| Version | Änderung | Migration |
| --- | --- | --- |
| 1 | Erste Version | – |
| 2 | Blutung außerhalb der Periode und Symptome (F-16) | `MIGRATION_1_2`: fünf Spalten ergänzt |
Schema-Historie: `data/schemas/de.easynfp.data.db.NfpDatabase/<version>.json` (wird beim Build erzeugt und eingecheckt).

## Tabellen

### `day_entries` – ein Datensatz pro Tag (F-01 bis F-14)

| Spalte | Typ | Bedeutung |
| --- | --- | --- |
| `date` | TEXT, PK | ISO-Datum `2026-10-05` |
| `temperatureHundredths` | INTEGER? | Temperatur in 1/100 °C (36,55 °C = 3655), Originalwert (R-01) |
| `measurementTime` | TEXT? | `HH:mm` |
| `temperatureExcluded` | INTEGER (bool) | ausgeklammert (F-04) |
| `mucusSensation` | TEXT? | Code: `DRY`, `NOTHING_FELT`, `MOIST`, `WET` (ab 1.1), `WET_SLIPPERY` |
| `mucusAppearance` | TEXT? | Code: `NOTHING_SEEN`, `WHITISH_CLOUDY`, `THICK`, `LUMPY_YELLOWISH`, `GLASSY`, `CLEAR_STRETCHY`, `LIQUID` (siehe unten) |
| `bleeding` | TEXT? | Periode (F-07). Code: `SPOTTING`, `LIGHT`, `MEDIUM`, `HEAVY` |
| `intercourse` | TEXT? | Code: `PROTECTED`, `UNPROTECTED` |
| `mittelschmerz`, `breastSymptom` | INTEGER (bool) | optionale Felder (F-14) |
| `otherBleeding`, `cramps`, `nausea` | TEXT? | ab Version 2 (F-16). Code: `LIGHT`, `MEDIUM`, `HEAVY`; NULL = nicht |
| `diarrhea`, `vomiting` | INTEGER (bool), Default 0 | ab Version 2 (F-16) |
| `note` | TEXT? | Freitext |
| `updatedAt` | INTEGER | letzte Änderung, Epoch-Millisekunden |

Die Schleimkategorie (F-06) wird nicht gespeichert, sondern immer aus Empfinden und Aussehen abgeleitet.
Ein Tag ohne Inhalte wird gelöscht statt gespeichert.

### `cycles` – Periodenbeginne (F-08)

| Spalte | Typ | Bedeutung |
| --- | --- | --- |
| `startDate` | TEXT, PK | erster Tag des Zyklus; der Zyklus endet am Tag vor dem nächsten `startDate` |
| `excludedFromStatistics` | INTEGER (bool) | F-46 |
| `overrideFirstHigherMeasurement` | TEXT? | F-42, Datum |
| `overrideMucusPeak` | TEXT? | F-42, Datum |

### `disturbances` – Störfaktoren (F-09, F-10)

| Spalte | Typ | Bedeutung |
| --- | --- | --- |
| `id` | INTEGER, PK, autoincrement | |
| `predefinedCode` | TEXT? | Code eines vorbelegten Störfaktors (`ALCOHOL`, …), Name kommt aus den Ressourcen |
| `customName` | TEXT? | eigener Name oder Umbenennung |
| `archived` | INTEGER (bool) | archiviert, bleibt an alten Tagen sichtbar |
| `sortOrder` | INTEGER | Reihenfolge |

### `day_disturbances` – Zuordnung Tag ↔ Störfaktor

`(date, disturbanceId)` als Primärschlüssel, Fremdschlüssel mit `ON DELETE CASCADE`.

## Stabile Codes

Alle Aufzählungen werden als feste Texte gespeichert (`data/.../model/Codes.kt`), nie als Ordinalzahl.
Enum-Konstanten dürfen umbenannt werden, die Codes nie.

Feinere Schleimstufen (1.1): Die Codes `THICK` und `CLEAR_STRETCHY` bleiben; sie bezeichnen jetzt
„dicklich, cremig“ bzw. „dehnbar, fadenziehend“. Alte Einträge behalten dadurch ihre Kategorie (S bzw. S+),
die Auswertung ändert sich nicht.

## Einstellungen

DataStore Preferences (`settings`), siehe `AppSettings`. Gerätespezifische Werte (Sicherungsordner,
App-Sperre) werden nicht in Sicherungen übernommen.

## Schema ändern (N-07)

1. Entity ändern, `NfpDatabase.VERSION` erhöhen.
2. Migration in `Migrations.ALL` ergänzen (nie destruktiv).
3. Build ausführen; die neue Datei unter `data/schemas/` committen (CI prüft das).
4. In `MigrationTest` einen Test ergänzen, der Daten der alten Version anlegt, migriert und prüft.

## Daten bei Updates (N-07)

- Keine destruktiven Migrationen; fehlt eine Migration, stürzt die App ab, statt Daten zu löschen.
- `DatabaseSafety` kopiert vor der ersten Migration nach einem Update die alte Datei (inkl. `-wal`/`-shm`)
  nach `databases/easynfp.db.v<alte Version>.bak` (nur die letzte Kopie bleibt).
- `android:hasFragileUserData="true"`: Android fragt beim Deinstallieren, ob die Daten behalten werden sollen.
- Updates nur über die installierte App installieren, mit demselben Schlüssel und derselben Variante
  (Debug `de.easynfp.debug` und Release `de.easynfp` sind getrennte Apps mit getrennten Daten).

## Sicherungsdatei (F-50)

- Endung `.easynfp`, Inhalt UTF-8-JSON (`BackupData`) mit `formatVersion` (aktuell 2) und `app = "easyNFP"`.
- Version 2 (1.1): optionale Felder `otherBleeding`, `cramps`, `nausea`, `diarrhea`, `vomiting` und feinere Schleimcodes.
  Die Migration 1 → 2 ändert nichts; die neue Nummer sorgt dafür, dass ältere App-Versionen solche Dateien
  mit „bitte zuerst aktualisieren“ ablehnen, statt an unbekannten Codes zu scheitern.
- Verschlüsselt: Hülle `EncryptedEnvelope` mit `PBKDF2WithHmacSHA256` (310 000 Iterationen, 16 Byte Salt)
  und `AES/GCM/NoPadding` (256 Bit, 12 Byte IV). Ein falsches Passwort wird durch GCM sicher erkannt.
- Neue optionale Felder sind kompatibel. Umbenennen/Entfernen erfordert eine neue Formatversion und
  einen Schritt in `BackupMigrations`; der eingefrorene Test „version 1 file“ muss weiter bestehen.
- Dateien einer neueren Formatversion werden abgelehnt statt falsch gelesen.
