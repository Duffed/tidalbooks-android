# Prüfung der privaten Hörbuchapp 0.3.0

## Ergänzung: Einheitliches Kartenraster (28.10.2026)

Die zwischenzeitlich verkürzten und zentrierten Balken wurden an die volle Inhaltsbreite angepasst. Buchkarten verwenden einen gemeinsamen Rand von 12 dp für Cover, Text und Fortschritt; die Balken sind 6 dp hoch. Titel haben bis zu drei Zeilen, Textabstände sind vereinheitlicht. Der Weiterhören-Balken schließt mit seiner Textspalte ab. Alle vier Buildprüfungen erfolgreich, Karten am Pixel visuell geprüft und Test-App aktualisiert.

## Ergänzung: Karten und feste Player-Aktionen (28.10.2026)

Bibliothekskarten haben eine kontrastreichere Fläche, Kontur und Schatten. Die Fortschrittsbalken in Buch- und Weiterhören-Karten sind 8 dp hoch und auf 78 Prozent der Inhaltsbreite begrenzt; ihre Spur ist auch auf der Weiterhören-Karte sichtbar. Der Einleitungsblock mit „Zeit für eine gute Geschichte“ ist entfernt. Merken/Timer/Abschnitte/Stellen liegen im großen Player als feste `Scaffold.bottomBar` mit Hintergrund, abgerundeten oberen Ecken und Abstand zur Systemnavigation. Die Inhaltsliste berücksichtigt die Leistenhöhe.

Alle vier Buildprüfungen erfolgreich; 31 JVM-Tests bestanden und Lint ohne Fehler. Bibliothekskarten mit vorhandenem Hörfortschritt und feste Aktionsleiste am Pixel visuell geprüft. Separate Test-App aktualisiert und gestartet. Unveränderte Gerätetests für diese Layoutkorrektur nicht erneut ausgeführt.

## Ergänzung: Ruhiges Design und Ziehen über den Inhalt (28.10.2026)

Der große Player lässt sich jetzt auch über Cover und scrollbaren Inhalt herunterziehen. Nested Scroll gibt zunächst der Inhaltsliste den Weg zurück zum Anfang; danach bewegt die verbleibende Geste den Player. Aus der Vollansicht reichen 96 dp langsamer Zug zum Minimieren, alternativ ein schneller Wisch. Horizontaler Fortschrittsregler und Aufwärtsscrollen behalten ihre Funktion.

Gestaltung: dezente Buchkarten, klare Bibliotheksüberschrift, Serifenschrift für den Buchtitel im Player, größere Cover mit weichem Schatten, sanfter Flächenverlauf und sichtbarer Ziehgriff. Animationen: Druckbewegung der Karten und Tasten, Fortschrittsbalken, Covergröße bei Play/Pause, Abschnittwechsel, Lesezeichenbestätigung erst nach Erscheinen des gespeicherten Lesezeichens, Play/Pause im Mini-Player und Farb-/Symbolwechsel der Aktionen. Bestehende Listen-, Tab- und Playerübergänge bleiben erhalten. Keine zusätzlichen Endlosschleifen für Dekoration.

Alle vier Buildprüfungen erfolgreich, 31 JVM-Tests bestanden, Lint ohne Fehler (24 Warnungen). Auf Pixel 9 Pro bestanden zehn Gerätetests: fünf Room-Tests und fünf Gestentests einschließlich Ziehen vom scrollbaren Cover, horizontalem Spulen ohne Minimieren und Aufwärtsscrollen im Player. Bibliothek und Vollansicht mit vorhandenem Buch visuell geprüft; langsames Herunterziehen vom echten Cover führt wieder zur Bibliothek. Separate Test-App aktualisiert und gestartet, Bildschirmzeit auf ursprüngliche 120000 ms zurückgesetzt.

## Ergänzung: Ziehbarer, wachsender Player (28.10.2026)

Der frühere Seitenwechsel wurde durch eine dauerhaft über der Bibliothek liegende Player-Fläche ersetzt. Sie wächst aus der Position des Mini-Players zur Vollansicht, einschließlich Breite und Eckenradius. Die Bibliothek bleibt komponiert und unbewegt, ohne weißen Ersatzhintergrund oder Abdunklung. Während des Ziehens folgt die Fläche direkt dem Finger; beim Loslassen entscheidet Position bzw. Wischgeschwindigkeit über das Einrasten mit Compose-Federanimation. Mini-Player hochziehen oder antippen; die Kopfzeile des geöffneten Players zum Minimieren herunterziehen. Die Inhaltsliste bleibt separat scrollbar.

Zwei neue Compose-Gerätetests auf Pixel 9 Pro bestanden: langsames Ziehen mit gehaltenem Finger, unveränderte Bibliotheksgrenzen und Zurückziehen; Antippen zur Vollansicht und Herunterziehen zur Mini-Ansicht. Zusätzlich fünf Room-Gerätetests bestanden. Mini- und Vollansicht mit echtem Buch visuell geprüft. Bestehende Anmeldung und Buch waren nach dem Update weiterhin verfügbar. Das zeitweise verlängerte Bildschirm-Timeout wurde wieder auf 120000 ms zurückgesetzt.

Für die UI-Tests wurden AndroidX Test Runner 1.7.0, JUnit Extension 1.3.0 und Espresso 3.7.0 eingebunden. Espresso 3.7.0 behebt den auf dem Pixel beobachteten Zugriff auf die entfernte InputManager.getInstance-Methode (Quelle: https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0). Die folgenden Abschnitte dokumentieren frühere Übergangsvarianten.

## Ergänzung: Hochfahrender Player (28.10.2026)

