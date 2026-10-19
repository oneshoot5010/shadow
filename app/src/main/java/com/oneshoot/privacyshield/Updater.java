package com.oneshoot.privacyshield;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/** بيشيّك على آخر Release، وينزّله بشريط تحميل جوه التطبيق، وبعدها يطلب التثبيت. */
public final class Updater {
    private static final String API =
            "https://api.github.com/repos/oneshoot5010/shadow/releases/latest";

    private Updater() { }

    public static void check(Activity a) {
        if (!a.getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(a, "اسمح للتطبيق بتثبيت التحديثات، وبعدين ارجع واضغط تحديث تاني",
                    Toast.LENGTH_LONG).show();
            a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + a.getPackageName())));
            return;
        }
        Toast.makeText(a, "بنتحقق من التحديثات...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(API).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
                String body;
                try (InputStream in = c.getInputStream(); Scanner sc = new Scanner(in, "UTF-8")) {
                    body = sc.useDelimiter("\\A").hasNext() ? sc.next() : "";
                }
                JSONObject j = new JSONObject(body);
                int latest = Integer.parseInt(j.getString("tag_name").replaceAll("[^0-9]", ""));
                String name = j.optString("name", "");
                JSONArray assets = j.getJSONArray("assets");
                String url = assets.length() > 0
                        ? assets.getJSONObject(0).getString("browser_download_url") : null;
                long current = a.getPackageManager()
                        .getPackageInfo(a.getPackageName(), 0).getLongVersionCode();
                a.runOnUiThread(() -> {
                    if (latest > current && url != null) {
                        Dlg.builder(a)
                                .setTitle("فيه تحديث جديد")
                                .setMessage(name)
                                .setPositiveButton("تحديث", (d, w) -> download(a, url))
                                .setNegativeButton("لاحقاً", null)
                                .show();
                    } else {
                        Toast.makeText(a, "إنت على آخر نسخة", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                ErrorLog.log(a, "التحقق من التحديث", e);
                a.runOnUiThread(() -> Toast.makeText(a,
                        "مقدرناش نتحقق من التحديث. تأكد من النت", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private static void download(Activity a, String url) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (20 * a.getResources().getDisplayMetrics().density);
        box.setPadding(p, p, p, p);
        TextView label = new TextView(a);
        label.setText("بننزّل التحديث... 0%");
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(17);
        label.setGravity(Gravity.CENTER_HORIZONTAL);
        ProgressBar bar = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFE53935));
        box.addView(label);
        box.addView(bar);
        AlertDialog dlg = Dlg.builder(a).setTitle("تحديث Shadow")
                .setView(box).setCancelable(false).create();
        dlg.show();

        new Thread(() -> {
            PackageInstaller.Session session = null;
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setInstanceFollowRedirects(true);
                if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
                long total = c.getContentLengthLong();

                PackageInstaller pi = a.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                int id = pi.createSession(sp);
                session = pi.openSession(id);
                long done = 0;
                int lastPct = -1;
                try (InputStream in = c.getInputStream();
                     OutputStream out = session.openWrite("shadow", 0, total > 0 ? total : -1)) {
                    byte[] buf = new byte[32768];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        done += n;
                        if (total > 0) {
                            final int pct = (int) (done * 100 / total);
                            if (pct != lastPct) {
                                lastPct = pct;
                                a.runOnUiThread(() -> {
                                    bar.setProgress(pct);
                                    label.setText("بننزّل التحديث... " + pct + "%");
                                });
                            }
                        }
                    }
                    session.fsync(out);
                }
                Intent cb = new Intent(a, InstallReceiver.class).setPackage(a.getPackageName());
                PendingIntent pend = PendingIntent.getBroadcast(a, id, cb,
                        PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                a.runOnUiThread(() -> label.setText("جاهز للتثبيت..."));
                session.commit(pend.getIntentSender());
                a.runOnUiThread(dlg::dismiss);
            } catch (Exception e) {
                ErrorLog.log(a, "تنزيل/تثبيت التحديث", e);
                if (session != null) {
                    try { session.abandon(); } catch (Exception ignored) { }
                }
                a.runOnUiThread(() -> {
                    dlg.dismiss();
                    Toast.makeText(a, "فشل التحديث. شوف سجل الأخطاء في الإعدادات",
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
