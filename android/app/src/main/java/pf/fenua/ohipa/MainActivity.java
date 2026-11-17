package pf.fenua.ohipa;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Enveloppe Android : affiche la version en ligne (GitHub Pages), donc toujours à jour.
 *  Sans réseau au tout premier lancement, elle affiche la copie embarquée dans l'APK. */
public class MainActivity extends Activity {
    private WebView web;
    private String home;
    private boolean fallback = false;
    private int tries = 0;
    private boolean errored = false;

    /** Copie de la progression hors du navigateur : elle survit aux mises à jour
     *  et reste la même pour la version en ligne et pour la copie embarquée. */
    public class Bridge {
        private SharedPreferences p() { return getSharedPreferences("progression", MODE_PRIVATE); }
        @JavascriptInterface public String load() { return p().getString("all", "{}"); }
        @JavascriptInterface public void save(String json) { if (json != null && json.length() < 4_000_000) p().edit().putString("all", json).apply(); }
        @JavascriptInterface public String home() { return home; }
    }

    /** Erreur sur la page principale : on réessaie la version en ligne avant de passer à la copie embarquée. */
    private void onMainError(WebView v) {
        errored = true;
        if (fallback) return;
        if (tries < 3) { tries++; v.postDelayed(() -> v.loadUrl(home), 2500L * tries); return; }
        fallback = true;
        v.loadUrl("file:///android_asset/www/index.html");
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        home = getString(R.string.app_url);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(s.getUserAgentString() + " CadreFenuaApp/1");
        web.addJavascriptInterface(new Bridge(), "CFNative");
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView v, String url, android.graphics.Bitmap f) { errored = false; }
            @Override public void onPageFinished(WebView v, String url) {
                if (!errored && url != null && url.startsWith(home)) { tries = 0; fallback = false; }
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String url = u.toString();
                if (url.startsWith(home) || url.startsWith("file:///android_asset/")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { /* aucun navigateur */ }
                return true;
            }
            @Override public void onReceivedHttpError(WebView v, WebResourceRequest r, android.webkit.WebResourceResponse resp) {
                // Adresse introuvable (dépôt pas encore en ligne) : copie embarquée.
                if (r.isForMainFrame() && resp.getStatusCode() >= 400) onMainError(v);
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) onMainError(v);
            }
        });
        if (b != null) web.restoreState(b); else web.loadUrl(home);
    }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