Der Player fährt jetzt in 380 ms von unten über die unbewegte Hauptansicht. Beim Minimieren über den Pfeil nach unten oder Zurück fährt er in 320 ms wieder hinaus. Seine Ebene bleibt während des Schließens über der Bibliothek. Filter und Scrollpositionen werden weiterhin erhalten; die System-Animationsdauer gilt auch für diesen Übergang.

Alle vier vorgeschriebenen Buildprüfungen erfolgreich; separate Test-App auf dem Pixel per `install -r` aktualisiert und erfolgreich gestartet. Die Sichtprüfung des neuen Übergangs (einschließlich schnellem Öffnen/Schließen und ausgeschalteten Systemanimationen) steht noch aus. Die folgende 240-ms-Überblendung beschreibt den vorherigen Stand.

## Ergänzung: Compose-Animationen (28.10.2026)

Nachfolgend separat mit `-PseparateApp=true` als **Hörbuch Test** (`de.tidalbooks.prototype.testapp`) gebaut und auf dem Pixel 9 Pro installiert. Alle vier Buildprüfungen erfolgreich; die fünf Room-Gerätetests mit isolierter Testdatenbank bestanden. App-Start erfolgreich, beide App-IDs parallel vorhanden. Die ursprüngliche App wurde nicht aktualisiert. Die Test-App benötigt eine eigene TIDAL-Anmeldung und Bibliothek; die visuelle Animationsprüfung mit Büchern steht noch aus.

Player und Hauptansicht überblenden in 240 ms, Bibliothek/Lesezeichen/Einstellungen in 180 ms. SaveableStateHolder erhält dabei Filter und Scrollpositionen. Bücher und Lesezeichen verwenden stabile Schlüssel und Compose-Itemanimationen beim Hinzufügen, Entfernen und Umordnen. Geladene Cover blenden in 220 ms ein, die Wiedergabetaste wechselt in 150 ms zwischen Laden/Play/Pause, die Timer-Aktionsfarbe in 180 ms. Die Animationen verwenden die systemgesteuerte Compose-Animationsdauer.

Buildprüfung erfolgreich: `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug`; 31 JVM-Tests ohne Fehler, Lint ohne Fehler und mit 24 Warnungen. Kein Geräteupdate: Der lokale neue Debugschlüssel hat eine andere Signatur als die installierte Pixel-App; der ursprüngliche Schlüssel fehlt auf diesem PC. Keine Nutzerdaten verändert. Android-Datenbanktests nicht erneut ausgeführt.

Manuelle Prüfung der Ergänzung noch offen:

- Player mehrfach öffnen/schließen; Tab wechseln und zurückkehren: Filter und Scrollposition bleiben erhalten.
- Bibliothek filtern sowie Bücher/Lesezeichen hinzufügen oder entfernen: Einträge bewegen sich ohne falsche Zuordnung.
- Cover laden und Buch wechseln: kein Cover des vorherigen Buchs während des Ladens.
- Play/Pause und Timer umschalten; auch während eines Übergangs schnell erneut bedienen.
- Android-Animator-Dauerskala auf 0: alle Ansichten und Aktionen bleiben unmittelbar nutzbar.

Die bisherigen Gerätetests unten beziehen sich auf den Stand vor dieser Ergänzung.

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

## Theme-Prüfung 28.10.2026
34 JVM-Tests einschließlich Theme-Fallbacks, Helligkeitswahl und Textkontrasten aller kuratierten Paletten (hell/dunkel, Material/Studio). Elf Gerätetests: fünf Room-, fünf Playergesten- und ein Theme-Auswahl-/Persistenztest. Separate Testpräferenzen; keine Nutzerdaten zurückgesetzt. Androids zusätzliche Force-Dark-Umfärbung deaktiviert, da Compose beide Helligkeiten selbst liefert.

## Eigenständiges Studio-Design (28.10.2026)
Studio verwendet Compose Unstyled 1.49.3 und Foundation für seine Bedienelemente; Material bleibt separat auswählbar. Drei zusätzliche Gerätetests prüfen Öffnen ohne Wiedergabestart/Filterwechsel, barrierefreies und horizontales Spulen mit Commit erst beim Loslassen sowie Transportcallbacks und Minimieren des Studio-Players. Bibliothek, Vollplayer und Importdialog auf Pixel visuell geprüft. Ein bestehender Zwischenbild-Gestentest war im ersten Gesamtlauf instabil; der gezielte Nachlauf aller fünf bestehenden Gestentests bestand.
Abschließender Gesamtlauf nach Dialog-Kontrastkorrektur: alle 14 Gerätetests bestanden, 34 Unit-Tests bestanden, alle vier Buildprüfungen erfolgreich. Lint ohne Fehler (26 Warnungen). Dunkler Importdialog erneut visuell geprüft.

## Ein einziges Material-3-Expressive-Design (28.10.2026)
Studio und Compose Unstyled entfernt. Offizielles MaterialExpressiveTheme mit MotionScheme.expressive, morphenden Buttons/Filtern und Play-/Pause-Steuerung sowie LoadingIndicator. Material3 1.5.0-alpha01 ist als zur bestehenden Toolchain kompatible Vorabversion gepinnt. Player-Ziehlogik, Room und Playback-Engine unverändert. Theme-Test deckt die Übernahme bestehender Studio-Farb-/Helligkeitspräferenzen und das Entfernen des alten Style-Schlüssels beim Speichern ab. Zusätzlicher Gerätetest prüft die Play-/Pause-Taste ohne echten Audiozugriff.
Finale Prüfung: alle vier Gradle-Aufgaben erfolgreich, 34 Unit-Tests und zwölf Gerätetests bestanden. Lint ohne Fehler (25 Warnungen). Bibliothek, Einstellungen hell/dunkel und Vollplayer auf Pixel visuell geprüft; Systemdarstellung und Bildschirmzeit anschließend wiederhergestellt.

