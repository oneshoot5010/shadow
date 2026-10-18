package com.oneshoot.privacyshield;

import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

/** منطقة مغطاة: مستطيل أو دايرة أو خط فرشاة. كل الإحداثيات نسبية من 0 لـ 1. */
public final class ShieldMask {

    public static final int RECT = 0;
    public static final int ELLIPSE = 1;
    public static final int BRUSH = 2;

    public final int type;
    public final RectF bounds;      // للفرشاة: مساحة النقط + عرض الفرشاة
    public final List<float[]> pts; // نقط الفرشاة فقط
    public final float brush;       // عرض الفرشاة نسبة لأقصر ضلع (للفرشاة فقط)

    private ShieldMask(int type, RectF bounds, List<float[]> pts, float brush) {
        this.type = type;
        this.bounds = bounds;
        this.pts = pts;
        this.brush = brush;
    }

    public static ShieldMask shape(int type, RectF bounds) {
        return new ShieldMask(type, new RectF(bounds), new ArrayList<float[]>(), 0f);
    }

    public static ShieldMask stroke(List<float[]> points, float brush) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (float[] p : points) {
            minX = Math.min(minX, p[0]);
            minY = Math.min(minY, p[1]);
            maxX = Math.max(maxX, p[0]);
            maxY = Math.max(maxY, p[1]);
        }
        RectF b = new RectF(minX - brush, minY - brush, maxX + brush, maxY + brush);
        return new ShieldMask(BRUSH, b, new ArrayList<>(points), brush);
    }

    public void shift(float dx, float dy) {
        bounds.offset(dx, dy);
        for (float[] p : pts) {
            p[0] += dx;
            p[1] += dy;
        }
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(',')
                .append(bounds.left).append(',').append(bounds.top).append(',')
                .append(bounds.right).append(',').append(bounds.bottom).append(',')
                .append(brush).append(',').append(pts.size());
        for (float[] p : pts) sb.append(',').append(p[0]).append(',').append(p[1]);
        return sb.toString();
    }

    /** بيرجع null لو السطر بايظ. */
    public static ShieldMask parse(String s) {
        String[] v = s.split(",");
        if (v.length < 7) return null;
        int type = Integer.parseInt(v[0]);
        RectF b = new RectF(f(v[1]), f(v[2]), f(v[3]), f(v[4]));
        float brush = f(v[5]);
        int n = Integer.parseInt(v[6]);
        if (n < 0 || v.length < 7 + n * 2) return null;
        List<float[]> pts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            pts.add(new float[]{f(v[7 + i * 2]), f(v[8 + i * 2])});
        }
        return new ShieldMask(type, b, pts, brush);
    }

    private static float f(String s) {
        return Float.parseFloat(s);
    }
}
