# NFP-App – Requirements

Oct 5, 2026 · @Jonathan Helmond

> Dieses Dokument ist die verbindliche Anforderungsliste. Umsetzungsstand: siehe `docs/status.md`.
> Entscheidungen zu den offenen Fragen stehen in Abschnitt 7.

Eine Offline-Android-App für eine Nutzerin, die Zyklen nach dem Sensiplan-Regelwerk erfasst und nachvollziehbar auswertet. Die morgendliche Temperatureingabe dauert höchstens zwei Taps.

## 1. Ziel und Rahmen

Die App ersetzt das Papier-Zyklusblatt: erfassen, Kurve sehen, regelkonforme Auswertung mit Begründung bekommen.

| Punkt | Festlegung |
| --- | --- |
| Plattform | Android, ein Gerät, eine Nutzerin, kein Login |
| Betrieb | Vollständig offline, keine Server, keine Tracker, keine Netzwerk-Berechtigung |
| Regelwerk | Sensiplan (symptothermale Methode der Arbeitsgruppe NFP), keine eigenen Heuristiken oder Vorhersage-Algorithmen in der Auswertung |
| Sprache | Deutsch, Texte ausgelagert, damit später übersetzbar |
| Lebensdauer | Mehrjährige Nutzung: Datenmigrationen, Backup und Tests sind Pflicht, kein Extra |

Nicht im Umfang von Version 1: iOS, Mehrbenutzer, Cloud-Sync, Partner-Zugang, Community, Bluetooth-Thermometer, Wechseljahre- und Stillzeit-Regeln.

Die App ist kein zertifiziertes Medizinprodukt. Sie zeigt zu jeder Auswertung, welche Regel mit welchen Tagen gegriffen hat, damit die Nutzerin das Ergebnis selbst prüfen kann.

## 2. Funktionale Anforderungen

Priorität: M = Muss (Version 1), S = Soll (Version 1.x), K = Kann (später).

### 2.1 Tageseintrag

| ID | Anforderung | Prio |
| --- | --- | --- |
| F-01 | Temperatur in °C mit zwei Nachkommastellen erfassen, gültiger Bereich 34,00–42,00, sonst Rückfrage | M |
| F-02 | Messzeit wird automatisch mit der aktuellen Uhrzeit vorbelegt und ist änderbar | M |
| F-03 | Eingabe über ein Zahlenrad oder Stepper, vorbelegt mit dem Wert des Vortags; keine Tastatur nötig | M |
| F-04 | Temperatur pro Tag ausklammern (Schalter). Der Wert bleibt gespeichert und sichtbar, geht aber nicht in die Auswertung ein | M |
| F-05 | Zervixschleim getrennt erfassen, in nach Qualität geordneten Stufen: Empfinden (trocken, nichts gefühlt, feucht, nass, schlüpfrig/glitschig) und Aussehen (nichts gesehen, weißlich/trüb, dicklich/cremig, klumpig/gelblich, glasig/durchsichtig, dehnbar/fadenziehend, flüssig/wässrig) | M |
| F-06 | Die App leitet aus F-05 die Kategorie t, ø, f, S oder S+ ab und zeigt sie an; maßgeblich ist die beste Beobachtung des Tages | M |
| F-07 | Periode erfassen (Abschnitt „Periode“): Stärke (Schmierblutung, leicht, mittel, stark) | M |
| F-08 | Zyklusbeginn markieren („Neue Periode beginnt heute“). Die App schlägt das beim ersten Blutungstag nach einer Hochlage vor, setzt es aber nie ohne Bestätigung | M |
| F-09 | Störfaktoren pro Tag aus einer Liste zuordnen, Mehrfachauswahl; eigene Störfaktoren anlegen, umbenennen, archivieren | M |
| F-10 | Vorbelegte Störfaktoren: wenig oder schlecht geschlafen, spät ins Bett, Alkohol, Krankheit/Fieber, Stress, Reise/Zeitumstellung, abweichende Messzeit, Medikamente, anderes Thermometer | M |
| F-11 | Bei gesetztem Störfaktor fragt die App einmal, ob der Wert ausgeklammert werden soll; kein automatisches Ausklammern | M |
| F-12 | Freitext-Notiz pro Tag | M |
| F-13 | Jeder Tag, auch rückwirkend, ist bearbeitbar; die Auswertung rechnet danach neu | M |
| F-14 | Geschlechtsverkehr (geschützt/ungeschützt), Mittelschmerz, Brustsymptom als optionale Felder, in den Einstellungen abschaltbar | S |
| F-15 | Muttermund (Lage, Öffnung, Festigkeit) als Alternative zum Zervixschleim | K |
| F-16 | Abschnitt „Blutung und Symptome“: Blutung außerhalb der Periode, Krämpfe und Übelkeit jeweils nicht/leicht/mittel/stark; Durchfall und Erbrechen jeweils ja/nein. Gehen nicht in die Auswertung ein | M |