## Wellen und Navigation (28.10.2026)
Native LinearWavyProgressIndicator-Spur im weiterhin nativen Slider; ergänzende Buch-/Mini-Player-Anzeigen. Wellenhöhe auf 35 Prozent und Strichstärke auf 2 dp begrenzt. Nur bei tatsächlichem PLAYING (nicht während Pufferung) sichtbar/bewegt; bei Pause weich zur geraden Linie. Buchkarten prüfen zusätzlich die aktive Album-ID. ShortNavigationBar in abgerundeter, abgesetzter Fläche, federnde Symbolauswahl. Bestehender horizontaler Gestentest verwendet jetzt den echten ExpressiveSeekBar; zusätzlicher Navigationstest prüft Beschriftungen und Auswahl.


28.10.2026 – Größeres Player-Cover und weicher Cover-Hintergrund:
Positionsgriff auf 4 × 22 dp verkleinert; Wellenamplitude von 35 auf 60 Prozent,
Player-Spur auf 14 dp erhöht. Cover nutzt die Inhaltsbreite bis maximal 380 dp.
Weichgezeichnetes Cover im Hintergrund mit Verlauf zur normalen Theme-Fläche;
kleinere Abstände im Player. Niedrig aufgelöste Hintergrundgrafik als Fallback
für ältere Androidversionen. Wiedergabe- und Speicherlogik unverändert.
Alle vier Buildprüfungen erfolgreich; 34 Unit-Tests, 13 Gerätetests bestanden.
Lint: 0 Fehler, 26 Warnungen (zusätzlicher Bitmap-KTX-Stilhinweis).
Separate Test-App auf Pixel durch Testlauf aktualisiert. Visuelle Beurteilung
mit echtem Cover noch offen. APK: app/build/outputs/apk/debug/Hoerbuch-Test.apk.

Korrektur: Nach connectedDebugAndroidTest fehlte die separate Test-App auf dem Pixel. Anschließend APK erfolgreich neu installiert und gestartet. Die ursprüngliche App blieb installiert. Datenbestand der Test-App nach dem Testlauf nicht verifiziert; künftig nach Instrumentierungstests den tatsächlichen Installationsstand kontrollieren.


28.10.2026 – Backup/Wiederherstellung und sanfter Einschlaftimer:
Einstellungen: JSON-Sicherung über Android CreateDocument/OpenDocument.
Bibliothek, Track-ID/Millisekunden-Hörstände, unabhängige Lesezeichen, letztes Buch
und inaktiver Einschlafanker; keine Zugangsdaten, Streams oder Audiodateien.
Versioniertes, größenbegrenztes Format mit Referenzprüfung. Bestätigte Wiederherstellung
ersetzt enthaltene Bücher/Hörstände und erhält andere Bücher sowie zusätzliche
Lesezeichen. Upsert innerhalb einer Transaktion über LibraryStore.write; kein
Datenbankschemawechsel. Fehler/Bookmark-Konflikte bewirken keine Teilwiederherstellung.
Wiedergabe bleibt pausiert, aktive Timer werden nicht aus einem Backup übernommen.

Timer: letzte 5 Sekunden bis Frist bzw. Trackende mit appinterner Gain-Absenkung.
Danach Pause und mindestens 5 Sekunden Rücksprung, über Track-/Discgrenzen hinweg,
begrenzt durch den Buchanfang. Bei verzögertem Stopp mindestens bis zum Beginn
der leisen Phase. Einschlafanker bleibt separat erhalten. Gain wird beim Abbrechen,
Verlängern und nach Stopp wiederhergestellt; Systemlautstärke unverändert.
Während der leisen Phase wird ihr Anfang als Fortschritt gesichert. Ein während
Prozessabwesenheit abgelaufener Minutentimer spult beim nächsten Start pausiert
zurück. SDK-Adapter und Upgrade-Grenzen: docs/INTEGRATION.md.

Alle vier Gradle-Prüfungen erfolgreich (-PseparateApp=true).
48 JVM-Tests bestanden; Lint 0 Fehler, 26 Warnungen.
18 Instrumentierungstests auf Pixel bestanden: 5 bisherige Room-, 4 neue Backup-,
5 Gesten-, 3 Theme-/Expressive- und 1 SDK-Volumentest. Backup-Tests prüfen Roundtrip,
Erhalt anderer Daten, Identitätskonflikte und Transaktions-Rollback bei DB-Fehler.
Erster UI-Testlauf scheiterte am gesperrten Display; nach Nutzerentsperrung alle
Tests erfolgreich. Direkter adb-Instrumentierungslauf statt Gradle-Aufräumen:
App bleibt installiert, anschließender Paketcheck und Start erfolgreich.
Backup-Karte und Dateidialoge visuell geprüft; erste echte Sicherung unter
Download/Hoerbuch-Backup-2026-10-28-1317.json auf dem Pixel erstellt (24664 Bytes).
Wieder eingelesen und Vorschau mit 1 Buch/0 Lesezeichen geprüft, danach abgebrochen.
Kein Überschreiben echter Hörstände zu Testzwecken, keine Zugangsdaten exportiert.
Die vorhandene Anmeldung und Bibliothek wurden auf dem Gerät sichtbar bestätigt.
Akustischer Ende-zu-Ende-Test des Fades mit laufendem Stream noch offen; der
SDK-Test prüft reale Gain-Halbierung, Stummschaltung und Wiederherstellung ohne Audio.
APK: app/build/outputs/apk/debug/Hoerbuch-Test.apk.


