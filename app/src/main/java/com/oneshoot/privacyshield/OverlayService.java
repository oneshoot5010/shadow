package com.oneshoot.privacyshield;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

/** بيشغّل الطبقة فوق كل التطبيقات ويفضل شغال في الخلفية بإشعار دائم. */
public class OverlayService extends Service implements SensorEventListener {

    public static final String ACTION_START = "com.oneshoot.privacyshield.START";
    public static final String ACTION_STOP = "com.oneshoot.privacyshield.STOP";
    public static final String ACTION_REFRESH = "com.oneshoot.privacyshield.REFRESH";

    private static final String CHANNEL = "shield";
    private static final int NOTIF_ID = 1;
    private static final float SHAKE_G = 2.7f;      // قوة الهزّة بوحدة الجاذبية
    private static final long SHAKE_GAP_MS = 1000;

    private WindowManager wm;
    private SensorManager sm;
    private ShieldView view;
    private WindowManager.LayoutParams lp;
    private boolean sensorOn = false;
    private boolean hidden = false;
    private long lastShake = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();

        if (ACTION_STOP.equals(action)) {
            ShieldPrefs.setRunning(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }

        // لازم نعمل startForeground في أول كل مسار، عشان أندرويد ما يقتلش الخدمة
        startForegroundCompat();

        if (!ShieldPrefs.running(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            ShieldPrefs.setRunning(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (view == null) showOverlay();
        applyPrefs();
        return START_STICKY;
    }

    private void startForegroundCompat() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "درع الخصوصية", NotificationManager.IMPORTANCE_LOW));
        }
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, OverlayService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notif = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle("درع الخصوصية شغال")
                .setContentText("الطبقة فوق الشاشة. اضغط لفتح الإعدادات")
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", stop).build())
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_ID, notif);
        }
    }

    private void showOverlay() {
        view = new ShieldView(this);
        view.setListener(() -> ShieldPrefs.saveMasks(this, view.getMasks()));
        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags(),
                PixelFormat.TRANSLUCENT);
        // تغطية المنطقة فوق الكاميرا الأمامية (النوتش)
        lp.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= 30
                ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        wm.addView(view, lp);
    }

    // وضع التعتيم بس هو اللي بيسيب اللمس يعدي للتطبيق تحت، الباقيين لازم يستقبلوا اللمس
    private int flags() {
        int f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        if (ShieldPrefs.mode(this) == ShieldView.MODE_DIM) {
            f |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        return f;
    }

    private void applyPrefs() {
        if (view == null) return;
        view.setAlpha(ShieldPrefs.alpha(this));
        view.setMasks(ShieldPrefs.loadMasks(this));
        view.setTool(ShieldPrefs.tool(this));
        view.setReveal(ShieldPrefs.reveal(this) / 100f);
        view.setMode(ShieldPrefs.mode(this));
        lp.flags = flags();
        wm.updateViewLayout(view, lp);
        view.setVisibility(hidden ? View.GONE : View.VISIBLE);
        setShake(ShieldPrefs.shake(this));
    }

    private void setShake(boolean on) {
        if (on && !sensorOn) {
            Sensor s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (s != null) {
                sm.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
                sensorOn = true;
            }
        } else if (!on && sensorOn) {
            sm.unregisterListener(this);
            sensorOn = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        float x = e.values[0], y = e.values[1], z = e.values[2];
        double g = Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
        long now = System.currentTimeMillis();
        if (g > SHAKE_G && now - lastShake > SHAKE_GAP_MS) {
            lastShake = now;
            hidden = !hidden;
            if (view != null) view.setVisibility(hidden ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        setShake(false);
        if (view != null) {
            try {
                wm.removeView(view);
            } catch (Exception ignored) {
                // الـ view اتشال قبل كده
            }
            view = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
}
