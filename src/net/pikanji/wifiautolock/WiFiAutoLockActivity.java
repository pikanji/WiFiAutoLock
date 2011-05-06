
package net.pikanji.wifiautolock;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.util.Log;

public class WiFiAutoLockActivity extends PreferenceActivity implements OnPreferenceChangeListener,
        OnPreferenceClickListener {
    private static final String DEBUG_TAG = "WiFiAutoLockActivity";
    private static final String mServiceName = WiFiAutoLockService.class.getCanonicalName();

    private static final int DIALOG_DISCLAIMER = 0;
    private static final int DIALOG_FIRST_RUN_OF_THIS_VERSION = 1;

    private PreferenceManager mPrefManager;
    private ListPreference mUnregdArea;
    private EditTextPreference mInterval;
    private int mCurrVerCode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        mPrefManager = new PreferenceManager(this);
        mUnregdArea = (ListPreference) findPreference(getText(R.string.pref_key_unreg_area));
        mInterval = (EditTextPreference) findPreference(getText(R.string.pref_key_interval));

        mCurrVerCode = getCurrentVersionCode();

        setupStartServicePref();
        setupDefaultLockPref();
        setupIntervalPref();
    }

    @Override
    protected void onResume() {
        if (isFirstRunOfThisVersion()) {
            showDialog(DIALOG_FIRST_RUN_OF_THIS_VERSION);
        }
        // This comes after first-run dialog, to show on top of it.
        if (!mPrefManager.getAgreed()) {
            showDialog(DIALOG_DISCLAIMER);
        }
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        String key = pref.getKey();

        if (getText(R.string.pref_key_run).equals(key)) {
            if (((Boolean) newValue).booleanValue()) {
                WiFiAutoLockService.startService(this);
            } else {
                WiFiAutoLockService.stopService(this);
            }
        } else if (getText(R.string.pref_key_unreg_area).equals(key)) {
            String value = (String) newValue;
            Log.v(DEBUG_TAG, "New default value: " + value);
            boolean lock;
            if (getString(R.string.common_true).equals(value)) {
                lock = true;
            } else {
                lock = false;
            }
            mPrefManager.setDefaultLock(lock);
            // Don't have to worry about concurrent issue with service
            // because it is the same process/thread as this activity.
            if (isServiceRunning()) {
                updateDefaultOnService(lock);
            }
        } else if (getText(R.string.pref_key_interval).equals(key)) {
            int value = Integer.parseInt((String) newValue);
            Log.v(DEBUG_TAG, "New interval: " + value);
            mPrefManager.setInterval(value);
            // Don't have to worry about concurrent issue with service
            // because it is the same process/thread as this activity.
            if (isServiceRunning()) {
                updateIntervalOnService(value);
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        String key = pref.getKey();
        if (getText(R.string.pref_key_unreg_area).equals(key)) {
            if (mPrefManager.getDefaultLock()) {
                mUnregdArea.setValue(getString(R.string.common_true));
            } else {
                mUnregdArea.setValue(getString(R.string.common_false));
            }
        } else if (getText(R.string.pref_key_interval).equals(key)) {
            mInterval.getEditText().setText(Integer.toString(mPrefManager.getInterval()));
        }
        return true;
    }

    private void setupStartServicePref() {
        CheckBoxPreference startService = (CheckBoxPreference) findPreference(getText(R.string.pref_key_run));
        startService.setOnPreferenceChangeListener(this);
        startService.setSummaryOff(R.string.pref_sum_stopped);
        startService.setSummaryOn(R.string.pref_sum_started);
        startService.setChecked(isServiceRunning());
    }

    private void setupDefaultLockPref() {
        mUnregdArea.setOnPreferenceChangeListener(this);
        mUnregdArea.setOnPreferenceClickListener(this);
        mUnregdArea.setPersistent(false);
        if (mPrefManager.getDefaultLock()) {
            mUnregdArea.setValue(getString(R.string.common_true));
        } else {
            mUnregdArea.setValue(getString(R.string.common_false));
        }
    }

    private void setupIntervalPref() {
        mInterval.setOnPreferenceChangeListener(this);
        mInterval.setOnPreferenceClickListener(this);
        String msg = this.getString(R.string.pref_dialog_msg_interval);
        msg += PreferenceManager.MINIMUM_INTERVAL + " ~ " + Integer.MAX_VALUE;
        mInterval.setDialogMessage(msg);
    }

    private void updateIntervalOnService(int value) {
        Intent intent = new Intent(this, WiFiAutoLockService.class);
        intent.setAction(WiFiAutoLockService.UPDATE_INTERVAL);
        intent.putExtra(WiFiAutoLockService.EXTRA_INTERVAL, value);
        startService(intent);
    }

    private void updateDefaultOnService(boolean value) {
        Intent intent = new Intent(this, WiFiAutoLockService.class);
        intent.setAction(WiFiAutoLockService.UPDATE_DEFAULT);
        intent.putExtra(WiFiAutoLockService.EXTRA_DEFAULT, value);
        startService(intent);
    }

    private boolean isServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (RunningServiceInfo info : services) {
            if (mServiceName.equals(info.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private int getCurrentVersionCode() {
        int curr = 0;
        try {
            curr = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            Log.w(DEBUG_TAG, e.getMessage());
        }
        return curr;
    }

    /*
     * The value obtained by PreferenceManager#getLastVersionRan should work as
     * a flag whether to show the first run dialog. So,
     * PreferenceManager#setLastVersionRan should be called when it is certain
     * that the user see the dialog.
     */
    private boolean isFirstRunOfThisVersion() {
        return (mCurrVerCode > mPrefManager.getLastVersionRan());
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
            case DIALOG_DISCLAIMER:
                builder.setTitle(R.string.dialog_disclaimer_title);
                builder.setMessage(R.string.dialog_disclaimer_msg);
                DisclaimerDialogListener dListener = new DisclaimerDialogListener();
                builder.setPositiveButton(R.string.dialog_disclaimer_agree, dListener);
                builder.setNegativeButton(R.string.dialog_disclaimer_exit, dListener);
                break;
            case DIALOG_FIRST_RUN_OF_THIS_VERSION:
                builder.setMessage(R.string.dialog_first_run_msg);
                FirstRunDialogListener pListener = new FirstRunDialogListener();
                builder.setPositiveButton(R.string.common_ok, pListener);
                builder.setNeutralButton(R.string.dialog_first_run_setting, pListener);
                break;
        }

        return builder.create();
    }

    private class DisclaimerDialogListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    mPrefManager.setAgreed();
                    dialog.dismiss();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    System.exit(0);
                    break;
            }
        }
    }

    private class FirstRunDialogListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    dialog.dismiss();
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_SECURITY_SETTINGS);
                    dialog.dismiss();
                    startActivity(intent);
                    break;
            }
            mPrefManager.setLastVersionRan(mCurrVerCode);
        }
    }

    // private boolean isDebuggable(boolean defaultValue) {
    // boolean ret = defaultValue;
    // PackageManager pm = this.getPackageManager();
    // try {
    // ApplicationInfo info = pm.getApplicationInfo(this.getPackageName(), 0);
    // int flag = info.flags & ApplicationInfo.FLAG_DEBUGGABLE;
    // ret = (ApplicationInfo.FLAG_DEBUGGABLE == flag);
    // } catch (NameNotFoundException e) {
    // Log.w(DEBUG_TAG, e.getMessage());
    // }
    // return ret;
    // }
}
