# Atemrunden – Android

Geführte Wim-Hof-Atmung. Läuft vollständig offline, die App fordert keine
Internet-Berechtigung an.

Die Oberfläche liegt als einzelne HTML-Datei unter
`app/src/main/assets/index.html`. Atemgeräusch und Gong werden zur Laufzeit
erzeugt. Die Sprachansagen laufen über die System-Sprachausgabe von Android
(`MainActivity` → `window.AndroidTTS`), weil die WebView die Web Speech API
nicht zuverlässig beherrscht – wahlweise auch über eigene Aufnahmen unter
`app/src/main/assets/voice/`.

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

## Sprachansagen

Es wird eine deutsche Stimme des Systems benutzt. Fehlt sie, spricht die
Standardstimme. Nachinstallieren unter **Einstellungen → Bedienungshilfen →
Text-in-Sprache-Ausgabe → Sprache → Deutsch**.

Statt der Systemstimme kann die App auch eingesprochene Aufnahmen abspielen.
Dazu die MP3-Dateien nach `app/src/main/assets/voice/` legen – welche das
sind und wie sie heißen müssen, steht in
[`docs/sprachansagen.md`](docs/sprachansagen.md). Vorhandene Aufnahmen haben
Vorrang, für alles Übrige spricht weiterhin das System. Ohne Aufnahmen
ändert sich nichts.

## Für den Play Store

Die Debug-APK ist nur zum Seitwärtsinstallieren gedacht. Für eine
Veröffentlichung in `app/build.gradle` eine `signingConfig` mit eigenem
Keystore ergänzen und `./gradlew assembleRelease` bauen.
