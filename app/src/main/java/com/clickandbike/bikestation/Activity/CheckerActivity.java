package com.clickandbike.bikestation.Activity;

import android.support.v4.app.Fragment;

import com.clickandbike.bikestation.Fragment.CheckerFragment;


public class CheckerActivity extends SingleFragmentActivity {
    @Override
    public Fragment createFragment() {
        return CheckerFragment.newInstance();
    }

}
