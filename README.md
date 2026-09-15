# Windstille – Android

Geführte Atemrunden mit Retention. Läuft vollständig offline, die App
fordert keine Internet-Berechtigung an.

## Aufbau

Die Oberfläche folgt der Wasserlinie: ein Pegel steigt beim Einatmen und
fällt beim Loslassen, in der Retention steht er still und der ganze
Bildschirm wird warm.

Der Startbildschirm zeigt die Wortmarke, den Kreis mit dem Pegel und der
gewählten Sitzung sowie den Startknopf – ein Tippen auf den Kreis startet
ebenfalls. Darunter führt das linke Rundsymbol zu den **Einstellungen**, das
rechte zur **Statistik**: Sitzungen und Durchläufe insgesamt, längste Pause
in der Schlussrunde, längste Pause überhaupt, gehaltene und geübte Zeit,
Atemzüge, Übungstage und die letzten zwölf Sitzungen als Balken. Die
Zurück-Taste des Telefons führt aus jedem Bereich zum Startbildschirm zurück
und verlässt die App erst von dort.

Während der Sitzung stehen oben die Runden als Striche, in der Mitte der
Atemzug oder die Haltezeit, darunter Phase und Hinweis. Beim Anhalten zeigt
eine Marke, wie weit die laufende Pause an die bisherige Bestzeit
heranreicht. Am Ende fasst der Abschluss die Sitzung in vier Kacheln
zusammen – längste Pause, Unterschied zum letzten Mal, geübte Zeit,
Atemzüge – und listet jeden Durchlauf einzeln auf.

Das Startsymbol zeigt dieselbe Marke: die glatte See mit Horizont, darüber
die ausgepustete Wolke, die nach rechts größer und heller wird.

## Technik

Die Oberfläche liegt als einzelne HTML-Datei unter
`app/src/main/assets/index.html`. Alle Klänge werden zur Laufzeit erzeugt,
es gibt keine Audiodateien. Die Sprachansagen laufen über die
System-Sprachausgabe von Android (`MainActivity` → `window.AndroidTTS`),
weil die WebView die Web Speech API nicht zuverlässig beherrscht.

Einstellungen und Statistik legt die Oberfläche über `window.AndroidStore`
in den SharedPreferences der App ab; außerhalb der App greift sie auf
`localStorage` zurück. Gezählt wird jede Sitzung mit mindestens einem
abgeschlossenen Durchlauf, auch eine vorzeitig beendete.

## APK bauen – Weg 1: Android Studio

1. Android Studio öffnen, **Open** und diesen Ordner wählen.
2. Der Gradle-Wrapper liegt bei (`gradlew`, Gradle 8.9); Studio lädt beim
   ersten Öffnen nur noch das Android SDK nach – bestätigen.
3. **Build → Build Bundle(s)/APK(s) → Build APK(s)**.
4. Ergebnis: `app/build/outputs/apk/debug/app-debug.apk`.

## APK bauen – Weg 2: GitHub, ohne lokale Installation

1. Diesen Ordner als Repository zu GitHub pushen.
2. Der Workflow `.github/workflows/build-apk.yml` startet bei jedem Push auf
   `main`, bei Pull Requests und lässt sich unter **Actions → APK bauen →
   Run workflow** auch von Hand starten.
3. Unter **Actions → APK bauen → Artifacts** die fertige APK (`Windstille-APK`)
   herunterladen.

## APK bauen – Weg 3: Kommandozeile

Vorausgesetzt sind JDK 17 und das Android SDK (`ANDROID_HOME` gesetzt).
Gradle selbst muss nicht installiert sein, der Wrapper holt es:

    ./gradlew assembleDebug

## Installieren

Die Debug-APK auf das Telefon kopieren und öffnen. Android fragt einmalig
nach der Erlaubnis, Apps aus dieser Quelle zu installieren. Alternativ per
Kabel:

    adb install -r app/build/outputs/apk/debug/app-debug.apk

Für den Dauergebrauch besser die signierte APK aus den **Releases** nehmen: nur
die lässt sich später durch ein Update ersetzen, ohne vorher zu deinstallieren
(und ohne dass die gespeicherten Einstellungen verloren gehen).

## Sprachansagen

Es wird eine deutsche Stimme des Systems benutzt. Fehlt sie, spricht die
Standardstimme. Nachinstallieren unter **Einstellungen → Bedienungshilfen →
Text-in-Sprache-Ausgabe → Sprache → Deutsch**.

## Signaturschlüssel einrichten

