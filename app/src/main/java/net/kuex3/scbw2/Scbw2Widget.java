package net.kuex3.scbw2;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static java.lang.String.format;

/**
 * Implementation of App Widget functionality.
 */
public class Scbw2Widget extends AppWidgetProvider {

    public static final String TAG = "SCBW2";

    public static final String ACTION_UPDATE = Scbw2Widget.class.getName() + ".UPDATE";

    public static final String ACTION_CLICK = Scbw2Widget.class.getName() + ".CLICK";

    public static final String DATA_NAME = TAG + "_DATA";

    private static final long INTERVAL = 10000;

    private static Set<Integer> click = null;

    private String[][] DEFAULT = {
            {"Asia/Tokyo", Locale.JAPAN.getLanguage(), "white"},
            {"Europe/Berlin", Locale.GERMANY.getLanguage(), "white"},
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        try {
            for (int id : appWidgetIds) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.scbw2_widget);
                Intent intent = new Intent(context, this.getClass());
                intent.setAction(ACTION_CLICK);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
                views.setOnClickPendingIntent(R.id.main_layout, PendingIntent.getBroadcast(context, id, intent, FLAG_UPDATE_CURRENT));
                appWidgetManager.updateAppWidget(id, views);
            }

            Scbw2Receiver.init(context.getApplicationContext());

            updateClock(context);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive:" + intent.getAction());
        super.onReceive(context, intent);
        try {
            if (ACTION_UPDATE.equals(intent.getAction())) {
                String battery = intent.getStringExtra(Scbw2Receiver.BATTERY);
                if (battery != null && battery.length() > 0) {
                    updateBattery(context, battery);
                } else {
                    updateClock(context);
                }
            } else if (ACTION_CLICK.equals(intent.getAction())) {
                if (click == null) {
                    click = new HashSet<>();
                }
                final Integer id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (click.contains(id)) {
                    Log.d(TAG, "double click");
                    // TODO
                    // Intent settings = new Intent(context, SettingsActivity.class);
                    // context.getApplicationContext().startActivity(settings);
                } else {
                    Log.d(TAG, "single click");
                    click.add(id);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "reset click");
                            click.remove(id);
                        }
                    }, 1000);
                }
            }
            Scbw2Receiver.init(context.getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @SuppressLint("DefaultLocale")
    private void updateClock(Context context) {
        Log.d(TAG, "updateClock");
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            for (int id : appWidgetManager.getAppWidgetIds(new ComponentName(context, Scbw2Widget.class))) {

                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.scbw2_widget);
                SharedPreferences sharedPreferences = context.getSharedPreferences(DATA_NAME, Context.MODE_PRIVATE);

                Date now = new Date();

                for (int i = 1; i <= DEFAULT.length; i++) {
                    TimeZone timeZone = TimeZone.getTimeZone(sharedPreferences.getString(format("timezone%d", i), DEFAULT[i - 1][0]));
                    Locale locale = new Locale(sharedPreferences.getString(format("locale%d", i), DEFAULT[i - 1][1]));
                    int color = Color.WHITE;
                    try {
                        color = Color.parseColor(sharedPreferences.getString(format("color%d", i), DEFAULT[i - 1][2]));
                    } catch (Exception e) {
                        Log.w(TAG, e.getMessage(), e);
                    }

                    int tzid = context.getResources().getIdentifier(format("tz%d_textView", i), "id", context.getPackageName());
                    views.setTextViewText(tzid, timeZone.getID());
                    views.setInt(tzid, "setTextColor", color);

                    SimpleDateFormat hms = new SimpleDateFormat("HH:mm", locale);
                    hms.setTimeZone(timeZone);
                    int hmid = context.getResources().getIdentifier(format("hm%d_textView", i), "id", context.getPackageName());
                    views.setTextViewText(hmid, hms.format(now));
                    views.setInt(hmid, "setTextColor", color);

                    SimpleDateFormat mde = new SimpleDateFormat("MM/dd(E)", locale);
                    mde.setTimeZone(timeZone);
                    int mdeid = context.getResources().getIdentifier(format("mde%d_textView", i), "id", context.getPackageName());
                    views.setTextViewText(mdeid, mde.format(now) + (timeZone.inDaylightTime(now) ? " s" : ""));
                    views.setInt(mdeid, "setTextColor", color);
                }
                appWidgetManager.updateAppWidget(id, views);
                Log.d(TAG, "updateAppWidget:" + id);
            }

            Intent intent = new Intent(context, this.getClass());
            intent.setAction(ACTION_UPDATE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            long time = Calendar.getInstance().getTimeInMillis();
            time = time + INTERVAL - (time % INTERVAL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC, time, pendingIntent);
                Log.d(TAG, "setExact:" + time);
            } else {
                alarmManager.set(AlarmManager.RTC, time, pendingIntent);
                Log.d(TAG, "set:" + time);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void updateBattery(Context context, String battery) {
        Log.d(TAG, "updateBattery:" + battery);
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.scbw2_widget);
            views.setTextViewText(R.id.battery_textView, battery);
            appWidgetManager.updateAppWidget(new ComponentName(context, Scbw2Widget.class), views);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}
