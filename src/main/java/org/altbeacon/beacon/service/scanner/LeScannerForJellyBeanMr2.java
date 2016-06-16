package org.altbeacon.beacon.service.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
        try {
            BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
            if (bluetoothAdapter != null) {
                bluetoothAdapter.stopLeScan(getLeScanCallback());
            }
        } catch (Exception e) {
            LogManager.e(e, TAG, "Internal Android exception scanning for beacons");
        }
    }

    @Override
    boolean onDeferScanIfNeeded(boolean isDefer) {
        return getBackgroundFlag();
    }

    @SuppressWarnings("deprecation")
    @Override
    void startScan() {
        getBluetoothAdapter().startLeScan(getLeScanCallback());
    }

    @SuppressWarnings("deprecation")
    @Override
    void finishScan() {
        getBluetoothAdapter().stopLeScan(getLeScanCallback());
    }

    private BluetoothAdapter.LeScanCallback getLeScanCallback() {
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
