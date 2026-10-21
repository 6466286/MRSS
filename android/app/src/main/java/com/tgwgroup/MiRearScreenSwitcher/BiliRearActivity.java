/* SPDX-License-Identifier: GPL-3.0-or-later
 * Bilibili rear-screen web mode, added 2026-10-21.
 */
package com.tgwgroup.MiRearScreenSwitcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Own web surface: never modifies the installed Bilibili app. */
public class BiliRearActivity extends Activity {
    private static final String HOME = "https://m.bilibili.com/";
    private WebView web;
    private LinearLayout root;
    private TextView title;
    private View fullscreen;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private String adapter = "";

    private boolean exiting;

    @Override public void finish() {
        if (exiting) return;
        exiting = true;
        android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("restoreRearScreen", true);
        android.app.ActivityOptions options = android.app.ActivityOptions.makeBasic();
        options.setLaunchDisplayId(0);
        try {
            startActivity(intent, options.toBundle());
        } catch (Exception e) {
            Toast.makeText(this, "请回到MRSS点击恢复背屏", Toast.LENGTH_LONG).show();
        }
        finishAndRemoveTask();
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        // Refuse a silent main-display fallback by the device's display policy.
        if (getWindowManager().getDefaultDisplay().getDisplayId() != 1) {
            Toast.makeText(this, "系统未允许背屏启动，请检查背屏权限", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (android.os.Build.VERSION.SDK_INT >= 27) setTurnScreenOn(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setFitsSystemWindows(true);
        setContentView(root);
        LinearLayout bar = new LinearLayout(this);
        root.addView(bar);
        button(bar, "返回", this::back);
        button(bar, "首页", () -> navigate(HOME));
        button(bar, "搜索", this::search);
        button(bar, "刷新", () -> { exitFullscreen(); web.reload(); });
        button(bar, "退出", this::finish);
        title = new TextView(this);
        title.setTextSize(14);
        title.setTextColor(Color.BLACK);
        title.setPadding(dp(8), dp(3), dp(8), dp(3));
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setMessage(title.getText()).setPositiveButton("关闭", null).show());
        root.addView(title);
        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        // Keep the real, current Chromium version rather than spoofing an old UA.
        settings.setUserAgentString(settings.getUserAgentString().replace("; wv", ""));
        CookieManager.getInstance().setAcceptCookie(true);
        try (InputStream in = getAssets().open("bili-rear.js")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            adapter = new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Toast.makeText(this, "页面适配文件加载失败", Toast.LENGTH_LONG).show();
        }
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if (!r.isForMainFrame()) return !isAllowed(r.getUrl());
                Uri uri = r.getUrl();
                if (isAllowed(uri)) return false; // Preserve POSTs, redirects and history.
                Toast.makeText(BiliRearActivity.this, "此链接无法在背屏网页打开", Toast.LENGTH_SHORT).show();
                return true;
            }
            @Override public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) {
                exitFullscreen();
                title.setText("加载中…");
            }
            @Override public void onPageFinished(WebView v, String url) {
                if (isAllowed(Uri.parse(url))) v.evaluateJavascript(adapter, null);
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) title.setText("加载失败，请点刷新或返回");
            }
            @Override public void onReceivedHttpError(WebView v, WebResourceRequest r, WebResourceResponse e) {
                if (r.isForMainFrame()) title.setText("网页暂不可用（" + e.getStatusCode() + "），请稍后刷新");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onReceivedTitle(WebView v, String text) { title.setText(text); }
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreen != null) { callback.onCustomViewHidden(); return; }
                fullscreen = view; fullscreenCallback = callback;
                web.setVisibility(View.GONE);
                title.setVisibility(View.GONE);
                root.addView(view, new LinearLayout.LayoutParams(-1, 0, 1));
                // Toolbar stays reachable: back always exits fullscreen first.
            }
            @Override public void onHideCustomView() { exitFullscreen(); }
            @Override public void onPermissionRequest(PermissionRequest r) { r.deny(); }
        });
        if (state == null || web.restoreState(state) == null) web.loadUrl(HOME);
    }

    static boolean isAllowed(Uri uri) {
        String host = uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme()) && host != null &&
            (host.equalsIgnoreCase("bilibili.com") || host.toLowerCase(java.util.Locale.ROOT).endsWith(".bilibili.com") || host.equalsIgnoreCase("b23.tv"));
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void button(LinearLayout bar, String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label); b.setTextSize(11); b.setMinWidth(0); b.setMinimumWidth(0);
        b.setPadding(0, 0, 0, 0);
        bar.addView(b, new LinearLayout.LayoutParams(0, dp(44), 1));
        b.setOnClickListener(v -> action.run());
    }
    private void navigate(String url) { exitFullscreen(); web.loadUrl(url); }
    private void search() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("关键词 / BV号 / B站链接");
        new AlertDialog.Builder(this).setTitle("搜索视频").setView(input)
            .setPositiveButton("打开", (d, w) -> {
                String q = input.getText().toString().trim();
                if (q.isEmpty()) return;
                if (q.matches("BV[0-9A-Za-z]{10}")) navigate(HOME + "video/" + q);
                else if (isAllowed(Uri.parse(q))) navigate(q);
                else navigate("https://m.bilibili.com/search?keyword=" + Uri.encode(q));
            }).setNegativeButton("取消", null).show();
    }
    private void exitFullscreen() {
        if (fullscreen == null) return;
        root.removeView(fullscreen); fullscreen = null;
        web.setVisibility(View.VISIBLE); title.setVisibility(View.VISIBLE);
        WebChromeClient.CustomViewCallback callback = fullscreenCallback;
        fullscreenCallback = null;
        if (callback != null) callback.onCustomViewHidden();
    }
    private void back() {
        if (fullscreen != null) exitFullscreen();
        else if (web.canGoBack()) web.goBack();
        else finish();
    }
    @Override public void onBackPressed() { back(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        if (web != null) web.saveState(out);
        super.onSaveInstanceState(out);
    }
    @Override protected void onPause() {
        if (web != null) { web.evaluateJavascript("document.querySelectorAll('video').forEach(v=>v.pause())", null); web.onPause(); }
        super.onPause();
    }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
    @Override protected void onDestroy() {
        if (web != null) { exitFullscreen(); root.removeView(web); web.stopLoading(); web.destroy(); web = null; }
        super.onDestroy();
    }
}
