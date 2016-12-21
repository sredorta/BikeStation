package com.clickandbike.bikestation.Activity;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.clickandbike.bikestation.Fragment.CheckerFragment;
import com.clickandbike.bikestation.Fragment.RunningFragment;

public class RunningActivity extends SingleFragmentActivity {

    @Override
    public Fragment createFragment() {
        return RunningFragment.newInstance();
    }

    public static Intent newIntent(Context packageContext, String param) {
        Intent intent = new Intent(packageContext,RunningActivity.class);
        intent.putExtra("test", param);
        return intent;
    }

}
