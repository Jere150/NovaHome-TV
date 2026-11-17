package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import java.io.File;

/**
 * Salvapantallas 100% HTML genérico y modular para Android TV (OLED puro #000000).
 * Carga salvapantallas HTML ligeros (integrados en assets o importados desde almacenamiento)
 * y libera completamente los recursos de WebView al salir para mantener el consumo < 30 MB.
 */
public final class GenericScreensaverView extends FrameLayout {

    public interface ScreensaverListener {
        void onScreensaverShown();
        void onScreensaverDismissed();
    }

    private WebView webView;
    private boolean isShown = false;
    private long activatedTime = 0;
    private ScreensaverListener listener;

    public GenericScreensaverView(Context context) {
        super(context);
        init();
    }

    public GenericScreensaverView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setBackgroundColor(Color.BLACK);
        setVisibility(View.GONE);
        setFocusable(false);
        setFocusableInTouchMode(false);
    }

    public void setScreensaverListener(ScreensaverListener listener) {
        this.listener = listener;
    }

    public void setAntiBurnInEnabled(boolean enabled) {
        // Los salvapantallas HTML gestionan su propio ciclo anti-burn-in internamente
    }

    public void activate() {
        if (isShown) return;
        isShown = true;
        setKeepScreenOn(true);
        if (getContext() instanceof android.app.Activity) {
            ((android.app.Activity) getContext()).getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        setVisibility(View.VISIBLE);
        setAlpha(1f);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        activatedTime = android.os.SystemClock.uptimeMillis();

        ThemeManager tm = ThemeManager.get(getContext());
        String source = tm.getScreensaverSource();
        if (source == null || source.trim().isEmpty()) {
            source = "screensavers/reloj_minimalista.html";
        }
        String cleanSource = source.trim();
        String url;

        if (cleanSource.startsWith("screensavers/") || cleanSource.startsWith("assets/screensavers/")) {
            if (cleanSource.startsWith("assets/")) cleanSource = cleanSource.substring(7);
            url = "file:///android_asset/" + cleanSource;
        } else if (cleanSource.startsWith("file://")) {
            url = cleanSource;
        } else {
            File f = new File(cleanSource);
            if (f.exists()) {
                url = "file://" + f.getAbsolutePath();
            } else {
                url = "file:///android_asset/screensavers/reloj_minimalista.html";
            }
        }

        try {
            if (webView != null) {
                removeView(webView);
                webView.destroy();
            }
            webView = new WebView(getContext());
            webView.setBackgroundColor(Color.BLACK);
            WebSettings ws = webView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setAllowFileAccess(true);
            ws.setMediaPlaybackRequiresUserGesture(false);
            webView.loadUrl(url);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            addView(webView, lp);
        } catch (Exception e) {
            android.util.Log.e("NovaHome", "Error launching screensaver WebView", e);
        }

        if (listener != null) {
            listener.onScreensaverShown();
        }
    }

    public void dismiss() {
        if (!isShown) return;
        isShown = false;
        setKeepScreenOn(false);
        if (getContext() instanceof android.app.Activity) {
            ((android.app.Activity) getContext()).getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (webView != null) {
            try {
                webView.stopLoading();
                webView.loadUrl("about:blank");
                webView.clearHistory();
                removeView(webView);
                webView.removeAllViews();
                webView.destroy();
            } catch (Exception ignored) {}
            webView = null;
        }

        setFocusable(false);
        setFocusableInTouchMode(false);
        clearFocus();
        setVisibility(View.GONE);

        if (listener != null) {
            listener.onScreensaverDismissed();
        }
    }

    public void deactivate() {
        dismiss();
    }

    public boolean isActive() {
        return isShown;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isShown && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (android.os.SystemClock.uptimeMillis() - activatedTime < 500) {
                return true;
            }
            dismiss();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (isShown) {
            dismiss();
            return true;
        }
        return super.dispatchGenericMotionEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isShown) {
            dismiss();
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void releaseResources() {
        dismiss();
    }
}
