package com.oneshoot.privacyshield;

import android.content.Context;
import android.content.res.Configuration;

/** ألوان المظهر: 0 تلقائي، 1 فاتح، 2 داكن. */
public final class AppTheme {
    public final int bg, card, text, button;

    /** لون شريط الحالة والتنقل + مسافات الحواف على أندرويد 15. */
    public static void style(android.app.Activity a, android.view.View scroll, AppTheme t) {
        a.getWindow().setStatusBarColor(t.bg);
        a.getWindow().setNavigationBarColor(t.bg);
        boolean light = t.bg == 0xFFF5F5F5;
        int f = light ? (android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) : 0;
        a.getWindow().getDecorView().setSystemUiVisibility(f);
        scroll.setOnApplyWindowInsetsListener((v, i) -> {
            v.setPadding(0, i.getSystemWindowInsetTop(), 0, i.getSystemWindowInsetBottom());
            return i;
        });
    }

    public AppTheme(Context c) {
        int t = ShieldPrefs.theme(c);
        boolean dark = t == 2 || (t == 0 && (c.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
        bg = dark ? 0xFF0B0B0B : 0xFFF5F5F5;
        card = dark ? 0xFF161616 : 0xFFFFFFFF;
        text = dark ? 0xFFFFFFFF : 0xFF111111;
        button = dark ? 0xFF2A2A2A : 0xFFDDDDDD;
    }
}
