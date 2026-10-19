package com.oneshoot.privacyshield;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** زرار في شريط الإعدادات السريعة لتشغيل وإيقاف الطبقة. */
public class ShieldTileService extends TileService {

    @Override
    public void onStartListening() {
        update();
    }

    @Override
    public void onClick() {
        if (!ShieldPrefs.running(this)) {
            if (!Settings.canDrawOverlays(this)) {
                openApp();
                return;
            }
            ShieldPrefs.setRunning(this, true);
            startForegroundService(new Intent(this, OverlayService.class)
                    .setAction(OverlayService.ACTION_START));
        } else {
            ShieldPrefs.setRunning(this, false);
            stopService(new Intent(this, OverlayService.class));
        }
        update();
    }

    private void openApp() {
        Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE));
        } else {
            startActivityAndCollapse(i);
        }
    }

    private void update() {
        Tile t = getQsTile();
        if (t == null) return;
        t.setIcon(Icon.createWithResource(this, R.drawable.ic_notif));
        t.setLabel("Shadow");
        t.setState(ShieldPrefs.running(this) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.updateTile();
    }
}
