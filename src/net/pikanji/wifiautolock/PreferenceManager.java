
package net.pikanji.wifiautolock;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class PreferenceManager extends FileManager {
    private static final String LOG_TAG = "PreferenceManager";
    private static final String PREF_FILE_NAME = "preference.json";
    private static final String JSON_NAME_AGREED = "agreed";
    private static final String JSON_NAME_INTERVAL = "interval";
    private static final String JSON_NAME_DEFAULT = "default";
    private static final String JSON_NAME_LAST_VERSION_RAN = "last_version";
    private static final int DEFAULT_INTERVAL = 60; // 1 min
    // If this interval is too short, entering pattern to unlock will be
    // difficult for user, because this service drags down the response of the
    // UI when it runs scanning.
    public static final int MINIMUM_INTERVAL = 10; // 30 sec
    private static final boolean DEFAULT_DEFAULT_LOCK = false;

    public PreferenceManager(Context context) {
        super(context);
    }

    public void setAgreed() {
        writeBoolToFile(JSON_NAME_AGREED, true);
    }

    public boolean getAgreed() {
        return readBoolFromFile(JSON_NAME_AGREED, false);
    }

    /**
     * @param value Interval in seconds
     */
    public void setInterval(int value) {
        if (value < MINIMUM_INTERVAL) {
            value = MINIMUM_INTERVAL;
        }
        writeIntToFile(JSON_NAME_INTERVAL, value);
    }

    public void setDefaultLock(boolean value) {
        writeBoolToFile(JSON_NAME_DEFAULT, value);
    }

    /**
     * @return Interval in seconds
     */
    public int getInterval() {
        return readIntFromFile(JSON_NAME_INTERVAL, DEFAULT_INTERVAL);
    }

    public boolean getDefaultLock() {
        return readBoolFromFile(JSON_NAME_DEFAULT, DEFAULT_DEFAULT_LOCK);
    }

    public void setLastVersionRan(int versionCode) {
        writeIntToFile(JSON_NAME_LAST_VERSION_RAN, versionCode);
    }

    public int getLastVersionRan() {
        return readIntFromFile(JSON_NAME_LAST_VERSION_RAN, 0);
    }

    private void writeIntToFile(String key, int value) {
        String json = readFromFile(PREF_FILE_NAME);
        Log.d(LOG_TAG, json);
        JSONObject root;
        try {
            root = new JSONObject(json);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "JSON file is not found, or broken at root level.");
            root = new JSONObject();
        }
        try {
            root.put(key, value);
            writeToFile(PREF_FILE_NAME, root.toString());
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Failed to put key/value pair to json object.");
        }
    }

    private void writeBoolToFile(String key, boolean value) {
        String json = readFromFile(PREF_FILE_NAME);
        Log.d(LOG_TAG, json);
        JSONObject root;
        try {
            root = new JSONObject(json);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "JSON file is not found, or broken at root level.");
            root = new JSONObject();
        }
        try {
            root.put(key, value);
            writeToFile(PREF_FILE_NAME, root.toString());
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Failed to put key/value pair to json object.");
        }
    }

    private int readIntFromFile(String key, int defaultValue) {
        int ret = defaultValue;
        try {
            String json = readFromFile(PREF_FILE_NAME);
            JSONObject root = new JSONObject(json);
            ret = root.getInt(key);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "JSON file is not found, or broken.");
        }
        return ret;
    }

    private boolean readBoolFromFile(String key, boolean defaultValue) {
        boolean ret = defaultValue;
        try {
            String json = readFromFile(PREF_FILE_NAME);
            JSONObject root = new JSONObject(json);
            ret = root.getBoolean(key);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "JSON file is not found, or broken.");
        }
        return ret;
    }
}
