
package net.pikanji.wifiautolock;

import java.util.List;

import net.pikanji.wifiautolock.SsidManager.SsidEntry;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.Log;

public class WiFiAutoLockService extends Service {
    private static final boolean DEBUGGING = false; // Don't make this variable
    private static final String DEBUG_TAG = "WiFiAutoLockService";

    // Intent related keys
    public static final String START_SERVICE = "start";
    public static final String STOP_SERVICE = "stop";
    public static final String UPDATE_INTERVAL = "interval";
    public static final String UPDATE_DEFAULT = "default";
    public static final String EXTRA_INTERVAL = "ext_int";
    public static final String EXTRA_DEFAULT = "ext_def";

    public static final String WIFI_LOCK_TAG = "WiFiAutoLockService";
    private static final int SYSTEM_PREF_LOCK = 1;
    // private static final int SYSTEM_PREF_UNLOCK = 0;

    private static final int STATE_IDLE = 0;
    private static final int STATE_PROCESSING = 1;

    private int mState;

    private AlarmManager mAlarmManager;

    private WifiManager mWifiManager;
    private WifiLock mLock;

    private PowerManager mPowerManager;

    private SsidManager mSsidManager;

    private KeyguardManager.KeyguardLock mKeyguardLock;

    private int mInterval; // in second
    private boolean mDefaultLock;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mState = STATE_IDLE;
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, WIFI_LOCK_TAG);
        mLock.setReferenceCounted(false);

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        mSsidManager = new SsidManager(this);

        PreferenceManager prefManager = new PreferenceManager(this);
        mInterval = prefManager.getInterval();
        mDefaultLock = prefManager.getDefaultLock();

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mKeyguardLock = km.newKeyguardLock(DEBUG_TAG);

        // Set up broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        this.registerReceiver(new AulBroadcastReceiver(), filter);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (DEBUGGING) {
            Log.v(DEBUG_TAG, "Service#onStart invoked.");
        }

        // Set default command
        if (null == i) {
            i = new Intent();
            i.setAction(START_SERVICE);
        }

        if (START_SERVICE.equals(i.getAction())) {
            initiateLockSequence();
        } else if (STOP_SERVICE.equals(i.getAction())) {
            if (DEBUGGING) {
                Log.v(DEBUG_TAG, "Service stopped from Service");
            }
            mAlarmManager.cancel(createPendingIntent(this));
            mState = STATE_IDLE;
            stopSelf();
        } else if (UPDATE_INTERVAL.equals(i.getAction())) {
            mInterval = i.getExtras().getInt(EXTRA_INTERVAL);
        } else if (UPDATE_DEFAULT.equals(i.getAction())) {
            mDefaultLock = i.getExtras().getBoolean(EXTRA_DEFAULT);
        }

        // If killed unexpectedly, restart with null intent.
        return Service.START_STICKY;
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, WiFiAutoLockService.class);
        intent.setAction(START_SERVICE);
        context.startService(intent);
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, WiFiAutoLockService.class);
        context.stopService(intent);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(createPendingIntent(context));
    }

    public static PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(context, WiFiAutoLockService.class);
        intent.setAction(WiFiAutoLockService.START_SERVICE);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    private void initiateLockSequence() {
        if (DEBUGGING) {
            Log.v(DEBUG_TAG, "Service#initiateLockSequence invoked.");
        }
        if (STATE_PROCESSING == mState) {
            return;
        }

        if (mPowerManager.isScreenOn()) {
            if (DEBUGGING) {
                Log.v(DEBUG_TAG, "Don't auto lock while the screen is on");
            }
            // Do nothing but schedule for next execution
            scheduleNext();
            return;
        }

        mState = STATE_PROCESSING;
        // WiFi enabled, then initiate scan, service task is done
        if (mWifiManager.isWifiEnabled()) {
            if (mWifiManager.startScan()) {
                // Scan initiated, proceed on WiFiStatusReceiver
                if (DEBUGGING) {
                    Log.v(DEBUG_TAG, "Scan started from Service");
                }
            }
        } else {
            if (DEBUGGING) {
                Log.v(DEBUG_TAG, "Enable WiFi from Service");
            }
            // no then enable WiFi, done
            mLock.acquire();
            mWifiManager.setWifiEnabled(true);
        }
    }

    private void releaseWifi() {
        // このプログラムにacquireされたことをチェックしているのか？
        // それとも他のにacquireされていてもtrueになるのか？
        if (mLock.isHeld()) {
            if (DEBUGGING) {
                Log.v(DEBUG_TAG, "disabling WiFi invoked");
            }
            mLock.release();
            mWifiManager.setWifiEnabled(false);
        }
    }

    public class AulBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                if (DEBUGGING) {
                    Log.v(DEBUG_TAG, "SCAN_RESULTS_AVAILABLE_ACTION received");
                }
                onScanComplete(context, intent);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                initiateLockSequence();
            } else {
                if (DEBUGGING) {
                    Log.v(DEBUG_TAG, "Actions received: " + action);
                }
            }
        }
    }

    private void onScanComplete(Context context, Intent intentResult) {
        if (DEBUGGING) {
            Log.v(DEBUG_TAG, "Scan completed!");
        }
        // Check if it's under the registered WiFi cover area.
        List<ScanResult> scannedSsids = mWifiManager.getScanResults();
        if ((null == scannedSsids) || (STATE_PROCESSING != mState)) {
            Log.w(DEBUG_TAG, "Scan result is null.");
            // Do nothing since the result notified is not expected.
            releaseWifi(); // Just in case
            return;
        }

        if (DEBUGGING) {
            Log.v(DEBUG_TAG, "Successfully obtained scan result.");
        }
        List<SsidEntry> registeredSsids = mSsidManager.getArrayListFromFile();
        if (null != registeredSsids) {
            doAutoLock(scannedSsids, registeredSsids);
        } else {
            Log.w(DEBUG_TAG, "Failed to read jason file, or no ssid has registered yet.");
            setLock(mDefaultLock);
        }
        releaseWifi();

        scheduleNext();
    }

    // Schedule next execution of this service.
    private void scheduleNext() {
        if (DEBUGGING) {
            Log.v(DEBUG_TAG, "Scheduling for next execution.");
        }
        long triggerTime = SystemClock.elapsedRealtime() + (mInterval * 1000);
        PendingIntent pi = createPendingIntent(this);
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pi);
    }

    private void doAutoLock(List<ScanResult> scannedSsids, List<SsidEntry> registeredSsids) {
        // for debug
        for (ScanResult scanned : scannedSsids) {
            if (DEBUGGING) {
                Log.v(DEBUG_TAG, "Found: " + scanned.SSID);
            }
        }
        for (SsidEntry entry : registeredSsids) {
            if (DEBUGGING) {
                Log.v(DEBUG_TAG, "Read: " + entry.mSsid);
            }
        }

        // Set or release the security lock.
        // The list should be small in the practical environment.
        for (ScanResult scanned : scannedSsids) {
            for (SsidEntry entry : registeredSsids) {
                if (entry.mSsid.equals(scanned.SSID)) {
                    setLock(entry.mLock);
                    return;
                }
            }
        }

        // No registered SSID matched. Apply default lock setting.
        setLock(mDefaultLock);
    }

    private void setLock(boolean lockBool) {
        if (!lockBool) {
            return;
        }

        if (DEBUGGING) {
            Log.v(DEBUG_TAG, "Locking the device");
            Log.v(DEBUG_TAG, "System.LOCK_PATTERN_ENABLE: " + Settings.System.LOCK_PATTERN_ENABLED);
        }

        // To prevent the keyguard from appearing over the security lock,
        // disable and enable it after the security lock is enabled.
        mKeyguardLock.disableKeyguard();
        System.putInt(getContentResolver(), Settings.System.LOCK_PATTERN_ENABLED, SYSTEM_PREF_LOCK);
        // Settings.System.LOCK_PATTERN_ENABLED is moved to
        // Settings.Secure.LOCK_PATTERN_ENABLED in 2.2.
        mKeyguardLock.reenableKeyguard();

        mState = STATE_IDLE;
    }
}
