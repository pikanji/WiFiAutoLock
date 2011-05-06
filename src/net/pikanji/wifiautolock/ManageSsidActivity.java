
package net.pikanji.wifiautolock;

import java.util.ArrayList;
import java.util.List;

import net.pikanji.wifiautolock.SsidManager.SsidEntry;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ManageSsidActivity extends Activity implements OnClickListener {
    private SsidManager mSsidManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.manage_ssid);

        Button buttonAdd = (Button) findViewById(R.id.button_add);
        buttonAdd.setOnClickListener(this);
        Button buttonDone = (Button) findViewById(R.id.button_done);
        buttonDone.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_add:
                Intent intent = new Intent(this, RegisterSsidActivity.class);
                startActivity(intent);
                break;
            case R.id.button_done:
                finish();
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        mSsidManager = new SsidManager(this);
        ArrayList<SsidEntry> ssidList = mSsidManager.getArrayListFromFile();
        if (null == ssidList) { // File not exists, or broken
            ssidList = new ArrayList<SsidEntry>();
        }
        // Remove the first entry, which is default setting
        // ssidList.remove(DEFAULT_ENTRY);
        SsidEntryArrayAdapter adapter = new SsidEntryArrayAdapter(this, R.layout.manage_ssid_row,
                ssidList);
        ListView listView = (ListView) findViewById(R.id.list_ssid);
        listView.setAdapter(adapter);
        super.onResume();
    }

    private class RemoveOnClickListener implements View.OnClickListener {
        SsidEntryArrayAdapter mAdapter;

        RemoveOnClickListener(SsidEntryArrayAdapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public void onClick(View v) {
            // No type check, because this listener is set to only button.
            SsidEntry entry = (SsidEntry) ((Button) v).getTag();

            ConfirmOnClickListener listener = new ConfirmOnClickListener(entry);

            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(ManageSsidActivity.this);
            builder.setMessage(R.string.alert_dialog_conf_rm);
            builder.setPositiveButton(R.string.common_remove, listener);
            builder.setNegativeButton(R.string.common_cancel, listener);
            builder.create().show();
        }

        private class ConfirmOnClickListener implements DialogInterface.OnClickListener {
            private SsidEntry mEntry;

            public ConfirmOnClickListener(SsidEntry entry) {
                mEntry = entry;
            }

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        mSsidManager.removeFromFile(mEntry.mSsid);
                        mAdapter.remove(mEntry);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        // do nothing
                        break;
                }
            }
        }
    }

    private class SsidEntryArrayAdapter extends ArrayAdapter<SsidEntry> {
        private int mViewResoruceId;

        public SsidEntryArrayAdapter(Context context, int viewResourceId, List<SsidEntry> objects) {
            super(context, viewResourceId, objects);
            this.mViewResoruceId = viewResourceId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                // Create View from layout resource
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                        LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(mViewResoruceId, null);

                holder = new ViewHolder();
                holder.imgLock = (ImageView) convertView.findViewById(R.id.img_lock);
                holder.textSsid = (TextView) convertView.findViewById(R.id.text_ssid);
                holder.button = (Button) convertView.findViewById(R.id.button_remove);
                holder.button.setOnClickListener(new RemoveOnClickListener(this));

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            SsidEntry entry = getItem(position);

            if (entry.mLock) {
                holder.imgLock.setImageResource(R.drawable.ic_list_lock);
            } else {
                holder.imgLock.setImageResource(R.drawable.ic_list_unlock);
            }
            holder.textSsid.setText(entry.mSsid);
            holder.button.setTag(entry);

            return convertView;
        }

        private class ViewHolder {
            ImageView imgLock;
            TextView textSsid;
            Button button;
        }
    }
}
