package com.clickandbike.bikestation.Fragment;

import android.animation.Animator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.clickandbike.bikestation.Activity.RunningActivity;
import com.clickandbike.bikestation.DAO.CloudFetchr;
import com.clickandbike.bikestation.DAO.GPIO;
import com.clickandbike.bikestation.R;
import com.clickandbike.bikestation.Service.GpsService;
import com.clickandbike.bikestation.Singleton.Locker;
import com.clickandbike.bikestation.View.ImageViewChecker;


/*
iBikeStationFragment
    onCreate:
        1)Starts a service to check for GPS location
        2)Starts a task to check if Internet is available
        3)
        x)Waits some time and check status of everything for moving forward
 */
public class CheckerFragment extends Fragment {
    private static Boolean DEBUG_MODE = true;
    private static final String TAG ="iBikeStationFragment::";
    //Name of the Locker as registered in the SQL server
    public static final String LOCKER_NAME = "station2";

    public BroadcastReceiver GpsServiceReceiver;
    private FetchCloudTask task;
    public Locker mLocker;
    private View mSceneView;

    // Constructor
    public static CheckerFragment newInstance() {
        return new CheckerFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Declare a new Locker Singleton
        mLocker = Locker.getLocker();
        mLocker.init();
        mLocker.setLockerName(LOCKER_NAME);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_checker, container, false);
        mSceneView = v;
        final ImageViewChecker mSettingsView = (ImageViewChecker) v.findViewById(R.id.imageSettings);
        final ImageViewChecker mNetworkView = (ImageViewChecker) v.findViewById(R.id.imageNetwork);
        final ImageViewChecker mCloudView = (ImageViewChecker) v.findViewById(R.id.imageCloud);
        final ImageViewChecker mGpioView = (ImageViewChecker) v.findViewById(R.id.imageGpio);
        final ImageViewChecker mGpsView = (ImageViewChecker) v.findViewById(R.id.imageGps);


        //Start checking all parameters
        checkSettings();
        updateGpsLocation();
        CloudFetchr.setDebugMode(true);
        checkInternetConnectivity();
        checkGPIO();

        checkCloudConnectivity();
//        updateCloud();

        //Define all views of ImageViewCheckers and start animations
        mGpsView.loadBitmapAsset("gps.png");
        mNetworkView.loadBitmapAsset("network.png");
        mCloudView.loadBitmapAsset("cloud.png");
        mSettingsView.loadBitmapAsset("settings.png");
        mGpioView.loadBitmapAsset("gpio.png");
        mSettingsView.setIterations(4);
        mNetworkView.setIterations(8);
        mCloudView.setIterations(12);
        mGpioView.setIterations(16);
        mGpsView.setIterations(20);

        mSettingsView.startAnimation();
        Animator test = mGpsView.startAnimation();
        mGpioView.startAnimation();
        mNetworkView.startAnimation();
        mCloudView.startAnimation();


