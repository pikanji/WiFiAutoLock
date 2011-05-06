
package net.pikanji.wifiautolock;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class SsidManager extends FileManager {
    private static final String LOG_TAG = "SsidManager";
    private static final String SSID_FILE_NAME = "ssid.json";
    private static final String JSON_NAME_SSID_LIST = "ssid_list";
    private static final String JSON_NAME_SSID = "ssid";
    private static final String JSON_NAME_LOCK = "lock";

    // public static final int DEFAULT_ENTRY_INDEX = 0;
    // private static final String DEFAULT_KEY = "autunlock_unregistered_area";

    // Default default is unlock, to prevent user from being unable to unlock
    // before using it.

    public SsidManager(Context context) {
        super(context);
    }

    // public boolean fileExists() {
    // for (String filename : mContext.fileList()) {
    // if (SSID_FILE_NAME.equals(filename)) {
    // return true;
    // }
    // }
    // return false;
    // }

    public void addToFile(String ssid, boolean lock) {
        if (null == ssid) {
            return;
        }

        ArrayList<SsidEntry> ssidList = getArrayListFromFile();
        if (null == ssidList) {
            ssidList = new ArrayList<SsidEntry>();
        }

        // Remove old entry if already registered.
        removeEntry(ssidList, ssid);

        ssidList.add(new SsidEntry(ssid, lock));
        writeToFile(ssidList);
    }

    public void removeFromFile(String ssid) {
        if (null == ssid) {
            return;
        }

        ArrayList<SsidEntry> ssidList = getArrayListFromFile();
        if (null == ssidList) {
            ssidList = new ArrayList<SsidEntry>();
        }

        // Remove old entry if already registered.
        removeEntry(ssidList, ssid);

        writeToFile(ssidList);
    }

    // /**
    // * This method also fixes wrong index for default entry.
    // *
    // * @param lock
    // */
    // public void updateDefault(boolean lock) {
    // ArrayList<SsidEntry> registeredSsids = getArrayListFromFile();
    // SsidEntry defaultEntry = null;
    // for (SsidEntry entry : registeredSsids) {
    // if (DEFAULT_KEY.equals(entry.mSsid)) {
    // defaultEntry = entry;
    // registeredSsids.remove(entry);
    // break;
    // }
    // }
    //
    // if (null == defaultEntry) {
    // defaultEntry = getNewSsidEntry(DEFAULT_KEY, lock);
    // } else {
    // defaultEntry.mLock = lock;
    // }
    //
    // registeredSsids.add(SsidManager.DEFAULT_ENTRY_INDEX, defaultEntry);
    // writeToFile(registeredSsids);
    //
    // }

    private void removeEntry(ArrayList<SsidEntry> ssidList, String ssid) {
        for (int i = 0; i < ssidList.size(); i++) {
            if (ssid.equals(ssidList.get(i).mSsid)) {
                ssidList.remove(i);
                // At most one is removed, presuming there would not be
                // duplicate entry.
                break;
            }
        }
    }

    public void writeToFile(ArrayList<SsidEntry> ssidList) {
        String encoded = createJson(ssidList);
        writeToFile(SSID_FILE_NAME, encoded);
    }

    private String createJson(ArrayList<SsidEntry> ssidList) {
        String json = null;
        try {
            JSONArray jsonArraSsid = new JSONArray();
            for (SsidEntry entry : ssidList) {
                JSONObject jsonSsid = new JSONObject();
                jsonSsid.put(JSON_NAME_SSID, entry.mSsid);
                if (entry.mLock) {
                    jsonSsid.put(JSON_NAME_LOCK, mContext.getString(R.string.common_true));
                } else {
                    jsonSsid.put(JSON_NAME_LOCK, mContext.getString(R.string.common_false));
                }
                jsonArraSsid.put(jsonSsid);
            }
            JSONObject root = new JSONObject();
            root.put(JSON_NAME_SSID_LIST, jsonArraSsid);
            json = root.toString();
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Exception while generating JSON.");
        }
        return json;
    }

    protected ArrayList<SsidEntry> getArrayListFromFile() {
        String json = readFromFile(SSID_FILE_NAME);
        ArrayList<SsidEntry> ssidList = new ArrayList<SsidEntry>();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray jsonArraySsid = (JSONArray) root.get(JSON_NAME_SSID_LIST);
            for (int i = 0; i < jsonArraySsid.length(); i++) {
                JSONObject jsonSsid = jsonArraySsid.getJSONObject(i);
                String ssid = jsonSsid.getString(JSON_NAME_SSID);
                boolean lock = jsonSsid.getBoolean(JSON_NAME_LOCK);
                ssidList.add(new SsidEntry(ssid, lock));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Exception while parsing JSON.");
            return null;
        }
        return ssidList;
    }

    protected HashMap<String, Boolean> getHashMapFromFile() {
        String json = readFromFile(SSID_FILE_NAME);
        HashMap<String, Boolean> ssidList = new HashMap<String, Boolean>();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray ssid_list = (JSONArray) root.get(JSON_NAME_SSID_LIST);
            for (int i = 0; i < ssid_list.length(); i++) {
                JSONObject ssid = ssid_list.getJSONObject(i);
                ssidList.put(ssid.getString(JSON_NAME_SSID), ssid.getBoolean(JSON_NAME_LOCK));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Exception while parsing JSON.");
            return null;
        }
        return ssidList;
    }

    public SsidEntry getNewSsidEntry(String ssid, boolean lock) {
        return new SsidEntry(ssid, lock);
    }

    public class SsidEntry {
        public String mSsid;
        public boolean mLock;

        public SsidEntry(String ssid, boolean lock) {
            mSsid = ssid;
            mLock = lock;
        }
    }
}