28.10.2026 – Weitere Alltagsfunktionen und dezente Suche:
Bibliothekssuche nach Titel/Autor mit Statusfilter; fünf persistente Sortieroptionen.
Nach Nutzerkorrektur Suche und Sortierung als kleine Symbole unter den Statusfiltern;
Suchfeld nur auf Antippen, Schließen hebt die Suche auf. Weiterhören bleibt oben.
Bis zu zehn persistente Positionen vor Sprüngen pro Buch; Rückgängig und Verlauf im
Player, konsumierte Einträge werden entfernt. Lesezeichen bleiben unabhängig.
Optionaler Rücksprung 5/10/15/30 s nach mindestens 5 min Pause (Standard Aus), mit
monotoner Zeit und Boot-Zähler. Keine automatische Wiedergabe beim Appstart.
Abschnitt/Gesamt-Umschaltung mit passenden Zeiten und präzisem Abschnittsspulen;
zuletzt verwendete Minutentimerdauer als Direktwahl. Geführter leerer Bibliotheksstart.
Verständliche Katalogfehler; persistente Wiedergabefehler mit explizitem Wiederholen.
Nach 20 s Pufferung Pause mit Wiederholen. Bei Fehlern keine Überschreibung der letzten
bekannten Hörposition durch nachträgliche Engine-Positionswerte.
Anpassungen für große Schrift: Coverlimit, Kartenbreite, umbrechende Timeroptionen,
zweireihige Playeraktionen; 48-dp-Optionsknöpfe und benannte Slider-Zeiten.

Room-Schema 2: position_history + books.lastPlayedAt; Migration 1→2 ohne Datenverlust.
Historie/letzter Wiedergabestart werden in neuen Backups mitgesichert; alte Backups
bleiben lesbar. Letzte Wiedergabe wird erst bei tatsächlichem PLAYING gesetzt.
Alle vier Buildprüfungen erfolgreich; 53 JVM-Tests bestanden. Lint: 0 Fehler,
33 Warnungen (u. a. Bibliotheksversionen und KTX-Stil).
Gesamtlauf: 23 Gerätetests bestanden. Nach abschließender Suchkorrektur alle drei
betroffenen ListeningUiTest-Tests erneut bestanden. Geprüft: Migration aus dem
exportierten v1-Schema mit vorhandenen Hörständen/Lesezeichen/Anker; Verlaufslimit,
Wiederöffnen, Backup und Löschen; Suche nach Autor, Einstieg und zugängliches
Abschnittsspulen bei 1,6-facher Schriftgröße. Erster Migrationstest hatte einen Fehler
beim Lesen optionaler Schema-Indizes; Testvorbereitung korrigiert, Nachläufe erfolgreich.
Bibliothek mit dezenter Suche und Vollplayer auf dem Pixel visuell geprüft. Vorhandenes
Buch und Hörstand sichtbar erhalten. Direkter adb-Testlauf ohne Deinstallation;
aktuelle Test-App installiert und geöffnet. APK: app/build/outputs/apk/debug/Hoerbuch-Test.apk.
Noch offen: echter Netzabbruch/abgelaufene Sitzung während laufendem Stream, vollständiger
TalkBack-Hörtest und akustischer Ende-zu-Ende-Test des Timer-Fades. Semantik-, UI-, SDK-
Gain- und Datenbanktests ersetzen diese manuellen Prüfungen nicht.


28.10.2026 – Cover-Cache und reduzierte Abfragen bei Pause:
Neuer gemeinsamer ArtworkStore: 16 MiB Bitmap-RAM, 64 MiB privater Dateicache,
gebündelte konkurrierende Ladevorgänge, SHA-256-Dateinamen, begrenzte Downloads
und Bildabmessungen. Ältere Dateien werden verdrängt, defekte Bilder neu geladen;
Cache-Schreibfehler verhindern die Anzeige nicht. Keine neue Abhängigkeit.
Player-Abfragen bei inaktiver Pause auf 1 Hz reduziert; Wiedergabe, Minutentimer
und ausstehende Positionssprünge behalten 10 Hz. SDK-Laden/Preloading unverändert.
Alle vier Gradle-Prüfungen erfolgreich; 53 JVM-Tests, Lint 0 Fehler/33 Warnungen.
Gezielter Pixel-Lauf: 3 neue ArtworkStore-Tests und 1 SDK-Lautstärketest bestanden.
Cachetests mit synthetischen Bildern/isoliertem Verzeichnis: parallele Ansichten
teilen einen Download, neue Store-Instanz liest ohne Netzwerk aus Datei, defekte
Datei wird ersetzt, ungültige Daten/Hosts verworfen, Dateibudget eingehalten.
Kein kompletter erneuter UI-/Datenbanktestlauf, keine Messung realer TIDAL-Requests
oder Akkulaufzeit; akustischer Timer-Ende-zu-Ende-Test weiterhin offen.
Test-App per adb install -r aktualisiert und geöffnet, keine Datenlöschung.
APK: app/build/outputs/apk/debug/Hoerbuch-Test.apk.

28.10.2026 - Kompakter Mini-Player:
Fortschrittsanzeige in die Textspalte zwischen Cover und Play-Taste verschoben;
keine durchgehende Linie am unteren Kartenrand mehr. Cover 48 dp, gleichmaessige
Abstaende, 48-dp-Play-Touchziel und kompakteres Symbol. Wellenverhalten erhalten.
Alle vier Gradle-Pruefungen erfolgreich; auf Pixel installiert und die pausierte
Ansicht mit vorhandenem Buch visuell geprueft. Keine neuen Instrumentierungstests
fuer diese Layoutaenderung. App-Daten erhalten. APK: Hoerbuch-Test.apk.


