package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.KeyEvent;
import android.view.View;

/**
 * Barra horizontal interactiva para controlar la Saturación (0%..100%).
 * Controlable 100% con cruceta D-Pad (DPAD_LEFT / DPAD_RIGHT a pasos finos de 0.02f).
 */
public final class HsvSaturationBarView extends View {

    public interface OnSaturationChangeListener {
        void onSaturationChanged(float saturation);
    }

    private float sat = 1.0f; // 0.0f .. 1.0f
    private float hue = 0f;
    private float val = 1.0f;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cursorBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final Path clipPath = new Path();

    private OnSaturationChangeListener listener;
    private View backTargetView;

    public HsvSaturationBarView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);

        cursorPaint.setStyle(Paint.Style.FILL);
        cursorBorderPaint.setStyle(Paint.Style.STROKE);
        focusBorderPaint.setStyle(Paint.Style.STROKE);
    }

    public void setBackTargetView(View v) {
        this.backTargetView = v;
    }

    public void setOnSaturationChangeListener(OnSaturationChangeListener l) {
        this.listener = l;
    }

    public void setSaturation(float s) {
        this.sat = Math.max(0f, Math.min(1f, s));
        updateShader();
        invalidate();
    }

    public float getSaturation() {
        return sat;
    }

    public void setHueVal(float h, float v) {
        this.hue = h;
        this.val = v;
        updateShader();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float density = getResources().getDisplayMetrics().density;
        float pad = 2 * density;
        bounds.set(pad, pad, w - pad, h - pad);

        clipPath.reset();
        float radius = 6 * density;
        clipPath.addRoundRect(bounds, radius, radius, Path.Direction.CW);

        updateShader();
    }

    private void updateShader() {
        if (bounds.width() <= 0) return;
        int colorStart = Color.HSVToColor(new float[]{hue, 0f, val});
        int colorEnd = Color.HSVToColor(new float[]{hue, 1f, val});
        LinearGradient grad = new LinearGradient(
                bounds.left, bounds.top, bounds.right, bounds.top,
                colorStart, colorEnd, Shader.TileMode.CLAMP);
        barPaint.setShader(grad);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;

        canvas.save();
        canvas.clipPath(clipPath);

        // 1. Barra de gradiente de saturación
        canvas.drawRect(bounds, barPaint);

        // 2. Cursor indicador
        float cursorX = bounds.left + sat * bounds.width();
        float cursorY = bounds.centerY();
        float cursorR = (hasFocus() ? 8 : 6) * density;

        int currentColor = Color.HSVToColor(new float[]{hue, sat, val});
        cursorPaint.setColor(currentColor);

        // Halo negro exterior
        cursorBorderPaint.setColor(Color.BLACK);
        cursorBorderPaint.setStrokeWidth(2.5f * density);
        canvas.drawCircle(cursorX, cursorY, cursorR + 1.5f * density, cursorBorderPaint);

        // Anillo blanco
        cursorBorderPaint.setColor(Color.WHITE);
        cursorBorderPaint.setStrokeWidth(1.8f * density);
        canvas.drawCircle(cursorX, cursorY, cursorR, cursorBorderPaint);

        // Centro con color
        canvas.drawCircle(cursorX, cursorY, cursorR - 1.5f * density, cursorPaint);

        canvas.restore();

        // 3. Borde de foco exterior
        if (hasFocus()) {
            focusBorderPaint.setColor(Color.WHITE);
            focusBorderPaint.setStrokeWidth(2f * density);
            canvas.drawRoundRect(bounds, 6 * density, 6 * density, focusBorderPaint);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (backTargetView != null) {
                backTargetView.requestFocus();
                return true; // No cierra la ventana, cede el foco al botón Aceptar
            }
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            View next = focusSearch(View.FOCUS_DOWN);
            if (next != null) {
                next.requestFocus();
                return true;
            }
        }

        float step = (event.getRepeatCount() > 2) ? 0.035f : 0.02f;
        boolean handled = false;

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            sat = Math.max(0.0f, sat - step);
            handled = true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            sat = Math.min(1.0f, sat + step);
            handled = true;
        }

        if (handled) {
            updateShader();
            invalidate();
            if (listener != null) {
                listener.onSaturationChanged(sat);
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