### 2.2 Schnelleingabe am Morgen

| ID | Anforderung | Prio |
| --- | --- | --- |
| F-20 | Homescreen-Widget zeigt Zyklustag und den heutigen Status; ein Tap aufs Widget öffnet die App, ein Tap auf „Temperatur eintragen“ direkt die Temperatureingabe, Speichern schließt sie wieder | M |
| F-21 | Die Schnelleingabe speichert Wert und Uhrzeit des Speicherns in höchstens zwei Taps, wenn der Wert nahe am Vortag liegt | M |
| F-22 | Dunkle Darstellung in der Schnelleingabe, unabhängig vom Systemmodus. Die Bildschirmhelligkeit wird nicht verändert (weder heller noch dunkler) | M |
| F-23 | Tägliche Erinnerung zu einer einstellbaren Uhrzeit; Tap führt in die Schnelleingabe; entfällt, wenn schon eingetragen | S |
| F-24 | Abend-Erinnerung für Zervixschleim, abschaltbar | S |

### 2.3 Zyklusblatt und Kalender

| ID | Anforderung | Prio |
| --- | --- | --- |
| F-30 | Zyklusblatt je Zyklus: ganz oben Datum und Zyklustag, darunter Phasenband und Temperaturkurve (Kurvenbereich 30 % höher als in Version 1.0), darunter Zeilen für Periode, Schleimkategorie, Höhepunkt, Störfaktoren. Die Legende öffnet sich über ein kleines „i“ | M |
| F-31 | In der Kurve sichtbar: ausgeklammerte Werte (eingeklammert, nicht verbunden), die sechs Referenzwerte, Hilfslinie, höhere Messungen (die ersten drei – bei Ausnahmeregeln auch die vierte – eingekreist), die Spalte der 1. höheren Messung farbig hinterlegt, Schleimhöhepunkt mit den Folgetagen 1–2–3 | M |
| F-32 | Phasen als Farbband: unfruchtbar am Zyklusanfang, fruchtbar, unfruchtbar nach dem Eisprung; unfruchtbare Tage zusätzlich dezent über die ganze Kurvenhöhe hinterlegt | M |
| F-33 | Tap auf einen Tag in der Kurve öffnet den Tageseintrag | M |
| F-34 | Wischen wechselt zwischen Zyklen; horizontal scrollbar bei langen Zyklen | M |
| F-35 | Monatskalender mit Blutung, Phasenfarbe und Markierung für eingetragene Tage; Tap öffnet den Tag | M |
| F-36 | Startseite „Heute“: Zyklustag, aktueller Status in einem Satz, offene Einträge, Schnellzugriff | M |
| F-37 | Zwei Zyklen übereinanderlegen zum Vergleich | K |

### 2.4 Auswertung und Statistik

