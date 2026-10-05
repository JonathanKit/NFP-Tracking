# Hinweise für Agenten-Sitzungen

- Anforderungen: `docs/requirements.md` (IDs F-xx, R-xx, N-xx). Stand: `docs/status.md`. Beides aktuell halten.
- Regelwerk nur in `:rules` ändern; jede Regeländerung braucht einen Test mit der Regel-ID im Testnamen
  und einen Eintrag in `docs/rules.md`. Im Zweifel fruchtbar, keine eigenen Heuristiken.
- Oberfläche kennt keine Regeln: Texte zu Auswertungen entstehen in `app/.../text/ExplanationFormatter.kt`
  aus `ExplanationStep`s. Alle Texte in `app/src/main/res/values/strings.xml` (Deutsch).
- Datenbankänderung: Version erhöhen, Migration + Test, Schema-JSON committen (`docs/data-model.md`).
- Sicherungsformat: nur kompatibel erweitern oder Formatversion + Migration (`backup/`).
- Keine Netzwerk-Berechtigung, keine Analyse-/Absturzdienste hinzufügen.

## Prüfen

```bash
./gradlew check assembleDebug                 # vollständig (Android SDK + Google Maven nötig)
./gradlew -Peasynfp.jvmOnly=true check        # ohne Android: :rules, :backup + ktlint/detekt aller Quellen
./gradlew -Peasynfp.jvmOnly=true ktlintFormat # Formatierung korrigieren
```

In Umgebungen ohne Zugriff auf Google Maven (`dl.google.com`) lassen sich die Android-Module nicht
kompilieren; dann die CI (GitHub Actions) als Prüfung nutzen.
