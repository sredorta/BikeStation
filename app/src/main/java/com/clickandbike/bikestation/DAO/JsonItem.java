package com.clickandbike.bikestation.DAO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * JsonItem class:
 *    This class is used to parse Json answers from the Cloud
 */
public class JsonItem {
        @SerializedName("success")
        private String mSuccess = "false";

        @SerializedName("message")
        private String mMessage = "Could not connect to cloud !";

        @SerializedName("action")
        private String mAction = "nothing";

        public boolean getSuccess() {
            if (mSuccess.equals("1")) {
                mSuccess = "true";
            }
            if (mSuccess.equals("0")) {
                mSuccess = "false";
            }
            return Boolean.parseBoolean(mSuccess);
        }

        public String getMessage() {
            return mMessage;
        }

        public String getAction() {
        return mAction;
    }

        public static JsonItem parseJSON(String response) {
            Gson gson = new GsonBuilder().create();
            JsonItem answer = gson.fromJson(response, JsonItem.class);
            return(answer);
        }
}
