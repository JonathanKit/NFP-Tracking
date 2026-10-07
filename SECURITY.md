# Sicherheit

easyNFP speichert Gesundheitsdaten. Sicherheitslücken bitte **nicht** als öffentliches Issue melden,
sondern vertraulich über **Security → Report a vulnerability** in diesem Repository
([direkt melden](https://github.com/JonathanKit/NFP-Tracking/security/advisories/new)).

Besonders relevant sind zum Beispiel:

- Daten verlassen das Gerät ungewollt (die App darf keine Netzwerk-Berechtigung haben)
- Schwächen der verschlüsselten Sicherung (AES-GCM, `backup/`)
- Umgehung der App-Sperre
- Andere Apps können auf die Daten oder exportierte Dateien zugreifen

Unterstützt wird jeweils nur die [neueste Version](https://github.com/JonathanKit/NFP-Tracking/releases/latest).
easyNFP ist ein Hobbyprojekt; Meldungen werden nach Möglichkeit zeitnah beantwortet, eine feste
Reaktionszeit gibt es nicht.
