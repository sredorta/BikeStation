package com.clickandbike.bikestation.DAO;

import android.util.Log;

import com.clickandbike.bikestation.Singleton.Locker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Created by sredorta on 12/1/2016.
 */
/* USAGE EXAMPLE
        GPIO.setDebugMode(true);
        Boolean isEnabled = GPIO.isSuperUserAvailable();
        if (isEnabled) {
        Toast.makeText(getApplicationContext(),"SuperUser on",Toast.LENGTH_SHORT).show();
        } else {
        Toast.makeText(getApplicationContext(),"SuperUser off",Toast.LENGTH_SHORT).show();
        }

        MyLed = new GPIO(36);


        MyLed.activationPin();
        MyLed.setInOut("out");
        MyLed.setState(1);
*/

public class GPIO {
    private String port;
    private int pin;
    private static String TAG_SHORT = "GPIO";
    private String TAG;
    private static Boolean DEBUG_MODE = true;
    private static final String NO_EXCEPTION = "no_exception";
    private Locker mLocker = Locker.getLocker();

    //Sets debug mode
    public static void setDebugMode(Boolean mode) {
        DEBUG_MODE = mode;
        if (DEBUG_MODE) Log.i(TAG_SHORT, "Debug mode enabled !");
    }

    //Constructor
    public GPIO(int pin){
        //Add dragonboard offset
        int pin2 = pin + 902;
        this.port = "gpio"+pin2;
        this.pin = pin2;
        //Add pin name in the TAG of debugging
        TAG = TAG_SHORT + "::" + pin + "::";
        if (DEBUG_MODE) Log.i(TAG,"Created GPIO: " + pin);
        if (DEBUG_MODE) Log.i(TAG,"Created GPIO file: " + this.port);
    }


    //This function checks if SuperUser is available
    public static boolean isSuperUserAvailable() {
        if (DEBUG_MODE) Log.i(TAG_SHORT, "Running isSuperUserAvailable");
        Result result = new Result();
        Boolean firstResult = false;
        //Check that we can export without having an issue
        result = runLinuxSUCommand("echo 938 > /sys/class/gpio/export");
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::success:: " + result.success.toString());
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::stdout:: " + result.stdout);
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::stderr:: " + result.stderr);
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::except:: " + result.exception);
        if ( result.exception.equals(NO_EXCEPTION)) {
            firstResult= true;
        }
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable:: firstResult = " + firstResult);
        // Now check that the exported file exists
        result = runLinuxSUCommand("ls -l /sys/class/gpio/gpio938");
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::success:: " + result.success);
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::stdout:: " + result.stdout);
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::stderr:: " + result.stderr);
        if (DEBUG_MODE) Log.i(TAG_SHORT, "isSuperUserAvailable::except:: " + result.exception);
        if ( result.stdout.length()>10 && firstResult) {
            return true;
        }
        return false;
    }

    // Runs SU shell (root access is required)
    private static Result runLinuxSUCommand(String command) {
        if (DEBUG_MODE) Log.i(TAG_SHORT, "Running runLinuxSUCommand: " + command);
        String[] commandFinal = {"su", "-c", command};
        return runLinuxCommand(commandFinal);
    }
    // Runs SH shell (no root access is required)
    private static Result runLinuxSHCommand(String command) {
        if (DEBUG_MODE) Log.i(TAG_SHORT, "Running runLinuxSHCommand: " + command);
        String[] commandFinal = {"sh", "-c", command};
        return runLinuxCommand(commandFinal);
    }

    // Runs SH shell (no root access is required)
    private static Result runLinuxCommand(String[] command) {
        if (DEBUG_MODE) Log.i(TAG_SHORT, "Running runLinuxCommand: ");
        Result result = new Result();
        String output = "";
        String outputErr = "";
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output = output.concat(line + "\n");
                if (DEBUG_MODE) Log.i(TAG_SHORT, "shell:>" + line);
            }
            reader.close();
            BufferedReader readerErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            line = "";
            while ((line = readerErr.readLine()) != null) {
                outputErr = outputErr.concat(line + "\n");
                if (DEBUG_MODE) Log.i(TAG_SHORT, "error:>" + line + "\n");
            }
            readerErr.close();
            process.waitFor();
            result.success =true;
            result.stdout = output;
            result.stderr = outputErr;
            result.exception = NO_EXCEPTION;
            if (outputErr.length() > 0 ) result.success = false;
        } catch (IOException e) {
            result.exception = e.toString();
            if (DEBUG_MODE) Log.i(TAG_SHORT, "runLinuxSUCommand got exception:" + e);
        } catch (InterruptedException e) {
            result.exception =e.toString();
            if (DEBUG_MODE) Log.i(TAG_SHORT, "runLinuxSUCommand got exception:" + e);
        }
        return result;
    }


    //set value of the output
    public boolean setState(int value) {
        String command = String.format("echo %d > /sys/class/gpio/%s/value\n", value,this.port);
        Result result = runLinuxSUCommand(command);
        return result.success;
    }

    // set direction
    public boolean setInOut(String direction){
        Result result = new Result();
        String command = String.format("echo %s > /sys/class/gpio/%s/direction\n", direction,this.port);
        result = runLinuxSUCommand(command);
        return result.success;
    }

    //export gpio
    public boolean activationPin(){
        String command = String.format("echo %d > /sys/class/gpio/export\n", this.pin);
        Result result = runLinuxSUCommand(command);
        return result.success;
    }

    // unexport gpio
    public boolean desactivationPin(){
        String command = String.format("echo %d > /sys/class/gpio/unexport", this.pin);
        Result result = runLinuxSUCommand(command);
        return result.success;
    }

    //get direction of gpio
    public String getInOut() {
        String command = String.format("cat /sys/class/gpio/%s/direction", this.port);
        Result result = runLinuxSUCommand(command);
        if (result.exception.equals(NO_EXCEPTION)) {
            return result.stdout;
        } else {
            return "";
        }

    }

    // get state of gpio for input and output
    //test if gpio is configurate
    public int getState()
    {
        String command = String.format("cat /sys/class/gpio/%s/value",this.port);
        Result result = runLinuxSUCommand(command);
        if (result.exception.equals(NO_EXCEPTION)) {
            if (result.stdout.equals("")) {
                return -1;
            } else {
                return Integer.parseInt(result.stdout.substring(0, 1));
            }
        }
        return -1;
    }



    //init the pin
    public int initPin(String direction){
        int retour=0;
        boolean ret=true;

        // see if gpio is already set
        retour=getState();
        if (retour==-1) {
            // unexport the gpio
            ret=desactivationPin();
            if(ret==false){ retour=-1; }

            //export the gpio
            ret=activationPin();
            if(ret==false){ retour=-2; }
        }

        // get If gpio direction is define
        String ret2 = getInOut();
        if (!ret2.contains(direction))
        {
            // set the direction (in or out)
            ret=setInOut(direction);
            if(ret==false){ retour=-3; }
        }
        return retour;
    }

    private static class Result {
        Boolean success = false;
        String  stdout = "";
        String  stderr = "";
        String  exception = NO_EXCEPTION;
    }
}