package com.oneshoot.privacyshield;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** حفظ الإعدادات: التشغيل، الوضع، الشفافية، الهزّ، أداة الرسم، حجم الدايرة، والمناطق المغطاة. */
public final class ShieldPrefs {

    private static final String FILE = "shield";

    private ShieldPrefs() { }

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean running(Context c) { return p(c).getBoolean("running", false); }
    public static void setRunning(Context c, boolean v) { p(c).edit().putBoolean("running", v).apply(); }

    public static int mode(Context c) { return p(c).getInt("mode", ShieldView.MODE_DIM); }
    public static void setMode(Context c, int m) { p(c).edit().putInt("mode", m).apply(); }

    public static int alpha(Context c) { return p(c).getInt("alpha", 160); }
    public static void setAlpha(Context c, int a) { p(c).edit().putInt("alpha", a).apply(); }

    public static int theme(Context c) { return p(c).getInt("theme", 0); }
    public static void setTheme(Context c, int t) { p(c).edit().putInt("theme", t).apply(); }

    public static boolean shake(Context c) { return p(c).getBoolean("shake", false); }
    public static void setShake(Context c, boolean v) { p(c).edit().putBoolean("shake", v).apply(); }

    public static int tool(Context c) { return p(c).getInt("tool", ShieldView.TOOL_RECT); }
    public static void setTool(Context c, int t) { p(c).edit().putInt("tool", t).apply(); }

    /** حجم الدايرة بالنسبة المئوية من أقصر ضلع (من 10 لـ 40). */
    public static int reveal(Context c) { return p(c).getInt("reveal", 22); }
    public static void setReveal(Context c, int v) { p(c).edit().putInt("reveal", v).apply(); }

    public static void saveMasks(Context c, List<ShieldMask> masks) {
        StringBuilder sb = new StringBuilder();
        for (ShieldMask m : masks) sb.append(m.serialize()).append(';');
        p(c).edit().putString("masks", sb.toString()).apply();
    }

    public static List<ShieldMask> loadMasks(Context c) {
        List<ShieldMask> out = new ArrayList<>();
        String raw = p(c).getString("masks", "");
        for (String item : raw.split(";")) {
            if (item.isEmpty()) continue;
            try {
                ShieldMask m = ShieldMask.parse(item);
                if (m != null) out.add(m);
            } catch (NumberFormatException ignored) {
                // سطر بايظ: نتجاهله
            }
        }
        return out;
    }

    public static void clearMasks(Context c) { p(c).edit().remove("masks").apply(); }
}
