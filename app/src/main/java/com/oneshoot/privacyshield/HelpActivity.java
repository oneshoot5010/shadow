package com.oneshoot.privacyshield;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** شرح الأوضاع وكل زرار في التطبيق، بخط كبير وواضح. */
public class HelpActivity extends Activity {

    private static final int RED = 0xFFE53935;
    private Typeface ruqaa;
    private int text;

    private static final String[][] SECTIONS = {
        {"الأوضاع", null},
        {"تعتيم", "الشاشة كلها مضلّمة، واللمس بيوصل للتطبيق اللي تحتها. تقدر تشتغل عادي والكلام بيبان خافت."},
        {"كشف بالإصبع", "الشاشة كلها مغطاة، والدايرة بتظهر فوق صباعك عشان الكلام اللي تحته يبان. في الوضع ده اللمس مش بيوصل للتطبيق."},
        {"تنبيه", "الطبقة حماية من النظر بس، مش من التصوير: أي صورة شاشة هتظهر الكلام."},

        {"الشاشة الرئيسية", null},
        {"تشغيل الطبقة / إيقاف الطبقة", "بيشغّل الطبقة فوق كل التطبيقات أو بيقفلها. أول مرة هيطلب إذن الظهور فوق التطبيقات."},
        {"شدة التعتيم", "السلايدر بيحدد الطبقة سودا قد إيه في وضع التعتيم. كل ما زاد الرقم الشاشة بتبقى أغمق."},
        {"الوضع", "بتختار بين تعتيم وكشف بالإصبع."},
        {"حجم الدايرة", "بيظهر في وضع كشف بالإصبع بس. بيكبّر أو بيصغّر الدايرة اللي بتظهر فوق صباعك."},
        {"الإعدادات", "بيفتح كل الخيارات الإضافية، ومنها الشرح ده."},
        {"معلومات المطور", "حسابات التواصل مع المطور، وتحميل باقي التطبيقات."},

        {"الإعدادات", null},
        {"المظهر", "تلقائي (زي الموبايل) أو فاتح أو داكن."},
        {"إخفاء/إظهار الطبقة بالاهتزاز", "لما يكون شغال، هزّة قوية للموبايل بتخفي الطبقة، وهزّة تانية بترجّعها."},
        {"شدة الاهتزاز", "قوة اهتزاز الموبايل لما الطبقة تتخفى أو تظهر بالهزّ: بدون، خفيفة، متوسطة، قوية."},
        {"تعتيم أقوى لما الموبايل يثبت", "لو الموبايل فضل ثابت من غير حركة المدة اللي تختارها (15 ثانية، 30 ثانية، دقيقة، 5 دقايق) التعتيم بيزيد لوحده، ولما تحركه بيرجع زي ما كان."},
        {"حفظ / تحميل الإعدادات", "3 خانات تحفظ فيهم إعداداتك الحالية وترجعلها بضغطة."},
        {"الطبقة تظهر فوق تطبيقات معينة بس", "لما يكون شغال، الطبقة بتظهر فوق التطبيقات اللي تختارها بس. محتاج إذن الوصول لاستخدام التطبيقات."},
        {"التطبيقات المختارة", "قايمة التطبيقات اللي الطبقة تظهر فوقها."},
        {"زرار تشغيل/إيقاف في الشريط", "بيضيف زرار Shadow في شريط الإعدادات السريعة عشان تشغّل وتقفل الطبقة من غير ما تفتح التطبيق."},
        {"إرشادات البطارية", "خطوات تمنع الموبايل يقفل الخدمة في الخلفية."},
        {"تحديث التطبيق", "بيشيّك على آخر نسخة، وينزّلها ويثبّتها."},
        {"نسخ / مسح سجل الأخطاء", "نسخ السجل عشان تبعته للمطور، أو مسحه."},

        {"الإشعار", null},
        {"إيقاف", "بيقفل الطبقة."},
        {"تعتيم", "بيحوّل للتعتيم من غير ما تفتح التطبيق."},
        {"كشف بالإصبع", "بيحوّل لكشف بالإصبع من غير ما تفتح التطبيق."},
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            ruqaa = Typeface.createFromAsset(getAssets(), "fonts/ruqaa.ttf");
        } catch (Exception e) {
            ruqaa = Typeface.DEFAULT_BOLD;
        }
        AppTheme th = new AppTheme(this);
        text = th.text;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(28));

        TextView title = tv("شرح الأوضاع", 32, RED);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        for (String[] s : SECTIONS) {
            if (s[1] == null) {
                TextView h = tv(s[0], 26, RED);
                h.setPadding(0, dp(22), 0, dp(4));
                root.addView(h);
            } else {
                root.addView(tv(s[0], 21, text));
                TextView d = tv(s[1], 19, text);
                d.setAlpha(0.9f);
                d.setPadding(0, 0, 0, dp(10));
                root.addView(d);
            }
        }

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(th.bg);
        sv.addView(root);
        AppTheme.style(this, sv, th);
        setContentView(sv);
    }

    private TextView tv(String t, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(ruqaa, Typeface.BOLD);
        v.setLineSpacing(0, 1.15f);
        v.setPadding(0, dp(6), 0, dp(2));
        return v;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