| ID | Anforderung | Prio |
| --- | --- | --- |
| F-40 | Automatische Auswertung nach Abschnitt 3 bei jeder Datenänderung | M |
| F-41 | Begründung in Klartext zu jedem Ergebnis, zum Beispiel: „Temperatur ausgewertet am Tag 17 nach der regulären Regel; Schleimhöhepunkt Tag 15; unfruchtbar ab Abend Tag 18“ | M |
| F-42 | Manuelle Übersteuerung je Zyklus: Schleimhöhepunkt und erste höhere Messung selbst setzen; gekennzeichnet und rücknehmbar | S |
| F-43 | Statistik über wählbaren Zeitraum: Zykluslänge (Mittel, Minimum, Maximum, Standardabweichung), Tag der ersten höheren Messung (Mittel, frühester, spätester), Länge der Hochlage, Länge der fruchtbaren Phase, unfruchtbare Tage am Anfang und am Ende | M |
| F-44 | Diagramme: Zykluslängen und Tag der ersten höheren Messung über die Zeit | S |
| F-45 | Prognose der nächsten Periode als Zeitspanne aus den bisherigen Zyklen, klar als Schätzung beschriftet und nie als Fruchtbarkeitsaussage | S |
| F-46 | Zyklen von der Statistik ausschließen (zum Beispiel krank, nicht auswertbar) | S |
| F-47 | Prognose im Zyklusblatt und im Kalender: fruchtbare und unfruchtbare Tage sowie Zyklusende/nächste Periode für die kommenden Tage, aus den letzten Zyklen geschätzt. Auf ausdrücklichen Wunsch der Nutzerin, entgegen Abschnitt 5 („bewusst nicht empfohlen“). Prognosetage sind blass und mit „?“ markiert, der heutige Tag ist deutlich hervorgehoben, damit Prognose und Auswertung nicht verwechselt werden. Die Auswertung nach Abschnitt 3 bleibt davon unberührt | S |
| F-48 | Demo-Modus in den Einstellungen: erfundene Daten (14 Zyklen) in einer eigenen Datenbank zum Ausprobieren der Oberfläche; echte Daten bleiben unverändert, automatische Sicherung ruht, deutlicher Hinweis „Demo-Modus“ auf jedem Bildschirm | S |

### 2.5 Daten

| ID | Anforderung | Prio |
| --- | --- | --- |
| F-50 | Export und Import einer vollständigen Sicherungsdatei über den Android-Dateidialog; optional mit Passwort verschlüsselt | M |
| F-51 | Automatische Sicherung in einen gewählten Ordner in einstellbarem Abstand, mit den letzten N Ständen | S |
| F-52 | Zyklusblatt als PDF exportieren, zum Beispiel für die Frauenärztin | S |
| F-53 | CSV-Export aller Tageseinträge | S |
| F-54 | App-Sperre per Biometrie oder PIN, abschaltbar; das Widget zeigt bei aktiver Sperre keine Werte | S |
| F-55 | Alle Daten löschen, mit doppelter Bestätigung | M |

## 3. Regelwerk (Sensiplan)

Maßgeblich ist die gelieferte Übersicht zum NFP-Regelwerk nach Sensiplan. Umsetzung im Detail: `docs/rules.md`.

### 3.1 Temperatur

| ID | Regel |
| --- | --- |
| R-01 | Werte werden für die Auswertung auf 0,05 °C gerundet (36,21–36,22 → 36,20; 36,23–36,24 → 36,25). Gespeichert wird der Originalwert |
| R-02 | Ausgeklammerte Werte und Tage ohne Messung werden übersprungen, als gäbe es sie nicht |
| R-03 | Erste höhere Messung (1. hM): ein Wert, der höher ist als jeder der sechs unmittelbar vorangehenden gültigen Werte. Der höchste dieser sechs bestimmt die Hilfslinie |
| R-04 | Reguläre Regel: Die 2. und 3. Messung liegen ebenfalls über der Hilfslinie, die 3. mindestens 0,2 °C darüber. Dann ist die Temperatur am Tag der 3. Messung ausgewertet |
| R-05 | 1. Ausnahmeregel: Liegt die 3. Messung über der Hilfslinie, aber weniger als 0,2 °C, wird eine 4. Messung abgewartet. Sie muss über der Hilfslinie liegen, nicht zwingend 0,2 °C |
| R-06 | 2. Ausnahmeregel: Fällt die 2. oder 3. Messung auf oder unter die Hilfslinie, zählt sie nicht. Eine 4. Messung wird abgewartet und muss mindestens 0,2 °C über der Hilfslinie liegen |
| R-07 | Die beiden Ausnahmeregeln dürfen nicht kombiniert werden. Greift keine Regel, wird die nächste mögliche 1. hM gesucht |
| R-08 | Hinweise in der App, nicht erzwungen: nur nach oben gestörte Werte und nur Werte in der Tieflage ausklammern; Messort im Zyklus beibehalten; nach mindestens einer Stunde Schlaf vor dem Aufstehen messen |

