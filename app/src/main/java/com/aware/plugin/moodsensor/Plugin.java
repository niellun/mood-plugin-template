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

public class Plugin extends Aware_Plugin  {

    private AlarmManager alarmManager;
    private PendingIntent pendingAlarmIntent;

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
        registerReceiver(moodListener, esm_filter);

        //Start alarm manager
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, Plugin.class);
        alarmIntent.putExtra("WAKEUP", true);
        pendingAlarmIntent = PendingIntent.getService(
                getApplicationContext(),
                0,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                100,
                1000 * 30, pendingAlarmIntent);

        //Activate plugin
        Aware.startPlugin(this, getPackageName());

        //Apply settings in AWARE
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));

    }

    private static MoodSensorListener moodListener = new MoodSensorListener();

    public static class MoodSensorListener extends BroadcastReceiver {

        public int ESMDone = 1000;

        @Override
        public void onReceive(Context context, Intent intent) {
            //DO STUFF < 15s

            // NOTIFY THAT ESM APPROVED
            Log.d("ESM", "DONE");

            ESMDone++;
        }
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Check if the user has toggled the debug messages
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        // CHEK PREVIOUS ESM

        // SHOW ESM
        if(intent.hasExtra("WAKEUP") && moodListener.ESMDone>=3)
        {
            Log.d("ALARM", "SHOW ESM");
            moodListener.ESMDone = 0;
            showEsm();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void showEsm() {
        //Define the ESM to be displayed
        String esmString = "[{'esm': {\n" +
                "'esm_type': 4,\n" +
                "'esm_title': 'ESM Likert',\n" +
                "'esm_instructions': 'How happy are you?',\n" +
                "'esm_likert_max': 5,\n" +
                "'esm_likert_max_label': 'Happy',\n" +
                "'esm_likert_min_label': 'Sad',\n" +
                "'esm_likert_step': 1,\n" +
                "'esm_submit': 'OK',\n" +
                "'esm_expiration_threashold': 600,\n" +
                "'esm_trigger': 'AWARE Tester'\n" +
                "}}," +
                "{'esm': {\n" +
                "'esm_type': 4,\n" +
                "'esm_title': 'ESM Likert',\n" +
                "'esm_instructions': 'How angry are you?',\n" +
                "'esm_likert_max': 5,\n" +
                "'esm_likert_max_label': 'Angry',\n" +
                "'esm_likert_min_label': 'Calm',\n" +
                "'esm_likert_step': 1,\n" +
                "'esm_submit': 'OK',\n" +
                "'esm_expiration_threashold': 600,\n" +
                "'esm_trigger': 'AWARE Tester'\n" +
                "}},{'esm': {\n" +
                "'esm_type': 4,\n" +
                "'esm_title': 'ESM Likert',\n" +
                "'esm_instructions': 'How in love are you?',\n" +
                "'esm_likert_max': 5,\n" +
                "'esm_likert_max_label': 'Lonely',\n" +
                "'esm_likert_min_label': 'In love',\n" +
                "'esm_likert_step': 1,\n" +
                "'esm_submit': 'OK',\n" +
                "'esm_expiration_threashold': 600,\n" +
                "'esm_trigger': 'AWARE Tester'\n" +
                "}}]";

        //Queue the ESM to be displayed when possible
        Intent esm = new Intent(ESM.ACTION_AWARE_QUEUE_ESM);
        esm.putExtra(ESM.EXTRA_ESM, esmString);
        sendBroadcast(esm);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_PLUGIN_MOODSENSOR, false);

        //Deactivate any sensors/plugins you activated here
        //e.g., Aware.setSetting(this, Aware_Preferences.STATUS_ACCELEROMETER, false);

        //Unregister receiver
        unregisterReceiver(moodListener);

        //Stop alarm
        alarmManager.cancel(pendingAlarmIntent);

        //Stop plugin
        Aware.stopPlugin(this, getPackageName());
    }
}
