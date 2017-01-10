package com.clickandbike.bikestation.DAO;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.clickandbike.bikestation.Activity.CheckerActivity;
import com.clickandbike.bikestation.Singleton.Locker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sredorta on 1/9/2017.
 */
public class ImageItem {
    private static Boolean DEBUG_MODE = true;
    private static final String TAG = "ImageItem::";
    private String mExtension;                          //Contains the extension of the file transferred
    private String mPath;                               //Local path of the downloaded file
    private Bitmap mBitmap;                             //Stores the bitmap of the image once reloaded from disk

    //Map JSON strings to our variables
    @SerializedName("id")
    private String mId;

    @SerializedName("name")
    private String mName;

    @SerializedName("description")
    private String mDescription;

    @SerializedName("stream")
    private String mStream;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getPath() { return mPath;}

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getExtension() {
        return mExtension;
    }


    public void setBitmap(Bitmap bitmap) {
         mBitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    //Static method (only one per class) that parses JSon string to object
    public static ImageItem parseJSON(String response) {
        Gson gson = new GsonBuilder().create();
        ImageItem itemResponse = gson.fromJson(response, ImageItem.class);
        //JSONArray
        return itemResponse;
    }

    //Returns the base64 decoded stream
    public byte[] getStream() {
        return Base64.decode(this.parseBase64(),Base64.DEFAULT);
    }

    // Parses the mStream and returns the stream without header
    //     it extracts all info from the header
    private String parseBase64() {
        String streamHeaderFree;
        String streamHeaderOnly;
        String streamExtension;
        String pattern;
        Pattern r;
        Matcher m;

        //Remove the header first
        pattern = "^.*base64,";
        r = Pattern.compile(pattern);
        m = r.matcher(mStream);
        streamHeaderFree = m.replaceAll("");
        if (DEBUG_MODE) Log.i(TAG, "Stream without header : " + streamHeaderFree);
        //Get the header
        pattern = ";base64,.*$";
        r = Pattern.compile(pattern);
        m = r.matcher(mStream);
        streamHeaderOnly = m.replaceAll("");
        if (DEBUG_MODE) Log.i(TAG, "Header : " + streamHeaderOnly);

        //Get the file extension and save it into the object
        pattern = "^.*/";
        r = Pattern.compile(pattern);
        m = r.matcher(streamHeaderOnly);
        streamExtension = m.replaceAll("");
        if (DEBUG_MODE) Log.i(TAG, "Extension : " + streamExtension);

        this.mExtension = streamExtension;
        //Returns now the stream without header
        return streamHeaderFree;
    }

    //Writes to a local file the image of the object
    public boolean saveToFile(File directory) {
        byte[] bitmapBytes = this.getStream();
        final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        if (DEBUG_MODE) Log.i(TAG, "Saving to disk image id : " + this.getId() + " : size = " + bitmap.getByteCount());

        //item.setBitmap(bitmap);
        if (DEBUG_MODE) Log.i(TAG, "Using directory: " + directory.getAbsolutePath());
        //Create the directory if it doesn't exists
        directory.mkdir();

        // Create imageDir
        File mypath = new File(directory, "downloaded_" + this.getId() + ".jpeg");

        //Delete the file if exists
        if (mypath.exists()) mypath.delete();

        //Store this path into the object
        this.mPath = mypath.getAbsolutePath();

        if (DEBUG_MODE) Log.i(TAG, "Saving to disk image id : " + mypath.getAbsolutePath());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            //fos.write(bitmapBytes);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos); //100 : Max quality
            fos.flush();
            fos.close();

        } catch (Exception e) {
            Log.i(TAG, "Caught exception: " + e);
            return false;
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                Log.i(TAG, "Caught exception: " + e);
                return false;
            } catch (NullPointerException ne) {
                Log.i(TAG, "Caught exception: " + ne);
                return false;
            }
        }
        return true;
    }
    //Loads from the stored file the image into the mBitmap field
    public boolean loadFromFile() {
        if (DEBUG_MODE) Log.i(TAG, "Loading image from disk : " + this.mPath);
        //Check if file exists and readable
        File mypath = new File(this.mPath);
        if (!mypath.exists()) {
            Log.i(TAG, "Missing image file : " + this.mPath);
            return false;
        }
        if (!mypath.canRead()) {
            Log.i(TAG, "Could not read image from file : " + this.mPath);
            return false;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        this.mBitmap = BitmapFactory.decodeFile(this.mPath, options);

        return true;
    }

    //Compare two Image array lists
    public static List<ImageItem> compareImageArray(List<ImageItem> referenceList, List<ImageItem>  newList) {
        //Detect new Ids to download
        Boolean idFound;
        List<ImageItem> idToDownload = new ArrayList<>();
        for (ImageItem item : newList) {
            idFound = false;
            for (ImageItem itemLocker: referenceList) {
                if (item.getId().equals(itemLocker.getId())) {
                    idFound = true;
                    break;
                }
            }
            //Append to list of need to download items
            if (!idFound) {
                if (DEBUG_MODE) Log.i(TAG,"Found delta for id: " + item.getId());
                idToDownload.add(item);
            }
        }
        return idToDownload;
    }

}
