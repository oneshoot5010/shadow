package com.oneshoot.privacyshield;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.widget.Toast;

/** بيستقبل نتيجة جلسة التثبيت: لو محتاج موافقة بيفتح شاشة التثبيت. */
public class InstallReceiver extends BroadcastReceiver {
    @Override
    @SuppressWarnings("deprecation")
    public void onReceive(Context c, Intent i) {
        int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, -1);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = Build.VERSION.SDK_INT >= 33
                    ? i.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class)
                    : (Intent) i.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    c.startActivity(confirm);
                } catch (Exception e) {
                    ErrorLog.log(c, "فتح شاشة التثبيت", e);
                }
            }
        } else if (status != PackageInstaller.STATUS_SUCCESS) {
            String msg = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            ErrorLog.log(c, "فشل التثبيت status=" + status, new Exception(String.valueOf(msg)));
            Toast.makeText(c, "فشل التثبيت: " + msg, Toast.LENGTH_LONG).show();
        }
    }
}
