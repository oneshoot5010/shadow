package com.oneshoot.privacyshield;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/** بيقرأ آخر Release من GitHub (الـ tag شكله v<versionCode>) ويقارنه بالنسخة المثبتة. */
public final class Updater {
    private static final String API =
            "https://api.github.com/repos/oneshoot5010/shadow/releases/latest";

    private Updater() { }

    public static void check(Activity a) {
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
                        new AlertDialog.Builder(a)
                                .setTitle("فيه تحديث جديد")
                                .setMessage(name + "\nهتنزل النسخة وتفتحها عشان تثبتها.")
                                .setPositiveButton("تحديث", (d, w) ->
                                        a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))))
                                .setNegativeButton("لاحقاً", null)
                                .show();
                    } else {
                        Toast.makeText(a, "إنت على آخر نسخة", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                a.runOnUiThread(() -> Toast.makeText(a,
                        "مقدرناش نتحقق من التحديث. تأكد من النت", Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
