package com.clickandbike.bikestation.Fragment;

import android.animation.Animator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.clickandbike.bikestation.Activity.CheckerActivity;
import com.clickandbike.bikestation.Activity.RunningActivity;
import com.clickandbike.bikestation.DAO.CloudFetchr;
import com.clickandbike.bikestation.DAO.GPIO;
import com.clickandbike.bikestation.DAO.ImageItem;
import com.clickandbike.bikestation.R;
import com.clickandbike.bikestation.Service.GpsService;
import com.clickandbike.bikestation.Singleton.Locker;
import com.clickandbike.bikestation.View.IconView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/*
iBikeStationFragment
    onCreate:
        1)Starts a service to check for GPS location
        2)Starts a task to check if Internet is available
        3)
        x)Waits some time and check status of everything for moving forward
 */
public class CheckerFragment extends Fragment {
    private static Boolean DEBUG_MODE = true;           // Enables/disables verbose logging
    private static final String TAG ="iBikeStationFragment::";
    private static final int CODE_CLOUD_CHECK = 0;      // Used for the async task to check cloud connection
    private static final int CODE_INTERNET_CHECK = 1;   // Used for the async task to check internet connection
    private static final int CODE_CLOUD_LOCATION = 2;   // Used for update location task
    private static final int CODE_SETTINGS_CHECK = 3;   // Used to check settings
    private static final int CODE_GPIO_CHECK = 4;       // Used to check GPIO access



    public BroadcastReceiver GpsServiceReceiver;
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
        mLocker.init(getContext());

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_checker, container, false);
        mSceneView = v;
        final IconView IconSettings = (IconView) v.findViewById(R.id.imageSettings);
        final IconView IconNetwork = (IconView) v.findViewById(R.id.imageNetwork);
        final IconView IconCloud = (IconView) v.findViewById(R.id.imageCloud);
        final IconView IconGPIO = (IconView) v.findViewById(R.id.imageGpio);
        final IconView IconGPS = (IconView) v.findViewById(R.id.imageGps);

        //Define all views of ImageViewCheckers and start animations
        IconGPS.loadBitmapAsset("gps.png");
        IconNetwork.loadBitmapAsset("network.png");
        IconCloud.loadBitmapAsset("cloud.png");
        IconGPIO.loadBitmapAsset("gpio.png");
        IconSettings.loadBitmapAsset("settings.png");

        IconSettings.setIterations(4);
        IconSettings.setDelay(0);
        IconNetwork.setIterations(8);
        IconNetwork.setDelay(100);
        IconCloud.setIterations(12);
        IconCloud.setDelay(200);
        IconGPIO.setIterations(16);
        IconGPIO.setDelay(300);
        IconGPS.setIterations(20);
        IconGPS.setDelay(400);

        //Only for debug !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        final Button button = (Button) v.findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CloudFetchr.setDebugMode(true);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(this.createRunnable());
                executor.shutdown();
            }

            // Create a runnable with the desired task to accomplish
            public Runnable createRunnable() {
                final Locker mLocker = Locker.getLocker();
                return new Runnable() {
                    @Override
                    public void run() {
                                Boolean myResult = false;
                                Log.i("SERGI:::", "Getting images delta !");
                                new CloudFetchr().getImagesDelta();

                    }
                };

            }
        });
        //***************************************************** End of for debug

        IconSettings.startAnimation();

        //Executes sequentially tasks
        CloudFetchr.setDebugMode(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(this.createRunnable(CODE_SETTINGS_CHECK, IconSettings));
        executor.execute(this.createRunnable(CODE_INTERNET_CHECK, IconNetwork));
        executor.execute(this.createRunnable(CODE_CLOUD_CHECK, IconCloud));
        executor.execute(this.createRunnable(CODE_GPIO_CHECK, IconGPIO));
        executor.shutdown();


        Animator test = IconGPS.startAnimation();
        IconGPIO.startAnimation();
        IconNetwork.startAnimation();
        IconCloud.startAnimation();
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
        if (Locker.lStatusNetwork) { //Missing here GPS for now and all other Status
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

    // Create a runnable with the desired task to accomplish
    public Runnable createRunnable(final int code, IconView v) {
        final IconView myIcon = v;
        final Locker mLocker = Locker.getLocker();

        return new Runnable() {
            @Override
            public void run() {
                Boolean myResult = false;
                switch (code) {
                    case CheckerFragment.CODE_SETTINGS_CHECK:
                        myResult = checkSettings();
                        myIcon.setResult(myResult);     //Set attribute to Icon
                        //mLocker.set
                        break;
                    case CheckerFragment.CODE_GPIO_CHECK:
                        myResult = GPIO.isSuperUserAvailable();
                        myIcon.setResult(myResult);  // set Attribute on icon
                        Locker.lStatusGPIO = myResult;                     //set Attribute on Locker singleton
                        break;
                    case CheckerFragment.CODE_CLOUD_CHECK :
                        Log.i("ASYNC:", "We are in isCoudConnected");
                        myResult = checkCloud();
                        myIcon.setResult(myResult);
                        Locker.lStatusCloud = myResult;
                        break;
                    case CheckerFragment.CODE_INTERNET_CHECK:
                        if (DEBUG_MODE) Log.i(TAG,"We are in isInternetConnected");
                        myResult = CloudFetchr.isNetworkConnected();
                        myIcon.setResult(myResult);
                        Locker.lStatusNetwork = myResult;
                        break;
                    case CheckerFragment.CODE_CLOUD_LOCATION:
                        Log.i("ASYNC:", "We are in setLocation");
                        if (Locker.lStatusGPS) {
                            //longitude = String.valueOf(mLocker.getLockerLocation().getLongitude());
                            //latitude = String.valueOf(mLocker.getLockerLocation().getLatitude());
                        } else {
                            //longitude = "not_available";
                            //latitude= "not_available";
                        }
                        //new CloudFetchr().setLocation(longitude,latitude);
                        break;
                    default:
                        new CloudFetchr().isCloudConnected();
                }
            }
        };

    }


    //Checks status of wifi/gps...
    public boolean checkSettings() {
        WifiManager wifiManager = (WifiManager)this.getContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        //Remove all downloaded images from disk so that we start in a clean way
        Locker.removeImagesFromDisk("all");

        return true;
    }

    //Checks that everything is ok in the cloud and adds station if does not exists
    public boolean checkCloud() {
        Boolean myResult = false;
        //Check server connectivity
        myResult = new CloudFetchr().isCloudConnected();
        if (!myResult) return myResult;

        //Check if station registered and if not register
        myResult = new CloudFetchr().isStationRegistered();
        if (!myResult) {
            Log.i(TAG, "Registering new station...");
            myResult = new CloudFetchr().registerStation();
            if (!myResult) return myResult;
        }
        //Download all images one by one and store them in the singleton
        myResult = new CloudFetchr().getImages("all");
        if (!myResult) {
            Log.i(TAG, "No images found...");
            return myResult;
        }
        Locker.saveImagesToDisk("all");
        Locker.loadImagesfromDisk("all");

        return myResult;
    }


    /*  updateGpsLocation
                Starts a GpsService service that will provide gps coords once stable
                Once gps Coords are available the service is stopped
         */
    /*
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
        //setLocation();
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
                    //setLocation();
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
*/


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }




} //End of Class