28.10.2026 – Korrekturen 0.3.2-private (versionCode 10)
GitHub-Stand 5b0c081 vor der Änderung per Fast-forward übernommen.
Welle: sanftes Einschwingen beim Start; gemeinsamer Container hält Position und
Layout konstant. Bei Pause, Pufferung oder Fehler sofort ein echter gerader Strich.
Ein verspäteter PLAYING-Wert nach angeforderter Pause aktiviert keine Welle.
Decoder: Am Pixel zuvor -1:3:4006 beobachtet (Media3 zurückgeforderte Decoder-
Ressourcen). Bei Pause nur Decoder invalidieren, Position erhalten, kein Autoplay.
Bei Wiedergabewunsch maximal ein Neuaufbau am selben Track/Offset; danach Fehler
sichtbar. Netzwerk-/Konto-/andere Decoderfehler werden nicht pauschal unterdrückt.
Build: assembleDebug, assembleDebugAndroidTest, testDebugUnitTest und lintDebug
bestanden; 57 JVM-Tests, 0 Fehler. Android Lint: 0 Fehler.
Drei Pixel-Grafiktests bestanden: sofort flach nach Pause, wiederholte Pause/Pufferung,
weiches Anwachsen der Welle und unveränderte Containerposition. Die Tests messen
Pixelkontrast und funktionieren auch bei invertierten Darstellungsfarben.
Ursprüngliche App de.tidalbooks.prototype auf Nutzerwunsch mit bisherigem Schlüssel
aktualisiert; Installation erfolgreich, Anmeldung und vorhandene Bücher erhalten.
Realer Start mit FULL, rund neun Sekunden Wiedergabe, anschließende Pause ohne
playbackProblem geprüft (0.3.1; Decoderlogik in 0.3.2 identisch). Die abschließende
0.3.2 wurde danach installiert; Grafiktests direkt in ihrer Instrumentierung bestanden.
Hörbuch Test besitzt einen anderen Schlüssel und wurde nicht ersetzt. Die vorüber-
gehende separate Prüf-App wurde wieder entfernt. Keine App-Daten gelöscht.
Eine erneute reale Decoder-Rückforderung durch Android ist nicht deterministisch
erzwingbar; die Recovery-Entscheidungen sind durch Regressionstests abgesichert.
APK: `TIDAL-Hoerbuch.apk`
SHA-256: 19999ea5930b479115e818e4d685d6db658c968feb71d93e5243a74db16fb5f8


28.10.2026 – Bibliotheksansichten 0.3.3-private (versionCode 11)
Ansichtssymbol neben Suche/Sortierung schaltet zwischen bestehenden Kacheln und
kompakten Buchzeilen um. Auswahl lokal in library-view/listView gespeichert;
Suche, Filter, Sortierung und Weiterhören bleiben gemeinsam. Die Liste zeigt
Cover, Titel, Autor, Hörstatus, Restzeit, Fortschritt und Optionsmenü. Öffnen
startet kein Audio. Keine Änderung an Wiedergabe, Hörständen oder Room-Schema.
Buildprüfungen assembleDebug, assembleDebugAndroidTest, testDebugUnitTest und
lintDebug erfolgreich; 57 JVM-Tests bestanden, Android Lint ohne Fehler.
Fünf gezielte Pixel-UI-Tests bestanden: zwei neue Tests für Umschalten,
Wiederherstellung der Ansicht, Öffnen ohne Autoplay und Autorensuche; drei
bestehende Tests für Suche, Einstieg und Abschnittsspulen mit großer Schrift.
Ursprüngliche App mit bisherigem Schlüssel auf dem Pixel aktualisiert.
Listenansicht mit echten Büchern visuell geprüft; nach force-stop/Neustart
weiterhin Listenansicht. Testdatenbank unverändert, keine Nutzerdaten gelöscht.


28.10.2026 – Widget und Nachtmodus 0.4.0-private (versionCode 12)
AppWidgetProvider/RemoteViews mit privaten, unveränderlichen PendingIntents, direkten
Wiedergabe-/Lesezeichen-/Timeraktionen und Einstieg in den Nachtmodus. Keine Wiedergabe
bei APPWIDGET_UPDATE oder Neustart. Gemeinsamer Player und persistierte Bibliothek.
Nachtmodus mit appbezogener Fensterdimmung und Wiederherstellung beim Schließen.
Build/AndroidTest-APK/Android Lint erfolgreich; 61 JVM-Tests bestanden.
Fünf neue Komponenten-Gerätetests bestanden (Nachtmodus und native Widgetansicht),
zusätzlicher Einstiegstest nach Korrektur seiner ActivityScenario-Aufräumlogik bestanden.
Widget im echten Smart Launcher geprüft: Start im Hintergrund mit FULL, rund zehn
Sekunden Wiedergabe und Pause, ohne die Launcher-Ansicht zu verlassen. Widget-Mondsichel
öffnet den Nachtmodus; dieser wurde visuell geprüft. Nutzerbücher/Anmeldung erhalten.
Neue zusätzliche Cache- und Vorschau-Zugänglichkeitstests sind kompiliert, wurden auf
Nutzerwunsch zur Zeitersparnis nicht zusätzlich auf dem Gerät ausgeführt.
Ursprüngliche App mit vorhandenem Schlüssel aktualisiert. Kein Room-Schemawechsel.


