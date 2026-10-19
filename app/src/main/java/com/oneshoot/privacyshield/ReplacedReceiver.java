package com.oneshoot.privacyshield;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** بعد ما التطبيق يتحدّث: بيفتحه تاني (وهو اللي بيرجّع الطبقة لو كانت شغالة). */
public class ReplacedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        try {
            c.startActivity(new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            ErrorLog.log(c, "إعادة فتح التطبيق بعد التحديث", e);
        }
    }
}
