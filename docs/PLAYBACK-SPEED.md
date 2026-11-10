# Wiedergabegeschwindigkeit

Im großen Player öffnet der Button rechts neben „Ganzes Buch / Kapitel“ eine Auswahl von 0,5× bis 2×
in Schritten von 0,25. Standard ist 1×. Die Auswahl gilt für alle Bücher und wird
lokal in `library-selection/playbackSpeed` gespeichert. Ein Neustart startet
weiterhin kein Audio. Die Auswahl gehört wie die Sprungweite nicht zum Backup.

`TidalPlaybackSpeed` ist an TIDAL Player 0.0.73 gebunden. Wie der vorhandene
Lautstärkeadapter greift er über `SingleHandlerPlaybackEngine.handler/delegate`
zu; `ExoPlayerPlaybackEngine.getExtendedExoPlayer()` liefert den aktuellen
Media3-Player. Alle Zugriffe laufen auf dem SDK-Handler. Nach Laden und
Kapitelwechsel sowie im vorhandenen Player-Abfragetakt wird die Auswahl erneut
abgeglichen, damit auch ein neu erzeugter interner Player sie übernimmt.
Nur abweichende Parameter werden gesetzt. Die Tonhöhe bleibt auf 1.
Referenz: [Media3 PlaybackParameters](https://developer.android.com/reference/androidx/media3/common/PlaybackParameters).

Der Adapter verändert weder Play/Pause noch Position, Audiofokus oder Lautstärke.
Die MediaSession meldet die gewählte Geschwindigkeit für die Fortschrittsanzeige.
Hörstände und Sprungweiten bleiben Medien-Millisekunden, Minutentimer verwenden
weiterhin die monotone Echtzeit. Kein Room-Schemawechsel.

Bei inkompatiblem SDK wird eine lokalisierte Meldung angezeigt und der Button
deaktiviert. Bei SDK-Upgrades oder aktivierter Minifizierung müssen die
reflektierten Mitglieder erneut geprüft bzw. erhalten werden.

Der Instrumentierungstest `TidalPlaybackSpeedTest` prüft am echten SDK die
Parameter, unveränderte Tonhöhe, keinen Wiedergabestart und erneutes Setzen nach
Reset. Er benötigt ein Android-Gerät. Zusätzlich manuell prüfen: laufender
Stream bei 0,75×/1,5×/2×, automatischer Kapitelwechsel, Pause/Fortsetzen,
Prozessneustart mit gespeicherter Auswahl ohne Autoplay und Minutentimer.

## Prüfstand 10.11.2026

- Basis: aktueller GitHub-Stand `48270ac`, Branch `codex/playback-speed`.
- Mit lokal bereitgestelltem JDK 17: `assembleDebug`,
  `assembleDebugAndroidTest`, `testDebugUnitTest` und `lintDebug` erfolgreich.
- 67 JVM-Tests, keine Fehler, einschließlich zwei neuer Tests für gespeicherte
  Geschwindigkeitswerte und ungültige Werte.
- Kein Android-Gerät verbunden. Der neue SDK-Gerätetest ist kompiliert, aber
  noch nicht ausgeführt; reale Geschwindigkeitsänderung und Kapitelwechsel
  sind noch nicht akustisch bestätigt. Keine Installation oder Datenlöschung.

### Nachprüfung mit angeschlossenem Gerät

Am 10.11.2026 wurde die Debug-APK nach erfolgreichem Vergleich der
Signaturzertifikate per `adb install -r` auf dem angeschlossenen Gerät
aktualisiert. Keine App-Daten gelöscht. `TidalPlaybackSpeedTest` erfolgreich
(1 Test): 1,5×, 0,75×, 2× nach Reset und zurück auf 1×, unveränderte Tonhöhe
und kein Wiedergabestart. Die App wurde anschließend geöffnet. Ein akustischer
Test mit laufendem Stream und automatischem Kapitelwechsel bleibt offen.

### Release 0.5.0-beta5

VersionCode 24. Finale Position rechts neben „Ganzes Buch / Kapitel“;
derselbe FilterChip-Typ sorgt für gleiche sichtbare Höhe und Form.
Alle vier Projektprüfungen und `assembleRelease` erfolgreich, 67 JVM-Tests
ohne Fehler. Release-APK nicht debuggbar und mit demselben Signaturzertifikat
wie die bisherige Installation signiert. Auf dem angeschlossenen Gerät per
`install -r` installiert und erfolgreich gestartet; Version 0.5.0-beta5/24
bestätigt. Keine App-Daten gelöscht.

APK-SHA-256:
`aceca006347a943ed48996c6837f60207c8b93e0d127d8c40bbb996c05c502cc`