28.10.2026 – 0.4.1-private (versionCode 13)
Separaten Nachtmodus samt Dimmung und Einstiegen entfernt. Normales hell/dunkel bleibt.
Sprungweite unter Einstellungen: 5/15/30/60 Sekunden, lokal gespeichert, gemeinsam
für Player, Widget und MediaSession, sofortige Aktualisierung der Mediensteuerung.
Build, AndroidTest-APK und Lint erfolgreich; 61 JVM-Tests bestanden.
Drei gezielte Widget-Gerätetests bestanden (native Steuerelemente mit 15 Sekunden,
Countdown/Leerzustand und zugängliche Vorschau ohne Nachtmodus).
Auf Pixel mit bisherigem Schlüssel per install -r aktualisiert, keine Daten gelöscht.


28.10.2026 – 0.4.2-private (versionCode 14)
Vor- und Zurückspringen im Player verwenden denselben runden Pfeil, für die
Vorwärtsrichtung horizontal gespiegelt. Größe, Strichstärke und Sekundenangabe
sind identisch. Keine Änderung an Sprungweiten, Wiedergabe oder Datenhaltung.
Build, Test-APK, 61 JVM-Tests und Android Lint erfolgreich. APK aktualisiert.
Kein ADB-Gerät verbunden; dieses Update wurde noch nicht auf dem Pixel installiert.


28.10.2026 – 0.4.3-private (versionCode 15)
Bibliothek mit kompakter einzeiliger App-Leiste statt großem ausklappendem Kopf.
Doppeltes Plus oben entfernt; Hinzufügen unten und im Leerzustand bleibt verfügbar.
Die Überschrift erhält die volle Breite. Andere Tabs behalten ihre bisherige Leiste.
Build, Test-APK, 61 JVM-Tests und Android Lint erfolgreich. Auf Pixel per install -r
mit bisherigem Schlüssel aktualisiert und geöffnet; keine App-Daten gelöscht.


28.10.2026 – 0.4.4-private (versionCode 16)
Kompaktere Listenansicht: Cover 80 statt 72 dp, weniger Innenabstand, kleinere
Titel, Autor einzeilig, engere Textabstände und schmalere Fortschrittsanzeige.
Listenabstand 8 statt 18 dp. Zeilen wachsen weiterhin bei großer Systemschrift;
Optionsmenü bleibt mit normaler Touchfläche. Kachelansicht unverändert.
Build, Test-APK, 61 JVM-Tests und Android Lint erfolgreich. Auf Pixel mit bisherigem
Schlüssel per install -r auf 0.4.4 aktualisiert und geöffnet, keine App-Daten gelöscht.


28.10.2026 – 0.4.5-private (versionCode 17)
Listencover auf 104 dp vergrößert und bündig an die linke Kartenkante gesetzt.
Text behält kompakte Abstände, bei großer Systemschrift kann die Zeile wachsen.
Weiterhören bleibt unverändert: zuletzt gehörtes unvollendetes Buch, während
der Mini-Player das aktuell geöffnete Buch zeigt.
Build, Test-APK, 61 JVM-Tests und Android Lint erfolgreich. Auf Pixel per install -r
mit bisherigem Schlüssel auf 0.4.5 aktualisiert und geöffnet, keine Daten gelöscht.


28.10.2026 – 0.4.6-private (versionCode 18)
Listenkarte an Weiterhören angeglichen: 90-dp-Cover (oben 82 dp), wieder
Innenabstand links/oben/unten und stärkere Kartenrundung. Nur Titel und Restzeit
bzw. Gesamtdauer/Beendet; Autor, Prozentzahl und Fortschrittsbalken entfallen
ausschließlich in Listenzeilen. Autorensuche bleibt verfügbar.
Build, Test-APK, 61 JVM-Tests und Android Lint erfolgreich. Auf Pixel per install -r
mit bisherigem Schlüssel auf 0.4.6 aktualisiert. Listenansicht mit vorhandenen
Büchern per Screenshot geprüft: Innenabstand vorhanden, nur Titel und Restzeit.
Keine App-Daten gelöscht.


28.10.2026 – 0.4.7-private (versionCode 19)
Bibliothekskopf ohne Collapse-/Nested-Scroll-Verbindung. Grid/Liste nimmt
Scrollgesten nur an, wenn vorwärts oder rückwärts Inhalt außerhalb des
sichtbaren Bereichs liegt. Lange Bibliotheken und Suche bleiben scrollbar.
Build, Test-APK, 61 JVM-Tests und Android Lint erfolgreich. Drei gezielte Pixel-
Tests bestanden: kurze Liste bleibt nach Wischen stehen, lange Liste reagiert
weiter auf Wischen und lässt sich bis Anfang/Ende scrollen; Ansichtswechsel,
Öffnen ohne Autoplay, Wiederherstellung und Autorensuche erhalten.
Vorhandene UI-Tests an fehlende Scrollaktion bei passendem Inhalt angepasst.
Auf Pixel per install -r auf 0.4.7 aktualisiert, App-Daten erhalten.

