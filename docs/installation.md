# easyNFP installieren

Diese Anleitung ist für alle, die noch nie eine App außerhalb des Play Stores installiert haben.
Du brauchst nur dein Android-Handy (Android 9 oder neuer) und etwa fünf Minuten. Für iPhones gibt es
easyNFP nicht.

> **Warum warnt Android?** easyNFP gibt es nicht im Play Store, du lädst die App direkt hier herunter.
> Android warnt bei solchen Apps grundsätzlich, egal von wem sie stammen. Die Warnungen sind bei
> diesem Weg normal. Lade die App aber nur über die Links auf dieser Seite herunter, nicht von anderen
> Webseiten.

Die Bezeichnungen der Schaltflächen unterscheiden sich je nach Hersteller und Android-Version etwas.
Wenn ein Text nicht genau passt, tippe auf die Schaltfläche mit der ähnlichsten Bedeutung.

## Schritt 1: App herunterladen

Öffne diese Seite **auf dem Handy** und tippe auf:

**[➜ easyNFP.apk herunterladen](https://github.com/JonathanKit/NFP-Tracking/releases/latest/download/easyNFP.apk)**

Fragt der Browser „Datei trotzdem herunterladen?“ oder „Diese Art von Datei kann schädlich sein“,
tippe auf **Trotzdem herunterladen**.

## Schritt 2: Datei öffnen

Wenn der Download fertig ist, tippe unten auf **Öffnen** – oder öffne die Datei über die
Benachrichtigung oder die App **Dateien** → **Downloads** → `easyNFP.apk`.

## Schritt 3: Installation erlauben (nur beim ersten Mal)

Android zeigt jetzt ungefähr: *„Dein Smartphone darf aus Sicherheitsgründen keine unbekannten Apps aus
dieser Quelle installieren.“*

1. Tippe auf **Einstellungen**.
2. Schalte **Dieser Quelle vertrauen** bzw. **Von dieser Quelle zulassen** ein.
3. Tippe auf **Zurück** (Pfeil oben links oder Zurück-Geste).

> **Samsung-Handys:** Erscheint stattdessen „Automatische Sperre“ / „Auto Blocker hat die Installation
> blockiert“, öffne **Einstellungen → Sicherheit und Datenschutz → Automatische Sperre** und schalte sie
> vorübergehend aus. Nach der Installation kannst du sie wieder einschalten.

## Schritt 4: Installieren

1. Tippe auf **Installieren**.
2. Möglicherweise meldet sich **Google Play Protect** („Unbekannte App“ oder „App scannen?“).
   - Bei „App zum Scannen senden?“ kannst du **Senden** oder **Nicht senden** wählen – beides funktioniert.
   - Bei „Unsichere App blockiert“ tippe auf **Weitere Details** → **Trotzdem installieren**.
3. Tippe auf **Öffnen**. Fertig!

## Schritt 5: Einrichten

- **Benachrichtigungen erlauben**, wenn die App fragt – sonst kommt die Morgen-Erinnerung nicht.
- **Widget hinzufügen**, um Zyklustag und Status auf einen Blick zu sehen: lange auf eine freie Stelle des Startbildschirms
  tippen → **Widgets** → **easyNFP** → auf den Startbildschirm ziehen.
- **Sicherung einrichten:** Die Daten liegen nur auf dem Handy. Unter **Einstellungen → Datensicherung**
  eine Sicherung exportieren oder die automatische Sicherung einschalten, damit bei Verlust oder Defekt
  des Handys nichts verloren geht.
- Optional kannst du die Erlaubnis aus Schritt 3 wieder entziehen: **Einstellungen → Apps →
  Spezieller App-Zugriff → Unbekannte Apps installieren** → Browser auswählen → ausschalten.
  Für spätere Updates musst du sie dann erneut erlauben.

## Updates installieren

Neue Versionen stehen unter [Releases](https://github.com/JonathanKit/NFP-Tracking/releases).
Für ein Update genauso vorgehen wie oben: herunterladen, öffnen, **Aktualisieren** bzw. **Installieren**.

> **Die App vor einem Update nicht deinstallieren!** Beim Deinstallieren werden alle Einträge gelöscht.
> Über die vorhandene App installieren behält alle Daten. Vorher eine Sicherung zu exportieren schadet
> trotzdem nicht.

## Wenn etwas nicht klappt

| Meldung | Lösung |
| --- | --- |
| „App nicht installiert“ beim Update | Die installierte Version stammt aus einer anderen Quelle (z. B. ein Test-Build). Sicherung exportieren, App deinstallieren, neu installieren, Sicherung importieren. |
| „Beim Parsen des Pakets ist ein Problem aufgetreten“ / „Paket ungültig“ | Der Download war unvollständig oder das Android ist älter als Version 9. Datei löschen und neu herunterladen. |
| Es passiert nichts beim Antippen der Datei | Die Datei über die App **Dateien** → **Downloads** öffnen. |
| Keine Morgen-Erinnerung | Benachrichtigungen für easyNFP in den Android-Einstellungen erlauben und den Akku-Sparmodus für easyNFP auf „Nicht eingeschränkt“ stellen. |

Nutze nur die Datei `easyNFP.apk` aus den Releases. Die Dateien unter „Actions“ sind Test-Builds für die
Entwicklung.

## Für Technikinteressierte: Datei prüfen

Zu jeder Version gibt es die Datei `easyNFP.apk.sha256` mit der SHA-256-Prüfsumme. Vergleich z. B. mit
`sha256sum easyNFP.apk` (Linux/macOS) oder `Get-FileHash easyNFP.apk` (Windows PowerShell).

## Ausblick

Google hat angekündigt, ab 2027 weltweit nur noch Apps von registrierten Entwicklern ohne Umwege
installieren zu lassen. Falls sich dadurch an der Installation etwas ändert, wird diese Anleitung
angepasst.

---

easyNFP ist ein privates Hobbyprojekt, kein Medizinprodukt und nicht zur Verhütung bestimmt. Mehr dazu
im [README](../README.md#wichtiger-hinweis).
