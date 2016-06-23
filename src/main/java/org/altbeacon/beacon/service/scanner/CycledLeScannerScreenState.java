package org.altbeacon.beacon.service.scanner;

import android.os.Parcel;
import android.os.Parcelable;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.scanner.screenstate.ScreenStateListener;

/**
 * Created by Connecthings on 17/06/16.
 */
public class CycledLeScannerScreenState extends CycledLeScanner implements ScreenStateListener{

    private static final String TAG = "CycledLeScannerScreenState";

    public static final int DELAY=20*1000;

    private Runnable mStopScanningRunnable;
    private final int mActiveScanPeriodOnScreenStateSwitch;

    public CycledLeScannerScreenState() {
        this(new ScanPeriods(BeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD, BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD), DELAY);
    }

    public CycledLeScannerScreenState(ScanPeriods activePeriods, int activeScanPeriodOnScreenStateSwitch) {
        super(activePeriods, new ScanPeriods(BeaconManager.DEFAULT_BACKGROUND_SCAN_PERIOD, BeaconManager.DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD));
        mActiveScanPeriodOnScreenStateSwitch = activeScanPeriodOnScreenStateSwitch;
    }

    public void updateCycledParameter(CycledParameter cycledParameter) {
        cancelRunnableStopScanning();
        super.updateCycledParameter(cycledParameter);
    }

    protected long calculateNextScanLeDeviceDelayBackground(){
        if(getActiveMode()){
            LogManager.d(TAG, "nextScanDelay - ScreenUpdate - scan start now");
            return 0;
        }else{
            LogManager.d(TAG, "nextScanDelay -screen of - scan lock");
            return 1000;
        }
    }

    protected long calculateNextStopCycleDelayBackground(){
        if(getActiveMode()){
            LogManager.d(TAG, "nextStopCycle - ScreenUpdate - stop cycle lock");
            return 1000;
        }else{
            LogManager.d(TAG, "nextStopCycle - screen of - stop scan now");
            return 0;
        }
    }

    private void cancelRunnableStopScanning(){
        cancelRunnable(mStopScanningRunnable);
    }

    private void launchBackgroundScanning(){
        if(getBackgroundFlag()){
            LogManager.d(TAG, "launch background scanning");
            cancelNextCycledRunnable();
            cancelRunnableStopScanning();
            this.updateMode(true);
            mStopScanningRunnable = new Runnable() {
                @Override
                public void run() {
                    LogManager.d(TAG, "stop active background scanning");
                    updateMode(false);
                }
            };
            scheduleRunnable(mStopScanningRunnable, mActiveScanPeriodOnScreenStateSwitch);
            scanLeDevice(true);
        }
    }

    @Override
    public void onScreenOn() {
        LogManager.d(TAG, "screen on -> test to launch an active background scanning");
        launchBackgroundScanning();
    }

    @Override
    public void onScreenOff() {
        LogManager.d(TAG, "screen off -> test to launch an active background scanning");
        launchBackgroundScanning();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mActiveScanPeriodOnScreenStateSwitch);
    }

    protected CycledLeScannerScreenState(Parcel in){
        super(in);
        mActiveScanPeriodOnScreenStateSwitch = in.readInt();
    }

    public static final Parcelable.Creator<CycledLeScannerScreenState> CREATOR
            = new Parcelable.Creator<CycledLeScannerScreenState>() {
        public CycledLeScannerScreenState createFromParcel(Parcel in) {
            return new CycledLeScannerScreenState(in);
        }

        public CycledLeScannerScreenState[] newArray(int size) {
            return new CycledLeScannerScreenState[size];
        }
    };
}
