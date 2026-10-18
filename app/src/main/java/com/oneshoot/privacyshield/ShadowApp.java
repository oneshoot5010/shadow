package com.oneshoot.privacyshield;

import android.app.Application;

/** بيسجل أي كراش في سجل الأخطاء قبل ما التطبيق يقفل. */
public class ShadowApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            ErrorLog.log(this, "CRASH في " + t.getName(), e);
            if (prev != null) prev.uncaughtException(t, e);
        });
    }
}
