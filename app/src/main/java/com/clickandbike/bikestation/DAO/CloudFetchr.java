package com.clickandbike.bikestation.DAO;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.clickandbike.bikestation.Singleton.Locker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sredorta on 11/24/2016.
 */

//ser=id228014_sergi&password=HIB2oB2f

public class CloudFetchr {
    private static Boolean DEBUG_MODE = true;
    private static final String TAG = "CloudFetchr::";
    private Context mContext;
    private static final String URI_BASE_GOOGLE = "http://clients3.google.com/generate_204";    //Only required to check if internet is available
    private static final String URI_BASE = "http://10.0.2.2/example1/api/";
    private static final String PHP_CONNECTION_CHECK = "locker.connection.check.php";           // Params required : none
    private static final String PHP_STATION_CHECK = "locker.stations.check.php";                // Params required : name,table_stations
    private static final String PHP_STATION_ADD = "locker.stations.add.php";                    // Params required : name,table_stations + optional
    private static final String PHP_IMAGES_GET = "locker.images.get.php";                       // Params required : name,table_stations,type(stream_all,details_all,details_last)

    private static final String PHP_STATION_UPDATE = "locker.stations.update.php";              // Params required : name + latitude...
    private static final String PHP_STATION_REGISTERED = "db_station_registered.php";           // Params required : name
    private static final String PHP_STATION_STATUS_REQUEST = "db_station_status_request.php";   // Params required: name
    private static String SEND_METHOD = "POST";                                           // POST or GET method

    private static final String USER = "sergi";
    private static final String PASSWORD = "HIB2oB2f" ;

    private Locker mLocker = Locker.getLocker();


    //Handle Logs in Debug mode
    public static void setDebugMode(Boolean mode) {
        DEBUG_MODE = mode;
        if (DEBUG_MODE) Log.i(TAG, "Debug mode enabled !");
    }

