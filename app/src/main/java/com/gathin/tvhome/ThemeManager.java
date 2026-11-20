package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de configuración modular y temas para NovaHome Launcher.
 * Administra theme.json en almacenamiento accesible por ADB y la app.
 */
public final class ThemeManager {
    private static final String TAG = "ThemeManager";
    private static final String FILE_NAME = "theme.json";
    private static final String USER_LAYOUT_FILE = "user_layout.json";
    private static final String PREFS_THEME = "novahome_theme";
    private static final String PREFS_USER_LAYOUT = "novahome_user_layout";
    public static final int MAX_DOCK_APPS = 6;
    private static volatile ThemeManager instance;

    private final Context appContext;
    private final File primaryConfigFile;
    private final File internalConfigFile;
    private final File downloadConfigFile;

    private final File primaryLayoutFile;
    private final File internalLayoutFile;

    // Campos del tema
    private String themeName = "obsidian_glass_default";

    // Wallpaper
    private String wallpaperSource = "";
    private int artModeFadeSeconds = 7;
    private boolean bottomGradientScrim = true;

    // Dock
    private String dockBackgroundColorHex = "#141824";
    private float dockOpacity = 0.55f;
    private int cornerRadiusDp = 18;
    private String borderColorHex = "#334155";
    private String glowColorHex = "#60A5FA";
    private float focusScale = 1.10f;
    private boolean showUnfocusedLabels = false;

    // Pinned packages y bandera de personalización del usuario
    private final List<String> dockPinnedPackages = new ArrayList<>();
    private boolean hasUserCustomPins = false;

    // Screensaver
    private boolean screensaverEnabled = true;
    private int idleTimeoutMinutes = 5;
    private String screensaverType = "custom_html";
    private String screensaverSource = "";
    private boolean antiBurnInCycle = true;

    private ThemeManager(Context context) {
        this.appContext = context.getApplicationContext();
        File externalFiles = appContext.getExternalFilesDir(null);
        if (externalFiles == null) {
            externalFiles = appContext.getFilesDir();
        }
        this.primaryConfigFile = new File(externalFiles, FILE_NAME);
        this.internalConfigFile = new File(appContext.getFilesDir(), FILE_NAME);
        this.downloadConfigFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE_NAME);