29.10.2026 - Import ohne Playerwechsel und klarere Begriffe:
Beim Hinzufuegen bleibt ein ausgewaehltes Buch erhalten. Import ruft dann weder
selectBook noch Engine-Reset/Load auf; Wiedergabe, Pause und Timer bleiben erhalten.
Ohne Buchauswahl wird das importierte Buch wie bisher pausiert geoeffnet.
Importdialog oeffnet den Player nur, wenn das importierte Buch ausgewaehlt ist.
Oberflaeche/Widget: Kapitel statt Abschnitte, Lesezeichen statt Stellen.
Weiterhoeren bleibt die Bezeichnung zum Fortsetzen; Jetzt hoeren war nicht vorhanden.
APK und Test-APK gebaut, 61 JVM-Tests bestanden, Lint ohne Fehler. Drei betroffene
ListeningUiTest-Tests auf Pixel bestanden: Suche, Einstieg, Kapitelspulen bei grosser
Schrift. Tests an nicht scrollbare Kurzlisten und eindeutige Chipauswahl angepasst.
Lokale SDK-/Schluesselpfade korrigiert; Lint musste nach Pfadkorrektur neu analysieren.
Original-App mit bereitgestelltem Schluessel per install -r aktualisiert, keine
Datenloeschung. Import waehrend echtem laufendem Stream noch nicht am Geraet erprobt.
APK: app/build/outputs/apk/debug/TIDAL-Hoerbuch.apk.

29.10.2026 – Zweites Design „TIDAL“:
Unter Einstellungen → Farben & Darstellung → Design zwischen Material Expressive
und TIDAL wechseln. Auswahl separat in appearance/design gespeichert; bisherige
Palette und Helligkeit bleiben erhalten. TIDAL verwendet feste schwarze/graue
Flächen, weiße Steuerelemente, kompakte Cover/Listenzeilen, schlichte Navigation,
gerade Fortschrittsanzeigen und einen runden Play-Knopf. Keine Datenmigration.
Vorlage: offizielle Android-Screenshots auf
https://play.google.com/store/apps/details?id=com.aspiro.tidal
assembleDebug mit JDK 17 erfolgreich. Auf Pixel mit vorhandenem Schlüssel per
install -r aktualisiert. Player/Lesezeichen visuell geprüft. Keine zusätzlichen
Testläufe gemäß Nutzerwunsch. Wiedergabe- und Speicherlogik unverändert.

29.10.2026 – Widget-Optionen im Launcher:
WidgetSettingsActivity als APPWIDGET_CONFIGURE registriert. reconfigurable erlaubt
unterstützten Launchern die erneute Konfiguration; ab Android 12 optional beim
Hinzufügen. Material You/TIDAL mit Vorschau, Übernehmen und Abbrechen. Auswahl
wie in den App-Einstellungen global für alle Widgets; App-Design unabhängig.

Widget-Deckkraft: eigener gespeicherter Wert 0–100 %, Standard 100 %. Regler in Launcher-Optionen und App-Einstellungen mit Vorschau. Nur separate Hintergrund-ImageView erhält setImageAlpha; Inhalte und Klickflächen bleiben unverändert. Beide Widget-Designs unterstützt.

29.10.2026 – Testfreigabe TIDAL Books 0.5.0-beta1 (versionCode 20)
Name/Icon aktualisiert; Material Expressive und TIDAL-Design umschaltbar.
Widget: Material You/TIDAL, Hintergrund-Deckkraft und Launcher-Konfiguration.
Weiterhören wird ausgeblendet, wenn es dasselbe Buch wie im Mini-Player zeigt.
TIDAL-Anmeldung steht für neue Nutzer am Anfang der Einstellungen.

Prüfung: assembleDebug, assembleDebugAndroidTest, testDebugUnitTest, lintDebug
und assembleRelease. 61 JVM-Tests, 37 Android-Gerätetests auf Pixel bestanden;
abschließend vier ListeningUiTest-Tests einschließlich neuer Weiterhören-Prüfung
bestanden (insgesamt 38 unterschiedliche Gerätetests). Lint: 0 Fehler, 57 Hinweise.
Anfänglicher APK-Paketierungsfehler im kombinierten Build; Wiederholung mit
max-workers=2 erfolgreich. Keine Codekorrektur für diesen Buildfehler erforderlich.

Frische Installation unter eigener ID de.tidalbooks.prototype.firststart:
leere Bibliothek, nicht angemeldet, kein Autoplay. Konto vom Nutzer bestätigt;
erster Gerätecode abgelaufen, erneute Anmeldung erfolgreich. Album 270256764
mit 141 Kapiteln über Android-Teilen importiert. Mehr als eine Minute Wiedergabe;
Pause bei 1:07, nach force-stop/Neustart derselbe Hörstand und weiterhin pausiert.
Gerätetests verwenden separate Testdatenbanken; persönliche App-Daten erhalten.

Weitergabe ist ein nicht-debuggbarer Release-Build, signiert mit demselben separat
verwahrten Entwicklungsschlüssel wie die vorhandene Installation. APK enthält
keine persönliche Sitzung. Kein Schlüssel und keine local.properties im Paket.
Einschränkungen: nur Pixel/Android API 37 physisch geprüft; andere Geräte und
Launcher, echte Telefonanrufe, lange Energiesparphasen und Offline-Fehlerfälle
nicht erneut manuell geprüft. Frischer Anmelde-/Streamtest im separaten Debug-
Prüfbuild derselben Quellen; Release-APK zusätzlich als Update/Start geprüft.
Finale Weitergabe-APK und auf Pixel installierte APK per SHA-256 bytegleich:
b0cdf213a3630a07a4891d874576cf5a3101f03c1469f713be52481d00e5ac7b
Paket: distribution/TIDAL-Books-0.5.0-beta1.zip (nur APK, Anleitung, Prüfsumme).
Persönliche App erfolgreich aktualisiert und gestartet, vorhandener Hörstand sichtbar.
Die isolierte Erststart-Prüf-App bleibt separat installiert.

29.10.2026 – Deutsch/Englisch, 0.5.0-beta2 (versionCode 21)
- Gesamte App einschließlich Ersteinrichtung, Player, Filter/Sortierung, Lesezeichen,
  Timer, Backups, Fehlermeldungen, Widgets und Mediensteuerung lokalisiert.
