package org.altbeacon.beacon.service.scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.SystemClock;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.startup.StartupBroadcastReceiver;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

import java.util.Date;

@TargetApi(18)
public abstract class LeScanner {

    private static final String TAG="LeScanner";


    private Context mContext;
    private CycledLeScanCallback mCycledLeScanCallback;
    private BluetoothCrashResolver mBluetoothCrashResolver;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mBackgroundFlag;

    public LeScanner(Context context,CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver bluetoothCrashResolver) {
        this.mContext = context;
        this.mCycledLeScanCallback = cycledLeScanCallback;
        this.mBluetoothCrashResolver = bluetoothCrashResolver;
    }

    void onBackground(){
        this.mBackgroundFlag = true;
    }

    void onForeground(){
        this.mBackgroundFlag = false;
    }

    abstract boolean onDeferScanIfNeeded(boolean isDefer);

    abstract void stopScan();

    abstract void startScan();

    abstract void finishScan();

    protected boolean getBackgroundFlag(){
        return mBackgroundFlag;
    }

    protected CycledLeScanCallback getCycledLeScanCallback() {
        return mCycledLeScanCallback;
    }

    protected BluetoothCrashResolver getBluetoothCrashResolver() {
        return mBluetoothCrashResolver;
    }

    protected BluetoothAdapter getBluetoothAdapter() {
        if (mBluetoothAdapter == null) {
            // Initializes Bluetooth adapter.
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                LogManager.w(TAG, "Failed to construct a BluetoothAdapter");
            }
        }
        return mBluetoothAdapter;
    }

}
