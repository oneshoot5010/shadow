package com.oneshoot.privacyshield;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** سجل أخطاء بسيط في ملف عشان تقدر تنسخه من الإعدادات. */
public final class ErrorLog {
    private static final int MAX = 60000;

    private ErrorLog() { }

    private static File file(Context c) {
        return new File(c.getApplicationContext().getFilesDir(), "errors.log");
    }

    public static synchronized void log(Context c, String where, Throwable t) {
        try {
            StringWriter sw = new StringWriter();
            sw.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()))
                    .append("  [").append(where).append("]\n");
            if (t != null) t.printStackTrace(new PrintWriter(sw));
            sw.append("\n");
            String old = read(c);
            String all = old + sw;
            if (all.length() > MAX) all = all.substring(all.length() - MAX);
            try (FileOutputStream o = new FileOutputStream(file(c))) {
                o.write(all.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            // لو التسجيل نفسه فشل مفيش حاجة نعملها
        }
    }

    public static synchronized String read(Context c) {
        try {
            File f = file(c);
            if (!f.exists()) return "";
            byte[] b = new byte[(int) f.length()];
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                int n = 0;
                while (n < b.length) {
                    int r = in.read(b, n, b.length - n);
                    if (r < 0) break;
                    n += r;
                }
            }
            return new String(b, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static synchronized void clear(Context c) {
        file(c).delete();
    }
}
