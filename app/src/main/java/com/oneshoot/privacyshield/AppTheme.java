package com.oneshoot.privacyshield;

import android.content.Context;
import android.content.res.Configuration;

/** ألوان المظهر: 0 تلقائي، 1 فاتح، 2 داكن. */
public final class AppTheme {
    public final int bg, card, text, button;

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