        test.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                startRunningActivity();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });


        return v;
    }
    //Run now the main activity
    public void startRunningActivity() {
        if (mLocker.isInternetConnected() && mLocker.isCloudAlive()) { //Missing here GPS for now
            Activity myActivity = getActivity();

            Toast.makeText(getActivity(), "All params ok !", Toast.LENGTH_LONG).show();
            Intent i = RunningActivity.newIntent(getActivity(), "test");
            startActivity(i);

        } else {
            Toast.makeText(getActivity(), "Parameters invalid ! ", Toast.LENGTH_LONG).show();
        }
    }



    @Override
    public void onDestroy() {
        //Unregistering the GPS service in case no location was got
        if (GpsServiceReceiver != null) {
            getActivity().unregisterReceiver(GpsServiceReceiver);
            Toast.makeText(getActivity(),"Destroyed GPS service !", Toast.LENGTH_SHORT).show();
        }
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    //Checks status of wifi/gps...
    public void checkSettings() {
        WifiManager wifiManager = (WifiManager)this.getContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
    }


    //Checks that we have root access and that GPIO works
    public void checkGPIO() {
        mLocker.setGpioAlive(GPIO.isSuperUserAvailable());
    }

    /*  updateGpsLocation
                Starts a GpsService service that will provide gps coords once stable
                Once gps Coords are available the service is stopped
         */
    private void updateGpsLocation() {
        GpsService.setDebugMode(true);
        //Get current GPS Location and update it
        final Intent GpsServiceIntent = GpsService.newIntent(getActivity());
        getActivity().startService(GpsServiceIntent);
        //Temp workaround while no antenna is done
        Location loc = new Location("DummyLocation");
        loc.setLongitude(7.1496);
        loc.setLatitude(43.722);
        mLocker.setLockerLocation(loc);
        mLocker.setIsGpsLocated(true);
        setLocation();
        // Create a BroadcastReceiver that receives the data from intent of GpsService Service
        GpsServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("SERGI", "onReceive new GPS data !");
                Location loc = new Location("DummyProvider");
                //Update the location
                if (!Boolean.parseBoolean(intent.getStringExtra("isLocationValid"))) {
                    //mLocker.setIsGpsLocated(false);
                    //Need to build antenna so for now patch it temporarily
                    mLocker.setIsGpsLocated(true);
                } else {
                    loc.setLongitude(Double.parseDouble(intent.getStringExtra("longitude")));
                    loc.setLatitude(Double.parseDouble(intent.getStringExtra("latitude")));
                    mLocker.setLockerLocation(loc);
                    mLocker.setIsGpsLocated(true);
                    setLocation();
                    Log.i("SERGI", "Lon: " + loc.getLongitude());
                    Log.i("SERGI", "Stopped service !");
                }
                getActivity().stopService(GpsServiceIntent);
                getActivity().unregisterReceiver(this);
            }
        };
        //Register the receiver
        getActivity().registerReceiver(GpsServiceReceiver, new IntentFilter(
                GpsService.BROADCAST_ACTION));
    }




    // Connects to the server
    private void checkInternetConnectivity() {
        //Start an async task to check if we can connect to the server
        task = new FetchCloudTask("isInternetConnected");
        task.execute();
    }

    // Connects to the server
    private void checkCloudConnectivity() {
        //Start an async task to check if we can connect to the server
        task = new FetchCloudTask("isCloudConnected");
        task.execute();
    }

    private void setLocation() {
        //Update fields of location in the SQL db
        task = new FetchCloudTask("setLocation");
        task.execute();
    }


    //Get data from website
    private class FetchCloudTask extends AsyncTask<Void,Void,Boolean> {
        private String mQuery;

        public FetchCloudTask(String query) {
            mQuery = query;
        }


        @Override
        protected Boolean doInBackground(Void... params) {
            String longitude;
            String latitude;
            Log.i("ASYNC:", "doInBackground");
            switch (mQuery) {
                case "isCloudConnected":
                    Log.i("ASYNC:", "We are in isCoudConnected");
                    return (new CloudFetchr().isCloudConnected());
                case "isInternetConnected":
                    if (DEBUG_MODE) Log.i(TAG,"We are in isInternetConnected");
                    return (new CloudFetchr().isNetworkConnected());
                case "setLocation":
                    Log.i("ASYNC:", "We are in setLocation");
                    if (mLocker.isGpsLocated()) {
                        longitude = String.valueOf(mLocker.getLockerLocation().getLongitude());
                        latitude = String.valueOf(mLocker.getLockerLocation().getLatitude());
                    } else {
                        longitude = "not_available";
                        latitude= "not_available";
                    }
                    return (new CloudFetchr().setLocation(longitude,latitude));
                default:
                    return (new CloudFetchr().isCloudConnected());
            }

        }

        @Override
        protected void onPostExecute(Boolean isConnected) {
            if (DEBUG_MODE) Log.i(TAG , "AsyncTask postExec, success = " + isConnected);
            switch (mQuery) {
                case "isCloudConnected":
                    mLocker.setCloudAlive(isConnected);
                    break;
                case "isInternetConnected":
                    if (DEBUG_MODE) Log.i(TAG,"We are in isInternetConnected and setting result to :" + isConnected);
                    mLocker.setInternetConnected(isConnected);
                case "setLocation":
                    Log.i("SERGI:CLOUD:", "AsyncTask postExec for setLocation, success = " + isConnected);
                    break;
                default:
                    //Do nothing
            }
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
} //End of Class
