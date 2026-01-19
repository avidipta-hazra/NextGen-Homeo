package com.webview.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import java.util.Locale;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private WebView mWebView;
    private TextToSpeech tts;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ConnectivityManager.NetworkCallback networkCallback;

    private static final String HOME_URL = "https://demo.goldenjubileehmai.in/home/";
    private static final String OFFLINE_URL = "file:///android_asset/offline.html";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Swipe Refresh
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#007D16"));

        // WebView
        mWebView = findViewById(R.id.activity_main_webview);
       WebSettings webSettings = mWebView.getSettings();
webSettings.setJavaScriptEnabled(true);
webSettings.setDomStorageEnabled(true);
webSettings.setAllowFileAccess(true);
webSettings.setAllowContentAccess(true);

// 🔊 THIS LINE IS CRITICAL
webSettings.setMediaPlaybackRequiresUserGesture(false);


        mWebView.setWebViewClient(new AppWebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        // ================= ANDROID NATIVE TTS =================
tts = new TextToSpeech(this, status -> {
    if (status == TextToSpeech.SUCCESS) {
        tts.setLanguage(Locale.ENGLISH);
        tts.setSpeechRate(0.9f);
        tts.setPitch(1.0f);
    }
});

// Expose TTS to JavaScript
mWebView.addJavascriptInterface(new Object() {

    @JavascriptInterface
    public void speak(String text) {
        if (tts != null) {
           tts.speak(text, TextToSpeech.QUEUE_ADD, null, "CT_TTS");

        }
    }

}, "AndroidTTS");

        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
    @Override
    public void onStart(String utteranceId) {}

    @Override
    public void onDone(String utteranceId) {
        mWebView.post(() -> mWebView.evaluateJavascript(
            "window.__ttsDone && window.__ttsDone();", null
        ));
    }

    @Override
    public void onError(String utteranceId) {}
});

// =====================================================
// 🔊 FORCE AUDIO OUTPUT FOR WEBVIEW
setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);

android.media.AudioManager audioManager =
        (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);

if (audioManager != null) {
    audioManager.requestAudioFocus(
            focusChange -> {},
            android.media.AudioManager.STREAM_MUSIC,
            android.media.AudioManager.AUDIOFOCUS_GAIN
    );
}
        // Pull to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isNetworkAvailable()) {
                mWebView.reload();
            } else {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
        });

        // Enable refresh only when at top
        mWebView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                swipeRefreshLayout.setEnabled(scrollY == 0)
        );

        // Download support
        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);
            request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url));
            request.addRequestHeader("User-Agent", userAgent);
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
            request.setDescription("Downloading file...");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimetype)
            );

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "Downloading file", Toast.LENGTH_SHORT).show();
            }
        });

        // Initial load
        if (isNetworkAvailable()) {
            mWebView.loadUrl(HOME_URL);
        } else {
            mWebView.loadUrl(OFFLINE_URL);
        }

        // Network callback
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    if (!HOME_URL.equals(mWebView.getUrl())) {
                        mWebView.loadUrl(HOME_URL);
                    }
                });
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> mWebView.loadUrl(OFFLINE_URL));
            }
        };

        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }

        // Request runtime permissions
        requestRequiredPermissions();
    }

    // Internet check
    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
    }

    // WebViewClient
    private class AppWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            swipeRefreshLayout.setRefreshing(false);
            super.onPageFinished(view, url);
        }
    }

    // Runtime permissions
    private void requestRequiredPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };

        boolean needRequest = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }

        if (needRequest) {
            ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,
                            "Some features may not work without permissions",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

    // Back button
    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // Cleanup
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            ConnectivityManager cm =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                cm.unregisterNetworkCallback(networkCallback);
            }
        }
    }
}
