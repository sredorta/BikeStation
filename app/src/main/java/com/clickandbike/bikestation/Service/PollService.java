package com.clickandbike.bikestation.Service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.clickandbike.bikestation.DAO.CloudFetchr;

/**
 * Created by sredorta on 11/29/2016.
 */

public class PollService extends Service {
    static final public String BROADCAST_ACTION = "com.clickandbike.bikestation.PollService";
    private static final String TAG = "PollService::";
    public static Context mContext;
    private static final int POLL_INTERVAL = 1000*2; // 6 seconds
    private static Handler handler = new Handler();

    //Constructor
    public PollService() {
        super();
        Log.i(TAG,"Created poll service !");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"Created poll service onCreate!");
        handler.postDelayed(sendData, POLL_INTERVAL);
    }



    //This is the code that will be generated
    private final Runnable sendData = new Runnable() {
        @Override
        public void run() {
            //Do something after POLL_INTERVAL
            String mAction="nothing ";
            Log.i(TAG, "Polling...");
            //mAction = new CloudFetchr().getAction();
            FetchCloudTask task = new FetchCloudTask();
            task.execute();
            handler.postDelayed(this, POLL_INTERVAL);
        }};


    //Get data from website
    private class FetchCloudTask extends AsyncTask<Void,Void,String> {
        public FetchCloudTask() {}

        @Override
        protected String doInBackground(Void... params) {
            Log.i(TAG,"Do in Background");
                    return (new CloudFetchr().getAction());
        }

        @Override
        protected void onPostExecute(String action) {
            Log.i(TAG,"Sending Poll Result Action : " + action);
            Intent myIntent = new Intent(BROADCAST_ACTION);
            myIntent.putExtra("action", action);
            sendBroadcast(myIntent);
        }
    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"Destroyed poll service !");
        handler.removeCallbacks(sendData);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
