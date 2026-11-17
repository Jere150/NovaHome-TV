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
 * Selector de color HSV nativo y ultra-ligero dibujado en Canvas.
 * Controlable 100% con cruceta D-Pad / control remoto de TV.
 * Eje X controla el Tono (Hue 0..360), Eje Y controla la Luminosidad (Value 0.0..1.0).
 * Soporta paso fino de 0.02f y alcance completo hasta las 4 esquinas.
 */
public final class HsvColorPickerView extends View {

    public interface OnColorChangeListener {
        void onColorChanged(int colorHex);
    }

    private float normX = 0.55f; // 0.0f .. 1.0f (Hue / 360f)
    private float normY = 0.10f; // 0.0f .. 1.0f (1.0f - Value)
    private float sat = 1.0f;   // 0.0f .. 1.0f

    private final Paint spectrumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cursorBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final Path clipPath = new Path();

    private OnColorChangeListener listener;
    private View backTargetView;

    public HsvColorPickerView(Context context) {
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

    public void setInitialColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        this.normX = Math.max(0.0f, Math.min(1.0f, hsv[0] / 360f));
        this.sat = Math.max(0.0f, Math.min(1.0f, hsv[1]));
        this.normY = Math.max(0.0f, Math.min(1.0f, 1.0f - hsv[2]));
        invalidate();
    }

    public void setSaturation(float s) {
        this.sat = Math.max(0.0f, Math.min(1.0f, s));
        invalidate();
        if (listener != null) {
            listener.onColorChanged(getCurrentColor());
        }
    }

    public float getHue() {
        return normX * 360f;
    }

    public float getValue() {
        return Math.max(0.02f, 1.0f - normY);
    }

    public float getSaturation() {
        return sat;
    }

    public void setOnColorChangeListener(OnColorChangeListener l) {
        this.listener = l;
    }

    public int getCurrentColor() {
        float h = getHue();
        float v = getValue();
        return Color.HSVToColor(new float[]{h, sat, v});
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float density = getResources().getDisplayMetrics().density;
        float pad = 2 * density;
        bounds.set(pad, pad, w - pad, h - pad);

        clipPath.reset();
        float radius = 8 * density;
        clipPath.addRoundRect(bounds, radius, radius, Path.Direction.CW);

        // Gradiente horizontal de espectro de tonos (Hue)
        int[] hues = new int[]{
                Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
        };
        LinearGradient hueGrad = new LinearGradient(
                bounds.left, bounds.top, bounds.right, bounds.top,
                hues, null, Shader.TileMode.CLAMP);
        spectrumPaint.setShader(hueGrad);

        // Gradiente vertical de luminosidad (Blanco/Transparente arriba a Negro abajo)
        LinearGradient valGrad = new LinearGradient(
                bounds.left, bounds.top, bounds.left, bounds.bottom,
                new int[]{Color.TRANSPARENT, Color.BLACK},
                null, Shader.TileMode.CLAMP);
        valGradientPaint.setShader(valGrad);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;

        canvas.save();
        // Recortar al contorno redondeado para que el cursor alcance bordes y esquinas reales
        canvas.clipPath(clipPath);

        // 1. Dibujar espectro de color base
        canvas.drawRect(bounds, spectrumPaint);
        // 2. Dibujar sombra vertical de luminosidad
        canvas.drawRect(bounds, valGradientPaint);

        // 3. Calcular posición del cursor indicador desde coordenadas normalizadas
        float cursorX = bounds.left + normX * bounds.width();
        float cursorY = bounds.top + normY * bounds.height();

        int currCol = getCurrentColor();
        cursorPaint.setColor(currCol);
        float cursorR = (hasFocus() ? 9 : 7) * density;

        // Halo negro exterior para contraste
        cursorBorderPaint.setColor(Color.BLACK);
        cursorBorderPaint.setStrokeWidth(3f * density);
        canvas.drawCircle(cursorX, cursorY, cursorR + 1.5f * density, cursorBorderPaint);

        // Anillo blanco interior
        cursorBorderPaint.setColor(Color.WHITE);
        cursorBorderPaint.setStrokeWidth(2f * density);
        canvas.drawCircle(cursorX, cursorY, cursorR, cursorBorderPaint);

        // Centro con el color actual
        canvas.drawCircle(cursorX, cursorY, cursorR - 2f * density, cursorPaint);

        canvas.restore();

        // 4. Dibujar borde si tiene foco
        if (hasFocus()) {
            focusBorderPaint.setColor(Color.WHITE);
            focusBorderPaint.setStrokeWidth(2.5f * density);
            canvas.drawRoundRect(bounds, 8 * density, 8 * density, focusBorderPaint);
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
            normX = Math.max(0.0f, normX - step);
            handled = true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            normX = Math.min(1.0f, normX + step);
            handled = true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            normY = Math.max(0.0f, normY - step);
            handled = true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (normY < 0.98f) {
                normY = Math.min(1.0f, normY + step);
                handled = true;
            } else {
                // Al llegar al borde inferior, ceder el foco hacia la barra de saturación / botones
                handled = false;
            }
        }

        if (handled) {
            invalidate();
            if (listener != null) {
                listener.onColorChanged(getCurrentColor());
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
