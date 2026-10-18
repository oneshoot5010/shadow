package com.oneshoot.privacyshield;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * الطبقة نفسها. ثلاث أوضاع:
 * - تعتيم: الشاشة كلها مضلّمة واللمس بيعدي للتطبيق تحتها.
 * - كشف بالإصبع: الشاشة مغطاة، والدايرة بتظهر فوق الصباع مش تحته.
 * - رسم مناطق: مستطيل/دايرة/فرشاة. اضغط على منطقة عشان تحددها، اسحبها عشان تحركها،
 *   واسحب الزاوية السفلية عشان تكبرها، واضغط مطوّل عشان تمسحها.
 */
public class ShieldView extends View {

    public static final int MODE_DIM = 0;
    public static final int MODE_PEEK = 1;
    public static final int MODE_EDIT = 2;

    public static final int TOOL_RECT = 0;
    public static final int TOOL_ELLIPSE = 1;
    public static final int TOOL_BRUSH = 2;

    public interface Listener {
        void onMasksChanged();
    }

    private static final int RED = 0xFFE53935;
    private static final float MIN_PX = 24f;          // أقل حجم للمنطقة
    private static final float HANDLE_PX = 48f;      // مساحة لمس مقبض التكبير
    private static final float BRUSH_FRAC = 0.09f;   // عرض الفرشاة من أقصر ضلع

    private static final int DRAG_NONE = 0;
    private static final int DRAG_NEW = 1;
    private static final int DRAG_MOVE = 2;
    private static final int DRAG_RESIZE = 3;
    private static final int DRAG_BRUSH = 4;

    private int mode = MODE_DIM;
    private int tool = TOOL_RECT;
    private int alpha = 160;
    private float revealFrac = 0.22f;
    private final List<ShieldMask> masks = new ArrayList<>();
    private int selected = -1;
    private Listener listener;

    private int dragState = DRAG_NONE;
    private float lastX, lastY;
    private RectF newRect;
    private final List<float[]> brushPts = new ArrayList<>();
    private float touchX = -1, touchY = -1;

    private final Paint coverPaint = new Paint();
    private final Paint maskPaint = new Paint();
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint previewPaint = new Paint();
    private final Paint selPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint();
    private final Paint brushPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tmp = new RectF();
    private final GestureDetector gesture;

