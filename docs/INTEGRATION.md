# TIDAL-Anbindung: Untersuchungsstand

Die folgenden Quellen und Laufzeitergebnisse stammen aus dem Integrationsnachweis. Der alternative Kontozugang mit vollständiger Wiedergabe wurde auf dem Pixel 9 Pro nachgewiesen und wird in Version 0.3.0-private weiterverwendet.

## Offizieller Weg

- Der [Android-Player-SDK](https://github.com/tidal-music/tidal-sdk-android/blob/main/player/README.md) bietet Laden, Wiedergabe, Pause, Positionssprünge und einen nächsten Track.
- Der SDK liefert über `PlaybackContext.assetPresentation` die Unterscheidung `FULL` / `PREVIEW`. Diese Information verwendet der Prototyp direkt statt aus der Katalogdauer auf Vollwiedergabe zu schließen.
- Die Engine meldet die Position in Sekunden; `seek(Float)` nimmt laut Implementierung Millisekunden entgegen. Die App rechnet an dieser Grenze ausdrücklich um.
- [Auth-SDK](https://github.com/tidal-music/tidal-sdk-android/blob/main/auth/README.md): Authorization-Code-Login über den eigenen Client. Client-Secret wird nicht in eine mobile App eingebettet. Der Auth-SDK verwaltet Tokens und verschlüsselte lokale Speicherung; HTTP-Body-Logging ist deaktiviert.
- Versionsabhängiger Rücksprung: Der hier eingebundene Auth-SDK **0.10.1** erwartet bei `finalizeLogin` die vollständige Callback-URI, nicht nur deren Query. Seine `RedirectUri`-Implementierung verwendet den Teil vor `?` beim Token-Austausch als `redirect_uri`. Der Adapter reicht die vollständige URI unverändert weiter. Regressionstests prüfen den Parser aus dem tatsächlich eingebundenen SDK-Artefakt.
- [Öffentliche API-Spezifikation](https://tidal-music.github.io/tidal-api-reference/): Albumdetails und paginierte Items werden über `/v2/albums/{id}` und `/v2/albums/{id}/relationships/items` gelesen. Positionsdaten der Albumbeziehung bestimmen Disc und Tracknummer.
- Auf dem angeschlossenen Pixel lieferte TIDAL für Album 270256764 einen absoluten Folgeseiten-Link zu `/albums/270256764/relationships/items` ohne `/v2`. Die App ergänzt das Präfix ausschließlich für denselben Album-Endpunkt und prüft weiterhin HTTPS, Host, Port und Pfad. Unversionierte Links werden nicht ungeprüft mit Anmeldedaten aufgerufen.

Die öffentliche SDK-Dokumentation nennt weiterhin Vorschau-Einschränkungen. Daraus folgt kein verlässlicher Rückschluss darauf, ob ein bestimmter Client Vollwiedergabe erhält. Entscheidend bleibt der Gerätetest.

Konkreter Gerätetest mit dem Client dieser privaten App und Album 270256764: Die Anmeldung und der paginierte Albumimport funktionieren. Der Player meldet nach dem Laden `PREVIEW`. Damit ist der benötigte Vollwiedergabezugang über diesen offiziellen Integrationsweg aktuell nicht nachgewiesen. Das ist kein Beleg dafür, dass jeder alternative Integrationsweg scheitert.

## Inoffizieller Weg: alternative Geräteanmeldung und Vollwiedergabe nachgewiesen

- [NOORwave](https://github.com/biggiesmallcap-blip/NOORwave#disclaimer) nennt eine inoffizielle API mit PKCE OAuth2. Die aktuelle Implementierung ist nicht quelloffen. Seine mobile Fernbedienung belegt keinen eigenständigen Android-Player.
- [Tides für Wear OS](https://github.com/luchfilip/Tides-WearOS) ist eine nähere offene Android-Referenz. Die geprüfte Anmeldung verwendet den TIDAL-Auth-SDK mit Device Login. Der eigene Client und dessen Berechtigungen bleiben damit Voraussetzung; das Projekt ist kein Beleg dafür, dass jede neu registrierte Client-ID dieselben Funktionen erhält.
- Der erste Prototyp verwendete ausschließlich die persönliche Developer-Client-ID. Ab Version 0.2.0 wird als ausdrücklich angeforderter alternativer Weg das öffentlich verteilte Geräte-Clientprofil der Bibliothek **tidalapi 0.8.11** verwendet. Persönliches Developer-Client-Secret und persönliche Kontotokens werden nicht in die APK eingebaut. Die gemeinsamen Client-Konstanten dieses Profils sind dagegen Bestandteil der experimentellen Anbindung und im Quellprojekt enthalten.

Die untersuchte tidalapi-Implementierung verwendet `POST /v1/oauth2/device_authorization`, den Device-Code-Grant und anschließend ältere Katalog-/Wiedergabeendpunkte auf `api.tidal.com/v1`. Der konkrete Client-Vorcheck mit Scope `r_usr` ergab HTTP 200 und einen gültigen Geräteanmeldevorgang. Das bestätigt die Verfügbarkeit der Anmeldung, nicht die Vollwiedergabeberechtigung des Kontos.

Der anschließende Kontotest war erfolgreich: Die alternative Geräteanmeldung wurde bestätigt und verschlüsselt gespeichert. Beide untersuchten V1-Wiedergabeendpunkte lieferten HTTP 200 / FULL / BTS / NONE. Der offizielle Player-SDK lieferte mit derselben alternativen Sitzung ebenfalls FULL und spielte das Hörbuch hörbar über eine Minute ab. Eine eigene Stream-Entschlüsselung oder ein anderer Audio-Player war dafür nicht erforderlich. Die Wiedergabe bleibt beim TIDAL-Player-SDK; alternative Anmeldung und V1-Katalog liefern den nun nachgewiesenen Kontozugang.

Der Android-Prototyp übernimmt nur das öffentliche Geräteprofil, implementiert Anmeldung und Token-Erneuerung selbst und bestätigt die Benutzersitzung über `/v1/sessions`. Zugriff-/Refresh-Tokens werden verschlüsselt auf dem Handy gespeichert; sie werden nicht zum Desktop exportiert. Der alternative Katalog liest paginierte Albumtracks über V1. Der zusätzliche Wiedergabetest prüft `/playbackinfopostpaywall` und `/playbackinfo` und gibt keine Stream-URLs oder Tokens aus.

Quellen: [tidalapi 0.8.11](https://pypi.org/project/tidalapi/0.8.11/), [tidalapi-Repository](https://github.com/EbbLabs/python-tidal), [Tides-Wiedergabeschnittstelle](https://github.com/luchfilip/Tides-WearOS/blob/main/feature-player/src/main/kotlin/dev/tidesapp/wearos/player/data/api/TidesPlaybackApi.kt). NOORwaves aktueller Quellcode und auch der geprüfte Tag v1.1.0 enthalten keine zugängliche Playerimplementierung. Die Alternative wird deshalb nicht als identische NOORwave-Implementierung bezeichnet.

Der persistente Hörbuch-Datenbestand ist in Version 0.3.0-private implementiert. Die TIDAL-Anbindung bleibt hinter dem Gateway und CredentialsProvider gekapselt; die lokale Bibliothek benötigt zum Anzeigen und Wiederherstellen des Hörstands keinen Katalogabruf. Der zusätzliche V1-Diagnosetest ist nicht Bestandteil des normalen Bedienablaufs.
