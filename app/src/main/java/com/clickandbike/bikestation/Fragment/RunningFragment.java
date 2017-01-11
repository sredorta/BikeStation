package com.clickandbike.bikestation.Fragment;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.clickandbike.bikestation.Activity.RunningActivity;
import com.clickandbike.bikestation.DAO.GPIO;
import com.clickandbike.bikestation.DAO.ImageItem;
import com.clickandbike.bikestation.R;
import com.clickandbike.bikestation.Service.PollService;
import com.clickandbike.bikestation.Singleton.Locker;

/**
 * Created by sredorta on 12/19/2016.
 */
public class RunningFragment extends Fragment {
    private static final int ADS_INTERVAL = 1000*10; // 6 seconds
    private static Handler handler = new Handler();
    private Locker mLocker = Locker.getLocker();
    private ImageItem lastImage;
    private Integer lastImageIndex = 0;
    private ImageView myAdsImage;
    private int myAdsImageXorigin;
    public String mAction="test";
    public boolean isActive = false;
    public BroadcastReceiver pollServiceReceiver;
    TextView myText;
    public final GPIO myLED = new GPIO(36);
    Intent pollServiceIntent;
    // Constructor
    public static RunningFragment newInstance() {
        return new RunningFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_running, container, false);
        myText = (TextView) v.findViewById(R.id.textView);
        myAdsImage = (ImageView) v.findViewById(R.id.fragment_running_imageAds);

//        DisplayMetrics dm = new DisplayMetrics();
//        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
 //       int leftOffset = dm.widthPixels - v.getMeasuredWidth();
 //       Toast.makeText(getActivity(),"Offset is" + leftOffset,Toast.LENGTH_SHORT).show();
        int posXY[] = new int[2];
        int offsetXY[] = new int[2];
        myAdsImage.getLocationOnScreen(posXY);
        v.getLocationOnScreen(offsetXY);
        myAdsImageXorigin = posXY[0] - offsetXY[0];

        myText.setText("No data     ");
        myLED.activationPin();
        myLED.setInOut("out");
        startAds();
        startPolling();
        return v;
    }
    @Override
    public void onDestroy() {
        //Stop the Polling service
        getActivity().stopService(pollServiceIntent);
        getActivity().unregisterReceiver(pollServiceReceiver);
        super.onDestroy();
    }

    private void startAds() {
        //Define last image as 0
        lastImage = mLocker.lImages.get(lastImageIndex);
        myAdsImage.setImageBitmap(lastImage.getBitmap());

        handler.postDelayed(updateAds, ADS_INTERVAL);

    }
    //This is the code that will be generated
    private final Runnable updateAds = new Runnable() {
        @Override
        public void run() {
            ObjectAnimator leftOutAnimator = ObjectAnimator.ofFloat(this, "x", myAdsImageXorigin,-1000).setDuration(2000);
            ObjectAnimator leftInAnimator = ObjectAnimator.ofFloat(this, "x",  1000, myAdsImageXorigin).setDuration(2000);
            ObjectAnimator fadeOutAnimator = ObjectAnimator.ofFloat(this, "alpha", 1, 0).setDuration(2000);
            fadeOutAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {}

                @Override
                public void onAnimationEnd(Animator animator) {
                    //Do something after ADS_INTERVAL
                    lastImageIndex = lastImageIndex + 1;
                    if (lastImageIndex >= mLocker.lImages.size()) lastImageIndex = 0;
                    lastImage = mLocker.lImages.get(lastImageIndex);
                    myAdsImage.setImageBitmap(lastImage.getBitmap());
                }
                @Override
                public void onAnimationCancel(Animator animator) {}
                @Override
                public void onAnimationRepeat(Animator animator) {}
            });
            ObjectAnimator fadeInAnimator = ObjectAnimator.ofFloat(this, "alpha", 0, 1).setDuration(2000);
            fadeInAnimator.setStartDelay(3000);
            leftInAnimator.setStartDelay(2000);
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(fadeOutAnimator, leftOutAnimator, fadeInAnimator,leftInAnimator);
            animatorSet.setTarget(myAdsImage);
            animatorSet.start();

            //Same but better can be achieved with a ViewFlipper!!!!!!!!!

            //Do this forever !
            handler.postDelayed(this, ADS_INTERVAL);
        }};

    private void startPolling() {
        //Start service of polling on SQL
        //pollService.setServiceAlarm(getActivity(),true);
        pollServiceIntent = new Intent(getContext(),PollService.class);
        getActivity().startService(pollServiceIntent);
        pollServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

//                Log.i("SERGI", "onReceive new POLL data !");
                mAction = intent.getStringExtra("action");
                if (mAction.equals("on")) myLED.setState(1);
                if (mAction.equals("off")) myLED.setState(0);
//                Log.i("SERGI", "onReceive action:" + mAction);
                myText.setText(mAction);
//                Toast.makeText(getActivity(),"Recieved data: " + mAction, Toast.LENGTH_SHORT).show();
            }
        };
        //Register the receiver
        getActivity().registerReceiver(pollServiceReceiver, new IntentFilter(
                PollService.BROADCAST_ACTION));
    }
}