    public ShieldView(Context c) {
        super(c);
        float density = c.getResources().getDisplayMetrics().density;
        coverPaint.setStyle(Paint.Style.FILL);
        maskPaint.setColor(Color.BLACK);
        maskPaint.setStyle(Paint.Style.FILL);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        clearPaint.setStyle(Paint.Style.FILL);
        ringPaint.setColor(RED);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(3 * density);
        previewPaint.setColor(Color.argb(110, 229, 57, 53));
        previewPaint.setStyle(Paint.Style.FILL);
        selPaint.setColor(RED);
        selPaint.setStyle(Paint.Style.STROKE);
        selPaint.setStrokeWidth(3 * density);
        handlePaint.setColor(RED);
        handlePaint.setStyle(Paint.Style.FILL);
        brushPaint.setColor(Color.BLACK);
        brushPaint.setStyle(Paint.Style.STROKE);
        brushPaint.setStrokeCap(Paint.Cap.ROUND);
        brushPaint.setStrokeJoin(Paint.Join.ROUND);

        gesture = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                if (mode != MODE_EDIT) return;
                int i = hit(e.getX(), e.getY());
                if (i < 0) return;
                masks.remove(i);
                selected = -1;
                dragState = DRAG_NONE;
                notifyMasks();
                invalidate();
            }
        });
    }

    public void setMode(int m) {
        mode = m;
        touchX = -1;
        touchY = -1;
        selected = -1;
        dragState = DRAG_NONE;
        newRect = null;
        brushPts.clear();
        invalidate();
    }

    public void setTool(int t) {
        tool = t;
        invalidate();
    }

    public void setReveal(float frac) {
        revealFrac = Math.max(0.10f, Math.min(0.40f, frac));
        invalidate();
    }

    public void setAlpha(int a) {
        alpha = Math.max(0, Math.min(255, a));
        invalidate();
    }

    public void setMasks(List<ShieldMask> list) {
        masks.clear();
        masks.addAll(list);
        selected = -1;
        invalidate();
    }

    public List<ShieldMask> getMasks() {
        return new ArrayList<>(masks);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void clearMasks() {
        masks.clear();
        selected = -1;
        notifyMasks();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        float minSide = Math.min(w, h);
        int sc = c.saveLayer(0, 0, w, h, null);

        coverPaint.setColor(Color.argb(mode == MODE_PEEK ? 255 : alpha, 0, 0, 0));
        c.drawRect(0, 0, w, h, coverPaint);

        if (mode == MODE_PEEK && touchX >= 0) {
            float r = revealFrac * minSide;
            float cx = touchX;
            float cy = Math.max(r, touchY - r * 1.5f);   // الدايرة فوق الصباع
            c.drawCircle(cx, cy, r, clearPaint);
            c.drawCircle(cx, cy, r, ringPaint);
        }

        for (ShieldMask m : masks) {
            if (m.type == ShieldMask.BRUSH) {
                brushPaint.setStrokeWidth(m.brush * minSide);
                drawBrush(c, m.pts, w, h, brushPaint);
            } else {
                tmp.set(m.bounds.left * w, m.bounds.top * h, m.bounds.right * w, m.bounds.bottom * h);
                if (m.type == ShieldMask.ELLIPSE) c.drawOval(tmp, maskPaint);
                else c.drawRect(tmp, maskPaint);
            }
        }

        if (mode == MODE_EDIT) {
            if (dragState == DRAG_BRUSH && brushPts.size() > 1) {
                previewPaint.setStyle(Paint.Style.STROKE);
                previewPaint.setStrokeWidth(BRUSH_FRAC * minSide);
                previewPaint.setStrokeCap(Paint.Cap.ROUND);
                previewPaint.setStrokeJoin(Paint.Join.ROUND);
                Path path = new Path();
                path.moveTo(brushPts.get(0)[0], brushPts.get(0)[1]);
                for (int i = 1; i < brushPts.size(); i++) {
                    path.lineTo(brushPts.get(i)[0], brushPts.get(i)[1]);
                }
                c.drawPath(path, previewPaint);
                previewPaint.setStyle(Paint.Style.FILL);
            }
            if (dragState == DRAG_NEW && newRect != null) {
                RectF pr = new RectF(
                        Math.min(newRect.left, newRect.right), Math.min(newRect.top, newRect.bottom),
                        Math.max(newRect.left, newRect.right), Math.max(newRect.top, newRect.bottom));
                if (tool == TOOL_ELLIPSE) c.drawOval(pr, previewPaint);
                else c.drawRect(pr, previewPaint);
            }
            if (selected >= 0 && selected < masks.size()) {
                ShieldMask m = masks.get(selected);
                RectF b = toPx(m.bounds, w, h);
                if (m.type == ShieldMask.ELLIPSE) c.drawOval(b, selPaint);
                else c.drawRect(b, selPaint);
                if (m.type != ShieldMask.BRUSH) {
                    c.drawRect(b.right - 14, b.bottom - 14, b.right + 14, b.bottom + 14, handlePaint);
                }
            }
        }

        c.restoreToCount(sc);
    }

    private void drawBrush(Canvas c, List<float[]> pts, int w, int h, Paint paint) {
        if (pts.size() < 2) return;
        Path path = new Path();
        path.moveTo(pts.get(0)[0] * w, pts.get(0)[1] * h);
        for (int i = 1; i < pts.size(); i++) {
            path.lineTo(pts.get(i)[0] * w, pts.get(i)[1] * h);
        }
        c.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();

        if (mode == MODE_PEEK) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    touchX = x;
                    touchY = y;
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    touchX = -1;
                    touchY = -1;
                    invalidate();
                    break;
                default:
                    break;
            }
            return true;
        }

        if (mode != MODE_EDIT) return false;

        gesture.onTouchEvent(e);
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            lastX = x;
            lastY = y;
            int hit = hit(x, y);
            if (hit >= 0) {
                selected = hit;
                ShieldMask m = masks.get(hit);
                RectF b = toPx(m.bounds, getWidth(), getHeight());
                boolean corner = m.type != ShieldMask.BRUSH
                        && Math.abs(x - b.right) < HANDLE_PX && Math.abs(y - b.bottom) < HANDLE_PX;
                dragState = corner ? DRAG_RESIZE : DRAG_MOVE;
            } else {
                selected = -1;
                if (tool == TOOL_BRUSH) {
                    dragState = DRAG_BRUSH;
                    brushPts.clear();
                    brushPts.add(new float[]{x, y});
                } else {
                    dragState = DRAG_NEW;
                    newRect = new RectF(x, y, x, y);
                }
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dx = x - lastX;
            float dy = y - lastY;
            lastX = x;
            lastY = y;
            if (dragState == DRAG_NEW && newRect != null) {
                newRect.right = x;
                newRect.bottom = y;
            } else if (dragState == DRAG_BRUSH) {
                brushPts.add(new float[]{x, y});
            } else if (dragState == DRAG_MOVE && selected >= 0) {
                masks.get(selected).shift(dx / getWidth(), dy / getHeight());
            } else if (dragState == DRAG_RESIZE && selected >= 0) {
                resize(masks.get(selected), x, y);
            }
        } else if (action == MotionEvent.ACTION_UP) {
            finishDrag();
        } else if (action == MotionEvent.ACTION_CANCEL) {
            dragState = DRAG_NONE;
            newRect = null;
            brushPts.clear();
        }
        invalidate();
        return true;
    }

    private void finishDrag() {
        int w = getWidth();
        int h = getHeight();
        if (dragState == DRAG_NEW && newRect != null) {
            if (Math.abs(newRect.width()) >= MIN_PX && Math.abs(newRect.height()) >= MIN_PX) {
                RectF n = new RectF(
                        Math.min(newRect.left, newRect.right) / w,
                        Math.min(newRect.top, newRect.bottom) / h,
                        Math.max(newRect.left, newRect.right) / w,
                        Math.max(newRect.top, newRect.bottom) / h);
                int type = tool == TOOL_ELLIPSE ? ShieldMask.ELLIPSE : ShieldMask.RECT;
                masks.add(ShieldMask.shape(type, n));
                notifyMasks();
            }
        } else if (dragState == DRAG_BRUSH && brushPts.size() > 1 && w > 0 && h > 0) {
            List<float[]> norm = new ArrayList<>();
            for (float[] p : brushPts) norm.add(new float[]{p[0] / w, p[1] / h});
            masks.add(ShieldMask.stroke(norm, BRUSH_FRAC));
            notifyMasks();
        } else if (dragState == DRAG_MOVE || dragState == DRAG_RESIZE) {
            notifyMasks();
        }
        dragState = DRAG_NONE;
        newRect = null;
        brushPts.clear();
    }

    private void resize(ShieldMask m, float x, float y) {
        int w = getWidth();
        int h = getHeight();
        RectF b = toPx(m.bounds, w, h);
        b.right = Math.max(x, b.left + MIN_PX);
        b.bottom = Math.max(y, b.top + MIN_PX);
        m.bounds.set(b.left / w, b.top / h, b.right / w, b.bottom / h);
    }

    // أعلى منطقة (آخر واحدة اترسمت) اللي المس وقع فيها، -1 لو مفيش
    private int hit(float x, float y) {
        int w = getWidth();
        int h = getHeight();
        for (int i = masks.size() - 1; i >= 0; i--) {
            ShieldMask m = masks.get(i);
            RectF b = toPx(m.bounds, w, h);
            if (m.type == ShieldMask.ELLIPSE) {
                float rx = b.width() / 2f;
                float ry = b.height() / 2f;
                if (rx <= 0 || ry <= 0) continue;
                float dx = (x - b.centerX()) / rx;
                float dy = (y - b.centerY()) / ry;
                if (dx * dx + dy * dy <= 1f) return i;
            } else if (b.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private static RectF toPx(RectF n, int w, int h) {
        return new RectF(n.left * w, n.top * h, n.right * w, n.bottom * h);
    }

    private void notifyMasks() {
        if (listener != null) listener.onMasksChanged();
    }
}
