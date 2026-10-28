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
