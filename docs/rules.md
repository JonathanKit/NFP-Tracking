# Regelwerk (Sensiplan) – Umsetzung in easyNFP

Dieses Dokument beschreibt, wie das Modul `:rules` die Regeln aus den Anforderungen
(`docs/requirements.md`, Abschnitt 3) umsetzt. Jede Regel-ID erscheint im Code, in der
Begründung der App (F-41) und in mindestens einem Test.

Grundsätze:

- Das Regelwerk ist reines Kotlin ohne Android-Abhängigkeit. Eingabe sind Tagesbeobachtungen,
  Ausgabe ist ein `CycleEvaluation`-Objekt mit Phasen, Markierungen und Begründungsschritten.
- Die Auswertung ist zustandslos und deterministisch. Sie läuft nach jeder Datenänderung neu (F-40, F-13).
- **Im Zweifel fruchtbar.** Unklare Fälle werden nicht ausgewertet; die Begründung sagt, warum.
- Keine eigenen Heuristiken oder Vorhersagen in der Auswertung. Die gewünschte Prognose (F-47) steht
  getrennt davon unter „Weitere Ableitungen“ und beeinflusst keine Auswertung.

## Temperatur

| ID | Umsetzung | Code | Tests |
| --- | --- | --- | --- |
| R-01 | `Temperature.roundedForEvaluation()`: `((h + 2) / 5) * 5` in Hundertstel. Gespeichert wird der Originalwert. | `model/Temperature.kt` | `TemperatureRulesTest` |
| R-02 | Nur nicht ausgeklammerte Werte werden an den Auswerter übergeben (`DayObservation.evaluationTemperature`). Lücken werden übersprungen. | `model/DayObservation.kt` | `R-02 …` |
| R-03 | Kandidat, wenn der Wert höher ist als jeder der sechs vorangehenden gültigen Werte (nur innerhalb des Zyklus). Hilfslinie = Maximum dieser sechs. | `temperature/TemperatureEvaluator.kt` | `R-03 …` |
| R-04 | 2. und 3. Messung über der Hilfslinie, 3. mindestens 0,20 °C darüber → ausgewertet am Tag der 3. Messung. | `assess()` | `R-04 …` |
| R-05 | 3. Messung über der Hilfslinie, aber < 0,20 °C → 4. Messung muss über der Hilfslinie liegen. | `assess()` | `R-05 …` |
| R-06 | 2. **oder** 3. Messung auf/unter der Hilfslinie → zählt nicht; 4. Messung muss ≥ 0,20 °C darüber liegen. Fallen beide, scheitert der Kandidat. | `assess()` | `R-06 …` |
| R-07 | Ausnahmeregeln werden nie kombiniert. Scheitert ein Kandidat, wird ab dem nächsten Wert erneut gesucht; verworfene Kandidaten erscheinen in der Begründung. | `evaluate()` | `R-07 …` |
| R-08 | Nur Hinweise in der App (Startseite, „Hinweise zum Messen“, Rückfrage bei Störfaktoren). Nichts wird erzwungen. | App | – |

Endet ein Zyklus, bevor genügend Messungen vorliegen, ist die Temperatur „nicht auswertbar“.

## Zervixschleim

| ID | Umsetzung | Code | Tests |
| --- | --- | --- | --- |
| R-10 | `MucusCategory` ist in Rangfolge t < ø < f < S < S+ deklariert; Vergleich über die Enum-Reihenfolge. | `model/Mucus.kt` | `R-10 …` |
| R-11 | `MucusCategory.derive()`: S+ bei nass oder schlüpfrig/glitschig oder Aussehen glasig, dehnbar/fadenziehend, flüssig; S bei weißlich/trüb, dicklich/cremig, klumpig/gelblich; sonst nach Empfinden (t, ø, f). Die bessere Beobachtung zählt. Fehlendes Aussehen = nichts gesehen. Nur „nichts gesehen“ ohne Empfinden ergibt keine Kategorie. Die feineren Stufen (F-05) dienen nur der Erfassung; jede Stufe gehört fest zu einer Kategorie (`MucusSensation.category`, `MucusAppearance.category`). | `model/Mucus.kt` | `R-11 …` |
| R-12 | Tag für Tag: Kandidat ist der letzte Tag mit der bisher besten Qualität (mindestens f). Höhepunkt, wenn die drei Kalendertage danach eingetragen und schlechter sind. | `mucus/MucusEvaluator.kt` | `R-12 …` |
| R-13 | Abgeschlossen am Abend des 3. Tages nach dem Höhepunkt. | ebd. | `R-13 …` |
| R-14 | Gleiche (oder bessere) Qualität innerhalb der Zählung 1–2–3 → Zählung beginnt neu, wird in der Begründung vermerkt. | ebd. | `R-14 …` |
| R-15 | Nach abgeschlossener Schleim-, aber vor abgeschlossener Temperaturauswertung: gleiche oder bessere Kategorie → Neubeginn. Sind beide abgeschlossen, ändert späterer Schleim nichts. Ein Schleimeintrag am Tag des Temperaturabschlusses löst (vorsichtig) noch einen Neubeginn aus. | ebd. | `R-15 …` |
| R-16 | Fehlt ein Eintrag innerhalb der drei Folgetage, wird nicht ausgewertet; die App weist darauf hin („Offene Einträge“). Der heutige Tag des laufenden Zyklus gilt noch nicht als fehlend. | ebd. | `R-16 …` |

Konservative Festlegung (im Zweifel fruchtbar): Ein Höhepunkt setzt mindestens die Kategorie f voraus.
Zyklen nur mit t/ø haben keinen Schleimhöhepunkt und damit keine unfruchtbare Zeit nach dem Eisprung.

