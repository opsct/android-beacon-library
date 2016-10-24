package org.altbeacon.beacon.service.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

@TargetApi(18)
public abstract class LeScanner implements RecordDetectionListener{

    private static final String TAG="LeScanner";


    private Context mContext;
    private CycledLeScanCallback mCycledLeScanCallback;
    private BluetoothCrashResolver mBluetoothCrashResolver;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mBackgroundFlag;
    private long mLastDetectionTime = 0l;
    private final Handler mScanHandler;
    private final HandlerThread mScanThread;

    public LeScanner(Context context,CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver bluetoothCrashResolver) {
        this.mContext = context;
        this.mCycledLeScanCallback = cycledLeScanCallback;
        this.mBluetoothCrashResolver = bluetoothCrashResolver;
        mScanThread = new HandlerThread("CycledLeScannerThread");
        mScanThread.start();
        mScanHandler = new Handler(mScanThread.getLooper());
    }

    void onBackground(){
        this.mBackgroundFlag = true;
    }

    void onForeground(){
        this.mBackgroundFlag = false;
    }

    void onDestroy(){
        mScanThread.quit();
    }

    protected Handler getScanHandler(){
        return mScanHandler;
    }

    protected void postStopLeScan() {
        final BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            return;
        }
        mScanHandler.removeCallbacksAndMessages(null);
        mScanHandler.post(generateStopScanRunnable());
    }

    protected void postStartLeScan() {
        mScanHandler.removeCallbacksAndMessages(null);
        mScanHandler.post(generateStartScanRunnable());
    }

    abstract boolean onDeferScanIfNeeded(boolean isDefer);

    abstract void stopScan();

    abstract void startScan();

    abstract void finishScan();

    abstract Runnable generateStopScanRunnable();

    abstract Runnable generateStartScanRunnable();

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

    public long getLastDetectionTime() {
        return mLastDetectionTime;
    }
    public void recordDetection() {
        mLastDetectionTime = SystemClock.elapsedRealtime();
    }
}
