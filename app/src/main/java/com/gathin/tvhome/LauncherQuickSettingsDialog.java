package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Panel lateral derecho translúcido de Ajustes de NovaHome TV.
 * Compacto, navegable con cruceta y con controles en vivo a 60 fps.
 */
public final class LauncherQuickSettingsDialog {

    private static final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private static Runnable saveRunnable;

    private static void scheduleDebouncedSave(ThemeManager tm) {
        if (saveRunnable == null) {
            saveRunnable = tm::save;
        }
        debounceHandler.removeCallbacks(saveRunnable);
        debounceHandler.postDelayed(saveRunnable, 400);
    }

    public interface OnSettingsChangeListener {
        void onThemeUpdated();
        void onThemeVisualInvalidate();
        void onTestScreensaver();
        void onPickWallpaper();
        void onPickScreensaverHtml();
        void onDismissed();
    }

    public static Dialog show(Context context, OnSettingsChangeListener listener) {
        ThemeManager tm = ThemeManager.get(context);
        Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = dialog.getWindow();
        float density = context.getResources().getDisplayMetrics().density;
        int screenW = context.getResources().getDisplayMetrics().widthPixels;
        int screenH = context.getResources().getDisplayMetrics().heightPixels;
        int panelWidth = (int) (350 * density);

        // Calculate dock capsule top in collapsed state to float strictly 12dp above it
        int defaultInnerW = screenW - (int) (20 * density);
        int tileW = AppAdapter.tileWidthPx(defaultInnerW, 6, density);
        int cardH = AppAdapter.tileHeightPx(tileW);
        // Dock capsule: want = screenH - 42*dp - cardH; capsule.top = want - 10*dp = screenH - 52*dp - cardH
        int dockCapsuleTop = screenH - (int) (52 * density) - cardH;
        int panelTopMargin = (int) (14 * density);
        int panelBottom = dockCapsuleTop - (int) (12 * density);
        int panelHeight = Math.max((int) (200 * density), panelBottom - panelTopMargin);

        float fixedRightMargin;
        if (context instanceof Activity) {
            View grid = ((Activity) context).findViewById(R.id.grid);
            if (grid != null && grid.getWidth() > 0) {
                int[] loc = new int[2];
                grid.getLocationOnScreen(loc);
                int gridW = grid.getWidth();
                int innerW = gridW - (int) (20 * density);
                int cellW = innerW / 6;
                int gridTileW = AppAdapter.tileWidthPx(innerW, 6, density);
                float span6Width = 5 * cellW + gridTileW;
                float sidePad = 16 * density;
                float fixedCapsuleWidth = span6Width + 2 * sidePad;
                float fixedLeft = (gridW - fixedCapsuleWidth) / 2f;
                float fixedRight = fixedLeft + fixedCapsuleWidth;
                float fixedRightScreen = loc[0] + fixedRight;
                fixedRightMargin = Math.max(0f, screenW - fixedRightScreen);
            } else {
                fixedRightMargin = DockDecoration.getFixedCapsuleScreenRightMargin(screenW, density);
            }
        } else {
            fixedRightMargin = DockDecoration.getFixedCapsuleScreenRightMargin(screenW, density);
        }
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.END | Gravity.TOP);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.x = (int) Math.round(fixedRightMargin);
            lp.y = panelTopMargin;
            lp.width = panelWidth;
            lp.height = panelHeight;
            window.setAttributes(lp);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setElevation(0f);
            window.setWindowAnimations(android.R.style.Animation_Translucent);
            View decor = window.getDecorView();
            if (decor != null) {
                decor.setPadding(0, 0, 0, 0);
                decor.setBackground(null);
                decor.setElevation(0f);
            }
        }

        // Contenedor principal del panel lateral (único contenedor con bordes limpios sin doble capa)
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(panelWidth, panelHeight));
        root.setClipToOutline(true);
        root.setElevation(0f);

        updatePanelBackground(root, tm, density);

        int padH = (int) (18 * density);
        int padTop = (int) (16 * density);
        root.setPadding(padH, padTop, padH, (int) (14 * density));

        // Contenedor scrollable
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Cabecera anclada al inicio del contenido scrollable
        TextView tvTitle = new TextView(context);
        tvTitle.setText(context.getString(R.string.settings_title));
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(19f);
        tvTitle.setSingleLine(true);
        content.addView(tvTitle);

        TextView tvSubtitle = new TextView(context);
        tvSubtitle.setText(context.getString(R.string.settings_subtitle));
        tvSubtitle.setTextColor(Color.parseColor("#80FFFFFF"));
        tvSubtitle.setTextSize(11f);
        tvSubtitle.setPadding(0, (int) (2 * density), 0, (int) (10 * density));
        content.addView(tvSubtitle);

        // --- 1. TEMAS Y PERFILES GUARDADOS ---
        addSectionHeader(content, context, context.getString(R.string.settings_section_themes));

        // Contenedor dinámico de temas (incluye chip inicial '+ Guardar')
        LinearLayout themesRowContainer = new LinearLayout(context);
        themesRowContainer.setOrientation(LinearLayout.VERTICAL);
        themesRowContainer.setClipChildren(false);
        themesRowContainer.setClipToPadding(false);
        content.addView(themesRowContainer);

        List<Runnable> dynamicThemedUpdaters = new ArrayList<>();
        Runnable updateThemedViews = () -> {
            updatePanelBackground(root, tm, density);
            for (Runnable r : dynamicThemedUpdaters) {
                try { r.run(); } catch (Exception ignored) {}
            }
        };

        Runnable refreshThemes = () -> populateThemesRow(themesRowContainer, context, tm, dynamicThemedUpdaters, listener, updateThemedViews);
        refreshThemes.run();

        // --- 2. COLOR DEL DOCK ---
        addSectionHeader(content, context, context.getString(R.string.settings_section_dock_color));
        addColorControl(content, context, tm, dynamicThemedUpdaters, context.getString(R.string.settings_dock_label), tm.getDockBackgroundColorHex(),
                new String[]{"#141824", "#1E174D", "#0B2019", "#2E3440", "#000000", "#1B263B", "#2D132C", "#1A1A24"},
                panelWidth, panelHeight, panelTopMargin, fixedRightMargin, scroll,
                hex -> {
                    tm.setDockBackgroundColorHex(hex);
                    updateThemedViews.run();
                    scheduleDebouncedSave(tm);
                    if (listener != null) listener.onThemeVisualInvalidate();
                });

        // --- 3. COLOR DE RESPLANDOR (GLOW) ---
        addSectionHeader(content, context, context.getString(R.string.settings_section_glow_color));
        addColorControl(content, context, tm, dynamicThemedUpdaters, context.getString(R.string.settings_glow_label), tm.getGlowColorHex(),
                new String[]{"#88C0D0", "#A29BFE", "#55EFC4", "#F39C12", "#FF7675", "#FFFFFF", "#74B9FF", "#FD79A8"},
                panelWidth, panelHeight, panelTopMargin, fixedRightMargin, scroll,
                hex -> {
                    tm.setGlowColorHex(hex);
                    updateThemedViews.run();
                    scheduleDebouncedSave(tm);
                    if (listener != null) listener.onThemeVisualInvalidate();
                });

        // --- 4. DIMENSIONES Y OPACIDAD ---
        addSectionHeader(content, context, context.getString(R.string.settings_section_dimensions));

        // Opacidad del Dock
        final int[] curOpacityPct = {Math.round(tm.getDockOpacity() * 100)};
        addStepper(content, context, tm, dynamicThemedUpdaters, context.getString(R.string.settings_dock_opacity), curOpacityPct[0] + "%", (isRight) -> {
            if (isRight) {
                curOpacityPct[0] = Math.min(100, curOpacityPct[0] + 5);
            } else {
                curOpacityPct[0] = Math.max(10, curOpacityPct[0] - 5);
            }
            tm.setDockOpacity(curOpacityPct[0] / 100f);
            scheduleDebouncedSave(tm);
            if (listener != null) listener.onThemeVisualInvalidate();
            return curOpacityPct[0] + "%";
        });

        // Radio de Esquinas
        final int[] curRadius = {tm.getCornerRadiusDp()};
        addStepper(content, context, tm, dynamicThemedUpdaters, context.getString(R.string.settings_corner_radius), context.getString(R.string.unit_dp, curRadius[0]), (isRight) -> {
            if (isRight) {
                curRadius[0] = Math.min(32, curRadius[0] + 2);
            } else {
                curRadius[0] = Math.max(8, curRadius[0] - 2);
            }
            tm.setCornerRadiusDp(curRadius[0]);
            scheduleDebouncedSave(tm);
            if (listener != null) listener.onThemeVisualInvalidate();
            return context.getString(R.string.unit_dp, curRadius[0]);
        });

        // Modo Arte
        final int[] artTimes = {0, 5, 7, 10, 15, 30, 60};
        final int[] artIndex = {findClosestIndex(artTimes, tm.getArtModeFadeSeconds())};
        addStepper(content, context, tm, dynamicThemedUpdaters, context.getString(R.string.settings_art_mode), formatArtTime(context, artTimes[artIndex[0]]), (isRight) -> {
            if (isRight) {
                artIndex[0] = Math.min(artTimes.length - 1, artIndex[0] + 1);
            } else {
                artIndex[0] = Math.max(0, artIndex[0] - 1);
            }
            int sec = artTimes[artIndex[0]];
            tm.setArtModeFadeSeconds(sec);
            scheduleDebouncedSave(tm);
            if (listener != null) listener.onThemeVisualInvalidate();
            return formatArtTime(context, sec);
        });

        // Salvapantallas temporizador
        final int[] idleTimes = {1, 2, 3, 5, 10, 15};
        final int[] idleIndex = {findClosestIndex(idleTimes, tm.getIdleTimeoutMinutes())};
        addStepper(content, context, tm, dynamicThemedUpdaters, context.getString(R.string.settings_screensaver_timer), context.getString(R.string.unit_minutes, idleTimes[idleIndex[0]]), (isRight) -> {
            if (isRight) {
                idleIndex[0] = Math.min(idleTimes.length - 1, idleIndex[0] + 1);
            } else {
                idleIndex[0] = Math.max(0, idleIndex[0] - 1);
            }
            int min = idleTimes[idleIndex[0]];
            tm.setIdleTimeoutMinutes(min);
            scheduleDebouncedSave(tm);
            if (listener != null) listener.onThemeVisualInvalidate();
            return context.getString(R.string.unit_minutes, min);
        });

        // --- 5. PERSONALIZACIÓN Y SISTEMA ---
        addSectionHeader(content, context, context.getString(R.string.settings_section_customization));

        // Fila Fondo de pantalla: [ Elegir ] [ Quitar ]
        TextView pillElegir = createPillButton(context, tm, dynamicThemedUpdaters, context.getString(R.string.btn_choose), v -> showWallpaperSelectorDialog(context, tm, listener));
        TextView pillQuitar = createPillButton(context, tm, dynamicThemedUpdaters, context.getString(R.string.btn_remove), v -> {
            tm.setWallpaperSource("");
            if (listener != null) listener.onThemeUpdated();
        });
        addSettingsRow(content, context, tm, dynamicThemedUpdaters, context.getString(R.string.settings_wallpaper), pillElegir, pillQuitar);

        // Fila Salvapantallas: [ Elegir ] [ Probar ]
        TextView pillElegirSS = createPillButton(context, tm, dynamicThemedUpdaters, context.getString(R.string.btn_choose), v -> showScreensaverSelectorDialog(context, tm, listener));
        TextView pillProbar = createPillButton(context, tm, dynamicThemedUpdaters, context.getString(R.string.btn_test), v -> {
            dialog.setOnDismissListener(null);
            dialog.dismiss();
            if (listener != null) listener.onTestScreensaver();
        });
        addSettingsRow(content, context, tm, dynamicThemedUpdaters, context.getString(R.string.settings_screensaver), pillElegirSS, pillProbar);

        // Fila Sistema: [ Android TV ] [ Fábrica ]
        TextView pillAndroidTv = createPillButton(context, tm, dynamicThemedUpdaters, context.getString(R.string.btn_android_tv), v -> {
            dialog.dismiss();
            try {
                Intent i = new Intent(Settings.ACTION_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            } catch (Exception e) {
                LauncherToast.show(context, context.getString(R.string.toast_error_open));
            }
        });
        TextView pillFabrica = createPillButton(context, tm, dynamicThemedUpdaters, context.getString(R.string.btn_factory_reset), v -> {
            tm.resetToFactoryDefaults();
            updateThemedViews.run();
            dialog.dismiss();
            if (listener != null) {
                listener.onThemeUpdated();
                listener.onDismissed();
            }
        });
        addSettingsRow(content, context, tm, dynamicThemedUpdaters, context.getString(R.string.settings_system), pillAndroidTv, pillFabrica);

        // Guardado automático persistente en theme.json al cerrar
        dialog.setOnDismissListener(d -> {
            debounceHandler.removeCallbacks(saveRunnable);
            tm.save();
            if (listener != null) {
                listener.onThemeUpdated();
                listener.onDismissed();
            }
        });

        dialog.setContentView(root);
        dialog.show();
        return dialog;
    }

    private static void updatePanelBackground(View root, ThemeManager tm, float density) {
        if (root == null || tm == null) return;
        int baseColor = ThemeManager.parseColorSafely(tm.getDockBackgroundColorHex(), Color.parseColor("#141824"));
        int r = Color.red(baseColor);
        int g = Color.green(baseColor);
        int b = Color.blue(baseColor);
        int panelColor = Color.argb((int) (0.92f * 255), r, g, b);
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setColor(panelColor);
        panelBg.setCornerRadius(14 * density);
        int borderColor = tm.getParsedBorderColor();
        panelBg.setStroke((int) Math.max(1, density), (borderColor & 0x00FFFFFF) | 0x44000000);
        root.setBackground(panelBg);
    }

    private static void updateHsvPanelBackground(View root, ThemeManager tm, float density) {
        if (root == null || tm == null) return;
        int baseColor = ThemeManager.parseColorSafely(tm.getDockBackgroundColorHex(), Color.parseColor("#141824"));
        int r = Color.red(baseColor);
        int g = Color.green(baseColor);
        int b = Color.blue(baseColor);
        int panelColor = Color.rgb(r, g, b); // 100% opaco
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setColor(panelColor);
        panelBg.setCornerRadius(14 * density);
        int borderColor = tm.getParsedBorderColor();
        panelBg.setStroke((int) Math.max(1, density), (borderColor & 0x00FFFFFF) | 0x44000000);
        root.setBackground(panelBg);
    }

    private static int blendWithElevation(int base, float factor, int alpha) {
        int r = Color.red(base);
        int g = Color.green(base);
        int b = Color.blue(base);
        r = Math.min(255, (int) (r + (255 - r) * factor));
        g = Math.min(255, (int) (g + (255 - g) * factor));
        b = Math.min(255, (int) (b + (255 - b) * factor));
        return Color.argb(alpha, r, g, b);
    }

    private static int getRowBackgroundColor(ThemeManager tm) {
        int base = ThemeManager.parseColorSafely(tm.getDockBackgroundColorHex(), 0xFF141824);
        return blendWithElevation(base, 0.08f, 0xEE);
    }

    private static int getPillNormalColor(ThemeManager tm) {
        int base = ThemeManager.parseColorSafely(tm.getDockBackgroundColorHex(), 0xFF141824);
        return blendWithElevation(base, 0.14f, 0xFF);
    }

    private static int getPillBorderColor(ThemeManager tm) {
        int base = ThemeManager.parseColorSafely(tm.getDockBackgroundColorHex(), 0xFF141824);
        return blendWithElevation(base, 0.28f, 0x88);
    }

    private static int getPillFocusColor(ThemeManager tm) {
        int base = ThemeManager.parseColorSafely(tm.getDockBackgroundColorHex(), 0xFF141824);
        return blendWithElevation(base, 0.26f, 0xFF);
    }

    private static int getStepperNormalColor(ThemeManager tm) {
        int base = ThemeManager.parseColorSafely(tm.getDockBackgroundColorHex(), 0xFF141824);
        return blendWithElevation(base, 0.08f, 0xFF);
    }

    private static int getStepperFocusColor(ThemeManager tm) {
        int base = ThemeManager.parseColorSafely(tm.getDockBackgroundColorHex(), 0xFF141824);
        return blendWithElevation(base, 0.20f, 0xFF);
    }

    private static int getFocusStrokeColor(ThemeManager tm) {
        return tm.getParsedGlowColor();
    }

    private static void addSectionHeader(ViewGroup parent, Context context, String title) {
        float density = context.getResources().getDisplayMetrics().density;
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextColor(Color.parseColor("#88C0D0"));
        tv.setTextSize(11f);
        tv.setLetterSpacing(0.08f);
        tv.setPadding(0, (int) (12 * density), 0, (int) (4 * density));
        parent.addView(tv);
    }

    private static class Preset {
        final String name;
        final String dockBg;
        final String glow;
        final String border;
        final float opacity;
        final int radius;
        Preset(String n, String d, String g, String b, float op, int r) {
            name = n; dockBg = d; glow = g; border = b; opacity = op; radius = r;
        }
    }

    private static void populateThemesRow(ViewGroup container, Context context, ThemeManager tm, List<Runnable> updaters, OnSettingsChangeListener listener, Runnable onColorChanged) {
        container.removeAllViews();
        float density = context.getResources().getDisplayMetrics().density;

        HorizontalScrollView hsv = new HorizontalScrollView(context);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setClipToPadding(false);
        hsv.setClipChildren(false);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        hsv.addView(row);

        int padH = (int) (12 * density);
        int padV = (int) (7 * density);

        // 0. Chip destacado: + Guardar
        TextView btnSave = new TextView(context);
        btnSave.setText(context.getString(R.string.btn_save_theme));
        btnSave.setTextColor(Color.WHITE);
        btnSave.setTextSize(12f);
        btnSave.setPadding(padH, padV, padH, padV);
        btnSave.setFocusable(true);
        btnSave.setClickable(true);

        GradientDrawable saveNormalBg = new GradientDrawable();
        saveNormalBg.setCornerRadius(8 * density);

        GradientDrawable saveFocusBg = new GradientDrawable();
        saveFocusBg.setCornerRadius(8 * density);

        Runnable refreshSave = () -> {
            saveNormalBg.setColor(getPillNormalColor(tm));
            saveNormalBg.setStroke((int) Math.max(1, density), getFocusStrokeColor(tm));
            saveFocusBg.setColor(getPillFocusColor(tm));
            saveFocusBg.setStroke((int) (2 * density), getFocusStrokeColor(tm));
            btnSave.setBackground(btnSave.hasFocus() ? saveFocusBg : saveNormalBg);
        };
        refreshSave.run();
        if (updaters != null) updaters.add(refreshSave);

        btnSave.setOnFocusChangeListener((v, hasFocus) -> {
            btnSave.setBackground(hasFocus ? saveFocusBg : saveNormalBg);
            if (hasFocus) {
                hsv.smoothScrollTo(Math.max(0, v.getLeft() - (int) (24 * density)), 0);
            }
        });

        btnSave.setOnClickListener(v -> showSaveThemeDialog(context, tm, () -> {
            populateThemesRow(container, context, tm, updaters, listener, onColorChanged);
            if (listener != null) listener.onThemeUpdated();
            if (onColorChanged != null) onColorChanged.run();
        }));

        LinearLayout.LayoutParams lpSave = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpSave.setMargins(0, 0, (int) (8 * density), 0);
        btnSave.setLayoutParams(lpSave);
        row.addView(btnSave);

        // 1. Temas guardados por el usuario
        List<String> savedThemes = tm.getSavedThemeNames();
        for (String savedName : savedThemes) {
            TextView btn = new TextView(context);
            btn.setText("★ " + savedName);
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(12f);
            btn.setPadding(padH, padV, padH, padV);
            btn.setFocusable(true);
            btn.setClickable(true);

            GradientDrawable normalBg = new GradientDrawable();
            normalBg.setCornerRadius(8 * density);

            GradientDrawable focusBg = new GradientDrawable();
            focusBg.setCornerRadius(8 * density);

            Runnable refreshSavedChip = () -> {
                normalBg.setColor(getPillNormalColor(tm));
                normalBg.setStroke((int) Math.max(1, density), getFocusStrokeColor(tm));
                focusBg.setColor(getPillFocusColor(tm));
                focusBg.setStroke((int) (2 * density), getFocusStrokeColor(tm));
                btn.setBackground(btn.hasFocus() ? focusBg : normalBg);
            };
            refreshSavedChip.run();
            if (updaters != null) updaters.add(refreshSavedChip);

            btn.setOnFocusChangeListener((v, hasFocus) -> {
                btn.setBackground(hasFocus ? focusBg : normalBg);
                if (hasFocus) {
                    hsv.smoothScrollTo(Math.max(0, v.getLeft() - (int) (24 * density)), 0);
                }
            });

            btn.setOnClickListener(v -> {
                if (tm.loadCustomThemeProfile(savedName)) {
                    if (onColorChanged != null) onColorChanged.run();
                    if (listener != null) listener.onThemeUpdated();
                }
            });

            btn.setOnLongClickListener(v -> {
                showDeleteProfileDialog(context, tm, savedName, () -> populateThemesRow(container, context, tm, updaters, listener, onColorChanged));
                return true;
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, (int) (8 * density), 0);
            btn.setLayoutParams(lp);
            row.addView(btn);
        }

        // 2. Presets preconfigurados
        Preset[] presets = new Preset[]{
                new Preset("Obsidian", "#141824", "#60A5FA", "#334155", 0.55f, 18),
                new Preset("Deep Indigo", "#1E174D", "#A29BFE", "#6C5CE7", 0.55f, 18),
                new Preset("Emerald", "#0B2019", "#55EFC4", "#00B894", 0.60f, 16),
                new Preset("Nord Frost", "#2E3440", "#88C0D0", "#81A1C1", 0.65f, 20),
                new Preset("Pure OLED", "#000000", "#FFFFFF", "#2A2A2A", 0.90f, 14)
        };

        for (Preset p : presets) {
            TextView btn = new TextView(context);
            btn.setText(p.name);
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(12f);
            btn.setPadding(padH, padV, padH, padV);
            btn.setFocusable(true);
            btn.setClickable(true);

            GradientDrawable normalBg = new GradientDrawable();
            normalBg.setCornerRadius(8 * density);

            GradientDrawable focusBg = new GradientDrawable();
            focusBg.setCornerRadius(8 * density);

            Runnable refreshPresetChip = () -> {
                normalBg.setColor(getPillNormalColor(tm));
                normalBg.setStroke((int) Math.max(1, density), getPillBorderColor(tm));
                focusBg.setColor(getPillFocusColor(tm));
                focusBg.setStroke((int) (2 * density), getFocusStrokeColor(tm));
                btn.setBackground(btn.hasFocus() ? focusBg : normalBg);
            };
            refreshPresetChip.run();
            if (updaters != null) updaters.add(refreshPresetChip);

            btn.setOnFocusChangeListener((v, hasFocus) -> {
                btn.setBackground(hasFocus ? focusBg : normalBg);
                if (hasFocus) {
                    hsv.smoothScrollTo(Math.max(0, v.getLeft() - (int) (24 * density)), 0);
                }
            });

            btn.setOnClickListener(v -> {
                tm.setThemeName(p.name.toLowerCase(Locale.US).replace(" ", "_"));
                tm.setDockBackgroundColorHex(p.dockBg);
                tm.setGlowColorHex(p.glow);
                tm.setBorderColorHex(p.border);
                tm.setDockOpacity(p.opacity);
                tm.setCornerRadiusDp(p.radius);
                if (onColorChanged != null) onColorChanged.run();
                scheduleDebouncedSave(tm);
                if (listener != null) listener.onThemeVisualInvalidate();
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, (int) (8 * density), 0);
            btn.setLayoutParams(lp);
            row.addView(btn);
        }

        container.addView(hsv);
    }

    private static void showSaveThemeDialog(Context context, ThemeManager tm, Runnable onSaved) {
        Dialog d = new Dialog(context);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        float density = context.getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * density);
        root.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F210121E"));
        bg.setCornerRadius(14 * density);
        bg.setStroke((int) Math.max(1, density), Color.parseColor("#60A5FA"));
        root.setBackground(bg);

        TextView title = new TextView(context);
        title.setText(context.getString(R.string.dialog_save_theme_title));
        title.setTextColor(Color.WHITE);
        title.setTextSize(16f);
        root.addView(title);

        TextView sub = new TextView(context);
        sub.setText(context.getString(R.string.dialog_save_theme_sub));
        sub.setTextColor(Color.parseColor("#99FFFFFF"));
        sub.setTextSize(11f);
        sub.setPadding(0, (int) (2 * density), 0, (int) (10 * density));
        root.addView(sub);

        // Nombres sugeridos en chips
        HorizontalScrollView hsv = new HorizontalScrollView(context);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chipsRow = new LinearLayout(context);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(chipsRow);

        EditText etName = new EditText(context);
        etName.setTextColor(Color.WHITE);
        etName.setTextSize(14f);
        etName.setSingleLine(true);
        etName.setHint(context.getString(R.string.hint_theme_name));
        etName.setHintTextColor(Color.parseColor("#66FFFFFF"));
        String curName = tm.getThemeName();
        if (curName == null || curName.equals("obsidian_glass_default")) {
            curName = context.getString(R.string.default_theme_name);
        }
        etName.setText(curName);

        String[] suggestions = new String[]{"Cine", "Minimal", "Noche", "Juegos", "Nord"};
        for (String s : suggestions) {
            TextView chip = new TextView(context);
            chip.setText(s);
            chip.setTextColor(Color.WHITE);
            chip.setTextSize(11f);
            int cp = (int) (8 * density);
            chip.setPadding(cp, (int) (4 * density), cp, (int) (4 * density));
            chip.setFocusable(true);
            chip.setClickable(true);

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(getPillNormalColor(tm));
            chipBg.setCornerRadius(6 * density);
            chipBg.setStroke((int) Math.max(1, density), getPillBorderColor(tm));
            chip.setBackground(chipBg);

            chip.setOnFocusChangeListener((v, hasFocus) -> {
                chipBg.setColor(hasFocus ? getPillFocusColor(tm) : getPillNormalColor(tm));
                chipBg.setStroke((int) (hasFocus ? 2 * density : Math.max(1, density)), hasFocus ? getFocusStrokeColor(tm) : getPillBorderColor(tm));
            });

            chip.setOnClickListener(v -> {
                etName.setText(s);
                etName.setSelection(s.length());
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, (int) (6 * density), 0);
            chip.setLayoutParams(lp);
            chipsRow.addView(chip);
        }
        root.addView(hsv);

        // Input de texto
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#141824"));
        inputBg.setCornerRadius(8 * density);
        inputBg.setStroke((int) Math.max(1, density), Color.parseColor("#3B4252"));
        etName.setBackground(inputBg);
        int etPad = (int) (10 * density);
        etName.setPadding(etPad, etPad, etPad, etPad);

        LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(
                (int) (280 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
        lpEt.setMargins(0, (int) (10 * density), 0, (int) (14 * density));
        etName.setLayoutParams(lpEt);
        root.addView(etName);

        // Botones de acción
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnSaveAction = createTvButton(context, context.getString(R.string.btn_save));
        btnSaveAction.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) name = context.getString(R.string.fallback_theme_name);
            if (tm.saveCustomThemeProfile(name)) {
                LauncherToast.show(context, context.getString(R.string.toast_theme_saved));
                d.dismiss();
                if (onSaved != null) onSaved.run();
            } else {
                LauncherToast.show(context, context.getString(R.string.toast_save_error));
            }
        });
        btnRow.addView(btnSaveAction, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnCancel = createTvButton(context, context.getString(R.string.btn_cancel));
        btnCancel.setOnClickListener(v -> d.dismiss());
        LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpCancel.setMargins((int) (6 * density), 0, 0, 0);
        btnRow.addView(btnCancel, lpCancel);

        root.addView(btnRow);

        d.setContentView(root);
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        d.show();
    }

    private static void showDeleteProfileDialog(Context context, ThemeManager tm, String profileName, Runnable onDeleted) {
        Dialog d = new Dialog(context);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        float density = context.getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * density);
        root.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F210121E"));
        bg.setCornerRadius(14 * density);
        bg.setStroke((int) Math.max(1, density), Color.parseColor("#FF7675"));
        root.setBackground(bg);

        TextView title = new TextView(context);
        title.setText(context.getString(R.string.dialog_delete_theme_title));
        title.setTextColor(Color.WHITE);
        title.setTextSize(16f);
        root.addView(title);

        TextView msg = new TextView(context);
        msg.setText(context.getString(R.string.dialog_delete_theme_msg, profileName));
        msg.setTextColor(Color.parseColor("#CCFFFFFF"));
        msg.setTextSize(12f);
        msg.setPadding(0, (int) (4 * density), 0, (int) (14 * density));
        root.addView(msg);

        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnDelete = createTvButton(context, context.getString(R.string.btn_delete));
        btnDelete.setOnClickListener(v -> {
            tm.deleteCustomThemeProfile(profileName);
            LauncherToast.show(context, context.getString(R.string.toast_theme_deleted));
            d.dismiss();
            if (onDeleted != null) onDeleted.run();
        });
        btnRow.addView(btnDelete, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnCancel = createTvButton(context, context.getString(R.string.btn_cancel));
        btnCancel.setOnClickListener(v -> d.dismiss());
        LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpCancel.setMargins((int) (6 * density), 0, 0, 0);
        btnRow.addView(btnCancel, lpCancel);

        root.addView(btnRow);

        d.setContentView(root);
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        d.show();
    }

    private static TextView createPillButton(Context context, ThemeManager tm, List<Runnable> updaters, String text, View.OnClickListener onClick) {
        float density = context.getResources().getDisplayMetrics().density;
        TextView pill = new TextView(context);
        pill.setText(text);
        pill.setTextColor(Color.WHITE);
        pill.setTextSize(12f);
        pill.setGravity(Gravity.CENTER);
        pill.setFocusable(true);
        pill.setClickable(true);
        int padH = (int) (10 * density);
        int padV = (int) (6 * density);
        pill.setPadding(padH, padV, padH, padV);

        GradientDrawable normalBg = new GradientDrawable();
        normalBg.setCornerRadius(6 * density);

        GradientDrawable focusBg = new GradientDrawable();
        focusBg.setCornerRadius(6 * density);

        Runnable refreshStyle = () -> {
            normalBg.setColor(getPillNormalColor(tm));
            normalBg.setStroke((int) Math.max(1, density), getPillBorderColor(tm));
            focusBg.setColor(getPillFocusColor(tm));
            focusBg.setStroke((int) (2 * density), getFocusStrokeColor(tm));
            pill.setBackground(pill.hasFocus() ? focusBg : normalBg);
        };
        refreshStyle.run();
        if (updaters != null) updaters.add(refreshStyle);

        pill.setOnFocusChangeListener((v, hasFocus) -> {
            pill.setBackground(hasFocus ? focusBg : normalBg);
        });
        pill.setOnClickListener(onClick);
        return pill;
    }

    private static void addSettingsRow(ViewGroup parent, Context context, ThemeManager tm, List<Runnable> updaters, String label, View... pills) {
        float density = context.getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padH = (int) (12 * density);
        int padV = (int) (6 * density);
        row.setPadding(padH, padV, padH, padV);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(8 * density);
        Runnable refreshRow = () -> {
            bg.setColor(getRowBackgroundColor(tm));
            row.setBackground(bg);
        };
        refreshRow.run();
        if (updaters != null) updaters.add(refreshRow);

        TextView tvLabel = new TextView(context);
        tvLabel.setText(label);
        tvLabel.setTextColor(Color.parseColor("#D8DEE9"));
        tvLabel.setTextSize(13f);
        row.addView(tvLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout pillsContainer = new LinearLayout(context);
        pillsContainer.setOrientation(LinearLayout.HORIZONTAL);
        pillsContainer.setGravity(Gravity.CENTER_VERTICAL);
        int containerW = (int) (170 * density);
        pillsContainer.setLayoutParams(new LinearLayout.LayoutParams(containerW, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < pills.length; i++) {
            View pill = pills[i];
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) {
                lp.setMargins((int) (6 * density), 0, 0, 0);
            }
            if (pill instanceof TextView) {
                ((TextView) pill).setGravity(Gravity.CENTER);
            }
            pillsContainer.addView(pill, lp);
        }
        row.addView(pillsContainer);

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, (int) (6 * density));
        parent.addView(row, rowLp);
    }

    private static void showScreensaverSelectorDialog(Context context, ThemeManager tm, OnSettingsChangeListener listener) {
        Dialog d = new Dialog(context);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        float density = context.getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * density);
        root.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        int baseColor = ThemeManager.parseColorSafely(tm.getDockBackgroundColorHex(), Color.parseColor("#141824"));
        bg.setColor(Color.argb((int) (0.92f * 255), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)));
        bg.setCornerRadius(14 * density);
        bg.setStroke((int) Math.max(1, density), (tm.getParsedBorderColor() & 0x00FFFFFF) | 0x44000000);
        root.setBackground(bg);

        TextView title = new TextView(context);
        title.setText(context.getString(R.string.dialog_choose_screensaver));
        title.setTextColor(Color.WHITE);
        title.setTextSize(16f);
        title.setPadding(0, 0, 0, (int) (10 * density));
        root.addView(title);

        ScrollView scroll = new ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                (int) (310 * density), (int) (240 * density)));

        String[][] integrated = new String[][]{
                {context.getString(R.string.ss_minimal_clock), "screensavers/reloj_minimalista.html"},
                {context.getString(R.string.ss_ambient_aurora), "screensavers/aurora_ambiental.html"},
                {context.getString(R.string.ss_night_waves), "screensavers/ondas_nocturnas.html"},
                {context.getString(R.string.ss_soft_particles), "screensavers/particulas_suaves.html"}
        };

        for (String[] ss : integrated) {
            Button btn = createTvButton(context, ss[0]);
            btn.setOnClickListener(v -> {
                tm.setScreensaverType("custom_html");
                tm.setScreensaverSource(ss[1]);
                d.dismiss();
                LauncherToast.show(context, ss[0]);
                if (listener != null) listener.onThemeUpdated();
            });
            list.addView(btn);
        }

        Button btnImport = createTvButton(context, context.getString(R.string.btn_import_html));
        btnImport.setOnClickListener(v -> {
            d.dismiss();
            if (listener != null) listener.onPickScreensaverHtml();
        });
        list.addView(btnImport);

        d.setContentView(root);
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        d.show();
    }

    private static String formatCleanWallpaperName(Context context, String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        String base = (dot > 0) ? filename.substring(0, dot) : filename;
        base = base.replace('_', ' ').replace('-', ' ').trim();
        base = base.replaceAll("(?i)\\b(por defecto|default)\\b", "").trim();
        if (base.isEmpty()) return context.getString(R.string.wp_custom);
        base = base.toLowerCase(Locale.US);
        return Character.toUpperCase(base.charAt(0)) + base.substring(1);
    }

    private static void showWallpaperSelectorDialog(Context context, ThemeManager tm, OnSettingsChangeListener listener) {
        Dialog d = new Dialog(context);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        float density = context.getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * density);
        root.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        int baseColor = ThemeManager.parseColorSafely(tm.getDockBackgroundColorHex(), Color.parseColor("#141824"));
        bg.setColor(Color.argb((int) (0.92f * 255), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)));
        bg.setCornerRadius(14 * density);
        bg.setStroke((int) Math.max(1, density), (tm.getParsedBorderColor() & 0x00FFFFFF) | 0x44000000);
        root.setBackground(bg);

        TextView title = new TextView(context);
        title.setText(context.getString(R.string.dialog_choose_wallpaper));
        title.setTextColor(Color.WHITE);
        title.setTextSize(16f);
        title.setPadding(0, 0, 0, (int) (10 * density));
        root.addView(title);

        ScrollView scroll = new ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                (int) (310 * density), (int) (240 * density)));

        // 1. Fondos integrados de fábrica
        String[][] builtIn = new String[][]{
                {context.getString(R.string.wp_night_gradient), "wallpapers/gradiente_nocturno.jpg"},
                {context.getString(R.string.wp_deep_horizon), "wallpapers/horizonte_profundo.jpg"},
                {context.getString(R.string.wp_violet_mist), "wallpapers/niebla_violeta.jpg"},
                {context.getString(R.string.wp_dark_forest), "wallpapers/bosque_oscuro.jpg"},
                {context.getString(R.string.wp_minimal_slate), "wallpapers/pizarra_minimal.jpg"}
        };

        for (String[] wp : builtIn) {
            Button btnWp = createTvButton(context, wp[0]);
            btnWp.setOnClickListener(v -> {
                tm.setWallpaperSource(wp[1]);
                d.dismiss();
                LauncherToast.show(context, wp[0]);
                if (listener != null) listener.onThemeUpdated();
            });
            list.addView(btnWp);
        }

        // 2. Fondo original del launcher (sin imagen)
        Button btnDefault = createTvButton(context, context.getString(R.string.wp_original));
        btnDefault.setOnClickListener(v -> {
            tm.setWallpaperSource("");
            d.dismiss();
            if (listener != null) listener.onThemeUpdated();
        });
        list.addView(btnDefault);

        // 3. Importar desde almacenamiento
        Button btnPickExplorer = createTvButton(context, context.getString(R.string.btn_file_explorer));
        btnPickExplorer.setOnClickListener(v -> {
            d.dismiss();
            if (listener != null) listener.onPickWallpaper();
        });
        list.addView(btnPickExplorer);

        // 4. Imágenes escaneadas en almacenamiento (sin duplicados, nombres limpios en formato oración)
        List<File> images = new ArrayList<>();
        Set<String> seenCanonical = new HashSet<>();
        scanImagesInDir(new File("/sdcard/Wallpapers"), images, seenCanonical);
        scanImagesInDir(new File("/sdcard/Pictures"), images, seenCanonical);
        scanImagesInDir(new File("/sdcard/Download"), images, seenCanonical);
        File extPics = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (extPics != null) scanImagesInDir(extPics, images, seenCanonical);

        for (File img : images) {
            String rawName = img.getName();
            String lower = rawName.toLowerCase(Locale.US);
            if (lower.equals("wallpaper.jpg") || lower.equals("wallpaper.jpeg") || lower.equals("wallpaper.png")) {
                continue;
            }
            String cleanName = formatCleanWallpaperName(context, rawName);
            Button btnImg = createTvButton(context, cleanName);
            btnImg.setOnClickListener(v -> {
                tm.setWallpaperSource(img.getAbsolutePath());
                d.dismiss();
                LauncherToast.show(context, cleanName);
                if (listener != null) listener.onThemeUpdated();
            });
            list.addView(btnImg);
        }

        d.setContentView(root);
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        d.show();
    }

    private static void scanImagesInDir(File dir, List<File> out, Set<String> seenCanonical) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile()) {
                String name = f.getName().toLowerCase(Locale.US);
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")) {
                    try {
                        String canon = f.getCanonicalPath();
                        if (seenCanonical.add(canon)) {
                            out.add(f);
                        }
                    } catch (Exception e) {
                        if (!out.contains(f)) out.add(f);
                    }
                }
            }
        }
    }

    interface ColorApplyListener {
        void onApply(String hex);
    }

    private static void addColorControl(ViewGroup parent, Context context, ThemeManager tm, List<Runnable> updaters,
                                        String label, String initialHex, String[] swatches,
                                        int panelWidth, int panelHeight, int panelTopMargin, float fixedRightMargin,
                                        View settingsScrollView,
                                        ColorApplyListener applyListener) {
        float density = context.getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setPadding(0, (int) (6 * density), 0, (int) (6 * density));

        final String[] currentSelectedHex = {initialHex};

        for (String hex : swatches) {
            View dot = new View(context);
            int size = (int) (24 * density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins((int) (2 * density), 0, (int) (4 * density), 0);
            dot.setLayoutParams(lp);
            dot.setFocusable(true);
            dot.setClickable(true);

            int parsed = Color.parseColor(hex);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(parsed);
            dotBg.setStroke((int) Math.max(1, density), Color.parseColor("#4C566A"));
            dot.setBackground(dotBg);

            dot.setOnFocusChangeListener((v, hasFocus) -> {
                dot.setScaleX(hasFocus ? 1.15f : 1.0f);
                dot.setScaleY(hasFocus ? 1.15f : 1.0f);
                dotBg.setStroke(hasFocus ? (int) (2.5f * density) : (int) Math.max(1, density),
                        hasFocus ? Color.WHITE : Color.parseColor("#4C566A"));
            });

            dot.setOnClickListener(v -> {
                currentSelectedHex[0] = hex;
                if (applyListener != null) applyListener.onApply(hex);
            });

            row.addView(dot);
        }

        TextView btnHsv = new TextView(context);
        btnHsv.setId(View.generateViewId());
        btnHsv.setText("HSV");
        btnHsv.setTextSize(10f);
        btnHsv.setTextColor(Color.WHITE);
        btnHsv.setGravity(Gravity.CENTER);
        btnHsv.setFocusable(true);
        btnHsv.setClickable(true);
        btnHsv.setNextFocusRightId(btnHsv.getId());
        btnHsv.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                return true; // Consume event: block jumping out of row / to Themes row
            }
            return false;
        });

        int padH = (int) (8 * density);
        int padV = (int) (4 * density);
        btnHsv.setPadding(padH, padV, padH, padV);

        GradientDrawable hsvBg = new GradientDrawable();
        hsvBg.setCornerRadius(6 * density);
        Runnable refreshHsv = () -> {
            int normalCol = getPillNormalColor(tm);
            int focusCol = getPillFocusColor(tm);
            hsvBg.setColor(btnHsv.hasFocus() ? focusCol : normalCol);
        };
        refreshHsv.run();
        if (updaters != null) updaters.add(refreshHsv);

        btnHsv.setOnFocusChangeListener((v, hasFocus) -> {
            hsvBg.setColor(hasFocus ? getPillFocusColor(tm) : getPillNormalColor(tm));
        });

        btnHsv.setOnClickListener(v -> showHsvDialog(context, label, currentSelectedHex[0],
                panelWidth, panelHeight, panelTopMargin, fixedRightMargin, settingsScrollView, tm, hex -> {
            currentSelectedHex[0] = hex;
            if (applyListener != null) applyListener.onApply(hex);
        }));

        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBtn.setMargins((int) (6 * density), 0, 0, 0);
        btnHsv.setLayoutParams(lpBtn);
        row.addView(btnHsv);

        parent.addView(row);
    }

    private static void showHsvDialog(Context context, String label, String initialHex,
                                      int panelWidth, int panelHeight, int panelTopMargin, float fixedRightMargin,
                                      View settingsScrollView,
                                      ThemeManager tm, ColorApplyListener applyListener) {
        Dialog d = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        float density = context.getResources().getDisplayMetrics().density;

        Window window = d.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.END | Gravity.TOP);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.x = (int) Math.round(fixedRightMargin);
            lp.y = panelTopMargin;
            lp.width = panelWidth;
            lp.height = panelHeight;
            window.setAttributes(lp);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setWindowAnimations(android.R.style.Animation_Translucent);
        }

        if (settingsScrollView != null) {
            settingsScrollView.setVisibility(View.INVISIBLE);
        }

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(panelWidth, panelHeight));
        root.setClipToOutline(true);
        updateHsvPanelBackground(root, tm, density);

        int padH = (int) (18 * density);
        int padTop = (int) (16 * density);
        root.setPadding(padH, padTop, padH, (int) (14 * density));

        TextView title = new TextView(context);
        title.setText(context.getString(R.string.dialog_hsv_title, label));
        title.setTextColor(Color.WHITE);
        title.setTextSize(17f);
        title.setSingleLine(true);
        root.addView(title);

        TextView hint = new TextView(context);
        hint.setText(context.getString(R.string.dialog_hsv_hint));
        hint.setTextColor(Color.parseColor("#88FFFFFF"));
        hint.setTextSize(11f);
        hint.setPadding(0, (int) (2 * density), 0, (int) (8 * density));
        root.addView(hint);

        final String originalHex = initialHex;
        final String[] activeHex = {initialHex};

        HsvColorPickerView picker = new HsvColorPickerView(context);
        picker.setId(View.generateViewId());
        int initColor = ThemeManager.parseColorSafely(initialHex, Color.CYAN);
        picker.setInitialColor(initColor);
        LinearLayout.LayoutParams lpPicker = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (115 * density));
        lpPicker.setMargins(0, 0, 0, (int) (8 * density));
        picker.setLayoutParams(lpPicker);
        root.addView(picker);

        TextView tvSatLabel = new TextView(context);
        tvSatLabel.setText(context.getString(R.string.dialog_hsv_saturation, Math.round(picker.getSaturation() * 100)));
        tvSatLabel.setTextColor(Color.parseColor("#D8DEE9"));
        tvSatLabel.setTextSize(12f);
        tvSatLabel.setPadding(0, 0, 0, (int) (4 * density));
        root.addView(tvSatLabel);

        HsvSaturationBarView satBar = new HsvSaturationBarView(context);
        satBar.setId(View.generateViewId());
        satBar.setHueVal(picker.getHue(), picker.getValue());
        satBar.setSaturation(picker.getSaturation());
        LinearLayout.LayoutParams lpSat = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (22 * density));
        lpSat.setMargins(0, 0, 0, (int) (12 * density));
        satBar.setLayoutParams(lpSat);
        root.addView(satBar);

        // Action buttons: [ Aceptar ] [ Cancelar ]
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER_VERTICAL);

        Button btnAccept = createTvButton(context, context.getString(R.string.btn_accept));
        Button btnCancel = createTvButton(context, context.getString(R.string.btn_cancel));

        btnAccept.setId(View.generateViewId());
        btnCancel.setId(View.generateViewId());

        picker.setNextFocusDownId(satBar.getId());
        satBar.setNextFocusUpId(picker.getId());
        satBar.setNextFocusDownId(btnAccept.getId());
        btnAccept.setNextFocusUpId(satBar.getId());
        btnCancel.setNextFocusUpId(satBar.getId());

        picker.setBackTargetView(btnAccept);
        satBar.setBackTargetView(btnAccept);

        picker.setOnColorChangeListener(color -> {
            String hex = String.format("#%06X", (0xFFFFFF & color));
            activeHex[0] = hex;
            satBar.setHueVal(picker.getHue(), picker.getValue());
            tvSatLabel.setText(context.getString(R.string.dialog_hsv_saturation, Math.round(satBar.getSaturation() * 100)));
            if (applyListener != null) applyListener.onApply(hex);
            updateHsvPanelBackground(root, tm, density);
        });

        satBar.setOnSaturationChangeListener(sat -> {
            picker.setSaturation(sat);
            tvSatLabel.setText(context.getString(R.string.dialog_hsv_saturation, Math.round(sat * 100)));
            int col = picker.getCurrentColor();
            String hex = String.format("#%06X", (0xFFFFFF & col));
            activeHex[0] = hex;
            if (applyListener != null) applyListener.onApply(hex);
            updateHsvPanelBackground(root, tm, density);
        });

        Runnable restoreScroll = () -> {
            if (settingsScrollView != null) {
                settingsScrollView.setVisibility(View.VISIBLE);
            }
        };

        btnAccept.setOnClickListener(v -> {
            restoreScroll.run();
            d.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            restoreScroll.run();
            if (applyListener != null) applyListener.onApply(originalHex);
            d.dismiss();
        });

        d.setOnCancelListener(di -> {
            restoreScroll.run();
            if (applyListener != null) applyListener.onApply(originalHex);
        });

        d.setOnDismissListener(di -> restoreScroll.run());

        LinearLayout.LayoutParams lpAccept = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpAccept.setMargins(0, 0, (int) (6 * density), 0);
        btnAccept.setLayoutParams(lpAccept);

        LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpCancel.setMargins((int) (6 * density), 0, 0, 0);
        btnCancel.setLayoutParams(lpCancel);

        btnRow.addView(btnAccept);
        btnRow.addView(btnCancel);
        root.addView(btnRow);

        d.setContentView(root);
        d.show();
        picker.requestFocus();
    }

    interface StepperStepListener {
        String onStep(boolean isRight);
    }

    private static void addStepper(ViewGroup parent, Context context, ThemeManager tm, List<Runnable> updaters,
                                   String label, String initialValue, StepperStepListener listener) {
        float density = context.getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setClickable(true);
        int padH = (int) (12 * density);
        int padV = (int) (8 * density);
        row.setPadding(padH, padV, padH, padV);

        GradientDrawable normalBg = new GradientDrawable();
        normalBg.setCornerRadius(8 * density);

        GradientDrawable focusBg = new GradientDrawable();
        focusBg.setCornerRadius(8 * density);

        Runnable refreshStepper = () -> {
            normalBg.setColor(getStepperNormalColor(tm));
            focusBg.setColor(getStepperFocusColor(tm));
            focusBg.setStroke((int) Math.max(1, density), getFocusStrokeColor(tm));
            row.setBackground(row.hasFocus() ? focusBg : normalBg);
        };
        refreshStepper.run();
        if (updaters != null) updaters.add(refreshStepper);

        TextView tvLabel = new TextView(context);
        tvLabel.setText(label);
        tvLabel.setTextColor(Color.parseColor("#D8DEE9"));
        tvLabel.setTextSize(13f);
        row.addView(tvLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvVal = new TextView(context);
        tvVal.setText("◄  " + initialValue + "  ►");
        tvVal.setTextColor(Color.WHITE);
        tvVal.setTextSize(13f);
        tvVal.setGravity(Gravity.END);
        row.addView(tvVal);

        row.setOnFocusChangeListener((v, hasFocus) -> {
            row.setBackground(hasFocus ? focusBg : normalBg);
            tvLabel.setTextColor(hasFocus ? Color.WHITE : Color.parseColor("#D8DEE9"));
            tvVal.setTextColor(hasFocus ? getFocusStrokeColor(tm) : Color.WHITE);
        });

        row.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (listener != null) {
                        String newVal = listener.onStep(false);
                        tvVal.setText("◄  " + newVal + "  ►");
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (listener != null) {
                        String newVal = listener.onStep(true);
                        tvVal.setText("◄  " + newVal + "  ►");
                    }
                    return true;
                }
            }
            return false;
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, (int) (3 * density), 0, (int) (3 * density));
        row.setLayoutParams(lp);

        parent.addView(row);
    }

    private static Button createTvButton(Context context, String text) {
        float density = context.getResources().getDisplayMetrics().density;
        ThemeManager tm = ThemeManager.get(context);
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12f);
        int padH = (int) (12 * density);
        int padV = (int) (8 * density);
        btn.setPadding(padH, padV, padH, padV);

        GradientDrawable normalBg = new GradientDrawable();
        normalBg.setColor(getPillNormalColor(tm));
        normalBg.setCornerRadius(8 * density);
        normalBg.setStroke((int) Math.max(1, density), getPillBorderColor(tm));
        btn.setBackground(normalBg);

        GradientDrawable focusBg = new GradientDrawable();
        focusBg.setColor(getPillFocusColor(tm));
        focusBg.setCornerRadius(8 * density);
        focusBg.setStroke((int) (2 * density), getFocusStrokeColor(tm));

        btn.setOnFocusChangeListener((v, hasFocus) -> {
            btn.setBackground(hasFocus ? focusBg : normalBg);
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, (int) (3 * density), 0, (int) (3 * density));
        btn.setLayoutParams(lp);
        return btn;
    }

    private static int findClosestIndex(int[] arr, int val) {
        int bestIdx = 0;
        int minDiff = Integer.MAX_VALUE;
        for (int i = 0; i < arr.length; i++) {
            int diff = Math.abs(arr[i] - val);
            if (diff < minDiff) {
                minDiff = diff;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private static String formatArtTime(Context context, int sec) {
        if (sec <= 0) return context.getString(R.string.unit_seconds_off);
        return context.getString(R.string.unit_seconds, sec);
    }
}
