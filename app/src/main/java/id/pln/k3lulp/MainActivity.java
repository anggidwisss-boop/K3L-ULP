package id.pln.k3lulp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_PERMISSIONS = 100;
    private static final int REQ_FILE = 101;
    private static final String WEB_APP_URL = "https://script.google.com/macros/s/AKfycbzrHPG-Mv90N__Dw8zAU9JgMhYlcrleCph2meq7WcUvpuBFkns7M0mSS-m5Ox3Is_kp/exec";
    private WebView webView;
    private ValueCallback<Uri[]> uploadCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setupWebView();
        requestNeededPermissions();
        webView.loadUrl(WEB_APP_URL);
    }

    private void setupWebView() {
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String u = r.getUrl().toString();
                if (u.startsWith("http://") || u.startsWith("https://")) { v.loadUrl(u); return true; }
                return false;
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                hideGoogleAppsScriptBanner(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                boolean ok = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                cb.invoke(origin, ok, false);
            }
            @Override public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    if (android.os.Build.VERSION.SDK_INT >= 21 && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    else request.deny();
                });
            }
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = cb;
                try { startActivityForResult(params.createIntent(), REQ_FILE); }
                catch (Exception e) { uploadCallback = null; Toast.makeText(MainActivity.this, "Pemilih foto tidak tersedia", Toast.LENGTH_SHORT).show(); return false; }
                return true;
            }
        });
    }

    private void hideGoogleAppsScriptBanner(WebView view) {
        String js = "javascript:(function(){" +
            "function hide(){" +
            "var all=document.querySelectorAll('*');" +
            "for(var i=0;i<all.length;i++){var t=(all[i].innerText||all[i].textContent||'').trim();" +
            "if(t.indexOf('Aplikasi ini dibuat oleh pengguna Google Apps Script')>=0 || t.indexOf('This application was created by a user of Google Apps Script')>=0){" +
            "var e=all[i]; var p=e; for(var j=0;j<5 && p && p.parentElement;j++){if(p.offsetHeight>20 && p.offsetHeight<180){p.style.display='none';break;} p=p.parentElement;}" +
            "}}" +
            "}" +
            "hide();setTimeout(hide,300);setTimeout(hide,1000);setTimeout(hide,2500);" +
            "})();";
        view.evaluateJavascript(js, null);
    }

    private void requestNeededPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            java.util.ArrayList<String> p = new java.util.ArrayList<>();
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) p.add(Manifest.permission.CAMERA);
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) p.add(Manifest.permission.ACCESS_FINE_LOCATION);
            if (!p.isEmpty()) requestPermissions(p.toArray(new String[0]), REQ_PERMISSIONS);
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && uploadCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                Uri u = data.getData();
                if (u != null) results = new Uri[]{u};
                else if (data.getClipData() != null) {
                    int n = data.getClipData().getItemCount(); results = new Uri[n];
                    for (int i=0;i<n;i++) results[i] = data.getClipData().getItemAt(i).getUri();
                }
            }
            uploadCallback.onReceiveValue(results); uploadCallback = null;
        }
    }

    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
