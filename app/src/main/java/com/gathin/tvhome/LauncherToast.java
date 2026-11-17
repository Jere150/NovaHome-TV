package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Píldora de aviso flotante estilizada para NovaHome TV.
 * Reemplaza los Toast blancos del sistema por una píldora translúcida (#E6141824)
 * con borde del tema y texto de 12sp (máximo 3 palabras).
 */
public final class LauncherToast {

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static TextView activeToast = null;
    private static Runnable dismissRunnable = null;

    public static void show(Context context, String text) {
        if (context == null || text == null || text.trim().isEmpty()) return;
        if (!(context instanceof Activity)) return;

        Activity activity = (Activity) context;
        if (activity.isFinishing() || activity.isDestroyed()) return;

        handler.post(() -> {
            ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
            if (decor == null) return;

            if (activeToast != null) {
                decor.removeView(activeToast);
                activeToast = null;
                if (dismissRunnable != null) handler.removeCallbacks(dismissRunnable);
            }

            float density = activity.getResources().getDisplayMetrics().density;
            TextView tv = new TextView(activity);
            tv.setText(text.trim());
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(12f);
            tv.setSingleLine(true);
            tv.setGravity(Gravity.CENTER);

            int padH = (int) (18 * density);
            int padV = (int) (8 * density);
            tv.setPadding(padH, padV, padH, padV);

            ThemeManager tm = ThemeManager.get(activity);
            int borderColor = tm.getParsedBorderColor();

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#E6141824"));
            bg.setCornerRadius(18 * density);
            bg.setStroke((int) Math.max(1, density), (borderColor & 0x00FFFFFF) | 0x88000000);
            tv.setBackground(bg);
            tv.setElevation(10 * density);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            lp.topMargin = (int) (48 * density);
            tv.setLayoutParams(lp);

            tv.setAlpha(0f);
            tv.setTranslationY(-12 * density);
            decor.addView(tv);
            activeToast = tv;

            tv.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180)
                    .start();

            dismissRunnable = () -> {
                if (activeToast == tv) {
                    tv.animate()
                            .alpha(0f)
                            .translationY(-12 * density)
                            .setDuration(200)
                            .withEndAction(() -> {
                                decor.removeView(tv);
                                if (activeToast == tv) activeToast = null;
                            }).start();
                }
            };
            handler.postDelayed(dismissRunnable, 1500);
        });
    }
}
