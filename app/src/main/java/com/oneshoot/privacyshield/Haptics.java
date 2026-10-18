package com.oneshoot.privacyshield;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** اهتزاز بشدة من 1 لـ 3. بنستخدم نوع "تنبيه" عشان أندرويد ما يتجاهلوش وإحنا في الخلفية. */
public final class Haptics {
    private Haptics() { }

    @SuppressWarnings("deprecation")
    public static void vibrate(Context c, int level) {
        if (level < 1 || level > 3) return;
        try {
            Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) {
                ErrorLog.log(c, "الاهتزاز: الجهاز مفيهوش موتور اهتزاز", null);
                return;
            }
            long[] ms = {0, 80, 160, 320};
            int[] amp = {0, 100, 180, 255};
            VibrationEffect e = v.hasAmplitudeControl()
                    ? VibrationEffect.createOneShot(ms[level], amp[level])
                    : VibrationEffect.createOneShot(ms[level], VibrationEffect.DEFAULT_AMPLITUDE);
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            v.vibrate(e, aa);
        } catch (Exception e) {
            ErrorLog.log(c, "الاهتزاز", e);
        }
    }
}
