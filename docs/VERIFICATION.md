# Prüfung der privaten Hörbuchapp 0.3.0

## Automatisierte Prüfungen

31 JVM-Tests: Album-URL/ID, SDK-Login-Redirect, sichere Katalogpagination, Geräteanmeldung und vorübergehende Fehler, Timergrenzen/Ablauf/Verlängern/Abbrechen, Positionen an Track- und Discgrenzen, Wiederherstellung über Track-ID, Metadaten-Rundlauf sowie Timer nach Prozess-/Geräteneustart.

5 Android-Instrumentierungstests auf dem angeschlossenen Pixel 9 Pro (API 37), mit einer ausschließlich für Tests angelegten separaten Room-Datenbank:

- Zwei Bücher behalten unterschiedliche Track-IDs und Positionen nach dem Schließen und erneuten Öffnen der Datenbank.
- Ein doppelter Import setzt Fortschritt nicht zurück.
- Lesezeichen überleben erneutes Öffnen, lassen sich umbenennen und werden beim Entfernen des zugehörigen Buchs mitgelöscht.
- Einschlafanker und Deadline bleiben erhalten; ein geänderter Boot-Zähler aktiviert den Timer nicht erneut.
- Zwanzig schnell eingereihte Fortschrittsänderungen landen in derselben Reihenfolge in der Datenbank.

Die Tests greifen nicht auf Kontotokens oder die Nutzerdatenbank zu. Build und Android Lint werden zusätzlich ausgeführt. Die Versionshinweise des Lint-Berichts sind keine Funktionsnachweise. Konkretes abschließendes Ergebnis: `BUILD-RESULT.txt`.

## Gerätetests dieser Version

- APK-Update mit gleicher Signatur; vorhandene TIDAL-Anmeldung bleibt erhalten.
- Bisherigen Prototyp-Hörstand einmalig übernommen: Album 270256764, Abschnitt 2, 2755 ms. Die Übernahmedatei enthält nur Buch-ID, Index, Offset und Land; sie wird nach erfolgreichem Import gelöscht und ist nicht Bestandteil der APK.
- Lesezeichen über die echte Oberfläche angelegt, benannt, nach einem 30-Sekunden-Sprung angesprungen: pausiert wieder bei exakt 2755 ms. Unabhängigkeit vom Fortschritt bestätigt.
- App über `am force-stop` beendet und gestartet: Hörstand und benanntes Lesezeichen wiederhergestellt, Audio startet nicht. Nach vorherigem regulärem Pausieren betrug die Abweichung 148 ms.
- Gerätemedientaste PAUSE erreicht dieselbe Wiedergabesitzung und sichert den Fortschritt.
- Minutentimer über Oberfläche auf eine Minute gesetzt, App-Prozess beendet und neu gestartet: verbleibende Frist wurde weitergezählt, kein automatischer Wiedergabestart. Nach Ablauf bleibt die separat gespeicherte Startposition erhalten.
- Native Material-You-Oberfläche mit dynamischen Pixel-Systemfarben, echtem Cover, Player und Bibliothek visuell am Gerät geprüft.

- Prozessabbruch während FULL-Wiedergabe: vor Abbruch 12914 ms, nach Neustart pausiert bei 11305 ms. Beobachteter Fortschrittsverlust 1609 ms, innerhalb der Fünf-Sekunden-Vorgabe.
- Medienbenachrichtigung als tatsächlicher Vordergrunddienst mit MediaStyle und MediaSession-Token im Androidsystem nachgewiesen. Nach Korrektur werden drei Aktionen bereitgestellt: ±30 Sekunden und Play/Pause. Die finale zusätzliche Anpassung ordnet diese auch in kompakten Benachrichtigungen älterer Androidversionen an; ältere Geräte wurden nicht physisch getestet.
- Trackende-Timer bei Abschnitt 2 / 122755 ms gestartet: pausiert am Ende desselben Tracks bei 131506 ms, kein Übergang zum nächsten Abschnitt und über eine weitere Beobachtungszeit stabil pausiert. Der Einschlafanker bleibt erhalten.

## Bereits erbrachter Integrationsnachweis aus Version 0.2.2

Der offizielle persönliche Developer-Client lieferte PREVIEW. Mit dem alternativen öffentlichen Geräteprofil und der vom Nutzer freigegebenen Sitzung: FULL, vom Nutzer bestätigter hörbarer Stream über eine Minute, zusätzlich 62,2 Sekunden kontinuierliche Wiedergabe beobachtet; exakter Sprung auf 60000 ms; Hintergrund mit ausgeschaltetem Bildschirm; automatischer Trackwechsel 1 → 2 und Pause durch Ein-Minuten-Timer. Der Timerstopp lag konservativ einschließlich Abfrageintervall höchstens 1176 ms nach der Frist. Der Nutzer bestätigte das automatische Pausieren. Kein Neustart des Audios nach Ablauf.

## Noch manuell zu prüfen

Echter eingehender Telefonanruf, verschiedene Bluetooth-/Kopfhörertasten und Audioausgänge, längere Hintergrundwiedergabe unter Batteriesparmodus sowie ein absichtlich herbeigeführter Netzausfall während der Pufferung. Ein physischer Geräteneustart wurde nicht durchgeführt; die Boot-Zähler-Regel ist durch Unit- und Android-Datenbanktests abgedeckt. Eine vollständig offline verfügbare Hörbuchwiedergabe ist kein Merkmal dieser Version.
