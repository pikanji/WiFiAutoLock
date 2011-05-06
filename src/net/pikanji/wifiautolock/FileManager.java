
package net.pikanji.wifiautolock;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.util.Log;

public class FileManager {
    private static final String LOG_TAG = "FileManager";
    private static final int BUF_SIZE = 256;
    protected Context mContext;

    protected FileManager(Context context) {
        mContext = context;
    }

    protected String readFromFile(String filename) {
        String jsonData = "";
        try {
            FileInputStream in = mContext.openFileInput(filename);
            byte[] buf = new byte[BUF_SIZE];
            int numRead = in.read(buf);
            while (-1 != numRead) {
                jsonData += new String(buf, 0, numRead);
                numRead = in.read(buf);
            }
            // FIXME: remove log print
            Log.v(LOG_TAG, jsonData);
        } catch (FileNotFoundException e) {
            Log.w(LOG_TAG, "File not found: " + filename);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Failed to open file for reading.");
        }
        return jsonData;
    }

    protected void writeToFile(String filename, String json) {
        try {
            FileOutputStream out = mContext.openFileOutput(filename, Context.MODE_WORLD_READABLE);
            out.write(json.getBytes());
            out.close();
            // FIXME: remove log print
            Log.v(LOG_TAG, json);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Failed to open a file for writing.");
        } catch (Exception e) {
            Log.w(LOG_TAG, "Other exception occurred!");
        }
    }
}
