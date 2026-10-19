package com.oneshoot.privacyshield;

import android.app.AlertDialog;
import android.content.Context;

/** كل النوافذ المنبثقة بخلفية سودا وكتابة بيضا (ستايل ShadowDialog). */
public final class Dlg {
    private Dlg() { }

    public static AlertDialog.Builder builder(Context c) {
        return new AlertDialog.Builder(c, R.style.ShadowDialog);
    }
}
