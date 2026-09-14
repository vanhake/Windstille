# Atemrunden – Android

Geführte Wim-Hof-Atmung. Läuft vollständig offline, die App fordert keine
Internet-Berechtigung an.

## Aufbau

Der Startbildschirm zeigt nur den Atemkreis und den Startknopf – ein Tippen
auf den Kreis startet ebenfalls. Unten in der Mitte führt das Zahnrad zu den
**Einstellungen**, unten rechts der Pokal zur **Statistik**: Sitzungen und
Durchläufe insgesamt, längste Pause in der Schlussrunde, längste Pause
überhaupt, gehaltene und geübte Zeit, Atemzüge, Übungstage und die letzten
zwölf Sitzungen. Die Zurück-Taste des Telefons führt aus jedem Bereich zum
Startbildschirm zurück und verlässt die App erst von dort.

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
3. Unter **Actions → APK bauen → Artifacts** die fertige APK (`Atemrunden-APK`)
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
      -keystore atemrunden-release.jks \
      -alias atemrunden \
      -keyalg RSA -keysize 4096 -validity 10000

Das Passwort frei wählen und die Datei sicher aufbewahren – **ohne sie sind
künftig keine Updates mehr möglich**, dann hilft nur Deinstallieren und
Neuinstallieren. Die Datei gehört nicht ins Repo; `.gitignore` hält `*.jks`
bereits heraus.

**2. Keystore in Text umwandeln**, damit er als Secret hinterlegt werden kann:

    base64 -w0 atemrunden-release.jks > atemrunden-release.jks.base64

Diese Datei ist genauso vertraulich wie der Keystore – sie enthält denselben
Schlüssel, nur als Text. Nach dem Eintragen in Schritt 3 wieder löschen.

**3. Vier Secrets anlegen** unter **Settings → Secrets and variables →
Actions → New repository secret**:

| Secret | Inhalt |
| --- | --- |
| `KEYSTORE_BASE64` | Inhalt der Datei aus Schritt 2 |
| `KEYSTORE_PASSWORD` | Passwort des Keystores |
| `KEY_ALIAS` | `atemrunden` |
| `KEY_PASSWORD` | Passwort des Schlüssels (bei `keytool` oben dasselbe) |

Fehlt eines davon, bricht der Release-Workflow gleich im ersten Schritt mit
einer Meldung ab, statt eine unbrauchbar signierte APK zu veröffentlichen.

## Releases

Der Workflow `.github/workflows/release.yml` baut bei jedem Push auf `main`
eine signierte Release-APK und hängt sie an ein GitHub-Release. Er lässt sich
unter **Actions → Release veröffentlichen → Run workflow** auch von Hand
starten.

Die APK liegt danach unter **Releases** und ist ohne GitHub-Login abrufbar –
anders als Actions-Artefakte, an die ein Update-Manager auf dem Telefon nicht
herankäme.

### Versionsnummern

`versionCode` wird nicht von Hand gepflegt, sondern ist die fortlaufende Nummer
des Release-Laufs; `versionName` und der Tag folgen daraus als `1.0.<Nummer>`.
Damit zählt jede Veröffentlichung garantiert hoch, und ein Updater kann „neuer"
von „gleich" unterscheiden. Lokale Builds bekommen `versionCode 1`.

## Für den Play Store

Release-Builds sind über die Secrets oben bereits signiert
(`./gradlew assembleRelease`). Für den Play Store zusätzlich ein App Bundle
bauen (`./gradlew bundleRelease`) und im Play-Console-Konto hochladen.
