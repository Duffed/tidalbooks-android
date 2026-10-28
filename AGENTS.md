# Projekt: private TIDAL-Hörbuchapp

Lies README.md, WEITERENTWICKLUNG.txt, BUILD-RESULT.txt und docs/VERIFICATION.md vor Änderungen. Sprache der Oberfläche und Kommunikation: Deutsch.

- Android Kotlin/Compose/Room. Keine Webversion oder Server ergänzen, sofern nicht angefordert.
- Anmeldung und Vollwiedergabe funktionieren über das alternative öffentliche Geräteprofil von tidalapi. Details/Lizenz: docs/INTEGRATION.md und third_party/. Keine persönlichen Secrets, Kontotokens, Manifeste oder Stream-URLs protokollieren oder ins Repository schreiben.
- TIDAL-Engine assetPosition liefert Sekunden; seek(Float) nimmt in dieser gepinnten Integration Millisekunden. Nicht ohne Gerätetest vereinheitlichen.
- Der TIDAL-Player verwaltet Audiofokus selbst. Keine zusätzliche konkurrierende AudioManager-Anforderung ergänzen.
- Room-Änderungen geordnet über LibraryStore.write einreichen; snapshots müssen vor asynchronem Schreiben eingefroren sein. Neue Schemas mit Migrationen ergänzen, keine destruktive Migration oder Zurücksetzen von Hörständen.
- Fortschritt: Track-ID plus Millisekunden pro Buch; bei Neustart kein Autoplay. Ein fertiges Buch nur ausdrücklich von vorne starten.
- Lesezeichen bleiben unabhängig vom Hörfortschritt. Anspringen erhält Play/Pause.
- Timer nutzt elapsedRealtime plus Boot-Zähler; Frist zählt während Pause/Pufferung weiter. Separaten Einschlafanker bei Ablauf/Abbrechen erhalten.
- MediaSessionService muss seine Sitzung per addSession registrieren. Native Sprungtasten (5/15/30/60 s, lokal gespeichert) sind SessionCommands mit SLOT_BACK/SLOT_FORWARD; Provider deckt kompakte Benachrichtigungen ab.
- Prüfen: :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug. Fünf Android-Datenbanktests verwenden eine separate Testdatenbank. Der Signaturschlüssel ist absichtlich separat; beim Update auf dem Handy denselben Schlüssel benutzen, niemals einfach App-Daten löschen.
