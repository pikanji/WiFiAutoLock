
package net.pikanji.wifiautolock;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ViewFlipper;

/**
 * @author kanji
 */
public class RegisterSsidActivity extends Activity implements OnClickListener,
        OnCheckedChangeListener {
    private static final String DEBUG_TAG = "---------------------";
    public static final String WIFI_LOCK_TAG = "WiFiAutoLockService";

    private static final int DIALOG_NO_SELECTION = 0;
    private static final int DIALOG_NO_INPUT = 1;
    private static final String EMPTY_STRING = "";

    private Spinner mSpinner;
    private Button mButtonScan;
    private EditText mEditSsid;

    private ArrayAdapter<String> mSpinnerAdapter;

    private RadioGroup mRadioGroupSsid;
    private RadioGroup mRadioGroupLock;

    private ProgressDialog mProgDialog;

    private WifiManager mWifiManager;
    private WifiLock mLock;
    private IntentFilter mReceiverIntentFilter;
    private WiFiScanStatusReceiver mWiFiScanStatusReceiver;

    private SsidManager mSsidManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.register_ssid);

        Button buttonAdd = (Button) findViewById(R.id.button_add);
        buttonAdd.setOnClickListener(this);
        Button buttonCancel = (Button) findViewById(R.id.button_done);
        buttonCancel.setOnClickListener(this);

        mButtonScan = (Button) findViewById(R.id.button_scan);
        mButtonScan.setOnClickListener(this);
        mSpinner = (Spinner) findViewById(R.id.spinner_ssid);

        mEditSsid = (EditText) findViewById(R.id.edit_ssid);

        mRadioGroupSsid = (RadioGroup) findViewById(R.id.raidogr_ssid);
        mRadioGroupSsid.setOnCheckedChangeListener(this);

        mRadioGroupLock = (RadioGroup) findViewById(R.id.raidogr_lock);
        mRadioGroupLock.setOnCheckedChangeListener(this);

        mProgDialog = new ProgressDialog(this);
        mProgDialog.setMessage(this.getText(R.string.prog_dialog_msg));

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, WIFI_LOCK_TAG);
        mLock.setReferenceCounted(false);

        mReceiverIntentFilter = new IntentFilter();
        mReceiverIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mWiFiScanStatusReceiver = new WiFiScanStatusReceiver();

        mSsidManager = new SsidManager(this);

        mSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(mSpinnerAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_add:
                addSsid();
                break;
            case R.id.button_scan:
                scan();
                break;
            case R.id.button_done:
                finish();
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup arg0, int radioId) {
        ViewFlipper vf = (ViewFlipper) findViewById(R.id.flipper);
        vf.setFlipInterval(500);
        switch (radioId) {
            case R.id.radio_manual:
                mSpinner.setEnabled(false);
                mButtonScan.setEnabled(false);
                mEditSsid.setEnabled(true);
                vf.setInAnimation(AnimationUtils.makeInAnimation(this, true));
                vf.setOutAnimation(AnimationUtils.makeOutAnimation(this, true));
                vf.showNext();
                mEditSsid.selectAll();
                break;
            case R.id.radio_select:
                mEditSsid.setEnabled(false);
                mSpinner.setEnabled(true);
                mButtonScan.setEnabled(true);
                vf.setInAnimation(AnimationUtils.makeInAnimation(this, false));
                vf.setOutAnimation(AnimationUtils.makeOutAnimation(this, false));
                vf.showPrevious();
                break;
        }
    }

    @Override
    protected void onResume() {
        this.registerReceiver(mWiFiScanStatusReceiver, mReceiverIntentFilter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        releaseWifi();
        this.unregisterReceiver(mWiFiScanStatusReceiver);
        super.onPause();
    }

    private void addSsid() {
        String ssid;
        boolean lock;
        if (R.id.radio_select == mRadioGroupSsid.getCheckedRadioButtonId()) {
            ssid = (String) mSpinner.getSelectedItem();
            if (null == ssid) {
                showDialog(DIALOG_NO_SELECTION);
                return;
            }
        } else {
            ssid = mEditSsid.getText().toString();
            if (EMPTY_STRING.equals(ssid)) {
                showDialog(DIALOG_NO_INPUT);
                return;
            }
        }
        if (R.id.radio_lock == mRadioGroupLock.getCheckedRadioButtonId()) {
            lock = true;
        } else {
            lock = false;
        }
        mSsidManager.addToFile(ssid, lock);
        Toast.makeText(this, R.string.toast_ssid_added, Toast.LENGTH_SHORT).show();
    }

    private void scan() {
        mProgDialog.show();
        if (mWifiManager.isWifiEnabled()) {
            Log.v(DEBUG_TAG, "startScan invoked");
            mWifiManager.startScan();
        } else {
            mLock.acquire();
            // Enabling also initiates scanning.
            Log.v(DEBUG_TAG, "setWifiEnabled invoked");
            mWifiManager.setWifiEnabled(true);
        }
    }

    private void releaseWifi() {
        Log.v(DEBUG_TAG, "releaseWifi invoked");
        // このプログラムにacquireされたことをチェックしているのか？
        // それとも他のにacquireされていてもtrueになるのか？
        if (mLock.isHeld()) {
            Log.v(DEBUG_TAG, "disabling WiFi invoked");
            mLock.release();
            mWifiManager.setWifiEnabled(false);
        }
    }

    /*
     * This handler is invoked when showDialog(id) is called. (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case DIALOG_NO_SELECTION:
                dialog = createWarningDialog(R.string.register_ssid_no_selection);
                break;
            case DIALOG_NO_INPUT:
                dialog = createWarningDialog(R.string.register_ssid_no_input);
                break;
        }
        return dialog;
    }

    private Dialog createWarningDialog(int res_id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(res_id);
        builder.setNeutralButton(R.string.common_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int id) {
                dlg.dismiss();
            }
        });
        return builder.create();
    }

    public class WiFiScanStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // There is no need to initiate scanning on receiving
            // WIFI_STATE_CHANGED_ACTION with ENABLED status,
            // since scanning will be initiated automatically on status changes
            // when the WiFi is enabled.
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Log.v(DEBUG_TAG, "SCAN_RESULTS_AVAILABLE_ACTION received");
                List<ScanResult> list = mWifiManager.getScanResults();
                // SCAN_RESULTS_AVAILABLE_ACTION could be thrown multiple times,
                // and it might be after the WiFi is disabled making the list
                // null.
                if (null != list) {
                    mSpinnerAdapter.clear();
                    for (ScanResult result : list) {
                        Log.v(DEBUG_TAG, result.SSID);
                        mSpinnerAdapter.add(result.SSID);
                    }
                }
                releaseWifi();
                mProgDialog.dismiss();
            } else {
                Log.v(DEBUG_TAG, "Other action received: " + action);
            }
        }
    }
}