Android ersetzt eine installierte App nur dann durch ein Update, wenn beide
APKs mit demselben Schlüssel signiert sind. Die Debug-APK aus dem Workflow
`APK bauen` taugt dafür nicht: ihr Schlüssel entsteht auf dem Build-Rechner neu
und ist bei jedem Lauf ein anderer. Für Updates ohne Deinstallation braucht es
einmalig einen festen Schlüssel.

**1. Keystore erzeugen** (lokal, auf einem Rechner mit JDK):

    keytool -genkeypair -v \
      -keystore windstille-release.jks \
      -alias windstille \
      -keyalg RSA -keysize 4096 -validity 10000

Das Passwort frei wählen und die Datei sicher aufbewahren – **ohne sie sind
künftig keine Updates mehr möglich**, dann hilft nur Deinstallieren und
Neuinstallieren. Die Datei gehört nicht ins Repo; `.gitignore` hält `*.jks`
bereits heraus.

**2. Keystore in Text umwandeln**, damit er als Secret hinterlegt werden kann:

    base64 -w0 windstille-release.jks > windstille-release.jks.base64

Diese Datei ist genauso vertraulich wie der Keystore – sie enthält denselben
Schlüssel, nur als Text. Nach dem Eintragen in Schritt 3 wieder löschen.

**3. Vier Secrets anlegen** unter **Settings → Secrets and variables →
Actions → New repository secret**:

| Secret | Inhalt |
| --- | --- |
| `KEYSTORE_BASE64` | Inhalt der Datei aus Schritt 2 |
| `KEYSTORE_PASSWORD` | Passwort des Keystores |
| `KEY_ALIAS` | `windstille` |
| `KEY_PASSWORD` | Passwort des Schlüssels (bei `keytool` oben dasselbe) |

Fehlt eines davon, bricht der Release-Workflow gleich im ersten Schritt mit
einer Meldung ab, statt eine unbrauchbar signierte APK zu veröffentlichen.

`KEY_ALIAS` muss zum Keystore passen, nicht zum App-Namen: Ein bereits
angelegter Keystore behält seinen ursprünglichen Alias, das Secret bleibt dann
unverändert. Nur ein neu erzeugter Keystore bekommt den Alias von oben.

## Releases

Der Workflow `.github/workflows/release.yml` baut bei jedem Push auf `main`
eine signierte Release-APK und hängt sie an ein GitHub-Release. Er lässt sich
unter **Actions → Release veröffentlichen → Run workflow** auch von Hand
starten.

Die APK liegt danach unter **Releases**. Dieses Repository ist privat, der
Download setzt also eine Anmeldung bei GitHub voraus – im Browser des Telefons
ist man ohnehin angemeldet. Nur falls das Repository einmal öffentlich wird,
sind Release-Dateien ohne Anmeldung abrufbar; Actions-Artefakte bleiben in
jedem Fall angemeldeten Nutzern vorbehalten.

### Aktualisieren

Die App prüft nicht selbst auf neue Versionen. Der Weg ist:

1. Unter **Releases** die neueste `Windstille-<Version>.apk` herunterladen.
2. Die Datei öffnen und installieren.

Weil alle Releases denselben Signaturschlüssel tragen, läuft das Update direkt
über die vorhandene Installation – ohne Deinstallation, und die gespeicherten
Einstellungen bleiben erhalten. Nur beim Wechsel von einer älteren Debug-APK
auf das erste Release ist einmalig zu deinstallieren, weil sich dabei die
Signatur ändert.

Dasselbe gilt einmalig für den Wechsel von **Atemrunden** auf **Windstille**:
Mit dem Namen hat sich der Package-Name geändert (`de.atemrunden` →
`de.windstille`), Android hält beide deshalb für verschiedene Apps. Die alte
Version bleibt neben der neuen stehen, bis sie von Hand entfernt wird, und
Einstellungen wie Statistik fangen in der neuen bei null an – sie liegen im
privaten Speicher der alten App und lassen sich nicht übernehmen. Ab dem
ersten Windstille-Release laufen Updates wieder wie beschrieben durch.

### Versionsnummern

`versionCode` wird nicht von Hand gepflegt, sondern ist die fortlaufende Nummer
des Release-Laufs; `versionName` und der Tag folgen daraus als `1.0.<Nummer>`.
Damit zählt jede Veröffentlichung garantiert hoch, und ein Updater kann „neuer"
von „gleich" unterscheiden. Lokale Builds bekommen `versionCode 1`.

## Für den Play Store

Release-Builds sind über die Secrets oben bereits signiert
(`./gradlew assembleRelease`). Für den Play Store zusätzlich ein App Bundle
bauen (`./gradlew bundleRelease`) und im Play-Console-Konto hochladen.