### 3.2 Zervixschleim

| ID | Regel |
| --- | --- |
| R-10 | Rangfolge der Kategorien: t < ø < f < S < S+ |
| R-11 | t: Empfinden trocken, nichts gesehen. ø: nichts gefühlt, nichts gesehen. f: feucht, nichts gesehen. S: feucht oder nichts gefühlt, Aussehen dicklich, weißlich, trüb, cremig, klumpig, gelblich. S+: nass/schlüpfrig oder Aussehen glasig, dehnbar, fadenziehend, flüssig |
| R-12 | Schleimhöhepunkt: der letzte Tag mit der individuell besten Qualität des Zyklus, auf den drei Tage mit schlechterer Qualität folgen. Er ist erst rückblickend feststellbar |
| R-13 | Die Schleimauswertung ist am Abend des 3. Tages nach dem Höhepunkt abgeschlossen |
| R-14 | Sonderregel 1: Kehrt innerhalb der Zählung 1–2–3 Schleim der gleichen Kategorie wie am Höhepunkt wieder, beginnt die Schleimauswertung neu |
| R-15 | Sonderregel 2: Tritt nach abgeschlossener Schleimauswertung, aber vor abgeschlossener Temperaturauswertung wieder Schleim der gleichen oder besseren Kategorie wie am Höhepunkt auf, beginnt die Schleimauswertung neu. Sind beide Auswertungen abgeschlossen, ändert späterer Schleim nichts mehr |
| R-16 | Tage ohne Schleimeintrag innerhalb der drei Folgetage: nicht auswerten und die Nutzerin darauf hinweisen |

### 3.3 Unfruchtbare Zeit nach dem Eisprung

| ID | Regel |
| --- | --- |
| R-20 | Doppelte Kontrolle: Die unfruchtbare Zeit beginnt am Abend des Tages, an dem die zuletzt abgeschlossene der beiden Auswertungen (R-04 bis R-06, R-13) endet |
| R-21 | Sie dauert bis zum Beginn der nächsten Periode |
| R-22 | Ohne abgeschlossene Auswertung beider Zeichen bleibt der Zyklus ab Ende der Anfangsphase als fruchtbar markiert |

### 3.4 Unfruchtbare Zeit am Zyklusanfang

| ID | Regel |
| --- | --- |
| R-30 | Zusatzregel 2: Eine unfruchtbare Zeit am Zyklusanfang gibt es nur, wenn der Vorzyklus eine auswertbare Temperaturhochlage mit den erforderlichen höheren Messungen hatte (R-04 bis R-06). Sonst gilt der neue Zyklus ab Tag 1 als fruchtbar |
| R-31 | 5-Tage-Regel: Die ersten 5 Zyklustage gelten als unfruchtbar, solange weniger als 12 auswertbare Temperaturkurven vorliegen |
| R-32 | Zusatzregel 1: Fällt die 1. hM schon während der ersten 12 Zyklen einmal auf Zyklustag 12 oder früher, gilt von da an nicht mehr die 5-Tage-Regel, sondern „früheste 1. hM minus 8“ als letzter unfruchtbarer Tag |
| R-33 | Minus-8-Regel: Ab 12 auswertbaren Temperaturkurven ist der letzte unfruchtbare Tag am Zyklusanfang der Tag der frühesten 1. hM aus allen Kurven minus 8. Sie ersetzt dann die 5-Tage-Regel und kann die Phase verkürzen oder verlängern |
| R-34 | Doppelte Kontrolle: Schleim der Kategorie f, S oder S+ beendet die unfruchtbare Anfangsphase sofort, unabhängig von R-31 bis R-33 |
| R-35 | Minus-20-Regel (kürzester bekannter Zyklus minus 20) nur als abschaltbare Option, standardmäßig aus; sie wird nur bedingt empfohlen — **Entscheidung: nicht anbieten** |
| R-36 | Früher erfasste Zyklen (Papier, andere App) können mit Länge und Tag der 1. hM nachgetragen werden und zählen für R-32 bis R-35 — **Entscheidung: ignorieren** |
| R-37 | Post-Pill-Regeln nach Absetzen hormoneller Verhütung — **Entscheidung: nicht relevant** |

