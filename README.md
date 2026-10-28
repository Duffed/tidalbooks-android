# TIDAL Hörbuch für Android

Private Androidapp mit lokaler Hörbibliothek, gespeichertem Hörfortschritt pro Buch, Lesezeichen, Einschlaftimer und Material-You-Oberfläche. Version **0.4.0-private**, Android 8 oder neuer. Die alternative TIDAL-Anmeldung und vollständige Wiedergabe wurden auf dem angeschlossenen Pixel 9 Pro nachgewiesen. Es gibt keinen eigenen Server oder zusätzlichen App-Account.

## Verwendung

1. APK installieren oder bestehende Installation aktualisieren. Die Anmeldung und die neue Bibliothek bleiben bei Updates erhalten.
2. Unter **Einstellungen → Mit TIDAL verbinden** den Gerätecode auf der TIDAL-Seite bestätigen. Danach per Zurück-Geste zur App zurückkehren. Die Geräteanmeldung hat keinen automatischen Browser-Rücksprung.
3. In der Bibliothek **Hinzufügen** wählen und einen Albumlink einfügen. Alternativ das Album direkt aus TIDAL an „Hörbuch“ teilen. Ein bereits vorhandenes Album wird geöffnet und sein Fortschritt erhalten.
4. Buch öffnen und auf Abspielen tippen. Beim erneuten App-Start wird das zuletzt geöffnete Buch pausiert an seiner gespeicherten Stelle angezeigt.
   Der Mini-Player lässt sich antippen oder langsam nach oben ziehen. Zum Minimieren im großen Player über Cover oder Inhalt herunterziehen oder den Pfeil nach unten verwenden. Bei gescrolltem Inhalt geht es zunächst zum Anfang zurück; anschließend folgt der Player derselben Ziehgeste. Die Bibliothek bleibt dahinter stehen.
5. Im Player setzt **Merken** sofort ein Lesezeichen. Unter **Stellen** oder im Bibliothek-Tab **Lesezeichen** lassen sich Stellen anspringen, über das Menü benennen und löschen. Ein Sprung erhält den Play-/Pausezustand.
6. **Timer** bietet 15/30/45/60 Minuten, eine eigene Dauer von 1–180 Minuten und Trackende. Ein aktiver Minutentimer lässt sich um zehn Minuten verlängern oder abschalten. „Zur Stelle vor dem Einschlafen“ stellt den separat gespeicherten Beginn der letzten Einschlafsitzung wieder her.

## Verhalten und Speicherung

- Room speichert jedes Buch mit Metadaten und Trackreihenfolge einschließlich mehrerer Discs, Track-ID und Position in Millisekunden. Fortschritt wird während Wiedergabe alle vier Sekunden und unmittelbar bei Pause, Sprung, Track-/Buchwechsel sowie beim Verlassen der Activity eingereiht. Ein einzelner geordneter Datenbankschreiber verhindert veraltete Überschreibungen.
- Die Bibliothek bietet Nicht begonnen, Angefangen und Beendet, Cover, Weiterhören, Gesamtfortschritt und Restzeit. ±30 Sekunden funktionieren über Trackgrenzen. Kein Shuffle oder Crossfade.
- Lesezeichen sind unabhängig vom aktuellen Fortschritt. Das Entfernen eines Buchs löscht nach Bestätigung dessen Fortschritt und Lesezeichen.
- Am Buchende wird pausiert und das Buch als beendet markiert. Erneutes Hören beginnt ausdrücklich über „Von vorne hören“.
- Der Timer zählt monotone abgelaufene Zeit einschließlich Pausen und Pufferung. Deadline, Boot-Zähler und Startposition werden gespeichert. Ein Prozessneustart startet kein Audio; ein abgelaufener Timer bleibt aus. Nach einem Geräteneustart wird kein Timer automatisch aktiviert. Die Startposition bleibt auch nach Ablauf/Abbrechen erhalten. In den letzten fünf Sekunden vor Frist bzw. Trackende wird die appinterne Lautstärke ausgeblendet; danach wird pausiert und mindestens fünf Sekunden zurückgesprungen, begrenzt durch den Buchanfang. Der Beginn der leisen Phase wird vorsorglich gespeichert. Abbrechen/Verlängern stellt die normale Lautstärke wieder her; die Systemlautstärke bleibt unverändert.
- Wiedergabe läuft in einem Media3 MediaSessionService. System-Mediensteuerung enthält Titel, Cover, Play/Pause und ±30 Sekunden. Die Benachrichtigung führt zurück zum Player. Die App verwendet die dynamischen Systemfarben ab Android 12 und das Systemdesign; ältere Geräte erhalten passende helle/dunkle Farben. Compose Material3 1.5.0-alpha01.
- Bei einem TIDAL-Fehler wird pausiert, ohne einen Abschnitt stillschweigend zu überspringen. Der zuletzt gespeicherte Fortschritt bleibt erhalten.

