package com.clickandbike.bikestation.Singleton;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;

import com.clickandbike.bikestation.DAO.ImageItem;

import java.io.File;
import java.io.FileDescriptor;
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
    private static Boolean DEBUG_MODE = true;
    private static final String TAG = "Locker::";

    //Unique instance of this class (Singleton)
    private static Locker mLocker = new Locker();

    public static String lName;            // Name of the locker
    public static String lTable;           // SQL table of the locker
    public static int    lCapacity;        // Capacity of the locker

    public static Location lLocation;      // Location of the locker
    public static boolean lStatusNetwork;  // Status of the Network
    public static boolean lStatusCloud;    // Status of the Cloud
    public static boolean lStatusGPS;      // Status of the GPS
    public static boolean lStatusGPIO;     // Status of the GPIO

    //Images handler
    private static final String IMAGES_DIR = "imagesDir";
    public static File lImagesPath;
    public static List<ImageItem> lImages;


    //Private constructor to avoid external calls
    private Locker() {
    }

    //Method to get the only instance of Locker
    public static Locker getLocker() {
        return mLocker;
    }

    //Inits the singleton (to be called only once in the app !)
    public void init(Context context) {
        Locker.lName = "station222";
        Locker.lTable = "stations";
        Locker.lCapacity = 10;
        Locker.lLocation = null;
        Locker.lStatusNetwork = false;
        Locker.lStatusCloud = false;
        Locker.lStatusGPIO = false;
        Locker.lStatusGPS = false;
        Locker.lImages = new ArrayList<>();
        ContextWrapper cw = new ContextWrapper(context);
        Locker.lImagesPath = cw.getDir(IMAGES_DIR, Context.MODE_PRIVATE);
        if (DEBUG_MODE) Log.i(TAG, "Images stored in path: " + Locker.lImagesPath.getAbsolutePath());

        //Create the locks arrayList
        List<Lock> locks = new ArrayList<>();
        for (int i = 0; i < lCapacity; i++) {
            locks.add(new Lock(i));
        }
    }

    //Remove images from Disk
    // if id == "all" -> remove all images
    // if id == "27" -> remove only image with id == 27
    public static void removeImagesFromDisk(String id) {
        if (id.equals("all")) {
            File mypath = Locker.lImagesPath;
            if (mypath.exists()) {
                if (DEBUG_MODE) Log.i(TAG, "Removing folder :" + Locker.lImagesPath);
                deleteDir(mypath);
            }
            //Reset the lImages list
            Locker.lImages = new ArrayList<>();
        } else {
            for (ImageItem item : Locker.lImages) {
                if (item.getId().equals(id)) {
                    //Remove the file and also remove the object from the list
                    File myFile = new File(item.getPath());
                    myFile.delete();
                    Locker.lImages.remove(item);
                    if (DEBUG_MODE) Log.i(TAG, "Removing image file :" + item.getPath());
                }
            }
        }
    }

    // delete directory and contents
    private static void deleteDir(File file) {
        if (file.isDirectory())
            for (String child : file.list())
                deleteDir(new File(file, child));
        file.delete();  // delete child file or empty directory
        if (DEBUG_MODE) Log.i(TAG, "Removing file: " + file.getAbsolutePath() );
    }

    //Saves all downloaded images to disk and removes first the folder to start with fresh data
    // id = all -> saves all images
    // id = "27" -> saves only the image 27
    public static boolean saveImagesToDisk(String id) {
        //Now save to files all images
        for (ImageItem image : Locker.lImages ) {
            if (id.equals("all") || id.equals(image.getId())) {
                if (!image.saveToFile(Locker.lImagesPath)) {
                    Log.i(TAG, "Error saving image :" + image.getId());
                    return false;
                } else {
                    if (DEBUG_MODE) Log.i(TAG, "Loaded image :" + image.getId());
                }
            }
        }
        return true;
    }

    //Loads all downloaded images from disk
    // Loads all images from file to mBitmap field
    // id = all -> loads all images
    // id = "27" -> loads only the image 27
    public static boolean loadImagesfromDisk(String id) {
        for (ImageItem image : Locker.lImages ) {
            if (id.equals("all") || id.equals(image.getId())) {
                if (!image.loadFromFile()) {
                    Log.i(TAG, "Error loading image :" + image.getId());
                    return false;
                } else {
                    if (DEBUG_MODE) Log.i(TAG, "Loaded image :" + image.getId());
                }
            }
        }
        return true;
    }

/*
    public void setAction(String action) {
        mAction = action;
    }

    public String getAction() {
        return mAction;
    }


    //Get GPS location of the locker
    public Location getLockerLocation() {
        return lLocation;
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
    */
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