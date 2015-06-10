package com.aware.plugin.moodsensor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.utils.Aware_Plugin;

import java.util.Calendar;

public class Plugin extends Aware_Plugin implements BroadcastReceiver {

    private AlarmManager alarmManager;

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "AWARE::"+getResources().getString(R.string.app_name);
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        //Initialize our plugin's settings
        if( Aware.getSetting(this, Settings.STATUS_PLUGIN_MOODSENSOR).length() == 0 ) {
            Aware.setSetting(this, Settings.STATUS_PLUGIN_MOODSENSOR, true);
        }

        //Activate programmatically any sensors/plugins you need here
        //e.g., Aware.setSetting(this, Aware_Preferences.STATUS_ACCELEROMETER,true);

        //Any active plugin/sensor shares its overall context using broadcasts
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
            }
        };

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        //DATABASE_TABLES = Provider.DATABASE_TABLES
        //TABLES_FIELDS = Provider.TABLES_FIELDS
        //CONTEXT_URIS = new Uri[]{ Provider.Table_Data.CONTENT_URI }

        //Activate ESM
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);

        IntentFilter esm_filter = new IntentFilter();
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_ANSWERED);
        registerReceiver(this, esm_filter);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        //Activate plugin
        Aware.startPlugin(this, getPackageName());

        //Apply settings in AWARE
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));

        //Show ESM

    }

    private void scheduleMorningQuestionnaire() {
        Intent alarmIntent = new Intent( this, Plugin.class );

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                AlarmManager.INTERVAL_HALF_HOUR,
                AlarmManager.INTERVAL_HOUR, alarmIntent);

        morningIntent = PendingIntent.getBroadcast(getApplicationContext(), morningIntentRC, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), morningIntent); //use WEEKLY_INTENT_RC, so this gets overwritten in case we call this one twice...
        Log.d(TAG, "Set get next bid alarm for :" + cal.getTimeInMillis());
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Check if the user has toggled the debug messages
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_PLUGIN_MOODSENSOR, false);

        //Deactivate any sensors/plugins you activated here
        //e.g., Aware.setSetting(this, Aware_Preferences.STATUS_ACCELEROMETER, false);

        //Unregister receiver
        unregisterReceiver(this);

        //Stop plugin
        Aware.stopPlugin(this, getPackageName());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Received broadcast
    }
}
