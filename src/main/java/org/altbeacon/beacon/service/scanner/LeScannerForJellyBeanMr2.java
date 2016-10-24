package org.altbeacon.beacon.service.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.os.SystemClock;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

@TargetApi(18)
public class LeScannerForJellyBeanMr2 extends LeScanner {
    private static final String TAG = "LeScannerForJellyBeanMr";
    private BluetoothAdapter.LeScanCallback leScanCallback;


    public LeScannerForJellyBeanMr2(Context context, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver bluetoothCrashResolver) {
        super(context, cycledLeScanCallback, bluetoothCrashResolver);
    }

    @SuppressWarnings("deprecation")
    @Override
    void stopScan() {
        postStopLeScan();
    }

    @Override
    Runnable generateStartScanRunnable() {
        final BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        final BluetoothAdapter.LeScanCallback scanCallback = getLeScanCallback();
        return new Runnable() {
            @Override
            public void run() {
                try {
                    if (bluetoothAdapter == null) {
                        LogManager.w(TAG, "BluetoothAdapter is null. Can't stop scan");
                    }else{
                        bluetoothAdapter.startLeScan(scanCallback);
                    }
                } catch (Exception e) {
                    LogManager.e(e, TAG, "Internal Android exception scanning for beacons");
                }
            }
        };
    }

    @Override
    Runnable generateStopScanRunnable() {
        final BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        final BluetoothAdapter.LeScanCallback scanCallback = getLeScanCallback();
        return new Runnable() {
            @Override
            public void run() {
                try {
                    if (bluetoothAdapter == null) {
                        LogManager.w(TAG, "BluetoothAdapter is null. Can't stop scan");
                    }else{
                        bluetoothAdapter.stopLeScan(scanCallback);
                    }
                } catch (Exception e) {
                    LogManager.e(e, TAG, "Internal Android exception scanning for beacons");
                }
            }
        };
    }

    @Override
    boolean onDeferScanIfNeeded(boolean isDefer) {
        return getBackgroundFlag();
    }

    @SuppressWarnings("deprecation")
    @Override
    void startScan() {
        postStopLeScan();
    }

    @SuppressWarnings("deprecation")
    @Override
    void finishScan() {
        getBluetoothAdapter().stopLeScan(getLeScanCallback());
    }

    BluetoothAdapter.LeScanCallback getLeScanCallback() {
        if (leScanCallback == null) {
            leScanCallback =
                    new BluetoothAdapter.LeScanCallback() {

                        @Override
                        public void onLeScan(final BluetoothDevice device, final int rssi,
                                             final byte[] scanRecord) {
                            LogManager.d(TAG, "got record");
                            getCycledLeScanCallback().onLeScan(device, rssi, scanRecord);
                            getBluetoothCrashResolver().notifyScannedDevice(device, getLeScanCallback());
                        }
                    };
        }
        return leScanCallback;
    }
}