- Einstellungen → Sprache: System / Deutsch / English. Native App-Sprache ab
  API 33; lokale Auswahl mit Activity-Kontext auf API 26–32. Beide Sprachen
  auch in zukünftigen App-Bundles enthalten, kein Sprachdownload erforderlich.
- Buchmetadaten und eigene Lesezeichennamen unverändert; stabile Filterwerte und
  gespeicherte Verlaufsschlüssel bleiben erhalten. Keine Room-Migration.
- Player-Schalter heißt auf Englisch „Whole book / Chapter“; die Übersicht „Chapters“.
- assembleDebug, assembleDebugAndroidTest, testDebugUnitTest, lintDebug und
  assembleRelease erfolgreich. 65 JVM-Tests erfolgreich; Lint 0 Fehler, 58 Warnungen.
- 40 unterschiedliche Android-Tests auf Pixel in isolierter Erststart-App geprüft:
  erster Gesamtlauf 39 erfolgreich und eine zu enge neue Testabfrage (derselbe
  Buchtitel in Weiterhören und Bibliotheksliste). Abfrage korrigiert; beide
  EnglishUiTest-Tests danach erfolgreich. Tests betreffen u.a. Sprachwechsel,
  Filteridentität, unveränderte Buchtitel, englische Ersteinrichtung und beide Widgets.
- Englische Oberfläche per Android-App-Sprache auf Pixel geöffnet; Einstellungen,
  Bibliothek und englische Widget-Vorschau geprüft. API 26–32 nicht physisch geprüft.
  Kein erneuter kompletter Anmelde-/Streamingtest erforderlich/durchgeführt.
- Finale APK nicht debuggbar, identisches Signaturzertifikat wie beta1.
- Reguläre App per install -r aktualisiert und gestartet; keine Daten gelöscht.
- Weitergabe: distribution/TIDAL-Books-0.5.0-beta2.zip (APK, DE/EN-Anleitungen, SHA256).
- SHA-256 der Weitergabe-APK:
  f55ff19f85aa5659a08d6d8b20aa7d07028a6cb21c750c37ee5aa969c7482002
Installierte APK zurückgelesen: SHA-256 stimmt exakt mit der Weitergabe-APK überein.

29.10.2026 – Kompakte, aufklappbare Einstellungen (0.5.0-beta3, versionCode 22)
- Sechs Abschnitte: TIDAL-Konto, Wiedergabe, Darstellung, Widget, Backup und Über die App.
- Überschriften zeigen aktuelle Werte; jeweils ein Bereich öffnet sich direkt darunter.
  Erneuter Tipp schließt ihn. Kurze Expand-/Fade-Animation, keine Unterseiten.
- Sprache und Design unter Darstellung; Sprungweite, Rücksprung und Benachrichtigung
  unter Wiedergabe. Widget-Vorschau und Erklärungen nur im geöffneten Abschnitt.
- Versionsblock in About mit 20 dp horizontalem und 16 dp vertikalem Innenabstand.
- Deutsch und Englisch ergänzt. Wiedergabe-, Speicher- und Anmeldelogik unverändert.
- assembleDebug, assembleDebugAndroidTest, testDebugUnitTest, lintDebug und
  assembleRelease erfolgreich; 65 JVM-Tests, Lint 0 Fehler/58 Warnungen.
  Die abschließende reine Padding-Korrektur zusätzlich als Debug und Release gebaut.
- Englische Übersicht sowie aufgeklappte Konto-/About-Abschnitte auf Pixel gesichtet.
  Keine erneuten Instrumentierungstests; bestehende Tests erfolgreich kompiliert.
- Weitergabepaket: distribution/TIDAL-Books-0.5.0-beta3.zip mit deutscher/englischer Anleitung.
Reguläre App per install -r aktualisiert und gestartet, keine Daten gelöscht.
Installierte und weitergegebene APK bytegleich; SHA-256:
b1abe0bcbf106ef99ba09484c260ee599bb6ce24587d11f2db5056a6984ecd91

29.10.2026 – Sprachwechsel-Absturz behoben (0.5.0-beta4, versionCode 23)
- Crashprotokoll: IllegalStateException „visitAncestors called on an unattached node“
  in Material3 ChildSemanticsNode.applySemantics beim Sprachwechsel.
- Einstellungs-Kopfzeilen verwenden eine normale Row statt ListItem der gepinnten
  Material3-Version. Lokalisierte Zustandsbeschreibungen werden während der
  Composition aufgelöst, nicht beim späteren Durchlaufen der Semantik-Knoten.
- Bedienhilfen, aufklappbare Bereiche und gespeicherte Einstellungen erhalten.
- Neuer LanguageRecreationTest verwendet die echte MainActivity und die native
  Sprachwahl in der isolierten firststart-App: Englisch, Deutsch, Englisch,
  anschließend expliziter Activity-Neuaufbau. Sprache und geöffneter Bereich erhalten;
  auf Pixel erfolgreich (1 Test). Die ursprüngliche Prüfsprache wird wiederhergestellt.
- Kein Löschen von App-Daten, keine Änderung an Anmeldung, Room oder Wiedergabelogik.
Alle vorgeschriebenen Builds/Prüfungen erfolgreich, 65 JVM-Tests; Lint 0 Fehler/58 Warnungen.
Finale Release-APK auf dem Pixel per install -r installiert und gestartet.
Installierte APK und Weitergabe-APK bytegleich. Paket: distribution/TIDAL-Books-0.5.0-beta4.zip.
SHA-256: 80b412c9a6414ad935a712d117c6b9c6e9b289fc39e509eeb3065ca8ae432558
