package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Draws the tvOS style lighter band behind the first (dock) row and adds a gap below it. */
public class DockDecoration extends RecyclerView.ItemDecoration {

    private int dockSize;
    private final float density;
    private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    /** Extra band below the tiles for their names (always_show_names), in px. */
    private final float namesRoom;
    /** Light glass like the top bar's pills (config.xml dock_glass): a white tint that fades downwards. */
    private final boolean lightGlass;
    /** Padding all round the tiles equal to the gap between them (config.xml dock_even_padding). */
    private final boolean evenPadding;
    /** The band's top and bottom the glass gradient was made for; it is remade when they move. */
    private float shaderTop = Float.NaN, shaderBottom = Float.NaN;
    /** Draw the band and its edge (config.xml dock_band); false leaves the tiles floating on the wallpaper. */
    private final boolean bandVisible;
    /** single_row: the tiles after the dock share its row, so they take the same offsets to stay level with it. */
    private final boolean singleRow;
    /** dock_band_all: in a single row, the band runs behind every app instead of the dock apps only. */
    private final boolean bandAll;
    private boolean expanded = false;
    private int totalRows = 1;

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public void setDockSize(int dockSize) {
        this.dockSize = dockSize;
    }

    /**
     * @param translucent the cinematic shelf's band: the artwork and the glow show through it.
     * @param names       the dock tiles show their names, so the band reaches down over them.
     * @param opacity     the plain band's opacity in percent (dock_opacity), so a wallpaper can show through it.
     * @param lightGlass  a light frosted band like the pills, for a sky or wallpaper behind it.
     * @param evenPadding the band's padding is the gap between the tiles on all four sides (dock_even_padding).
     * @param bandVisible false skips the band and its edge, so the tiles float on the wallpaper (dock_band).
     * @param singleRow   every app sits in the dock's row (single_row); the band covers the dock apps only, unless bandAll.
     * @param bandAll     with singleRow, the band runs behind every app in the row (dock_band_all).
     */
    DockDecoration(int dockSize, float density, boolean translucent, boolean names, int opacity, boolean lightGlass,
                   boolean evenPadding, boolean bandVisible, boolean singleRow, boolean bandAll) {
        this.dockSize = dockSize;
        this.singleRow = singleRow;
        this.bandAll = singleRow && bandAll;
        this.density = density;
        this.namesRoom = names ? 22 * density : 0;
        this.lightGlass = lightGlass;
        this.evenPadding = evenPadding;
        this.bandVisible = bandVisible;
        band.setColor(translucent ? 0x8C121014 : (Math.round(opacity * 2.55f) << 24) | 0x141416);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(density);
        edge.setColor(lightGlass ? 0x38FFFFFF : translucent ? 0x26FFFFFF : 0x1FFFFFFF);
    }

    /** Concentric corners: the band's radius is the tiles' radius plus the band's padding around them. */
    static float radiusPx(float tileRadiusPx, float paddingPx) {
        return tileRadiusPx + paddingPx;
    }

    public static float getFixedCapsuleRightMargin(int parentW, float density) {
        int innerW = parentW - (int) (20 * density);
        int cellW = innerW / 6;
        int tileW = AppAdapter.tileWidthPx(innerW, 6, density);
        float span6Width = 5 * cellW + tileW;
        float sidePad = 16 * density;
        float fixedCapsuleWidth = span6Width + 2 * sidePad;
        return Math.max(0f, (parentW - fixedCapsuleWidth) / 2f);
    }

    public static float getFixedCapsuleScreenRightMargin(int screenW, float density) {
        float sideMarginPx = 32f * density; // Padding horizontal de la actividad principal (activity_main.xml)
        int gridW = (int) (screenW - 2 * sideMarginPx);
        int innerW = gridW - (int) (20 * density);
        int cellW = innerW / 6;
        int tileW = AppAdapter.tileWidthPx(innerW, 6, density);
        float span6Width = 5 * cellW + tileW;
        float sidePad = 16 * density;
        float fixedCapsuleWidth = span6Width + 2 * sidePad;
        float fixedLeft = (gridW - fixedCapsuleWidth) / 2f;
        float fixedRight = fixedLeft + fixedCapsuleWidth;
        float fixedRightScreen = sideMarginPx + fixedRight;
        return Math.max(0f, screenW - fixedRightScreen);
    }