### 3.5 Umsetzung

- Das Regelwerk ist ein eigenes Modul ohne Android-Abhängigkeit: Eingabe sind die Tageseinträge, Ausgabe ist ein Ergebnisobjekt mit Phasen, Markierungen und der angewendeten Regel-ID je Schritt.
- Jede Regel-ID hat mindestens einen Testfall. Zusätzlich werden die Beispielzyklen aus dem Sensiplan-Übungsheft als Referenztests nachgebaut; die Lösungen dort sind die Sollwerte.
- Unklare Fälle wertet die App nicht aus und sagt, warum. Im Zweifel fruchtbar.

## 4. Nicht-funktionale Anforderungen

| ID | Anforderung |
| --- | --- |
| N-01 | Offline: Die App fordert die INTERNET-Berechtigung nicht an. Keine Analyse-, Werbe- oder Absturz-Dienste |
| N-02 | Datenschutz: Daten liegen nur im privaten App-Speicher; Android-Cloud-Backup ist deaktiviert, Sicherung läuft nur über F-50 und F-51 |
| N-03 | Bedienung: Schnelleingabe ist in unter einer Sekunde bedienbar; jede Hauptfunktion ist von der Startseite in höchstens zwei Taps erreichbar |
| N-04 | Übersicht: untere Navigation mit vier Zielen (Heute, Zyklusblatt, Kalender, Statistik); Einstellungen über ein Symbol |
| N-05 | Barrierefreiheit: Phasen nie nur über Farbe, sondern auch über Muster oder Text; Schriftgröße folgt dem System; Hell- und Dunkelmodus |
| N-06 | Architektur: getrennte Module für Regelwerk, Datenhaltung, Oberfläche und Widget. Die Oberfläche kennt keine Regeln, das Regelwerk kennt keine Datenbank |
| N-07 | Datenmodell: versioniertes Schema mit getesteten Migrationen; kein Update darf Daten verlieren (keine destruktiven Migrationen, Kopie der alten Datenbank vor jeder Migration, gleicher Signaturschlüssel, Android fragt beim Deinstallieren, ob die Daten behalten werden sollen). Sicherungsdateien tragen eine Formatversion und bleiben importierbar |
| N-08 | Tests: Unit-Tests für das gesamte Regelwerk (Ziel 100 % der Regel-IDs), Migrationstests, UI-Tests für die Schnelleingabe und den Tageseintrag |
| N-09 | Qualitätssicherung: CI baut bei jedem Commit, führt Tests und Lint aus und liefert eine installierbare APK |
| N-10 | Android-Version: mindestens Android 9 (API 28), Ziel ist die jeweils aktuelle API |
| N-11 | Verteilung: signierte APK zur direkten Installation; kein Play Store nötig. Der Signaturschlüssel wird gesichert, sonst sind spätere Updates nicht installierbar |
| N-12 | Dokumentation im Repository: Architekturübersicht, Regelwerk mit IDs, Datenmodell, Anleitung für Build und Release |

## 5. Feature-Vorschläge