        this.primaryLayoutFile = new File(externalFiles, USER_LAYOUT_FILE);
        this.internalLayoutFile = new File(appContext.getFilesDir(), USER_LAYOUT_FILE);
        load();
    }

    public static ThemeManager get(Context context) {
        if (instance == null) {
            synchronized (ThemeManager.class) {
                if (instance == null) {
                    instance = new ThemeManager(context);
                }
            }
        }
        return instance;
    }

    public synchronized void load() {
        File target = null;
        if (downloadConfigFile.exists() && downloadConfigFile.canRead()) {
            if (!primaryConfigFile.exists() || downloadConfigFile.lastModified() > primaryConfigFile.lastModified()) {
                target = downloadConfigFile;
                Log.i(TAG, "Importando theme.json desde carpeta Downloads: " + downloadConfigFile.getAbsolutePath());
            } else {
                target = primaryConfigFile;
            }
        } else if (primaryConfigFile.exists() && primaryConfigFile.canRead()) {
            target = primaryConfigFile;
        } else if (internalConfigFile.exists() && internalConfigFile.canRead()) {
            target = internalConfigFile;
        }

        if (target != null) {
            try {
                String jsonStr = readFile(target);
                JSONObject root = new JSONObject(jsonStr);
                parseJson(root);
                Log.i(TAG, "Configuración visual cargada exitosamente de: " + target.getName());
            } catch (Exception e) {
                Log.e(TAG, "Error al leer o parsear " + target.getAbsolutePath(), e);
            }
        } else {
            createDefaultVisuals();
        }

        // Cargar layout de usuario desacoplado de la estética visual
        loadUserLayout();

        save();
    }

    public synchronized void loadUserLayout() {
        File targetLayout = null;
        if (primaryLayoutFile.exists() && primaryLayoutFile.canRead()) {
            targetLayout = primaryLayoutFile;
        } else if (internalLayoutFile.exists() && internalLayoutFile.canRead()) {
            targetLayout = internalLayoutFile;
        }

        if (targetLayout != null) {
            try {
                String jsonStr = readFile(targetLayout);
                JSONObject root = new JSONObject(jsonStr);
                parseUserLayoutJson(root);
                Log.i(TAG, "Layout de usuario cargado exitosamente de: " + targetLayout.getName());
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error al leer o parsear " + targetLayout.getAbsolutePath(), e);
            }
        }

        SharedPreferences spLayout = appContext.getSharedPreferences(PREFS_USER_LAYOUT, Context.MODE_PRIVATE);
        if (spLayout.contains("has_user_custom_pins")) {
            this.hasUserCustomPins = spLayout.getBoolean("has_user_custom_pins", false);
            String spPkgsJson = spLayout.getString("dock_pinned_packages", null);
            dockPinnedPackages.clear();
            if (spPkgsJson != null) {
                try {
                    JSONArray arr = new JSONArray(spPkgsJson);
                    for (int i = 0; i < arr.length(); i++) {
                        String pkg = arr.optString(i, "").trim();
                        if (!pkg.isEmpty() && !dockPinnedPackages.contains(pkg)) {
                            dockPinnedPackages.add(pkg);
                            if (dockPinnedPackages.size() >= MAX_DOCK_APPS) break;
                        }
                    }
                } catch (Exception ignored) {}
            }
            saveUserLayout();
            return;
        }

        // Migración hacia atrás desde el SharedPreferences original novahome_theme
        SharedPreferences spLegacy = appContext.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE);
        if (spLegacy.contains("has_user_custom_pins")) {
            this.hasUserCustomPins = spLegacy.getBoolean("has_user_custom_pins", false);
            String spPkgsJson = spLegacy.getString("dock_pinned_packages", null);
            dockPinnedPackages.clear();
            if (spPkgsJson != null) {
                try {
                    JSONArray arr = new JSONArray(spPkgsJson);
                    for (int i = 0; i < arr.length(); i++) {
                        String pkg = arr.optString(i, "").trim();
                        if (!pkg.isEmpty() && !dockPinnedPackages.contains(pkg)) {
                            dockPinnedPackages.add(pkg);
                            if (dockPinnedPackages.size() >= MAX_DOCK_APPS) break;
                        }
                    }
                } catch (Exception ignored) {}
            }
            saveUserLayout();
            return;
        }

        // Si es una primera instalación sin apps fijadas, autopoblar
        if (dockPinnedPackages.isEmpty()) {
            autoPopulateInitialApps();
            saveUserLayout();
        }
    }

    private void parseUserLayoutJson(JSONObject root) {
        this.hasUserCustomPins = root.optBoolean("has_user_custom_pins", false);
        dockPinnedPackages.clear();
        JSONArray pkgs = root.optJSONArray("dock_pinned_packages");
        if (pkgs != null) {
            for (int i = 0; i < pkgs.length(); i++) {
                String pkg = pkgs.optString(i, "").trim();
                if (!pkg.isEmpty() && !dockPinnedPackages.contains(pkg)) {
                    dockPinnedPackages.add(pkg);
                    if (dockPinnedPackages.size() >= MAX_DOCK_APPS) break;
                }
            }
        }
    }

    public synchronized boolean saveUserLayout() {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("has_user_custom_pins", hasUserCustomPins);
            JSONArray pkgs = new JSONArray();
            for (String pkg : dockPinnedPackages) {
                pkgs.put(pkg);
            }
            root.put("dock_pinned_packages", pkgs);
            String outStr = root.toString(2);

            writeFile(primaryLayoutFile, outStr);
            if (internalLayoutFile != null) {
                try {
                    writeFile(internalLayoutFile, outStr);
                } catch (Exception ignored) {}
            }

            SharedPreferences sp = appContext.getSharedPreferences(PREFS_USER_LAYOUT, Context.MODE_PRIVATE);
            sp.edit()
                .putBoolean("has_user_custom_pins", hasUserCustomPins)
                .putString("dock_pinned_packages", pkgs.toString())
                .apply();

            Log.i(TAG, "user_layout.json guardado correctamente");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error guardando user_layout.json", e);
            return false;
        }
    }

    private void parseJson(JSONObject root) {
        this.themeName = root.optString("theme_name", "obsidian_glass_default");

        JSONObject wp = root.optJSONObject("wallpaper");
        if (wp != null) {
            this.wallpaperSource = wp.optString("source", "");
            this.artModeFadeSeconds = wp.optInt("art_mode_fade_seconds", 7);
            this.bottomGradientScrim = wp.optBoolean("bottom_gradient_scrim", true);
        }

        JSONObject d = root.optJSONObject("dock");
        if (d != null) {
            this.dockBackgroundColorHex = d.optString("background_color", "#141824");
            this.dockOpacity = (float) d.optDouble("opacity", 0.55);
            this.cornerRadiusDp = d.optInt("corner_radius_dp", 18);
            this.borderColorHex = d.optString("border_color", "#334155");
            this.glowColorHex = d.optString("glow_color", "#60A5FA");
            this.focusScale = (float) d.optDouble("focus_scale", 1.10);
            this.showUnfocusedLabels = d.optBoolean("show_unfocused_labels", false);
        }

        // Si el usuario no tiene apps fijadas ni layout previo, permitir que el tema sugiera iniciales
        if (!this.hasUserCustomPins && this.dockPinnedPackages.isEmpty()) {
            JSONArray pkgs = root.optJSONArray("dock_pinned_packages");
            if (pkgs != null && pkgs.length() > 0) {
                for (int i = 0; i < pkgs.length(); i++) {
                    String pkg = pkgs.optString(i, "").trim();
                    if (!pkg.isEmpty() && !dockPinnedPackages.contains(pkg)) {
                        dockPinnedPackages.add(pkg);
                        if (dockPinnedPackages.size() >= MAX_DOCK_APPS) break;
                    }
                }
            }
        }

        JSONObject ss = root.optJSONObject("screensaver");
        if (ss != null) {
            this.screensaverEnabled = ss.optBoolean("enabled", true);
            this.idleTimeoutMinutes = ss.optInt("idle_timeout_minutes", 5);
            this.screensaverType = ss.optString("type", "custom_html");
            this.screensaverSource = ss.optString("source", "");
            this.antiBurnInCycle = ss.optBoolean("anti_burn_in_cycle", true);
        }
    }

    private void autoPopulateInitialApps() {
        PackageManager pm = appContext.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0);
        if (activities.isEmpty()) {
            activities = pm.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        }

        for (ResolveInfo ri : activities) {
            if (ri.activityInfo != null && !appContext.getPackageName().equals(ri.activityInfo.packageName)) {
                String pkg = ri.activityInfo.packageName;
                if (!dockPinnedPackages.contains(pkg)) {
                    dockPinnedPackages.add(pkg);
                    if (dockPinnedPackages.size() >= MAX_DOCK_APPS) break;
                }
            }
        }
    }

    private void createDefaultVisuals() {
        this.themeName = "obsidian_glass_default";
        this.wallpaperSource = "";
        this.artModeFadeSeconds = 7;
        this.bottomGradientScrim = true;
        this.dockBackgroundColorHex = "#141824";
        this.dockOpacity = 0.55f;
        this.cornerRadiusDp = 18;
        this.borderColorHex = "#334155";
        this.glowColorHex = "#60A5FA";
        this.focusScale = 1.10f;
        this.showUnfocusedLabels = false;

        this.screensaverEnabled = true;
        this.idleTimeoutMinutes = 5;
        this.screensaverType = "custom_html";
        this.screensaverSource = "";
        this.antiBurnInCycle = true;
    }

    private void createDefaultAndSave() {
        createDefaultVisuals();
        this.hasUserCustomPins = false;
        this.dockPinnedPackages.clear();
        saveUserLayout();
        save();
    }

    public synchronized boolean save() {
        try {
            JSONObject root = buildJsonObject();
            String outStr = root.toString(2);
            writeFile(primaryConfigFile, outStr);

            if (internalConfigFile != null) {
                try {
                    writeFile(internalConfigFile, outStr);
                } catch (Exception ignored) {}
            }

            if (downloadConfigFile != null) {
                try {
                    writeFile(downloadConfigFile, outStr);
                } catch (Exception ignored) {}
            }

            saveUserLayout();

            SharedPreferences sp = appContext.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray();
            for (String p : dockPinnedPackages) {
                arr.put(p);
            }
            sp.edit()
                .putBoolean("has_user_custom_pins", hasUserCustomPins)
                .putString("dock_pinned_packages", arr.toString())
                .apply();

            Log.i(TAG, "theme.json guardado correctamente en: " + primaryConfigFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error guardando theme.json", e);
            return false;
        }
    }

    public JSONObject buildJsonObject() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("theme_name", themeName);
        root.put("has_user_custom_pins", hasUserCustomPins);

        JSONObject wp = new JSONObject();
        wp.put("source", wallpaperSource);
        wp.put("art_mode_fade_seconds", artModeFadeSeconds);
        wp.put("bottom_gradient_scrim", bottomGradientScrim);
        root.put("wallpaper", wp);

        JSONObject d = new JSONObject();
        d.put("background_color", dockBackgroundColorHex);
        d.put("opacity", dockOpacity);
        d.put("corner_radius_dp", cornerRadiusDp);
        d.put("border_color", borderColorHex);
        d.put("glow_color", glowColorHex);
        d.put("focus_scale", focusScale);
        d.put("show_unfocused_labels", showUnfocusedLabels);
        root.put("dock", d);

        JSONArray pkgs = new JSONArray();
        for (String pkg : dockPinnedPackages) {
            pkgs.put(pkg);
        }
        root.put("dock_pinned_packages", pkgs);

        JSONObject ss = new JSONObject();
        ss.put("enabled", screensaverEnabled);
        ss.put("idle_timeout_minutes", idleTimeoutMinutes);
        ss.put("type", screensaverType);
        ss.put("source", screensaverSource);
        ss.put("anti_burn_in_cycle", antiBurnInCycle);
        root.put("screensaver", ss);
        return root;
    }

    public File getThemesDir() {
        File base = appContext.getExternalFilesDir(null);
        if (base == null) base = appContext.getFilesDir();
        File dir = new File(base, "themes");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public List<String> getSavedThemeNames() {
        List<String> list = new ArrayList<>();
        File dir = getThemesDir();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory() && new File(f, "theme.json").exists()) {
                    list.add(f.getName());
                }
            }
        }
        return list;
    }

    public synchronized boolean saveCustomThemeProfile(String profileName) {
        if (profileName == null || profileName.trim().isEmpty()) {
            profileName = "Personalizado";
        }
        String cleanName = profileName.trim();
        File themeDir = new File(getThemesDir(), cleanName);
        if (!themeDir.exists()) themeDir.mkdirs();

        this.themeName = cleanName;
        File targetThemeJson = new File(themeDir, "theme.json");
        try {
            JSONObject root = buildJsonObject();
            writeFile(targetThemeJson, root.toString(2));

            File ext = appContext.getExternalFilesDir(null);
            if (ext != null) {
                File wpFile = new File(ext, "custom_wallpaper.jpg");
                if (wallpaperSource != null && !wallpaperSource.trim().isEmpty()) {
                    File srcWp = new File(wallpaperSource.trim());
                    if (srcWp.exists()) {
                        copyFile(srcWp, new File(themeDir, "custom_wallpaper.jpg"));
                    }
                } else if (wpFile.exists()) {
                    copyFile(wpFile, new File(themeDir, "custom_wallpaper.jpg"));
                }

                File ssFile = new File(ext, "custom_screensaver.html");
                if (screensaverSource != null && !screensaverSource.trim().isEmpty()) {
                    File srcSs = new File(screensaverSource.trim());
                    if (srcSs.exists()) {
                        copyFile(srcSs, new File(themeDir, "custom_screensaver.html"));
                    }
                } else if (ssFile.exists()) {
                    copyFile(ssFile, new File(themeDir, "custom_screensaver.html"));
                }
            }

            save();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error guardando perfil de tema: " + cleanName, e);
            return false;
        }
    }

    public synchronized boolean loadCustomThemeProfile(String profileName) {
        if (profileName == null || profileName.trim().isEmpty()) return false;
        File themeDir = new File(getThemesDir(), profileName.trim());
        File targetJson = new File(themeDir, "theme.json");
        if (!targetJson.exists()) return false;

        List<String> curPins = new ArrayList<>(this.dockPinnedPackages);
        boolean curHasCustom = this.hasUserCustomPins;

        try {
            String jsonStr = readFile(targetJson);
            JSONObject root = new JSONObject(jsonStr);
            parseJson(root);
            this.themeName = profileName.trim();

            // Las preferencias de apps fijadas no se alteran al cambiar de tema
            this.dockPinnedPackages.clear();
            this.dockPinnedPackages.addAll(curPins);
            this.hasUserCustomPins = curHasCustom;

            File ext = appContext.getExternalFilesDir(null);
            if (ext != null) {
                File savedWp = new File(themeDir, "custom_wallpaper.jpg");
                if (savedWp.exists()) {
                    File localWp = new File(ext, "custom_wallpaper.jpg");
                    copyFile(savedWp, localWp);
                    this.wallpaperSource = localWp.getAbsolutePath();
                }

                File savedSs = new File(themeDir, "custom_screensaver.html");
                if (savedSs.exists()) {
                    File localSs = new File(ext, "custom_screensaver.html");
                    copyFile(savedSs, localSs);
                    this.screensaverSource = localSs.getAbsolutePath();
                }
            }

            save();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error cargando perfil de tema: " + profileName, e);
            return false;
        }
    }

    public synchronized boolean deleteCustomThemeProfile(String profileName) {
        if (profileName == null || profileName.trim().isEmpty()) return false;
        File themeDir = new File(getThemesDir(), profileName.trim());
        if (themeDir.exists()) {
            deleteRecursive(themeDir);
            return true;
        }
        return false;
    }

    private static void copyFile(File src, File dst) {
        if (!src.exists()) return;
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
        } catch (Exception ignored) {}
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File child : files) deleteRecursive(child);
            }
        }
        f.delete();
    }

    private static String readFile(File file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    private static void writeFile(File file, String content) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
            writer.flush();
        }
    }

    // Métodos para colores con canal alfa calculado
    public int getParsedDockBackgroundColor() {
        int base = parseColorSafely(dockBackgroundColorHex, 0xFF141824);
        int alpha = Math.max(0, Math.min(255, Math.round(dockOpacity * 255)));
        return Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base));
    }

    public int getParsedBorderColor() {
        int base = parseColorSafely(borderColorHex, 0xFF4C566A);
        // 40% de opacidad para el borde perimetral sutil
        int alpha = Math.round(0.40f * 255);
        return Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base));
    }

    public int getParsedGlowColor() {
        return parseColorSafely(glowColorHex, 0xFF88C0D0);
    }

    public static int parseColorSafely(String hex, int fallback) {
        if (hex == null || hex.trim().isEmpty()) return fallback;
        try {
            return Color.parseColor(hex.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    // Métodos de manipulación de apps en el dock
    public synchronized boolean hasUserCustomPins() {
        return hasUserCustomPins;
    }

    public synchronized void setHasUserCustomPins(boolean val) {
        this.hasUserCustomPins = val;
    }

    public synchronized void initializeCustomPins(List<String> pins) {
        this.hasUserCustomPins = true;
        this.dockPinnedPackages.clear();
        if (pins != null) {
            for (String p : pins) {
                if (p != null && !p.trim().isEmpty() && !dockPinnedPackages.contains(p.trim())) {
                    dockPinnedPackages.add(p.trim());
                    if (dockPinnedPackages.size() >= MAX_DOCK_APPS) break;
                }
            }
        }
        save();
    }

    public synchronized void movePinnedPackage(int from, int to) {
        this.hasUserCustomPins = true;
        movePinnedPackageInMemory(from, to);
        save();
    }

    public synchronized void movePinnedPackageInMemory(int from, int to) {
        if (from >= 0 && from < dockPinnedPackages.size() && to >= 0 && to < dockPinnedPackages.size() && from != to) {
            String item = dockPinnedPackages.remove(from);
            dockPinnedPackages.add(to, item);
        }
    }

    public synchronized void setDockPinnedPackages(List<String> pkgs) {
        dockPinnedPackages.clear();
        if (pkgs != null) {
            for (String p : pkgs) {
                if (p != null && !p.trim().isEmpty() && !dockPinnedPackages.contains(p.trim())) {
                    dockPinnedPackages.add(p.trim());
                    if (dockPinnedPackages.size() >= MAX_DOCK_APPS) break;
                }
            }
        }
        this.hasUserCustomPins = true;
        save();
    }

    public synchronized boolean removePinnedPackage(String pkg) {
        this.hasUserCustomPins = true;
        boolean removed = dockPinnedPackages.remove(pkg);
        if (removed) {
            save();
        }
        return removed;
    }

    public synchronized boolean isPackagePinned(String pkg) {
        return pkg != null && dockPinnedPackages.contains(pkg.trim());
    }

    public synchronized boolean replacePinnedPackage(String oldPkg, String newPkg) {
        if (oldPkg == null || newPkg == null) return false;
        int idx = dockPinnedPackages.indexOf(oldPkg.trim());
        if (idx >= 0) {
            dockPinnedPackages.remove(idx);
            dockPinnedPackages.add(idx, newPkg.trim());
            this.hasUserCustomPins = true;
            save();
            return true;
        }
        return false;
    }

    public synchronized boolean addPinnedPackage(String pkg) {
        if (dockPinnedPackages.size() >= MAX_DOCK_APPS) {
            return false;
        }
        if (pkg != null && !pkg.trim().isEmpty() && !dockPinnedPackages.contains(pkg.trim())) {
            this.hasUserCustomPins = true;
            dockPinnedPackages.add(pkg.trim());
            save();
            return true;
        }
        return false;
    }

    public synchronized void resetToFactoryDefaults() {
        try {
            if (primaryConfigFile != null && primaryConfigFile.exists()) {
                primaryConfigFile.delete();
            }
            if (internalConfigFile != null && internalConfigFile.exists()) {
                internalConfigFile.delete();
            }
            if (downloadConfigFile != null && downloadConfigFile.exists()) {
                downloadConfigFile.delete();
            }
            if (primaryLayoutFile != null && primaryLayoutFile.exists()) {
                primaryLayoutFile.delete();
            }
            if (internalLayoutFile != null && internalLayoutFile.exists()) {
                internalLayoutFile.delete();
            }
            appContext.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE).edit().clear().apply();
            appContext.getSharedPreferences(PREFS_USER_LAYOUT, Context.MODE_PRIVATE).edit().clear().apply();
            File externalFiles = appContext.getExternalFilesDir(null);
            if (externalFiles != null) {
                new File(externalFiles, "custom_wallpaper.jpg").delete();
                new File(externalFiles, "custom_screensaver.html").delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error limpiando archivos al restaurar fábrica", e);
        }
        dockPinnedPackages.clear();
        hasUserCustomPins = false;
        createDefaultAndSave();
    }

    // Getters y Setters
    public String getThemeName() { return themeName; }
    public void setThemeName(String themeName) { this.themeName = themeName; }

    public String getWallpaperSource() { return wallpaperSource; }
    public void setWallpaperSource(String wallpaperSource) { this.wallpaperSource = wallpaperSource; }

    public int getArtModeFadeSeconds() { return artModeFadeSeconds; }
    public void setArtModeFadeSeconds(int seconds) { this.artModeFadeSeconds = seconds; }

    public boolean isBottomGradientScrim() { return bottomGradientScrim; }
    public void setBottomGradientScrim(boolean enable) { this.bottomGradientScrim = enable; }

    public String getDockBackgroundColorHex() { return dockBackgroundColorHex; }
    public void setDockBackgroundColorHex(String hex) {
        this.dockBackgroundColorHex = hex;
        updateDynamicBorderColor();
    }

    public float getDockOpacity() { return dockOpacity; }
    public void setDockOpacity(float opacity) { this.dockOpacity = opacity; }

    public int getCornerRadiusDp() { return cornerRadiusDp; }
    public void setCornerRadiusDp(int dp) { this.cornerRadiusDp = dp; }

    public String getBorderColorHex() { return borderColorHex; }
    public void setBorderColorHex(String hex) { this.borderColorHex = hex; }

    public String getGlowColorHex() { return glowColorHex; }
    public void setGlowColorHex(String hex) {
        this.glowColorHex = hex;
        updateDynamicBorderColor();
    }

    public void updateDynamicBorderColor() {
        int dock = parseColorSafely(dockBackgroundColorHex, 0xFF141824);
        int glow = parseColorSafely(glowColorHex, 0xFFA29BFE);
        int r = Math.min(255, (int) (Color.red(dock) * 0.65f + Color.red(glow) * 0.35f) + 30);
        int g = Math.min(255, (int) (Color.green(dock) * 0.65f + Color.green(glow) * 0.35f) + 30);
        int b = Math.min(255, (int) (Color.blue(dock) * 0.65f + Color.blue(glow) * 0.35f) + 30);
        this.borderColorHex = String.format("#%02X%02X%02X", r, g, b);
    }

    public float getFocusScale() { return focusScale; }
    public void setFocusScale(float scale) { this.focusScale = scale; }

    public boolean isShowUnfocusedLabels() { return showUnfocusedLabels; }
    public void setShowUnfocusedLabels(boolean show) { this.showUnfocusedLabels = show; }

    public List<String> getDockPinnedPackages() { return new ArrayList<>(dockPinnedPackages); }
    public boolean isPinnedInitialized() { return hasUserCustomPins; }
    public void setPinnedInitialized(boolean initialized) { this.hasUserCustomPins = initialized; }

    public boolean isScreensaverEnabled() { return screensaverEnabled; }
    public void setScreensaverEnabled(boolean enabled) { this.screensaverEnabled = enabled; }

    public int getIdleTimeoutMinutes() { return idleTimeoutMinutes; }
    public void setIdleTimeoutMinutes(int minutes) { this.idleTimeoutMinutes = minutes; }

    public String getScreensaverType() { return screensaverType; }
    public void setScreensaverType(String type) { this.screensaverType = type; }

    public String getScreensaverSource() { return screensaverSource; }
    public void setScreensaverSource(String source) { this.screensaverSource = source; }

    public boolean isAntiBurnInCycle() { return antiBurnInCycle; }
    public void setAntiBurnInCycle(boolean enable) { this.antiBurnInCycle = enable; }
}