Die Daten sind lokal. Unter **Einstellungen → Backup & Wiederherstellung** lässt sich eine JSON-Sicherung über Androids Dateiauswahl speichern und wieder einspielen. Enthalten sind Bibliothek, Hörstände, Lesezeichen, Positionsverlauf, das zuletzt geöffnete Buch und der Einschlafanker; keine Audiodateien, Anmeldedaten oder Darstellungseinstellungen. Vor der Wiederherstellung wird bestätigt: enthaltene Hörstände werden ersetzt, andere Bücher und zusätzliche Lesezeichen bleiben erhalten. Die Wiedergabe bleibt pausiert, Timer werden nicht aktiviert. Auf einem neuen Gerät ist eine erneute TIDAL-Anmeldung erforderlich. Deinstallieren oder „App-Daten löschen“ entfernt die lokale Bibliothek; eine separat aufbewahrte Sicherung erlaubt ihre Wiederherstellung. Es gibt keine automatische Sicherung, Synchronisierung, Webversion, Downloads oder Geschwindigkeitsregelung.

## TIDAL-Anbindung

Der persönliche offizielle Developer-Client lieferte im Integrationsnachweis nur PREVIEW. Die App nutzt für dieses Konto deshalb das öffentlich verteilte Geräteprofil von **tidalapi 0.8.11** mit `r_usr`, gekapselt hinter dem bestehenden Gateway und CredentialsProvider. Wiedergabe erfolgt weiterhin über den TIDAL-Player-SDK. Herkunft und Lizenz der gemeinsam verwendeten öffentlichen Geräte-Client-Konstanten stehen in `third_party/`.

Persönliches Developer-Client-Secret, Passwörter, Kontotokens und signierte Stream-URLs werden nicht in das Quellprojekt oder die APK-Konfiguration übernommen. Zugriff-/Refresh-Tokens liegen ausschließlich auf dem Gerät, verschlüsselt mit AES-GCM und Android-Keystore. Backups der App-Daten sind deaktiviert. Der TIDAL-SDK kann eigene Betriebs-/Wiedergabeereignisse an TIDAL senden; die App betreibt keine eigene Nutzungsanalyse. Die inoffizielle Geräteanbindung kann sich unabhängig von dieser App ändern.

Für die erhaltene offizielle PKCE-Integration gilt weiterhin `tidalbooks://oauth/callback`. Ihre Client-ID lässt sich über `tidal.clientId` in `local.properties` überschreiben; sie ist für die alternative Geräteanmeldung nicht nötig. Ein persönliches Client-Secret gehört nicht in eine Androidapp.

## Selbst bauen

JDK 17, Android SDK Plattform 35 / Build-Tools 35.0.0. Als Gradle-Projekt in Android Studio öffnen und den SDK-Pfad über `ANDROID_HOME` oder `sdk.dir` in `local.properties` setzen.

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Für ein Update einer bereits installierten APK ist derselbe lokale Signaturschlüssel erforderlich; private Schlüssel und `local.properties` sind absichtlich nicht im Quellarchiv enthalten.

### Separate Test-App

Mit `./gradlew.bat -PseparateApp=true :app:assembleDebug` entsteht **Hörbuch Test** mit der eigenen App-ID `de.tidalbooks.prototype.testapp`. Sie kann neben der bisherigen App installiert werden und hat eine eigene Anmeldung, Bibliothek und Hörstände. Die vorhandenen Daten werden nicht übernommen oder verändert. Die APK liegt ebenfalls unter `app/build/outputs/apk/debug/app-debug.apk`; zum normalen Build den Parameter weglassen. Für spätere Updates der Test-App denselben lokalen Debugschlüssel behalten.

Technik: Kotlin 2.2.21, AGP 8.13.0, Gradle 8.13, Compose BOM 2025.10.01 / Material3 1.5.0-alpha01, Room 2.8.5, KSP 2.2.21-2.0.4, TIDAL Player 0.0.73, Auth 0.10.1 und Media3 1.5.0. Room-Schema liegt unter `app/schemas/`.

