package vn.team03.bepnha;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.webkit.WebViewAssetLoader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public final class MainActivity extends ComponentActivity {
    private static final String HOST = "appassets.androidplatform.net";
    private static final String START = "https://" + HOST + "/assets/web/index.html";
    private static final int NOTIFICATIONS_REQUEST = 101;
    private WebView webView;
    private volatile String launchContext = "{}";

    // JavaScript is required by the bundled UI; remote navigation/resources and file access are blocked.
    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ReminderStore.createChannel(this);
        setLaunchContext(getIntent());

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(250, 249, 245));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(250, 249, 245));
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                Insets keyboard = insets.getInsets(WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setTextZoom(Math.round(100 * getResources().getConfiguration().fontScale));
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AndroidBridge(), "BepNhaAndroid");

        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (!isLocal(uri)) return blocked();
                WebResourceResponse response = loader.shouldInterceptRequest(uri);
                return response != null ? response : blocked();
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Native bridge methods are available exclusively to bundled app content.
                return !isLocal(request.getUrl());
            }
            @Override public void onPageFinished(WebView view, String url) { sendStatus(); }
        });
        webView.loadUrl(START);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { handleBack(); }
        });
    }

    private static boolean isLocal(Uri uri) {
        return "https".equals(uri.getScheme()) && HOST.equals(uri.getHost())
                && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getPath() != null && uri.getPath().startsWith("/assets/");
    }

    private static WebResourceResponse blocked() {
        return new WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden",
                Collections.singletonMap("Cache-Control", "no-store"), new ByteArrayInputStream("Blocked".getBytes(StandardCharsets.UTF_8)));
    }

    @Override protected void onResume() {
        super.onResume();
        ReminderStore.restore(this);
        if (webView != null) { webView.onResume(); sendStatus(); }
    }

    @Override protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        setLaunchContext(intent);
        if (webView != null) webView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('bepnha:open',{detail:" + launchContext + "}));", null);
    }

    private void setLaunchContext(Intent intent) {
        String date = intent.getStringExtra("date");
        String meal = intent.getStringExtra("meal");
        JSONObject object = new JSONObject();
        if (date != null && date.matches("\\d{4}-\\d{2}-\\d{2}") && ("lunch".equals(meal) || "dinner".equals(meal))) {
            try { object.put("date", date).put("meal", meal).put("view", "cook"); }
            catch (JSONException ignored) { }
        }
        launchContext = object.toString();
    }

    private String statusJson() {
        JSONObject object = new JSONObject();
        try { object.put("notifications", ReminderStore.canNotify(this))
                .put("exactAlarms", ReminderStore.canScheduleExact(this)).put("sdk", Build.VERSION.SDK_INT); }
        catch (JSONException ignored) { }
        return object.toString();
    }

    private void sendStatus() {
        if (webView != null) webView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('bepnha:status',{detail:" + statusJson() + "}));", null);
    }

    private void requestNotifications() {
        if (ReminderStore.canNotify(this)) { sendStatus(); return; }
        boolean asked = ReminderStore.preferences(this).getBoolean("asked_notifications", false);
        if (Build.VERSION.SDK_INT >= 33 && (!asked || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))) {
            ReminderStore.preferences(this).edit().putBoolean("asked_notifications", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATIONS_REQUEST);
        } else {
            startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATIONS_REQUEST) {
            ReminderStore.restore(this);
            sendStatus();
        }
    }

    private void handleBack() {
        webView.evaluateJavascript("window.bepNhaBack ? window.bepNhaBack() : false", handled -> {
            if (!"true".equals(handled)) finish();
        });
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("BepNhaAndroid");
            if (webView.getParent() instanceof ViewGroup) ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy(); webView = null;
        }
        super.onDestroy();
    }

    public final class AndroidBridge {
        @JavascriptInterface public String getStatus() { return statusJson(); }
        @JavascriptInterface public String getLaunchContext() { return launchContext; }

        @JavascriptInterface public boolean syncReminders(String json) {
            try { ReminderStore.synchronize(MainActivity.this, json); return true; }
            catch (JSONException | IllegalArgumentException | SecurityException exception) { return false; }
        }

        @JavascriptInterface public void requestNotifications() {
            runOnUiThread(MainActivity.this::requestNotifications);
        }

        @JavascriptInterface public void requestExactAlarms() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 31 && !ReminderStore.canScheduleExact(MainActivity.this)) {
                    try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()))); }
                    catch (ActivityNotFoundException exception) { Toast.makeText(MainActivity.this, "Thiết bị không có mục cấp quyền nhắc đúng giờ.", Toast.LENGTH_LONG).show(); }
                } else sendStatus();
            });
        }

        @JavascriptInterface public void copyText(String text) {
            if (text == null || text.length() > 20_000) return;
            runOnUiThread(() -> getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Bếp Nhà", text)));
        }

        @JavascriptInterface public void openCalendar(String json) {
            try {
                JSONObject item = new JSONObject(json);
                long start = item.getLong("start"), end = item.getLong("end");
                String title = item.getString("title"), description = item.getString("description");
                if (start <= 0 || end <= start || end - start > 24 * 60 * 60 * 1000L || title.length() > 100 || description.length() > 5000) return;
                Intent intent = new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                        .putExtra(CalendarContract.Events.TITLE, title)
                        .putExtra(CalendarContract.Events.DESCRIPTION, description)
                        .putExtra(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Ho_Chi_Minh");
                runOnUiThread(() -> {
                    try { startActivity(intent); }
                    catch (ActivityNotFoundException exception) { Toast.makeText(MainActivity.this, "Bạn cần cài ứng dụng lịch để thêm sự kiện.", Toast.LENGTH_LONG).show(); }
                });
            } catch (JSONException ignored) { }
        }
    }
}
