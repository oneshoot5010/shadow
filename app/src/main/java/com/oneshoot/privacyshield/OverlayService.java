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
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
    private long lastMove = System.currentTimeMillis();
    private boolean idleDim = false;
    private boolean autoHide = false;
    private String curPkg = "";
    private long lastEventQuery = System.currentTimeMillis() - 60000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean ticking = false;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            onTick();
            if (ticking) handler.postDelayed(this, 1000);
        }
    };

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
                .setSmallIcon(R.drawable.ic_notif)
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
        idleDim = false;
        lastMove = System.currentTimeMillis();
        int idle = ShieldPrefs.geti(this, "idle", 0);
        setShake(ShieldPrefs.shake(this) || idle > 0);
        boolean needTick = idle > 0 || ShieldPrefs.geti(this, "autoapps", 0) == 1;
        ticking = needTick;
        handler.removeCallbacks(tick);
        if (needTick) handler.post(tick); else autoHide = false;
        updateVisibility();
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
        if (Math.abs(g - 1.0) > 0.12) {
            lastMove = now;
            if (idleDim && view != null) {
                idleDim = false;
                view.setAlpha(ShieldPrefs.alpha(this));
            }
        }
        if (ShieldPrefs.shake(this) && g > SHAKE_G && now - lastShake > SHAKE_GAP_MS) {
            lastShake = now;
            hidden = !hidden;
            updateVisibility();
            vibrateOnShake();
        }
    }

    private void updateVisibility() {
        if (view != null) view.setVisibility((hidden || autoHide) ? View.GONE : View.VISIBLE);
    }

    private void vibrateOnShake() {
        Haptics.vibrate(this, ShieldPrefs.geti(this, "vibrate", 0));
    }

    private void onTick() {
        if (view == null) return;
        long now = System.currentTimeMillis();
        int idle = ShieldPrefs.geti(this, "idle", 0);
        if (idle > 0 && !idleDim && now - lastMove > idle * 1000L) {
            idleDim = true;
            view.setAlpha(Math.max(ShieldPrefs.alpha(this), 235));
        }
        if (ShieldPrefs.geti(this, "autoapps", 0) == 1 && hasUsageAccess()) {
            try {
                UsageStatsManager us = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
                UsageEvents ev = us.queryEvents(lastEventQuery, now);
                UsageEvents.Event e = new UsageEvents.Event();
                while (ev.hasNextEvent()) {
                    ev.getNextEvent(e);
                    if (e.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                        curPkg = e.getPackageName();
                    }
                }
                lastEventQuery = now;
                Set<String> sel = new HashSet<>(Arrays.asList(
                        ShieldPrefs.gets(this, "apps", "").split(",")));
                boolean hide = !sel.contains(curPkg);
                if (hide != autoHide) {
                    autoHide = hide;
                    updateVisibility();
                }
            } catch (Exception e) {
                ErrorLog.log(this, "قراءة التطبيق المفتوح", e);
            }
        } else if (autoHide) {
            autoHide = false;
            updateVisibility();
        }
    }

    private boolean hasUsageAccess() {
        AppOpsManager ops = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        ticking = false;
        handler.removeCallbacks(tick);
        setShake(false);
        if (view != null) {
            try {
                wm.removeView(view);
            } catch (Exception e) {
                ErrorLog.log(this, "إزالة الطبقة", e);
            }
            view = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
}