Build- und Gerätetests sind in `BUILD-RESULT.txt` und `docs/VERIFICATION.md` dokumentiert.

## Material 3 Expressive (28.10.2026)
Die App verwendet ein einziges Material-3-Expressive-Design. Studio, der Designumschalter und Compose Unstyled sind entfernt. Offizielles MaterialExpressiveTheme und MotionScheme.expressive, formverändernde Buttons/Filter, eine große Play-/Pause-Taste mit animiertem Formwechsel und der Expressive-Ladeindikator sind integriert. Die direkt ziehbare Player-Fläche bleibt erhalten.

Unter Einstellungen → Farben & Darstellung lassen sich Material You oder Ozean, Wald, Pflaume, Terrakotta und Graphit sowie System/Hell/Dunkel wählen. Bestehende Farb- und Helligkeitspräferenzen bleiben auch bei zuvor ausgewähltem Studio erhalten. Ab Android 12 stehen dynamische Systemfarben zur Verfügung, darunter dient Wald als Ersatz.

Material3 ist auf 1.5.0-alpha01 gepinnt, eine mit der bestehenden SDK-/Compose-Toolchain kompatible Vorabversion mit den offiziellen Expressive-APIs. Ein späteres Upgrade ist bewusst separat zu prüfen. Referenz: https://developer.android.com/develop/ui/compose/designsystems/material3
Die Fortschrittsanzeigen verwenden die offizielle wellenförmige Expressive-Anzeige. Eine dünne, gut sichtbare Welle erscheint ausschließlich bei tatsächlicher Wiedergabe, auf Buchkarten nur beim aktiven Buch. Bei Pause und Pufferung wird die Anzeige zur geraden Linie. Die native Slider-Bedienung bleibt erhalten. Die untere Navigation verwendet ShortNavigationBar in einer abgesetzten, abgerundeten Fläche mit animierten Symbolen.


## Weiterhören und Bibliothek (28.10.2026)
- Dezente Such- und Sortiersymbole unter den Statusfiltern; das Suchfeld öffnet sich erst beim Antippen. Suche nach Titel/Autor kombiniert mit Statusfiltern. Sortierung: zuletzt gehört,
  zuletzt hinzugefügt, Titel, Autor oder kürzeste Restzeit; Sortierwahl bleibt gespeichert.
- Der Player bietet „Ganzes Buch“ und „Abschnitt“ für Zeitangaben und präzises Spulen.
  Bei Abschnittsspulen bleibt ein Sprung am rechten Rand innerhalb des Abschnitts.
- Vor manuellen Sprüngen werden bis zu zehn vorherige Positionen pro Buch gespeichert.
  „Letzten Sprung rückgängig“ oder das Menü „Positionsverlauf“ stellt eine Stelle unter
  Erhalt von Play/Pause wieder her. Verwendete Verlaufseinträge werden entfernt.
  Lesezeichen bleiben unabhängig. Der Verlauf wird in neuen Backups mitgesichert;
  frühere Backup-Dateien bleiben kompatibel.
- Einstellungen → Wieder ins Hörbuch finden: optional 5/10/15/30 Sekunden Rücksprung
  nach mindestens fünf Minuten Pause. Standard ist Aus. Der Zeitvergleich verwendet
  elapsedRealtime und Boot-Zähler; nach Geräte-Neustart erfolgt kein zusätzlicher
  automatischer Rücksprung. Bewusst gewählte Stellen lösen keinen Zusatzsprung aus.
- Der Timer bietet die zuletzt verwendete Minutendauer als Direktwahl an.
- Eine leere Bibliothek erklärt Anmeldung, Albumimport und Fortsetzen. Katalogfehler
  unterscheiden Verbindungsprobleme, abgelaufene Anmeldung, fehlende Berechtigung,
  nicht verfügbare Bücher und zu viele Anfragen. Wiedergabefehler bieten einen
  ausdrücklichen Wiederholversuch; bei mehr als 20 Sekunden Pufferung wird pausiert.
- Große Systemschrift: größere Mindestbreite der Buchkarten, kleineres Player-Cover,
  umbrechende Timerwahl und zweireihige Player-Aktionen. Spulregler benennt Bereich
  und Zeiten für Bedienungshilfen; Optionsknöpfe haben mindestens 48 dp Fläche.

