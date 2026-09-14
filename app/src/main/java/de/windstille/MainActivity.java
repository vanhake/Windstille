package de.windstille;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
        web.setWebViewClient(new WebViewClient() {
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

        setContentView(web);
        hideBars();
        web.loadUrl("file:///android_asset/index.html");
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