    /**
     * The band's padding on every side for dock_even_padding: the gap between two dock tiles (a cell minus its tile, as
     * AppAdapter.tileWidthPx lays them out), capped by the room the grid leaves at its edges.
     */
    static float evenPaddingPx(int innerWidthPx, int columns, int tileWidthPx, float roomPx) {
        float gap = columns > 0 ? innerWidthPx / (float) columns - tileWidthPx : 0f;
        return Math.max(0f, Math.min(gap, roomPx));
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos >= 0 && (singleRow || pos < dockSize)) {
            outRect.top = 0;
            outRect.bottom = 0;
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (!bandVisible) return;

        ThemeManager tm = ThemeManager.get(parent.getContext());
        band.setColor(tm.getParsedDockBackgroundColor());
        edge.setColor(tm.getParsedBorderColor());
        float radius = tm.getCornerRadiusDp() * density;

        if (expanded && totalRows > 3) {
            // Marco fijo para cajón de 3 filas con scroll vertical interno
            float left = 10 * density;
            float right = parent.getWidth() - 10 * density;
            float top = parent.getPaddingTop() - 10 * density;
            float bottom = parent.getHeight() - 12 * density;
            rect.set(left, top, right, bottom);
            c.drawRoundRect(rect, radius, radius, band);
            c.drawRoundRect(rect, radius, radius, edge);
            return;
        }

        boolean any = false;
        int cardWidth = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) continue;
            int pos = parent.getChildAdapterPosition(child);
            if (pos < 0 || (!expanded && !bandAll && pos >= dockSize)) continue;
            RecyclerView.ViewHolder vh = parent.getChildViewHolder(child);
            View card = vh instanceof AppAdapter.Holder ? ((AppAdapter.Holder) vh).card : child.findViewById(R.id.card);
            if (card == null || card.getVisibility() != View.VISIBLE) continue;
            float l = child.getLeft() + card.getLeft();
            float t = child.getTop() + card.getTop() + child.getTranslationY();
            float r = l + card.getWidth();
            float b = t + card.getHeight();
            cardWidth = card.getWidth();
            if (!any) {
                rect.set(l, t, r, b);
                any = true;
            } else {
                rect.union(l, t, r, b);
            }
        }
        if (!any) return;

        // Cápsula de dock: 10dp por encima de los banners y 30dp por debajo para encerrar el nombre con aire limpio
        float top = rect.top - 10 * density;
        float bottom = parent.getHeight() - 12 * density;

        if (!expanded) {
            // Ancho constante referenciado a 6 tarjetas fijas centradas simétricamente
            int parentW = parent.getWidth();
            int innerW = parentW - (int) (20 * density);
            int cellW = innerW / 6;
            int tileW = cardWidth > 0 ? cardWidth : AppAdapter.tileWidthPx(innerW, 6, density);
            float span6Width = 5 * cellW + tileW;
            float sidePad = 16 * density;
            float fixedCapsuleWidth = span6Width + 2 * sidePad;
            float fixedLeft = (parentW - fixedCapsuleWidth) / 2f;
            float fixedRight = fixedLeft + fixedCapsuleWidth;
            rect.set(fixedLeft, top, fixedRight, bottom);
        } else {
            float sidePad = 16 * density;
            float side = Math.max(0f, Math.min(sidePad, Math.min(rect.left, parent.getWidth() - rect.right) - density));
            rect.set(rect.left - side, top, rect.right + side, bottom);
        }

        if (singleRow) {
            rect.left = Math.max(rect.left, density);
            rect.right = Math.min(rect.right, parent.getWidth() - density);
        }
        c.drawRoundRect(rect, radius, radius, band);
        c.drawRoundRect(rect, radius, radius, edge);
    }
}
