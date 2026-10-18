package com.oneshoot.privacyshield;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** عنوان Shadow بنقط على نص دايرة، واللوجو في نص الدايرة. */
public class HeaderView extends View {

    private static final int RED = 0xFFE53935;
    private static final String WORD = "SHADOW";
    private static final String[][] FONT = {
            {".####", "#....", "#....", ".###.", "....#", "....#", "####."},   // S
            {"#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"},   // H
            {".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"},   // A
            {"####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."},   // D
            {".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."},   // O
            {"#...#", "#...#", "#...#", "#.#.#", "#.#.#", "##.##", "#...#"},   // W
    };

    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint faint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap logo;
    private final float d;

    public HeaderView(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        dot.setColor(RED);
        glow.setColor(RED);
        glow.setAlpha(90);
        glow.setMaskFilter(new android.graphics.BlurMaskFilter(5 * d, android.graphics.BlurMaskFilter.Blur.NORMAL));
        faint.setColor(RED);
        faint.setAlpha(70);
        logo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), (int) (205 * d));
    }

    @Override
    protected void onDraw(Canvas c) {
        float cx = getWidth() / 2f;
        float r = 78 * d;                 // نصف قطر الدايرة (عند خط أساس الحروف)
        float cy = getHeight() - 52 * d;  // مركز الدايرة = مركز اللوجو
        float cell = 4.2f * d;
        float dotR = 1.7f * d;

        // نقط خفيفة على نص الدايرة الخارجي
        for (int i = 0; i <= 60; i++) {
            double th = Math.PI + Math.PI * i / 60.0;
            float x = cx + (float) Math.cos(th) * (r + 52 * d);
            float y = cy + (float) Math.sin(th) * (r + 52 * d);
            c.drawCircle(x, y, 0.9f * d, faint);
        }

        // الحروف
        double start = 1.5 * Math.PI - 0.92;
        double step = 0.368;
        for (int i = 0; i < WORD.length(); i++) {
            double th = start + i * step;
            float px = cx + (float) Math.cos(th) * r;
            float py = cy + (float) Math.sin(th) * r;
            c.save();
            c.translate(px, py);
            c.rotate((float) Math.toDegrees(th - 1.5 * Math.PI));
            String[] g = FONT[i];
            for (int row = 0; row < 7; row++) {
                for (int col = 0; col < 5; col++) {
                    if (g[row].charAt(col) != '#') continue;
                    float x = (col - 2) * cell;
                    float y = -(7 - row - 0.5f) * cell;
                    c.drawCircle(x, y, dotR * 1.6f, glow);
                    c.drawCircle(x, y, dotR, dot);
                }
            }
            c.restore();
        }

        // اللوجو في النص
        float s = 64 * d;
        c.drawBitmap(logo, null, new RectF(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2), null);
    }
}
