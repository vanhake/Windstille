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

## Sprachansagen

Es wird eine deutsche Stimme des Systems benutzt. Fehlt sie, spricht die
Standardstimme. Nachinstallieren unter **Einstellungen → Bedienungshilfen →
Text-in-Sprache-Ausgabe → Sprache → Deutsch**.

## Für den Play Store

Die Debug-APK ist nur zum Seitwärtsinstallieren gedacht. Für eine
Veröffentlichung in `app/build.gradle` eine `signingConfig` mit eigenem
Keystore ergänzen und `./gradlew assembleRelease` bauen.
