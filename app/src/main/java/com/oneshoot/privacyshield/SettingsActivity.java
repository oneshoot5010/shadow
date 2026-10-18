package com.oneshoot.privacyshield;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** كل الخيارات الإضافية في مكان واحد، وكل خيار ليه اختيارات يتغير منها. */
public class SettingsActivity extends Activity {

    private static final int RED = 0xFFE53935;
    private int bg, text, btnBg;
    private Typeface ruqaa;

    private static final String[] THEMES = {"تلقائي", "فاتح", "داكن"};
    private static final String[] ONOFF = {"مقفول", "شغال"};
    private static final String[] VIBES = {"بدون", "خفيفة", "متوسطة", "قوية"};
    private static final String[] IDLE_NAMES = {"مقفول", "بعد 15 ثانية", "بعد 30 ثانية", "بعد دقيقة", "بعد 5 دقايق"};
    private static final int[] IDLE_VALUES = {0, 15, 30, 60, 300};

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            ruqaa = Typeface.createFromAsset(getAssets(), "fonts/ruqaa.ttf");
        } catch (Exception e) {
            ruqaa = Typeface.DEFAULT_BOLD;
        }
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    private void build() {
        AppTheme th = new AppTheme(this);
        bg = th.bg; text = th.text; btnBg = th.button;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(24));

        TextView title = label("الإعدادات", 30, RED, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        // المظهر
        header(root, "المظهر");
        option(root, R.drawable.ic_theme, "المظهر", THEMES[ShieldPrefs.theme(this)], () ->
                choose("المظهر", THEMES, ShieldPrefs.theme(this), i -> {
                    ShieldPrefs.setTheme(this, i);
                    recreate();
                }));

        // الهزّ
        header(root, "الاهتزاز");
        option(root, R.drawable.ic_vibrate, "إخفاء/إظهار الطبقة بالاهتزاز", ONOFF[ShieldPrefs.shake(this) ? 1 : 0], () ->
                choose("الاهتزاز", ONOFF, ShieldPrefs.shake(this) ? 1 : 0, i -> {
                    ShieldPrefs.setShake(this, i == 1);
                    apply();
                }));
        option(root, R.drawable.ic_vibrate, "شدة الاهتزاز", VIBES[clamp(ShieldPrefs.geti(this, "vibrate", 0), 0, 3)], () ->
                choose("شدة الاهتزاز", VIBES, ShieldPrefs.geti(this, "vibrate", 0), i -> {
                    ShieldPrefs.seti(this, "vibrate", i);
                    Haptics.vibrate(this, i);
                    apply();
                }));

        // التعتيم التلقائي
        header(root, "تعتيم تلقائي");
        int idle = ShieldPrefs.geti(this, "idle", 0);
        int idleIdx = 0;
        for (int i = 0; i < IDLE_VALUES.length; i++) if (IDLE_VALUES[i] == idle) idleIdx = i;
        final int idleSel = idleIdx;
        option(root, R.drawable.ic_clock, "تعتيم أقوى لما الموبايل يثبت", IDLE_NAMES[idleIdx], () ->
                choose("بعد قد إيه من غير حركة؟", IDLE_NAMES, idleSel, i -> {
                    ShieldPrefs.seti(this, "idle", IDLE_VALUES[i]);
                    apply();
                }));

        // إعدادات جاهزة
        header(root, "إعدادات جاهزة");
        option(root, R.drawable.ic_bookmark, "حفظ الإعدادات الحالية", "اختار خانة", () ->
                choose("احفظ في", slotNames(), -1, i -> {
                    ShieldPrefs.sets(this, "preset" + i, presetString());
                    Toast.makeText(this, "اتحفظت في الخانة " + (i + 1), Toast.LENGTH_SHORT).show();
                    build();
                }));
        option(root, R.drawable.ic_bookmark, "تحميل إعدادات محفوظة", "اختار خانة", () ->
                choose("حمّل من", slotNames(), -1, i -> {
                    if (!loadPreset(i)) {
                        Toast.makeText(this, "الخانة فاضية", Toast.LENGTH_SHORT).show();
                    } else {
                        apply();
                        Toast.makeText(this, "اتحمّلت الخانة " + (i + 1), Toast.LENGTH_SHORT).show();
                    }
                }));

        // التشغيل حسب التطبيق
        header(root, "تشغيل حسب التطبيق");
        boolean auto = ShieldPrefs.geti(this, "autoapps", 0) == 1;
        option(root, R.drawable.ic_grid, "الطبقة تظهر فوق تطبيقات معينة بس", ONOFF[auto ? 1 : 0], () ->
                choose("التشغيل حسب التطبيق", ONOFF, auto ? 1 : 0, i -> {
                    if (i == 1 && !hasUsageAccess()) {
                        new AlertDialog.Builder(this)
                                .setMessage("محتاج إذن \"الوصول لاستخدام التطبيقات\" عشان نعرف التطبيق المفتوح.")
                                .setPositiveButton("فتح الإعدادات", (d, w) -> startActivity(
                                        new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)))
                                .setNegativeButton("إلغاء", null).show();
                    }
                    ShieldPrefs.seti(this, "autoapps", i);
                    apply();
                }));
        int count = selectedApps().size();
        option(root, R.drawable.ic_grid, "التطبيقات المختارة", count + " تطبيق", this::pickApps);

        // شريط الإعدادات السريعة
        header(root, "شريط الإعدادات السريعة");
        option(root, R.drawable.ic_toggle, "زرار تشغيل/إيقاف في الشريط", "إضافة", this::addTile);

        // البطارية
        header(root, "استمرار الخدمة في الخلفية");
        option(root, R.drawable.ic_clock, "إرشادات البطارية والتشغيل التلقائي", "عرض", this::batteryGuide);

        // التحديث
        header(root, "التحديث");
        option(root, R.drawable.ic_refresh, "تحديث التطبيق", "تحقق", () -> Updater.check(this));

        // الأخطاء
        header(root, "الأخطاء");
        option(root, R.drawable.ic_bug, "نسخ سجل الأخطاء", "نسخ", () -> {
            String log = ErrorLog.read(this);
            if (log.isEmpty()) {
                Toast.makeText(this, "مفيش أخطاء متسجلة", Toast.LENGTH_SHORT).show();
                return;
            }
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Shadow errors", log));
            Toast.makeText(this, "اتنسخ سجل الأخطاء", Toast.LENGTH_SHORT).show();
        });
        option(root, R.drawable.ic_trash, "مسح سجل الأخطاء", "مسح", () -> {
            ErrorLog.clear(this);
            Toast.makeText(this, "اتمسح السجل", Toast.LENGTH_SHORT).show();
        });

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(bg);
        sv.addView(root);
        AppTheme.style(this, sv, th);
        setContentView(sv);
    }

    // ---------- خيارات ----------

    private interface IntCb { void run(int i); }

    private void choose(String title, String[] items, int checked, IntCb cb) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(items, checked, (d, which) -> {
                    d.dismiss();
                    cb.run(which);
                    build();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void apply() {
        if (ShieldPrefs.running(this)) {
            startService(new Intent(this, OverlayService.class)
                    .setAction(OverlayService.ACTION_REFRESH));
        }
    }

    private String[] slotNames() {
        String[] n = new String[3];
        for (int i = 0; i < 3; i++) {
            n[i] = "خانة " + (i + 1) + (ShieldPrefs.gets(this, "preset" + i, "").isEmpty() ? " (فاضية)" : " (محفوظة)");
        }
        return n;
    }

    private String presetString() {
        return ShieldPrefs.alpha(this) + "," + ShieldPrefs.mode(this) + "," + ShieldPrefs.reveal(this)
                + "," + (ShieldPrefs.shake(this) ? 1 : 0) + "," + ShieldPrefs.geti(this, "vibrate", 0)
                + "," + ShieldPrefs.geti(this, "idle", 0);
    }

    private boolean loadPreset(int slot) {
        String s = ShieldPrefs.gets(this, "preset" + slot, "");
        if (s.isEmpty()) return false;
        try {
            String[] p = s.split(",");
            ShieldPrefs.setAlpha(this, Integer.parseInt(p[0]));
            ShieldPrefs.setMode(this, Integer.parseInt(p[1]));
            ShieldPrefs.setReveal(this, Integer.parseInt(p[2]));
            ShieldPrefs.setShake(this, "1".equals(p[3]));
            ShieldPrefs.seti(this, "vibrate", Integer.parseInt(p[4]));
            ShieldPrefs.seti(this, "idle", Integer.parseInt(p[5]));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Set<String> selectedApps() {
        Set<String> out = new HashSet<>(Arrays.asList(ShieldPrefs.gets(this, "apps", "").split(",")));
        out.remove("");
        return out;
    }

    private void pickApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> all = pm.queryIntentActivities(main, 0);
        final List<String> pkgs = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        for (ResolveInfo r : all) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || pkgs.contains(pkg)) continue;
            pkgs.add(pkg);
            names.add(r.loadLabel(pm).toString());
        }
        // ترتيب بالاسم
        Integer[] order = new Integer[pkgs.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> names.get(a).compareToIgnoreCase(names.get(b)));
        final String[] labels = new String[order.length];
        final String[] ids = new String[order.length];
        final boolean[] checked = new boolean[order.length];
        Set<String> sel = selectedApps();
        for (int i = 0; i < order.length; i++) {
            labels[i] = names.get(order[i]);
            ids[i] = pkgs.get(order[i]);
            checked[i] = sel.contains(ids[i]);
        }
        new AlertDialog.Builder(this)
                .setTitle("اختار التطبيقات")
                .setMultiChoiceItems(labels, checked, (d, which, on) -> checked[which] = on)
                .setPositiveButton("حفظ", (d, w) -> {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < ids.length; i++) if (checked[i]) sb.append(ids[i]).append(',');
                    ShieldPrefs.sets(this, "apps", sb.toString());
                    apply();
                    build();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private boolean hasUsageAccess() {
        AppOpsManager ops = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    private void addTile() {
        if (Build.VERSION.SDK_INT >= 33) {
            StatusBarManager sbm = getSystemService(StatusBarManager.class);
            sbm.requestAddTileService(new ComponentName(this, ShieldTileService.class), "Shadow",
                    Icon.createWithResource(this, R.drawable.ic_notif), getMainExecutor(), r -> { });
        } else {
            new AlertDialog.Builder(this)
                    .setMessage("اسحب شريط الإشعارات لتحت، اضغط على أيقونة القلم (تعديل)، وبعدين اسحب زرار Shadow لشريط الإعدادات السريعة.")
                    .setPositiveButton("تمام", null).show();
        }
    }

    private void batteryGuide() {
        new AlertDialog.Builder(this)
                .setTitle("عشان الخدمة ما تتقفلش")
                .setMessage("1) اسمح للتطبيق يشتغل في الخلفية من غير قيود بطارية.\n\n"
                        + "2) في بعض الأجهزة (شاومي، أوبو، ريلمي، فيفو، سامسونج، هواوي وغيرهم) "
                        + "فعّل \"التشغيل التلقائي\" أو \"Auto-start\" وامنع التطبيق من التنظيف التلقائي "
                        + "من إعدادات التطبيق أو إعدادات البطارية. أسماء الخيارات بتختلف حسب الجهاز.\n\n"
                        + "3) ثبّت التطبيق في قايمة التطبيقات الأخيرة لو جهازك بيدعم كده.")
                .setPositiveButton("استثناء البطارية", (d, w) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    } catch (Exception e) {
                        openAppDetails();
                    }
                })
                .setNeutralButton("إعدادات التطبيق", (d, w) -> openAppDetails())
                .setNegativeButton("إغلاق", null)
                .show();
    }

    private void openAppDetails() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName())));
    }

    // ---------- واجهة ----------

    private void header(LinearLayout root, String t) {
        root.addView(label(t, 20, text, true));
    }

    private void option(LinearLayout root, int icon, String name, String value, Runnable action) {
        Button b = new Button(this);
        b.setText(name + "  ←  " + value);
        b.setTextColor(0xFFFFFFFF);
        b.setAllCaps(false);
        b.setTypeface(ruqaa, Typeface.BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(RED);
        g.setCornerRadius(dp(12));
        b.setBackground(g);
        android.graphics.drawable.Drawable ic = getDrawable(icon);
        ic.setBounds(0, 0, dp(24), dp(24));
        b.setCompoundDrawables(ic, null, null, null);
        b.setCompoundDrawablePadding(dp(8));
        b.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> action.run());
        root.addView(b);
    }

    private TextView label(String t, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setTypeface(ruqaa, bold ? Typeface.BOLD : Typeface.NORMAL);
        tv.setPadding(0, dp(12), 0, dp(4));
        return tv;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
