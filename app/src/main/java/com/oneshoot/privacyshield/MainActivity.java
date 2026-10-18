package com.oneshoot.privacyshield;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int RED = 0xFFE53935;
    private int BG, DARK, WHITE, MUTED;
    private static final String[] THEME_NAMES = {"تلقائي", "فاتح", "داكن"};
    private final Button[] themeButtons = new Button[THEME_NAMES.length];

    private static final String[] MODE_NAMES = {"تعتيم", "كشف بالإصبع"};

    private static final String HELP =
            "تعتيم: الشاشة كلها مضلّمة، واللمس بيوصل للتطبيق اللي تحتها.\n\n"
            + "كشف بالإصبع: الشاشة كلها مغطاة، والدايرة بتظهر فوق صباعك عشان الكلام اللي تحته يبان. "
            + "في الوضع ده اللمس مش بيوصل للتطبيق.\n\n"
            + "الطبقة دي حماية من النظر بس، مش من التصوير: أي صورة شاشة هتظهر الكلام.";

    private Typeface ruqaa;
    private TextView status, dimLabel, revealLabel, helpText;
    private Button toggleBtn, helpBtn;
    private final Button[] modeButtons = new Button[MODE_NAMES.length];
    private LinearLayout revealRow;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        ruqaa = loadRuqaa();
        AppTheme th = new AppTheme(this);
        BG = th.bg; DARK = th.button; WHITE = th.text; MUTED = th.text;
        if (ShieldPrefs.mode(this) > ShieldView.MODE_PEEK) ShieldPrefs.setMode(this, ShieldView.MODE_DIM);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(24));

        TextView title = text("Shadow", 40, RED, true);
        title.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        status = text("", 18, MUTED, false);
        status.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        root.addView(status);

        toggleBtn = btn("", RED);
        toggleBtn.setOnClickListener(v -> toggle());
        root.addView(toggleBtn);

        // شدة التعتيم
        dimLabel = text("", 17, WHITE, false);
        root.addView(dimLabel);
        SeekBar dimBar = new SeekBar(this);
        dimBar.setMax(255);
        dimBar.setProgress(ShieldPrefs.alpha(this));
        dimBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) {
                    ShieldPrefs.setAlpha(MainActivity.this, p);
                    refresh();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) { }

            @Override
            public void onStopTrackingTouch(SeekBar s) { }
        });
        root.addView(dimBar);

        // الوضع
        root.addView(text("الوضع", 17, WHITE, true));
        LinearLayout modeRow = row();
        for (int i = 0; i < MODE_NAMES.length; i++) {
            final int m = i;
            modeButtons[i] = rowBtn(MODE_NAMES[i]);
            modeButtons[i].setOnClickListener(v -> {
                ShieldPrefs.setMode(this, m);
                refresh();
            });
            modeRow.addView(modeButtons[i]);
        }
        root.addView(modeRow);

        // حجم الدايرة (بيظهر في وضع كشف بالإصبع بس)
        revealRow = column();
        revealLabel = text("", 17, WHITE, false);
        revealRow.addView(revealLabel);
        SeekBar revealBar = new SeekBar(this);
        revealBar.setMax(30);                       // من 10% لـ 40%
        revealBar.setProgress(ShieldPrefs.reveal(this) - 10);
        revealBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) {
                    ShieldPrefs.setReveal(MainActivity.this, p + 10);
                    refresh();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) { }

            @Override
            public void onStopTrackingTouch(SeekBar s) { }
        });
        revealRow.addView(revealBar);
        root.addView(revealRow);

        root.addView(text("المظهر", 17, WHITE, true));
        LinearLayout themeRow = row();
        for (int i = 0; i < THEME_NAMES.length; i++) {
            final int t = i;
            themeButtons[i] = rowBtn(THEME_NAMES[i]);
            choice(themeButtons[i], ShieldPrefs.theme(this) == i);
            themeButtons[i].setOnClickListener(v -> {
                ShieldPrefs.setTheme(this, t);
                recreate();
            });
            themeRow.addView(themeButtons[i]);
        }
        root.addView(themeRow);

        CheckBox shake = new CheckBox(this);
        shake.setText("هزّ الموبايل عشان تخفي/تظهر الطبقة");
        shake.setTextColor(WHITE);
        shake.setTypeface(ruqaa);
        shake.setChecked(ShieldPrefs.shake(this));
        shake.setOnCheckedChangeListener((b, on) -> {
            ShieldPrefs.setShake(this, on);
            refresh();
        });
        root.addView(shake);

        helpBtn = btn("شرح الأوضاع", DARK);
        root.addView(helpBtn);
        helpText = text(HELP, 15, MUTED, false);
        helpText.setVisibility(View.GONE);
        root.addView(helpText);
        helpBtn.setOnClickListener(v -> helpText.setVisibility(
                helpText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        Button upd = btn("تحديث التطبيق", DARK);
        upd.setOnClickListener(v -> Updater.check(this));
        root.addView(upd);

        Button dev = btn("معلومات المطور", RED);
        dev.setOnClickListener(v -> startActivity(new Intent(this, DeveloperActivity.class)));
        root.addView(dev);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.addView(root);
        setContentView(sv);

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void toggle() {
        if (ShieldPrefs.running(this)) stopShield();
        else start();
    }

    private void start() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            Toast.makeText(this, "اسمح بالظهور فوق التطبيقات، وبعدين ارجع واضغط تشغيل",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
        ShieldPrefs.setRunning(this, true);
        startForegroundService(new Intent(this, OverlayService.class)
                .setAction(OverlayService.ACTION_START));
        refresh();
    }

    private void stopShield() {
        ShieldPrefs.setRunning(this, false);
        stopService(new Intent(this, OverlayService.class));
        refresh();
    }

    private void refresh() {
        if (ShieldPrefs.running(this)) {
            startService(new Intent(this, OverlayService.class)
                    .setAction(OverlayService.ACTION_REFRESH));
        }
        updateView();
    }

    private void updateView() {
        boolean on = ShieldPrefs.running(this);
        String s = on ? "● الطبقة شغالة" : "○ الطبقة متوقفة";
        if (!Settings.canDrawOverlays(this)) s += "\nلازم تسمح بالظهور فوق التطبيقات الأول";
        status.setText(s);
        status.setTextColor(on ? RED : MUTED);
        toggleBtn.setText(on ? "إيقاف الطبقة" : "تشغيل الطبقة");

        int m = ShieldPrefs.mode(this);
        for (int i = 0; i < modeButtons.length; i++) {
            choice(modeButtons[i], i == m);
        }
        revealRow.setVisibility(m == ShieldView.MODE_PEEK ? View.VISIBLE : View.GONE);

        dimLabel.setText("شدة التعتيم: " + (ShieldPrefs.alpha(this) * 100 / 255) + "%");
        revealLabel.setText("حجم الدايرة: " + ShieldPrefs.reveal(this) + "%");
    }

    private void choice(Button b, boolean selected) {
        b.setBackground(rounded(selected ? RED : DARK));
    }

    private Typeface loadRuqaa() {
        try {
            return Typeface.createFromAsset(getAssets(), "fonts/ruqaa.ttf");
        } catch (Exception e) {
            return Typeface.DEFAULT_BOLD;
        }
    }

    private LinearLayout column() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        return c;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(android.view.Gravity.CENTER_VERTICAL);
        return r;
    }

    private Button rowBtn(String text) {
        Button b = styled(text, DARK);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(3), dp(4), dp(3), dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    private Button btn(String text, int color) {
        Button b = styled(text, color);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        b.setLayoutParams(lp);
        return b;
    }

    private Button styled(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(color == RED ? 0xFFFFFFFF : WHITE);
        b.setAllCaps(false);
        b.setTypeface(ruqaa, Typeface.BOLD);
        b.setBackground(rounded(color));
        return b;
    }

    private TextView text(String t, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setTypeface(ruqaa, bold ? Typeface.BOLD : Typeface.NORMAL);
        tv.setPadding(0, dp(6), 0, dp(6));
        return tv;
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(12));
        return g;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
