package de.windstille;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

/**
 * Zeigt die Atem-App als lokale Web-Oberfläche und stellt ihr die
 * System-Sprachausgabe zur Verfügung. Die WebView selbst kann die
 * Web Speech API nicht, deshalb die Brücke window.AndroidTTS.
 * Einstellungen und Statistik legt die Oberfläche über window.AndroidStore
 * in den SharedPreferences ab, damit sie ein Neustart überlebt.
 */
public class MainActivity extends Activity {

    private static final String PREFS = "windstille";
    private static final String APP_URL_PREFIX = "file:///android_asset/";
    private static final int REQ_EXPORT = 1;
    private static final int REQ_IMPORT = 2;
    /** Eine Sicherung ist wenige KB groß; mehr wird beim Import nicht gelesen. */
    private static final int MAX_BACKUP_BYTES = 1024 * 1024;

    /** Inhalt des laufenden Exports, bis der Nutzer den Speicherort gewählt hat. */
    private String pendingExport;

    private WebView web;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean pageReady = false;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);

        // Bildschirm bleibt während der Übung an
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(Locale.GERMAN);
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
                tts.setSpeechRate(0.95f);
                if (Build.VERSION.SDK_INT >= 21) {
                    tts.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build());
                }
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    public void onStart(String id) { }
                    public void onDone(String id) { }
                    public void onError(String id) { }
                });
                ttsReady = true;
            }
        });

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        web.setWebViewClient(new WebViewClient() {
            /**
             * Die Brücken AndroidTTS und AndroidStore stehen jeder Seite in dieser
             * WebView offen. Deshalb darf hier nur die eigene Oberfläche laufen:
             * jeder Wechsel zu einer anderen Adresse wird verworfen.
             */
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !request.getUrl().toString().startsWith(APP_URL_PREFIX);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                pushInsets();
            }
        });
        // Die Oberfläche zeichnet hinter den Systemleisten; melde ihr, wie viel
        // Platz Status- und Navigationsleiste oben und unten beanspruchen.
        web.setOnApplyWindowInsetsListener((v, insets) -> {
            pushInsets();
            return insets;
        });
        web.setBackgroundColor(0xFF06131C);
        web.addJavascriptInterface(new TtsBridge(), "AndroidTTS");
        web.addJavascriptInterface(new StoreBridge(), "AndroidStore");
        web.addJavascriptInterface(new BackupBridge(), "AndroidBackup");

        setContentView(web);
        hideBars();
        web.loadUrl(APP_URL_PREFIX + "index.html");
    }

    /**
     * Reicht die Höhe der Systemleisten als CSS-Variablen an die Oberfläche
     * weiter. Im Vollbild liefert env(safe-area-inset-top) im WebView 0, die
     * Statusleiste blendet sich aber jederzeit wieder ein und schnitte sonst
     * die Überschriften oben ab. Gemessen wird deshalb die Höhe, die die
     * Leisten einnehmen, wenn sie sichtbar sind.
     */
    private void pushInsets() {
        if (web == null || !pageReady) return;
        float density = getResources().getDisplayMetrics().density;
        if (density <= 0f) density = 1f;
        int top = Math.round(barInsetPx(true) / density);
        int bottom = Math.round(barInsetPx(false) / density);
        String js = "document.documentElement.style.setProperty('--safe-top','" + top + "px');"
                + "document.documentElement.style.setProperty('--safe-bottom','" + bottom + "px');";
        web.evaluateJavascript(js, null);
    }

    /**
     * Oben die Höhe der Statusleiste, auch wenn sie gerade verborgen ist: im
     * Vollbild schiebt sie sich jederzeit wieder über die Seite und darf die
     * Überschrift nie abdecken. Unten dagegen nur die tatsächlich sichtbare
     * Navigationsleiste, damit ohne sie keine leere Lücke entsteht.
     */
    private int barInsetPx(boolean top) {
        WindowInsets insets = web.getRootWindowInsets();
        if (insets != null) {
            if (Build.VERSION.SDK_INT >= 30) {
                if (top) {
                    return insets.getInsetsIgnoringVisibility(
                            WindowInsets.Type.statusBars()
                                    | WindowInsets.Type.displayCutout()).top;
                }
                return insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            }
            // Vor Android 11: "stable" meint die Leiste unabhängig vom Vollbild,
            // "systemWindow" nur die gerade sichtbare.
            return top ? insets.getStableInsetTop() : insets.getSystemWindowInsetBottom();
        }
        return top ? dimenPx("status_bar_height") : 0;
    }

    /** Rückfall, solange das Fenster noch keine Insets kennt. */
    private int dimenPx(String name) {
        int id = getResources().getIdentifier(name, "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    private void hideBars() {
        View d = getWindow().getDecorView();
        d.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if (focus) {
            hideBars();
            pushInsets();
        }
    }

    /**
     * Die Zurück-Taste navigiert erst innerhalb der Oberfläche
     * (Sitzung → Start, Einstellungen oder Statistik → Start).
     * Nur auf dem Startbildschirm verlässt sie die App.
     */
    @Override
    public void onBackPressed() {
        if (web == null) {
            super.onBackPressed();
            return;
        }
        web.evaluateJavascript("(window.atemBack && window.atemBack()) === true",
                value -> {
                    if (!"true".equals(value)) MainActivity.super.onBackPressed();
                });
    }

    /** Von der Web-Oberfläche aufgerufen: window.AndroidTTS.speak("…") */
    private class TtsBridge {
        @JavascriptInterface
        public void speak(String text) {
            if (!ttsReady || text == null) return;
            if (Build.VERSION.SDK_INT >= 21) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "atem");
            } else {
                HashMap<String, String> p = new HashMap<>();
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, p);
            }
        }

        @JavascriptInterface
        public void stop() {
            if (ttsReady) tts.stop();
        }

        @JavascriptInterface
        public boolean ready() {
            return ttsReady;
        }
    }

    /** Von der Web-Oberfläche aufgerufen: window.AndroidStore.get/set("…") */
    private class StoreBridge {
        @JavascriptInterface
        public String get(String key) {
            if (key == null) return null;
            return prefs.getString(key, null);
        }

        @JavascriptInterface
        public void set(String key, String value) {
            if (key == null) return;
            prefs.edit().putString(key, value).apply();
        }

        @JavascriptInterface
        public void remove(String key) {
            if (key == null) return;
            prefs.edit().remove(key).apply();
        }
    }

    /**
     * Von der Web-Oberfläche aufgerufen: window.AndroidBackup.exportData(json, name)
     * bzw. importData(). Die Datei wählt der Nutzer über die Dateiauswahl des
     * Systems; die App braucht dafür keine Speicher-Berechtigung.
     */
    private class BackupBridge {
        @JavascriptInterface
        public void exportData(String json, String fileName) {
            if (json == null) return;
            runOnUiThread(() -> {
                pendingExport = json;
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/json")
                        .putExtra(Intent.EXTRA_TITLE,
                                fileName != null ? fileName : "windstille-sicherung.json");
                try {
                    startActivityForResult(i, REQ_EXPORT);
                } catch (Exception e) {
                    pendingExport = null;
                    backupResult(false, "Keine Dateiauswahl verfügbar.");
                }
            });
        }

        @JavascriptInterface
        public void importData() {
            runOnUiThread(() -> {
                // Viele Dateimanager melden .json als octet-stream oder text/plain.
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("*/*")
                        .putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                                "application/json", "application/octet-stream", "text/plain"});
                try {
                    startActivityForResult(i, REQ_IMPORT);
                } catch (Exception e) {
                    backupResult(false, "Keine Dateiauswahl verfügbar.");
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = (resultCode == RESULT_OK && data != null) ? data.getData() : null;
        if (requestCode == REQ_EXPORT) {
            String json = pendingExport;
            pendingExport = null;
            if (uri == null || json == null) return;
            try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                if (out == null) throw new IOException("Kein Ausgabestrom");
                out.write(json.getBytes(StandardCharsets.UTF_8));
                backupResult(true, "Sicherung gespeichert.");
            } catch (Exception e) {
                backupResult(false, "Sicherung konnte nicht gespeichert werden.");
            }
        } else if (requestCode == REQ_IMPORT) {
            if (uri == null) return;
            String text;
            try {
                text = readLimited(uri);
            } catch (Exception e) {
                backupResult(false, e instanceof TooLarge
                        ? "Die Datei ist zu groß für eine Sicherung."
                        : "Die Datei konnte nicht gelesen werden.");
                return;
            }
            // JSONObject.quote macht aus dem Dateiinhalt ein sicheres JS-Zeichenkettenliteral.
            evaluate("window.windstilleImport && window.windstilleImport(" + JSONObject.quote(text) + ")");
        }
    }

    private static class TooLarge extends IOException { }

    private String readLimited(Uri uri) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Kein Eingabestrom");
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] block = new byte[8192];
            int n;
            while ((n = in.read(block)) > 0) {
                if (buf.size() + n > MAX_BACKUP_BYTES) throw new TooLarge();
                buf.write(block, 0, n);
            }
            return new String(buf.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void backupResult(boolean ok, String message) {
        evaluate("window.windstilleBackupResult && window.windstilleBackupResult("
                + ok + "," + JSONObject.quote(message) + ")");
    }

    private void evaluate(String js) {
        if (web != null) web.evaluateJavascript(js, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
        if (ttsReady) tts.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