| Vorschlag | Nutzen | ID |
| --- | --- | --- |
| Begründung der Auswertung in Klartext | Vertrauen und Fehlersuche; bei einer Verhütungsmethode wichtiger als jede Statistik | F-41 |
| Sicherung mit automatischem Export | Jahre an Daten hängen sonst an einem Handy | F-50, F-51 |
| Frühere Zyklen nachtragen | Minus-8-Regel greift sofort richtig, statt 12 Zyklen neu zu sammeln | R-36 |
| Vorbelegung mit dem Vortageswert | Macht die Zwei-Tap-Eingabe erst möglich | F-03, F-21 |
| Dunkle Schnelleingabe | Eingabe im dunklen Schlafzimmer direkt nach dem Aufwachen | F-22 |
| Manuelle Übersteuerung | Sonderfälle, die das Buch dem Urteil der Anwenderin überlässt | F-42 |
| Erinnerungen morgens und abends | Lücken im Schleimeintrag verhindern die Auswertung | F-23, F-24 |
| PDF des Zyklusblatts | Arzttermin oder NFP-Beratung | F-52 |
| Prognose der nächsten Periode | Planung im Alltag; strikt getrennt von der Fruchtbarkeitsaussage | F-45 |

Bewusst nicht empfohlen (Ausnahme auf Wunsch der Nutzerin: F-47, klar als Prognose gekennzeichnet): Eisprung-Vorhersage in die Zukunft, „sichere Tage“ im Voraus, Stimmungs- und Symptomtagebuch mit vielen Feldern, Gamification, Cloud-Konto.

Für später denkbar: Kinderwunsch-Modus, Partner-Ansicht nur lesend, Bluetooth-Thermometer, iOS.

## 6. Tech-Stack

Native Android-App in Kotlin mit Jetpack Compose (Material 3), Widget mit Jetpack Glance, Room, DataStore,
reines Kotlin-Modul für das Regelwerk, eigene Canvas-Zeichnung für das Zyklusblatt, AlarmManager/WorkManager,
kotlinx.serialization + Storage Access Framework für Sicherungen, GitHub Actions als CI.
Details: `docs/architecture.md`.

## 7. Entscheidungen zu den offenen Fragen

- Zweck: **Verhütung** – die Phasenanzeige ist streng (im Zweifel fruchtbar) und prominent.
- Bereits aufgezeichnete Zyklen: **ignorieren** (kein Import, R-36 nicht umgesetzt).
- Thermometer: **zwei Nachkommastellen, zeigt den letzten Wert erneut an** → Vorbelegung mit dem letzten Wert.
- Hormonelle Verhütung kürzlich: **nein** (R-37 nicht umgesetzt).
- Minus-20-Regel: **nicht anbieten** (R-35 nicht umgesetzt).
- Praxisbuch „Natürlich und sicher“: **liegt nicht vor** – R-16 umgesetzt wie vorgeschlagen; Abgleich steht aus.
- Name und Farbwelt: **easyNFP**, simpel und modern.
- Prognose fruchtbar/unfruchtbar und Zyklus (Oktober 2026): **gewünscht**, obwohl sie dem Regelwerk widerspricht →
  F-47, strikt getrennt von der Auswertung und deutlich als Prognose gekennzeichnet.
- Feedback nach dem ersten Gerätetest (Oktober 2026): F-05, F-07, F-16, F-20, F-22, F-30 – F-32, F-47, F-48, N-07 angepasst.

## Quellen

- [myNFP: NFP-Regeln im Überblick](https://www.mynfp.de/nfp-regeln)
- [myNFP: Basaltemperatur messen und Temperaturkurve auswerten](https://www.mynfp.de/temperatur-auswerten)
- [myNFP: Zervixschleim beobachten und auswerten](https://www.mynfp.de/zervixschleim-beobachten)
- [myNFP: Unfruchtbare Tage nach dem Eisprung bestimmen](https://www.mynfp.de/temperatur-und-zervixschleim-kombinieren)
- [myNFP: Unfruchtbarkeit am Zyklusanfang](https://www.mynfp.de/unfruchtbarkeit-am-zyklusanfang)
- [Sensiplan-Übungsheft, Malteser Arbeitsgruppe NFP](https://sensiplan.profilschmiede.de/storage/media/documents/45/sensiplan-uebungsheft-web.pdf)
- Maßgeblich: Arbeitsgruppe NFP, „Natürlich und sicher – Das Praxisbuch“
