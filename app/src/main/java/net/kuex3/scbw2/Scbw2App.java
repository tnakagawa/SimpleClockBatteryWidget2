package net.kuex3.scbw2;

import android.app.Application;
import android.util.Log;

/**
 * SimpleClockBatteryWidget2 Application Class
 */
public class Scbw2App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Scbw2Widget.TAG, "Scbw2App#onCreate");

        Scbw2Receiver.init(this);
    }
}
