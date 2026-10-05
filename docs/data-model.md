# Datenmodell

Datenbank: Room (SQLite), Datei `easynfp.db` im privaten App-Speicher. Aktuelle Version: **1**.
Schema-Historie: `data/schemas/de.easynfp.data.db.NfpDatabase/<version>.json` (wird beim Build erzeugt und eingecheckt).

## Tabellen

### `day_entries` – ein Datensatz pro Tag (F-01 bis F-14)

| Spalte | Typ | Bedeutung |
| --- | --- | --- |
| `date` | TEXT, PK | ISO-Datum `2026-10-05` |
| `temperatureHundredths` | INTEGER? | Temperatur in 1/100 °C (36,55 °C = 3655), Originalwert (R-01) |
| `measurementTime` | TEXT? | `HH:mm` |
| `temperatureExcluded` | INTEGER (bool) | ausgeklammert (F-04) |
| `mucusSensation` | TEXT? | Code: `DRY`, `NOTHING_FELT`, `MOIST`, `WET_SLIPPERY` |
| `mucusAppearance` | TEXT? | Code: `NOTHING_SEEN`, `THICK`, `CLEAR_STRETCHY` |
| `bleeding` | TEXT? | Code: `SPOTTING`, `LIGHT`, `MEDIUM`, `HEAVY` |
| `intercourse` | TEXT? | Code: `PROTECTED`, `UNPROTECTED` |
| `mittelschmerz`, `breastSymptom` | INTEGER (bool) | optionale Felder (F-14) |
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

## Einstellungen

DataStore Preferences (`settings`), siehe `AppSettings`. Gerätespezifische Werte (Sicherungsordner,
App-Sperre) werden nicht in Sicherungen übernommen.

## Schema ändern (N-07)

1. Entity ändern, `NfpDatabase.VERSION` erhöhen.
2. Migration in `Migrations.ALL` ergänzen (nie destruktiv).
3. Build ausführen; die neue Datei unter `data/schemas/` committen (CI prüft das).
4. In `MigrationTest` einen Test ergänzen, der Daten der alten Version anlegt, migriert und prüft.

## Sicherungsdatei (F-50)

- Endung `.easynfp`, Inhalt UTF-8-JSON (`BackupData`) mit `formatVersion` (aktuell 1) und `app = "easyNFP"`.
- Verschlüsselt: Hülle `EncryptedEnvelope` mit `PBKDF2WithHmacSHA256` (310 000 Iterationen, 16 Byte Salt)
  und `AES/GCM/NoPadding` (256 Bit, 12 Byte IV). Ein falsches Passwort wird durch GCM sicher erkannt.
- Neue optionale Felder sind kompatibel. Umbenennen/Entfernen erfordert eine neue Formatversion und
  einen Schritt in `BackupMigrations`; der eingefrorene Test „version 1 file“ muss weiter bestehen.
- Dateien einer neueren Formatversion werden abgelehnt statt falsch gelesen.
