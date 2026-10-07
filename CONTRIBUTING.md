# Mitmachen

Danke für dein Interesse! easyNFP ist ein kleines Hobbyprojekt. Fehlermeldungen und Vorschläge sind
willkommen; größere Änderungen bitte vorher in einem Issue besprechen.

## Fehler melden

- [Issue anlegen](https://github.com/JonathanKit/NFP-Tracking/issues/new/choose) und die Vorlage ausfüllen.
- **Keine persönlichen Zyklusdaten veröffentlichen.** Issues sind öffentlich. Für Fehler in der
  Auswertung den Demo-Modus nutzen oder die Werte frei erfundenen Tagen zuordnen.
- Sicherheitslücken vertraulich melden, siehe [SECURITY.md](SECURITY.md).
- Für Fragen zur Methode selbst ist eine Sensiplan-Beratung die richtige Stelle, nicht dieses Projekt.

## Code beitragen

Grundsätze (ausführlich in [CLAUDE.md](CLAUDE.md) und unter [docs/](docs/)):

- **Regelwerk** nur im Modul `:rules` ändern. Jede Regeländerung braucht einen Test mit der Regel-ID im
  Testnamen und einen Eintrag in [docs/rules.md](docs/rules.md). Im Zweifel fruchtbar, keine eigenen
  Heuristiken. Keine Texte, Grafiken oder Beispielzyklen aus urheberrechtlich geschützten Büchern übernehmen.
- **Texte** auf Deutsch in `app/src/main/res/values/strings.xml`; die Oberfläche kennt keine Regeln.
- **Datenbank:** Version erhöhen, Migration mit Test, Schema-JSON einchecken.
- **Sicherungsformat:** nur kompatibel erweitern oder Formatversion + Migration.
- **Keine Netzwerk-Berechtigung**, keine Analyse-, Absturz- oder Werbedienste. Die CI prüft das.

Vor dem Pull Request:

```bash
./gradlew check assembleDebug
./gradlew ktlintFormat        # Formatierung korrigieren
```

Mit einem Beitrag erklärst du dich einverstanden, dass er unter der [GPL-3.0](LICENSE) veröffentlicht wird.
