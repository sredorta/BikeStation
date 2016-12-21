package com.clickandbike.bikestation.Singleton;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sredorta on 11/10/2016.
 */
/* Locker class (Singleton)
    This is the class for one locker and provides locker info
    Each Lock is an object Lock
 */
public class Locker {
    //Unique instance of this class (Singleton)
    private static Locker mLocker = new Locker();

    private String mName;
    private String mAction = "nothing";
    private static final int LockerCapacity = 10;
    private static int LockerAvail = 10;

    //Stores the GPS location of the locker
    private Location mLockerLocation = null;
    private boolean mLockerGpsLocated = false;
    //Stores Internet connectivity of the Locker
    private boolean mLockerConnected = false;
    //Stores if GPIO is ok
    private boolean mLockerGpioAlive = false;
    //Defines if PHP server is running
    private boolean mLockerCloudAlive = false;

    //Private constructor to avoid external calls
    private Locker() {
    }

    //Method to get the only instance of Locker
    public static Locker getLocker() {
        return mLocker;
    }

    //Inits the singleton (to be called only once in the app !)
    public void init() {
        List<Lock> locks = new ArrayList<>();
        for (int i = 0; i < LockerCapacity; i++) {
            locks.add(new Lock(i));
        }
    }


    public void setAction(String action) {
        mAction = action;
    }

    public String getAction() {
        return mAction;
    }

    //Set the name
    public void setLockerName(String name) {
        mName = name;
    }

    public String getLockerName() {
        return mName;
    }

    //Get GPS location of the locker
    public Location getLockerLocation() {
        return mLockerLocation;
    }

    //Sets the GPS location of the locker
    public void setLockerLocation(Location location) {
        mLockerLocation = location;
    }

    public boolean isInternetConnected() {
        return mLockerConnected;
    }

    public void setInternetConnected(boolean isConnected) {
        mLockerConnected = isConnected;
    }


    public void setIsGpsLocated(boolean isGpsLocated) {
        mLockerGpsLocated = isGpsLocated;
    }

    public boolean isGpsLocated() {
        return mLockerGpsLocated;
    }

    public boolean isCloudAlive() {
        return mLockerCloudAlive;
    }

    public void setCloudAlive(boolean isAlive) {
        mLockerCloudAlive = isAlive;
    }

    public boolean isGpioAlive() {
        return mLockerGpioAlive;
    }

    public void setGpioAlive(boolean isAlive) {
        mLockerGpioAlive = isAlive;
    }
    // Private class to store each Lock data
    private class Lock {
        private boolean mLocked;
        private boolean mAvailable;
        private int mLockId;

        //Handles if locker is locked
        public boolean isLocked() {
            return mLocked;
        }

        public void setLocked(boolean locked) {
            mLocked = locked;
        }

        //Handles if locker is available
        public boolean isAvailable() {
            return mAvailable;
        }

        public void setAvailable(boolean available) {
            mAvailable = available;
        }

        //Handles if locker is locked
        public int getLockId() {
            return mLockId;
        }

        public void setLockId(int id) {
            mLockId = id;
        }

        //Creates a Lock with ID
        public Lock(int id) {
            mLockId = id;
        }

        //Opens the Lock door
        public void open(int id) {

        }

        //Closes the Lock door
        public void close(int id) {

        }

    }

}