## Unfruchtbare Zeit nach dem Eisprung

| ID | Umsetzung |
| --- | --- |
| R-20 | `infertileFromEveningOfDay = max(Temperatur-Abschlusstag, Schleim-Abschlusstag)`. Dieser Tag selbst gilt bis zum Abend als fruchtbar (`DayPhase.infertileFromEvening`). |
| R-21 | Alle folgenden Tage des Zyklus sind `INFERTILE_END`; der Zyklus endet mit dem nächsten Periodenbeginn. |
| R-22 | Fehlt eine der beiden Auswertungen, bleibt der Zyklus nach der Anfangsphase `FERTILE`. |

## Unfruchtbare Zeit am Zyklusanfang

| ID | Umsetzung |
| --- | --- |
| R-30 | Nur wenn der unmittelbare Vorzyklus eine abgeschlossene Temperaturauswertung hat. Für den ersten erfassten Zyklus ist kein Vorzyklus bekannt → fruchtbar ab Tag 1. |
| R-31 | Weniger als 12 auswertbare Kurven → Tag 1–5 unfruchtbar. |
| R-32 | Weniger als 12 Kurven, aber eine 1. hM an Tag ≤ 12 → letzter unfruchtbarer Tag = früheste 1. hM − 8. |
| R-33 | Ab 12 auswertbaren Kurven → früheste 1. hM aller Kurven − 8 (kann kürzer oder länger als 5 Tage sein). Ergibt sich 0 oder weniger, gibt es keine unfruchtbaren Tage. |
| R-34 | Schleim f, S oder S+ beendet die Phase sofort; der Tag mit dieser Beobachtung ist fruchtbar. |
| R-35 | Minus-20-Regel: **nicht umgesetzt** (Entscheidung: nicht anbieten). |
| R-36 | Nachtragen früherer Zyklen: **nicht umgesetzt** (Entscheidung: ignorieren). |
| R-37 | Post-Pill-Regeln: **nicht umgesetzt** (nicht relevant). |

„Auswertbare Kurve“ = früherer Zyklus mit abgeschlossener Temperaturauswertung (auch manuell gesetzt).
Der Ausschluss eines Zyklus von der Statistik (F-46) ändert das Regelwerk nicht.

## Manuelle Übersteuerung (F-42)

Für einen Zyklus können die 1. höhere Messung und der Schleimhöhepunkt gesetzt werden.

- Manuelle 1. hM: Hilfslinie aus den sechs vorangehenden gültigen Werten, dann R-04 bis R-06.
  Passt der Tag nicht (kein Wert, < 6 Vorwerte, nicht höher, Regeln nicht erfüllt) → nicht auswertbar.
- Manueller Höhepunkt: abgeschlossen am Abend des 3. Folgetages; R-15 wird dann nicht angewendet.
- Beides ist in der Begründung als „manuell gesetzt“ (F-42) gekennzeichnet und rücknehmbar.

## Weitere Ableitungen

- **Neuer Zyklus vorschlagen (F-08):** erste Blutung (nicht Schmierblutung) nach abgeschlossener
  Temperaturauswertung. Die App setzt den Zyklusbeginn nie selbst.
- **Statistik (F-43):** nur abgeschlossene Zyklen; Mittel, Minimum, Maximum, Standardabweichung (Population).
- **Prognose (F-45):** Spanne aus kürzestem und längstem der letzten 6 abgeschlossenen Zyklen
  (mindestens 3). Ausdrücklich keine Fruchtbarkeitsaussage.
- **Prognose der kommenden Tage (F-47, `statistics/CycleForecast.kt`):** auf ausdrücklichen Wunsch der
  Nutzerin, **keine Regel und kein Teil der Auswertung** – sie ändert keine `CycleEvaluation`.
  Grundlage sind die letzten 6 abgeschlossenen Zyklen, die für die Statistik zählen (F-46), mindestens einer:
  - Zykluslänge = gerundeter Mittelwert; ist er schon überschritten, beginnt der nächste Zyklus morgen.
  - Periodentage = gerundeter Mittelwert der Blutungstage ab Tag 1.
  - Unfruchtbar nach dem Eisprung ab dem Abend des *spätesten* bisherigen Tages (vorsichtig).
    Ist der laufende Zyklus schon ausgewertet, gilt R-21 bis zum Zyklusende. Ist dieser Tag schon vorbei,
    ohne dass ausgewertet wurde, bleiben die restlichen Tage fruchtbar (im Zweifel fruchtbar).
  - Anfangsphase künftiger Zyklen mit R-30 bis R-33 unter der Annahme, dass der laufende Zyklus wie die
    bisherigen ausgewertet wird; Schleim wird nicht vorhergesagt.
  - Die Oberfläche zeigt Prognosetage immer blass und mit „?“ und hebt den heutigen Tag hervor.
  - Tests: `StatisticsTest` (`F-47 …`), `ChartModelTest`.

## Referenztests

`SensiplanEngineTest` enthält einen vollständigen typischen Zyklus, der genau das Beispiel aus F-41
ergibt („Temperatur ausgewertet am Tag 17 nach der regulären Regel; Schleimhöhepunkt Tag 15;
unfruchtbar ab Abend Tag 18“), sowie Ketten mehrerer Zyklen für R-30 bis R-33.

**Offen:** Die Beispielzyklen aus dem Sensiplan-Übungsheft und der Abgleich mit dem Praxisbuch
„Natürlich und sicher“ lagen bei der Umsetzung nicht vor. Sie sollten als weitere Referenztests in
`rules/src/test` ergänzt werden (Format siehe `TestCycles.kt`); die Lösungen im Heft sind die Sollwerte.