Room-Schema 2 ergänzt Positionsverlauf und letzten Wiedergabestart. Migration 1→2
bewahrt vorhandene Bücher, Hörstände, Lesezeichen und Einschlafanker.

## Cover-Cache und Hintergrundarbeit (28.10.2026)
Cover werden gemeinsam in einem nach Bytes begrenzten RAM-Cache (16 MiB) und im
privaten Android-Cacheverzeichnis (64 MiB) gehalten. Der Dateicache überlebt
Prozessneustarts; Android oder „Cache leeren“ kann ihn entfernen. Ältere Cover
werden bei Platzbedarf verdrängt. Die inhaltsbezogenen TIDAL-Cover-URLs dienen
als Schlüssel; parallele Ansichten teilen erfolgreich geladene Bilder.
Beschädigte Cachebilder werden neu geladen. Keine Audio- oder Zugangsdaten im Cache.
Der Cache gilt für die App-Oberfläche; System-Medienbilder lädt Media3 separat.
Bei Pause ohne laufenden Minutentimer oder ausstehenden Positionssprung prüft
die App den Player einmal statt zehnmal pro Sekunde. Während Wiedergabe und
Minutentimer bleibt der bisherige 100-ms-Takt für Timer/Fade erhalten.


## Korrekturen 0.3.2 (28.10.2026)

Beim Start schwingt die Wellenhöhe weich aus einer geraden Linie ein. Ein gemeinsamer Container hält Position und Layout dabei konstant. Fortschrittsanzeigen wechseln beim Pausieren, Puffern oder Fehler direkt zur geraden Linie. Die Wellen-Komponente wird in diesen Zuständen vollständig durch einen dünnen geraden Fortschrittsbalken ersetzt. Ein noch nachlaufender PLAYING-Wert der Engine aktiviert keine Welle, wenn bereits Pause angefordert wurde.

Der am Pixel beobachtete TIDAL-Code `-1:3:4006` bezeichnet zurückgeforderte Decoder-Ressourcen (Media3 `ERROR_CODE_DECODING_RESOURCES_RECLAIMED`), keinen fehlenden Abschnitt. Während Pause wird der ungültige Decoder zurückgesetzt und beim nächsten bewussten Start neu geladen. Bei angeforderter Wiedergabe erfolgt höchstens ein automatischer Neuaufbau am selben Track/Offset. Wiederholte Decoderfehler sowie echte Netzwerk-/Kontofehler bleiben sichtbar. Keine automatische Wiedergabe im Pausenzustand und kein Überspringen von Abschnitten.

Referenz: https://developer.android.com/reference/androidx/media3/common/PlaybackException#ERROR_CODE_DECODING_RESOURCES_RECLAIMED


## Kachel- und Listenansicht (0.3.3)

In der Bibliothek wechselt das Ansichtssymbol neben Suche und Sortierung zwischen
Kacheln und Liste. Die Auswahl bleibt nach App-Neustarts erhalten. Die Listenansicht
zeigt Cover, Titel, Autor, Hörstatus, Restzeit und Fortschritt. Antippen öffnet das Buch
pausiert; das Optionsmenü erlaubt weiterhin das Entfernen mit Bestätigung. Suche,
Statusfilter, Sortierung und „Weiterhören“ funktionieren in beiden Ansichten.


## Widget und Nachtmodus (0.4.0)

Einstellungen → Homescreen-Widget bietet eine bedienbare Vorschau und „Widget hinzufügen“.
Das Widget steuert Start/Pause, 30 Sekunden zurück, Lesezeichen und den zuletzt verwendeten
Minutentimer (erneutes Tippen schaltet ihn aus). Die Mondsichel öffnet direkt den Nachtmodus.
Systemaktualisierungen starten niemals Audio. Countdown läuft mit monotoner Deadline;
Cover werden nur aus dem bestehenden Cache gelesen. Während Wiedergabe wird der Fortschritt
höchstens alle 15 Sekunden aktualisiert; Steuerungszustände ändern sich unmittelbar.

Nachtmodus über die Mondsichel im Player oder Einstellungen → Nachtmodus öffnen:
schwarze Oberfläche, große Transporttasten, Lesezeichen, Timerwahl/Verlängern/Abbrechen
und Rückkehr zum Einschlafanker. Die einstellbare Dimmung gilt nur für das App-Fenster;
Schließen stellt die vorherige Helligkeit wieder her. Systemhelligkeit und Farbpräferenzen
bleiben erhalten, der Bildschirm darf sich weiterhin normal ausschalten.
