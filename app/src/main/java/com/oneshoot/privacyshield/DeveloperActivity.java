package com.oneshoot.privacyshield;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** صفحة معلومات المطور: روابط التواصل وتحميل باقي التطبيقات. */
public class DeveloperActivity extends Activity {

    private static final int RED = 0xFFE53935;
    private int BG, CARD, WHITE;

    private static final String WHATSAPP = "https://wa.me/201044947639";
    private static final String GITHUB = "https://github.com/oneshoot5010";
    private static final String VIBER = "viber://chat?number=201044947639";

    private Typeface ruqaa;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            ruqaa = Typeface.createFromAsset(getAssets(), "fonts/ruqaa.ttf");
        } catch (Exception e) {
            ruqaa = Typeface.DEFAULT_BOLD;
        }

        AppTheme th = new AppTheme(this);
        BG = th.bg; CARD = th.card; WHITE = th.text;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(24));

        TextView title = text("معلومات المطور", 30, RED, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);
        TextView sub = text("تطبيقات بتتعمل بحب ومجانية بالكامل. تواصل معايا أو جرّب باقي تطبيقاتي.", 16, WHITE, false);
        sub.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(sub);

        root.addView(text("تواصل معايا", 20, WHITE, true));
        LinearLayout contact = new LinearLayout(this);
        contact.setOrientation(LinearLayout.HORIZONTAL);
        contact.addView(small("واتساب", WHATSAPP));
        contact.addView(small("جيت هاب", GITHUB));
        contact.addView(small("فايبر", VIBER));
        root.addView(contact);

        root.addView(text("تطبيقات من نفس المطور", 20, WHITE, true));
        root.addView(card(R.drawable.ic_emoji, "ستيكرات إيموجي",
                "350 ستيكر تنسخها وتبعتها في أي شات.",
                "https://github.com/oneshoot5010/emoji_stickers/releases/download/latest/emoji-stickers.apk"));
        root.addView(card(R.drawable.ic_rafiq, "رفيق",
                "رفيقك اليومي في الصلاة والذكر والقرآن.",
                "https://github.com/oneshoot5010/rafiq_app/releases/download/latest/app-arm64-v8a-release.apk"));
        root.addView(card(R.drawable.ic_font, "استيكر فونت",
                "اكتب بخطوط وزخارف مميزة في أي تطبيق.",
                "https://github.com/oneshoot5010/font-stickers"));
        root.addView(card(R.drawable.ic_leo, "Leo Copilot Pro",
                "مساعدك الذكي الشامل لكل ما تحتاجه في يومك.",
                "https://github.com/oneshoot5010/leo-copilot/releases/download/latest/leo-copilot.apk"));

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.addView(root);
        setContentView(sv);
    }

    private Button small(String label, String url) {
        Button b = styled(label, RED);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(3), dp(4), dp(3), dp(4));
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> open(url));
        return b;
    }

    private LinearLayout card(int icon, String name, String desc, String url) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD);
        g.setStroke(dp(1), RED);
        g.setCornerRadius(dp(14));
        c.setBackground(g);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, dp(6), 0, dp(6));
        c.setLayoutParams(clp);

        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        c.addView(iv, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding(dp(12), 0, dp(12), 0);
        mid.addView(text(name, 18, WHITE, true));
        mid.addView(text(desc, 14, WHITE, false));
        c.addView(mid, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button dl = styled("تحميل", RED);
        dl.setOnClickListener(v -> open(url));
        c.addView(dl);
        return c;
    }

    private void open(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "مفيش تطبيق يفتح اللينك ده", Toast.LENGTH_SHORT).show();
        }
    }

    private Button styled(String t, int color) {
        Button b = new Button(this);
        b.setText(t);
        b.setTextColor(0xFFFFFFFF);
        b.setAllCaps(false);
        b.setTypeface(ruqaa, Typeface.BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(12));
        b.setBackground(g);
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

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
