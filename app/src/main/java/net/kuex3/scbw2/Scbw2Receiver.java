package net.kuex3.scbw2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class Scbw2Receiver extends BroadcastReceiver {

    public static final String BATTERY = "battery";

    private static Scbw2Receiver receiver = null;

    private Scbw2Receiver() {

    }

    public static void init(Context context) {
        if (receiver == null) {
            Log.d(Scbw2Widget.TAG, "new Scbw2Receiver");
            receiver = new Scbw2Receiver();

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            Intent intent = context.registerReceiver(receiver, filter);
            receiver.onReceive(context, intent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(Scbw2Widget.TAG, "Scbw2Receiver#onReceive:" + action);
        if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
            Intent updateIntent = new Intent(context, Scbw2Widget.class);
            updateIntent.setAction(Scbw2Widget.ACTION_UPDATE);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            String battery = "";
            if (scale > 0 && level > -1) {
                int per = (level * 100) / scale;
                battery = per + "%" + (plugged != 0 ? " c" : "");
                updateIntent.putExtra(BATTERY, battery);
            }
            Log.d(Scbw2Widget.TAG, "Scbw2Receiver#onReceive:battery>" + battery);
            context.sendBroadcast(updateIntent);
        }
    }
}