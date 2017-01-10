package com.clickandbike.bikestation.Fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.clickandbike.bikestation.DAO.GPIO;
import com.clickandbike.bikestation.R;
import com.clickandbike.bikestation.Service.PollService;

/**
 * Created by sredorta on 12/19/2016.
 */
public class RunningFragment extends Fragment {
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
        myText.setText("No data     ");
        myLED.activationPin();
        myLED.setInOut("out");
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
