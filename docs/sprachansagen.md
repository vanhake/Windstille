# Eigene Sprachansagen aufnehmen

Standardmäßig spricht die System-Sprachausgabe von Android. Wer lieber die
eigene Stimme hört, legt MP3-Dateien unter
`app/src/main/assets/voice/` ab. Die App prüft beim Start, welche Dateien
da sind: Für jede Ansage, deren Aufnahmen vollständig vorliegen, läuft die
Aufnahme, für alle übrigen weiterhin die Systemstimme. Es muss also nicht
alles auf einmal eingesprochen werden, und ohne Dateien ändert sich nichts.

## Aufnahmeformat

* MP3, Mono, 44,1 kHz, 96–128 kBit/s (alle Dateien zusammen bleiben so
  deutlich unter einem Megabyte).
* Dateiname exakt wie unten, kleingeschrieben, Endung `.mp3`.
* Vorne keine Stille, hinten höchstens etwa 0,2 Sekunden. Die zweiteiligen
  Ansagen werden direkt aneinandergehängt – führende Stille hörte man dort
  als Stolperer.
* Ruhig und eher langsam sprechen; die Ansagen kommen mitten in der Übung.

## Die Dateien

### Einzelne Sätze

| Datei | Text |
| --- | --- |
| `ausatmen-halten.mp3` | Alles ausatmen und halten. |
| `tief-einatmen.mp3` | Tief einatmen. |
| `halten.mp3` | Halten. |
| `ausatmen-entspannen.mp3` | Ausatmen und entspannen. |
| `sitzung-beendet.mp3` | Sitzung beendet. Gut gemacht. |

### Rundenansagen

Die Rundennummer wechselt, deshalb bestehen diese beiden Ansagen aus einem
festen Anfang und einer angehängten Zahl. Den Anfang ohne Punkt am Ende
sprechen, damit die Zahl sich anschließt:

| Datei | Text |
| --- | --- |
| `start-runde.mp3` | Setz oder leg dich hin. Wir beginnen mit Runde |
| `naechste-runde.mp3` | Gleich beginnt Runde |

Dazu die Zahlen, jeweils mit fallender Betonung wie am Satzende:

| Datei | Text | Datei | Text |
| --- | --- | --- | --- |
| `zahl-1.mp3` | eins | `zahl-5.mp3` | fünf |
| `zahl-2.mp3` | zwei | `zahl-6.mp3` | sechs |
| `zahl-3.mp3` | drei | `zahl-7.mp3` | sieben |
| `zahl-4.mp3` | vier | `zahl-8.mp3` | acht |

Mehr als acht Runden lassen sich nicht einstellen, darum enden die Zahlen
hier.

### Zwischenzählung beim Atmen

Alle zehn Atemzüge wird die Zahl angesagt:

| Datei | Text |
| --- | --- |
| `zahl-10.mp3` | zehn |
| `zahl-20.mp3` | zwanzig |
| `zahl-30.mp3` | dreißig |
| `zahl-40.mp3` | vierzig |
| `zahl-50.mp3` | fünfzig |

## Prüfen

Nach dem Ablegen der Dateien im Startbildschirm neben **Sprachansagen** auf
**Testen** tippen. Kommt die eigene Stimme, meldet der Hinweis darunter
„Läuft – eigene Aufnahme“; bei „Läuft – <Stimmenname>“ fehlt die Datei
`ausatmen-halten.mp3` oder der Dateiname stimmt nicht.

Zum Ausprobieren ohne Android genügt es, `app/src/main/assets/index.html`
im Browser zu öffnen – der sucht die Aufnahmen im selben `voice/`-Ordner.
