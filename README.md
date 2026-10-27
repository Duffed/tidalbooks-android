# TIDAL Hörbuch für Android

Private Androidapp mit lokaler Hörbibliothek, gespeichertem Hörfortschritt pro Buch, Lesezeichen, Einschlaftimer und Material-You-Oberfläche. Version **0.3.0-private**, Android 8 oder neuer. Die alternative TIDAL-Anmeldung und vollständige Wiedergabe wurden auf dem angeschlossenen Pixel 9 Pro nachgewiesen. Es gibt keinen eigenen Server oder zusätzlichen App-Account.

## Verwendung

1. APK installieren oder bestehende Installation aktualisieren. Die Anmeldung und die neue Bibliothek bleiben bei Updates erhalten.
2. Unter **Einstellungen → Mit TIDAL verbinden** den Gerätecode auf der TIDAL-Seite bestätigen. Danach per Zurück-Geste zur App zurückkehren. Die Geräteanmeldung hat keinen automatischen Browser-Rücksprung.
3. In der Bibliothek **Hinzufügen** wählen und einen Albumlink einfügen. Alternativ das Album direkt aus TIDAL an „Hörbuch“ teilen. Ein bereits vorhandenes Album wird geöffnet und sein Fortschritt erhalten.
4. Buch öffnen und auf Abspielen tippen. Beim erneuten App-Start wird das zuletzt geöffnete Buch pausiert an seiner gespeicherten Stelle angezeigt.
5. Im Player setzt **Merken** sofort ein Lesezeichen. Unter **Stellen** oder im Bibliothek-Tab **Lesezeichen** lassen sich Stellen anspringen, über das Menü benennen und löschen. Ein Sprung erhält den Play-/Pausezustand.
6. **Timer** bietet 15/30/45/60 Minuten, eine eigene Dauer von 1–180 Minuten und Trackende. Ein aktiver Minutentimer lässt sich um zehn Minuten verlängern oder abschalten. „Zur Stelle vor dem Einschlafen“ stellt den separat gespeicherten Beginn der letzten Einschlafsitzung wieder her.

## Verhalten und Speicherung

- Room speichert jedes Buch mit Metadaten und Trackreihenfolge einschließlich mehrerer Discs, Track-ID und Position in Millisekunden. Fortschritt wird während Wiedergabe alle vier Sekunden und unmittelbar bei Pause, Sprung, Track-/Buchwechsel sowie beim Verlassen der Activity eingereiht. Ein einzelner geordneter Datenbankschreiber verhindert veraltete Überschreibungen.
- Die Bibliothek bietet Nicht begonnen, Angefangen und Beendet, Cover, Weiterhören, Gesamtfortschritt und Restzeit. ±30 Sekunden funktionieren über Trackgrenzen. Kein Shuffle oder Crossfade.
- Lesezeichen sind unabhängig vom aktuellen Fortschritt. Das Entfernen eines Buchs löscht nach Bestätigung dessen Fortschritt und Lesezeichen.
- Am Buchende wird pausiert und das Buch als beendet markiert. Erneutes Hören beginnt ausdrücklich über „Von vorne hören“.
- Der Timer zählt monotone abgelaufene Zeit einschließlich Pausen und Pufferung. Deadline, Boot-Zähler und Startposition werden gespeichert. Ein Prozessneustart startet kein Audio; ein abgelaufener Timer bleibt aus. Nach einem Geräteneustart wird kein Timer automatisch aktiviert. Die Startposition bleibt auch nach Ablauf/Abbrechen erhalten.
- Wiedergabe läuft in einem Media3 MediaSessionService. System-Mediensteuerung enthält Titel, Cover, Play/Pause und ±30 Sekunden. Die Benachrichtigung führt zurück zum Player. Die App verwendet die dynamischen Systemfarben ab Android 12 und das Systemdesign; ältere Geräte erhalten passende helle/dunkle Farben. Compose Material3 1.4.0.
- Bei einem TIDAL-Fehler wird pausiert, ohne einen Abschnitt stillschweigend zu überspringen. Der zuletzt gespeicherte Fortschritt bleibt erhalten.

Die Daten sind lokal. Deinstallieren oder „App-Daten löschen“ entfernt die Bibliothek. Es gibt noch keinen Export, keine Synchronisierung, Webversion, Downloads oder Geschwindigkeitsregelung.

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

Technik: Kotlin 2.2.21, AGP 8.13.0, Gradle 8.13, Compose BOM 2025.10.01 / Material3 1.4.0, Room 2.8.5, KSP 2.2.21-2.0.4, TIDAL Player 0.0.73, Auth 0.10.1 und Media3 1.5.0. Room-Schema liegt unter `app/schemas/`.

Build- und Gerätetests sind in `BUILD-RESULT.txt` und `docs/VERIFICATION.md` dokumentiert.