    //We try to see if we can connect to google for example
    public static Boolean isNetworkConnected() {
        URL url = null;
        Uri ENDPOINT = Uri
                .parse(URI_BASE_GOOGLE)
                .buildUpon()
                .build();
        Uri.Builder uriBuilder = ENDPOINT.buildUpon();
        try {
            url = new URL(uriBuilder.toString());
            if (DEBUG_MODE) Log.i(TAG, "Trying to access: " + uriBuilder.toString());
        } catch(MalformedURLException e) {
            Log.i(TAG, "Malformed URL !");
        }
        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Android");
            connection.setRequestProperty("Connection", "close");
            connection.setConnectTimeout(1000);
            connection.connect();
            if (connection.getResponseCode() == 204 && connection.getContentLength() == 0) {
                if (DEBUG_MODE) Log.i(TAG, "Connected !");
                return true;
            }
            if (DEBUG_MODE) Log.i(TAG, "Not connected");
            return false;
        } catch (IOException e) {
            //Network not connected
            if (DEBUG_MODE) Log.i(TAG, "Caught IOE"+ e);
            return false;
        }
    }



    //Build http string besed on method and query
    private URL buildUrl(String Action,HashMap<String, String> params) {
        Uri ENDPOINT = Uri
                .parse(URI_BASE + Action)
                .buildUpon()
                .build();

        URL url = null;
        Uri.Builder uriBuilder = ENDPOINT.buildUpon();
        if (this.SEND_METHOD.equals("GET")) {
            //Add GET query parameters using the HashMap in the URL
            try {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    uriBuilder.appendQueryParameter(URLEncoder.encode(entry.getKey(), "utf-8"), URLEncoder.encode(entry.getValue(), "utf-8"));
                }
            } catch (UnsupportedEncodingException e) {
                // do nothing
            }
        }
        String result = uriBuilder.build().toString();
        try {
            url = new URL(result);
        } catch(MalformedURLException e) {
            //Do nothing
        }
        Log.i(TAG,"Final URL :" + url.toString());
        return url;
    }

    //Gets the data from the server and aditionally sends POST parameters if SEND_METHOD is set to POST
    private byte[] getURLBytes(URL url,HashMap<String,String> parametersPOST) throws IOException {

        HttpURLConnection connection;
        OutputStreamWriter request = null;
        byte[] response = null;
        JsonItem json = new JsonItem();  //json answer in case network not available


        json.setSuccess(false);
        json.setResult(false);
        try {
            connection = (HttpURLConnection) url.openConnection();
            //Required to enable input stream, otherwise we get EOF (When using POST DoOutput is required
            connection.setDoInput(true);
            if (this.SEND_METHOD.equals("POST")) connection.setDoOutput(true);
            connection.setReadTimeout(2000);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            connection.setRequestMethod(this.SEND_METHOD);
            connection.connect();

            //Write the POST parameters
            if (this.SEND_METHOD.equals("POST")) {
                OutputStream os = connection.getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
                Log.i(TAG, "POST HEADER : " + getPostDataString(parametersPOST));
                writer.write(getPostDataString(parametersPOST));
                writer.flush();
                writer.close();
                os.close();
            }

            switch(connection.getResponseCode())
            {
                case HttpURLConnection.HTTP_OK:
                    Log.i(TAG, "Connected !");
                    break;
                case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                    Log.i(TAG, "Timout !");
                    json.setMessage("ERROR: Server timeout !");
                    break;
                case HttpURLConnection.HTTP_UNAVAILABLE:
                    Log.i(TAG, "Server not available");
                    json.setMessage("ERROR: Server not available !");
                    break;
                default:
                    Log.i(TAG, "Not connected  !");
                    json.setMessage("ERROR: Not connected to server !");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in            =  connection.getInputStream();
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer))>0) {
                out.write(buffer,0,bytesRead);
            }
            out.close();

            // Response from server after login process will be stored in response variable.
            response = out.toByteArray();
            // You can perform UI operations here
            //Log.i(TAG, "Message from Server: \n" + response);

        } catch (IOException e) {
            // Error
            Log.i(TAG, "Caught exception :", e);
        }
        // In case that response is null we output the json we have created
        if (response == null) {
            Log.i(TAG, "Error during access to server");
            response = json.encodeJSON().getBytes();
        }
        //Log.i(TAG, new String(response));
        return response;
    }

    //Get string data from URL
    public String getURLString(URL url,HashMap<String,String> parametersPOST) throws IOException {
        byte[] test =  getURLBytes(url,parametersPOST);
        if (test == null) {
            return "";
        } else {
            return new String(test);
        }
    }

    // Converts a HashMap of string parameter pairs into a string for POST send
    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException{
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }


    // Sends PHP request and returns JSON object
    private JsonItem getJSON(URL url,HashMap<String,String> parametersPOST){
        JsonItem item = new JsonItem();
        try {
            String jsonString = getURLString(url,parametersPOST);
            Log.i(TAG, "Received JSON:" + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            item = JsonItem.parseJSON(jsonBody.toString());
        } catch (JSONException je) {
            Log.i(TAG,"Failed to parse JSON", je);
            item.setSuccess(false);
            item.setResult(false);
            item.setMessage("ERROR: Failed to parse JSON !");
        } catch (IOException ioe) {
            item.setSuccess(false);
            item.setResult(false);
            item.setMessage("ERROR: Failed to fetch JSON !");
            Log.i(TAG,"Falied to fetch items !", ioe);
        }
        return item;
    }

    // Sends PHP request and returns JSON object
    private JsonItem getJSONImages(List<ImageItem> items, URL url, HashMap<String,String> parametersPOST) {
        JsonItem item = new JsonItem();
        try {
            String jsonString = getURLString(url, parametersPOST);
            Log.i(TAG, "Received JSON:" + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            item = JsonItem.parseJSON(jsonBody.toString());
            JSONArray photoJsonArray = jsonBody.getJSONArray("images");
            for (int i = 0; i < photoJsonArray.length(); i++) {
                JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
                ImageItem itemImage = ImageItem.parseJSON(photoJsonObject.toString());
                items.add(itemImage);
            }

        } catch (JSONException je) {
            Log.i(TAG, "Failed to parse JSON", je);
            item.setSuccess(false);
            item.setResult(false);
            item.setMessage("ERROR: Failed to parse JSON !");

        } catch (IOException ioe) {
            Log.i(TAG, "Falied to fetch items !", ioe);
            item.setSuccess(false);
            item.setResult(false);
            item.setMessage("ERROR: Failed to fetch JSON !");
        }
        return item;
    }
/*******************************************************************************************/

    public String getAction() {
        //Define the POST/GET parameters in a HashMap
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("name", Locker.lName);
        parameters.put("time_delta", String.valueOf(new Date().getTime()));

        URL url = buildUrl(PHP_STATION_STATUS_REQUEST,parameters);
        JsonItem networkAnswer = getJSON(url,parameters);
        return (networkAnswer.getAction());
    }

    public Boolean setLocation(String longitude,String latitude) {
        //Define the POST/GET parameters in a HashMap
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("name", Locker.lName);
        parameters.put("longitude", longitude);
        parameters.put("latitude", latitude);

        URL url = buildUrl(PHP_STATION_UPDATE,parameters);
        Log.i("POLL", url.toString());
        JsonItem networkAnswer = getJSON(url,parameters);
        return (networkAnswer.getSuccess());
    }


    public Boolean isCloudConnected() {
        //Define the POST/GET parameters in a HashMap
        this.SEND_METHOD="POST";
        HashMap<String, String> parameters = new HashMap<>();

        URL url = buildUrl(PHP_CONNECTION_CHECK,parameters);
        JsonItem networkAnswer = getJSON(url,parameters);
        return (networkAnswer.getResult());
    }

    //Checks if the station is registered
    public Boolean isStationRegistered() {
        this.SEND_METHOD="POST";
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("name", Locker.lName);
        parameters.put("table_stations", Locker.lTable);

        URL url = buildUrl(PHP_STATION_CHECK,parameters);
        JsonItem networkAnswer = getJSON(url,parameters);
        return (networkAnswer.getResult());
    }

    public Boolean registerStation() {
        this.SEND_METHOD="POST";
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("name", Locker.lName);
        parameters.put("table_stations", Locker.lTable);
        parameters.put("capacity", String.valueOf(Locker.lCapacity));

        URL url = buildUrl(PHP_STATION_ADD,parameters);
        JsonItem networkAnswer = getJSON(url,parameters);
        return (networkAnswer.getResult());
    }

    //Downloads all images and stores them in ImageItem List of Locker and in the disk
    // if id == "all" -> downloads all images
    // if id == "27" -> downloads image with id == 27 in the db
    public boolean getImages(String id) {
        List<ImageItem> mItems = new ArrayList<>();
        List<ImageItem> deltaItems = new ArrayList<>();

        this.SEND_METHOD="POST";
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("name", Locker.lName);
        parameters.put("table_stations", Locker.lTable);
        parameters.put("type", "stream_all");
        parameters.put("id", id);
        URL url = buildUrl(PHP_IMAGES_GET,parameters);
        JsonItem networkAnswer = getJSONImages(mItems,url,parameters);
        //We need now to refresh the Locker variable
        deltaItems = ImageItem.compareImageArray(Locker.lImages, mItems);
        for (ImageItem imageNew : deltaItems) {
            Locker.lImages.add(imageNew);
            Locker.saveImagesToDisk(imageNew.getId());
            Locker.loadImagesfromDisk(imageNew.getId());
            if (DEBUG_MODE) Log.i(TAG, "Added in Locker.lImages and stored to disk the image with id :" + imageNew.getId());
        }
        return (networkAnswer.getResult());
    }

    //Checks which images are already available and which images needs download and accordingly
    // removes/adds the new images
    public boolean getImagesDelta() {
        List<ImageItem> mItems = new ArrayList<>();
        List<ImageItem> deltaList = new ArrayList<>();

        this.SEND_METHOD="POST";
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("name", Locker.lName);
        parameters.put("table_stations", Locker.lTable);
        parameters.put("type", "details_all");

        URL url = buildUrl(PHP_IMAGES_GET,parameters);
        JsonItem networkAnswer = getJSONImages(mItems,url,parameters);
        if (!networkAnswer.getSuccess()) {
            Log.i(TAG, "Error while accessing to the server !");
            return false;
        }
         //Detect new Ids to download, download them and save them into disk and update Locker.lImages
        deltaList = ImageItem.compareImageArray(Locker.lImages,mItems);
        for (int i=0; i<deltaList.size(); i++) {
            Log.i(TAG, "Need to add :" + deltaList.get(i).getId());
            this.getImages(deltaList.get(i).getId());
        }

        //Detect Ids to remove
        //Detect new Ids to download
        deltaList = ImageItem.compareImageArray(mItems, Locker.lImages);
        for (int i=0; i<deltaList.size(); i++) {
            Log.i(TAG, "Need to remove :" + deltaList.get(i).getId());
            Locker.removeImagesFromDisk(deltaList.get(i).getId());
        }

        //Locker.lImages = mItems;
        return (networkAnswer.getResult());
    }








}

