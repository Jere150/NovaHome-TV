package com.gathin.tvhome;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

import androidx.core.content.ContextCompat;

/**
 * Menú de opciones compacto y translúcido anclado lateralmente a la tarjeta enfocada
 * al mantener presionado OK (Long-Press).
 * Diseño profesional sin emojis, usando iconos vectoriales limpios.
 */
public final class AppInlineMenu {

    public interface MenuCallback {
        void onActionCompleted(String pkg, boolean wasPinned);
        void onDismissed();
        void onStartMove(String pkg, int position);
    }

    public static PopupWindow show(Context context, View card, int position, int totalCount,
                                   String pkg, String label, boolean isDockExpanded,
                                   List<String> currentDockPackages, MenuCallback callback) {
        if (card == null || pkg == null) return null;

        float density = context.getResources().getDisplayMetrics().density;
        ThemeManager tm = ThemeManager.get(context);
        int themeGlow = tm.getParsedGlowColor();

        // 1. Ampliar suavemente la tarjeta a 1.10x mientras el menú está activo
        card.animate().scaleX(1.10f).scaleY(1.10f).setDuration(160).start();

        // 2. Contenedor del panel lateral (~200dp de ancho)
        int panelWidth = (int) (204 * density);
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        int padH = (int) (8 * density);
        int padV = (int) (4 * density);
        panel.setPadding(padH, padV, padH, padV);

        // Fondo oscuro translúcido (#E610121E) con esquinas redondeadas de 12dp y borde sutil
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setColor(Color.parseColor("#E610121E"));
        panelBg.setCornerRadius(12 * density);
        int borderColor = tm.getParsedBorderColor();
        panelBg.setStroke((int) Math.max(1, density), (borderColor & 0x00FFFFFF) | 0x66000000);
        panel.setBackground(panelBg);

        int cardH = card.getHeight();
        if (cardH <= 0) cardH = (int) (101 * density);
        int popupHeight = (int) (168 * density);

        PopupWindow popup = new PopupWindow(panel, panelWidth, popupHeight, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        popup.setElevation(12 * density);

        // 3. Crear opciones limpias con drawables vectoriales (sin emojis)
        boolean isPinned = (currentDockPackages != null && currentDockPackages.contains(pkg)) || tm.isPackagePinned(pkg);
        final boolean[] actionExecuted = new boolean[]{false};

        // Opción: Mover (libre con D-Pad en el dock colapsado)
        if (!isDockExpanded && isPinned && totalCount > 1) {
            panel.addView(createMenuItem(context, context.getString(R.string.menu_move_position), R.drawable.ic_move, density, themeGlow, () -> {
                if (!tm.hasUserCustomPins() && currentDockPackages != null) {
                    tm.initializeCustomPins(currentDockPackages);
                }
                popup.dismiss();
                if (callback != null) callback.onStartMove(pkg, position);
            }));
        }

        // Opción: Fijar en dock o Quitar del dock
        if (isPinned) {
            panel.addView(createMenuItem(context, context.getString(R.string.menu_unpin_dock), R.drawable.ic_unpin, density, themeGlow, () -> {
                actionExecuted[0] = true;
                if (!tm.hasUserCustomPins() && currentDockPackages != null) {
                    tm.initializeCustomPins(currentDockPackages);
                }
                tm.removePinnedPackage(pkg);
                popup.dismiss();
                if (callback != null) callback.onActionCompleted(pkg, true);
            }));
        } else {
            int curPinnedCount = tm.getDockPinnedPackages().size();
            if (curPinnedCount >= ThemeManager.MAX_DOCK_APPS) {
                panel.addView(createMenuItem(context, context.getString(R.string.menu_pin_dock), R.drawable.ic_pin, density, themeGlow, () -> {
                    popup.dismiss();
                    showReplacePinnedDialog(context, pkg, label, callback);
                }));
            } else {
                panel.addView(createMenuItem(context, context.getString(R.string.menu_pin_dock), R.drawable.ic_pin, density, themeGlow, () -> {
                    actionExecuted[0] = true;
                    if (!tm.addPinnedPackage(pkg)) {
                        popup.dismiss();
                        showReplacePinnedDialog(context, pkg, label, callback);
                    } else {
                        popup.dismiss();
                        if (callback != null) callback.onActionCompleted(pkg, false);
                    }
                }));
            }
        }

        // Opción: Información de aplicación
        panel.addView(createMenuItem(context, context.getString(R.string.menu_app_info), R.drawable.ic_info, density, themeGlow, () -> {
            popup.dismiss();
            try {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.setData(Uri.parse("package:" + pkg));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            } catch (Exception e) {
                LauncherToast.show(context, context.getString(R.string.toast_error_open));
            }
        }));

        // Opción: Desinstalar
        panel.addView(createMenuItem(context, context.getString(R.string.menu_uninstall), R.drawable.ic_delete, density, themeGlow, () -> {
            popup.dismiss();
            try {
                Intent i = new Intent(Intent.ACTION_DELETE);
                i.setData(Uri.parse("package:" + pkg));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            } catch (Exception e) {
                LauncherToast.show(context, context.getString(R.string.toast_error_uninstall));
            }
        }));

        // 4. Calcular coordenadas exactas ancladas al costado de la tarjeta
        int[] loc = new int[2];
        card.getLocationOnScreen(loc);
        int cardX = loc[0];
        int cardY = loc[1];
        int cardW = card.getWidth();
        if (cardW <= 0) cardW = (int) (180 * density);
        cardH = card.getHeight();
        if (cardH <= 0) cardH = (int) (101 * density);

        int screenW = context.getResources().getDisplayMetrics().widthPixels;
        int margin = (int) (10 * density);

        // Si cabe a la derecha, anclar estrictamente a su costado derecho; si está cerca del borde, al costado izquierdo
        boolean fitsRight = (cardX + cardW + margin + panelWidth) <= (screenW - (int) (16 * density));
        int posX = fitsRight ? (cardX + cardW + margin) : Math.max((int) (16 * density), cardX - panelWidth - margin);
        // Altura cómoda de 168dp: sobresale ligeramente por encima del dock
        int posY = Math.max((int) (16 * density), cardY - (popupHeight - cardH));

        popup.setOnDismissListener(() -> {
            if (!actionExecuted[0]) {
                card.animate().scaleX(tm.getFocusScale()).scaleY(tm.getFocusScale()).setDuration(160).start();
                card.requestFocus();
            }
            if (callback != null) callback.onDismissed();
        });

        popup.showAtLocation(card, Gravity.NO_GRAVITY, posX, posY);
        return popup;
    }

    private static View createMenuItem(Context context, String title, int iconRes, float density, int themeGlow, Runnable action) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(13f);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

        Drawable rawIcon = ContextCompat.getDrawable(context, iconRes);
        final Drawable icon = rawIcon != null ? rawIcon.mutate() : null;
        if (icon != null) {
            int iconSize = (int) (17 * density);
            icon.setBounds(0, 0, iconSize, iconSize);
            icon.setTint(Color.parseColor("#E6FFFFFF"));
            tv.setCompoundDrawables(icon, null, null, null);
            tv.setCompoundDrawablePadding((int) (10 * density));
        }

        int padH = (int) (10 * density);
        tv.setPadding(padH, 0, padH, 0);
        tv.setFocusable(true);
        tv.setClickable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        lp.setMargins(0, (int) (2 * density), 0, (int) (2 * density));
        tv.setLayoutParams(lp);

        GradientDrawable focusBg = new GradientDrawable();
        focusBg.setColor((themeGlow & 0x00FFFFFF) | 0x33000000);
        focusBg.setCornerRadius(6 * density);
        focusBg.setStroke((int) Math.max(1, density), themeGlow);

        tv.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tv.setBackground(focusBg);
                tv.setTextColor(Color.WHITE);
                if (icon != null) icon.setTint(Color.WHITE);
            } else {
                tv.setBackground(null);
                tv.setTextColor(Color.parseColor("#D9FFFFFF"));
                if (icon != null) icon.setTint(Color.parseColor("#B3FFFFFF"));
            }
        });

        tv.setOnClickListener(v -> {
            if (action != null) action.run();
        });

        return tv;
    }

    private static void showReplacePinnedDialog(Context context, String newPkg, String newLabel, MenuCallback callback) {
        ThemeManager tm = ThemeManager.get(context);
        List<String> pinned = tm.getDockPinnedPackages();
        if (pinned.isEmpty()) {
            tm.addPinnedPackage(newPkg);
            if (callback != null) callback.onActionCompleted(newPkg, false);
            return;
        }

        Dialog d = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        float density = context.getResources().getDisplayMetrics().density;

        Window window = d.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.70f);
        }

        int dialogWidth = (int) (400 * density);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int padH = (int) (22 * density);
        int padV = (int) (20 * density);
        root.setPadding(padH, padV, padH, padV);

        int glowColor = tm.getParsedGlowColor();
        int borderColor = tm.getParsedBorderColor();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F0101422"));
        bg.setCornerRadius(16 * density);
        bg.setStroke((int) Math.max(1, density), (borderColor & 0x00FFFFFF) | 0x66000000);
        root.setBackground(bg);
        root.setClipToOutline(true);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(context.getString(R.string.dialog_replace_pinned_title));
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tvTitle);

        TextView tvHint = new TextView(context);
        tvHint.setText(context.getString(R.string.dialog_replace_pinned_hint, newLabel != null ? newLabel : newPkg));
        tvHint.setTextColor(Color.parseColor("#99FFFFFF"));
        tvHint.setTextSize(12f);
        tvHint.setPadding(0, (int) (3 * density), 0, (int) (14 * density));
        root.addView(tvHint);

        PackageManager pm = context.getPackageManager();

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);

        int scrollHeight = Math.min((int) (240 * density), Math.max((int) (80 * density), pinned.size() * (int) (48 * density)));
        LinearLayout.LayoutParams lpScroll = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, scrollHeight);
        root.addView(scroll, lpScroll);

        final View[] firstItemView = new View[1];

        for (int i = 0; i < pinned.size(); i++) {
            final String oldPkg = pinned.get(i);
            String itemLabel = oldPkg;
            Drawable itemIcon = null;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(oldPkg, 0);
                CharSequence cs = pm.getApplicationLabel(ai);
                if (cs != null) itemLabel = cs.toString();
                itemIcon = pm.getApplicationIcon(ai);
            } catch (Exception ignored) {}

            LinearLayout itemRow = new LinearLayout(context);
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            itemRow.setFocusable(true);
            itemRow.setClickable(true);
            int rowPadH = (int) (14 * density);
            int rowPadV = (int) (9 * density);
            itemRow.setPadding(rowPadH, rowPadV, rowPadH, rowPadV);

            GradientDrawable itemBg = new GradientDrawable();
            itemBg.setColor(Color.parseColor("#1C2130"));
            itemBg.setCornerRadius(10 * density);
            itemBg.setStroke((int) Math.max(1, density), Color.parseColor("#262D3E"));
            itemRow.setBackground(itemBg);

            if (itemIcon != null) {
                ImageView ivIcon = new ImageView(context);
                int iconSize = (int) (26 * density);
                ivIcon.setImageDrawable(itemIcon);
                ivIcon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
                itemRow.addView(ivIcon);
            }

            TextView tvItemName = new TextView(context);
            tvItemName.setText(itemLabel);
            tvItemName.setTextColor(Color.parseColor("#E6FFFFFF"));
            tvItemName.setTextSize(13f);
            tvItemName.setSingleLine(true);
            tvItemName.setEllipsize(TextUtils.TruncateAt.END);
            tvItemName.setPadding((int) (12 * density), 0, 0, 0);
            itemRow.addView(tvItemName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            ImageView ivSwap = new ImageView(context);
            int swapSize = (int) (16 * density);
            ivSwap.setImageResource(R.drawable.ic_swap_vert);
            ivSwap.setImageTintList(ColorStateList.valueOf(Color.parseColor("#55FFFFFF")));
            ivSwap.setLayoutParams(new LinearLayout.LayoutParams(swapSize, swapSize));
            itemRow.addView(ivSwap);

            itemRow.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    itemBg.setColor((glowColor & 0x00FFFFFF) | 0x33000000);
                    itemBg.setStroke((int) (2 * density), glowColor);
                    itemRow.setScaleX(1.02f);
                    itemRow.setScaleY(1.02f);
                    tvItemName.setTextColor(Color.WHITE);
                    ivSwap.setImageTintList(ColorStateList.valueOf(glowColor));
                } else {
                    itemBg.setColor(Color.parseColor("#1C2130"));
                    itemBg.setStroke((int) Math.max(1, density), Color.parseColor("#262D3E"));
                    itemRow.setScaleX(1.0f);
                    itemRow.setScaleY(1.0f);
                    tvItemName.setTextColor(Color.parseColor("#E6FFFFFF"));
                    ivSwap.setImageTintList(ColorStateList.valueOf(Color.parseColor("#55FFFFFF")));
                }
            });

            itemRow.setOnClickListener(v -> {
                tm.replacePinnedPackage(oldPkg, newPkg);
                d.dismiss();
                LauncherToast.show(context, context.getString(R.string.toast_app_replaced));
                if (callback != null) callback.onActionCompleted(newPkg, false);
            });

            LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpRow.setMargins(0, (int) (3 * density), 0, (int) (3 * density));
            list.addView(itemRow, lpRow);

            if (i == 0) firstItemView[0] = itemRow;
        }

        Button btnCancel = new Button(context);
        btnCancel.setText(context.getString(R.string.btn_cancel));
        btnCancel.setTextColor(Color.parseColor("#CCCCCC"));
        btnCancel.setTextSize(12f);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setColor(Color.parseColor("#1E2433"));
        cancelBg.setCornerRadius(10 * density);
        cancelBg.setStroke((int) Math.max(1, density), Color.parseColor("#2A3348"));
        btnCancel.setBackground(cancelBg);

        btnCancel.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                cancelBg.setColor((glowColor & 0x00FFFFFF) | 0x44000000);
                cancelBg.setStroke((int) (2 * density), glowColor);
                btnCancel.setTextColor(Color.WHITE);
                btnCancel.setScaleX(1.02f);
                btnCancel.setScaleY(1.02f);
            } else {
                cancelBg.setColor(Color.parseColor("#1E2433"));
                cancelBg.setStroke((int) Math.max(1, density), Color.parseColor("#2A3348"));
                btnCancel.setTextColor(Color.parseColor("#CCCCCC"));
                btnCancel.setScaleX(1.0f);
                btnCancel.setScaleY(1.0f);
            }
        });
        btnCancel.setOnClickListener(v -> d.dismiss());

        LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (40 * density));
        lpCancel.setMargins(0, (int) (14 * density), 0, 0);
        root.addView(btnCancel, lpCancel);

        root.setLayoutParams(new ViewGroup.LayoutParams(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

        d.setContentView(root);

        d.setOnShowListener(di -> {
            if (window != null) {
                window.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT);
            }
            if (firstItemView[0] != null) {
                firstItemView[0].requestFocus();
            }
        });

        d.show();
    }
}
