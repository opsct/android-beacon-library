package org.altbeacon.beacon.service.scanner;

import android.os.Parcel;
import android.os.Parcelable;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.service.scanner.optimizer.ScreenStateListener;

/**
 * Created by Connecthings on 17/06/16.
 */
public class CycledLeScannerScreenState extends CycledLeScanner implements ScreenStateListener{

    private static final int DELAY=40*1000;

    private Runnable mStopScanningRunnable;
    private final int mActiveScanPeriodOnScreenStateSwitch;

    public CycledLeScannerScreenState() {
        this(new ScanPeriods(BeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD, BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD),
                new ScanPeriods(BeaconManager.DEFAULT_BACKGROUND_SCAN_PERIOD, BeaconManager.DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD),
                DELAY);
    }

    public CycledLeScannerScreenState(ScanPeriods activePeriods, ScanPeriods passivePeriods, int activeScanPeriodOnScreenStateSwitch) {
        super(activePeriods, passivePeriods);
        mActiveScanPeriodOnScreenStateSwitch = activeScanPeriodOnScreenStateSwitch;
    }

    public void updateCycledParameter(CycledParameter cycledParameter) {
        cancelRunnableStopScanning();
        super.updateCycledParameter(cycledParameter);
    }

    protected long calculateNextScanLeDeviceDelayBackground(){
        if(getActiveMode()){
            return 0;
        }else{
            return 1000;
        }
    }

    protected long calculateNextStopCyleDelayBackground(){
        if(getActiveMode()){
            return 1000;
        }else{
            return 0;
        }
    }

    private void cancelRunnableStopScanning(){
        cancelRunnable(mStopScanningRunnable);
    }

    private void launchBackgroundScanning(){
        if(getBackgroundFlag()){
            cancelNextCycledRunnable();
            cancelRunnableStopScanning();
            this.updateActiveMode(true);
            mStopScanningRunnable = new Runnable() {
                @Override
                public void run() {
                    updateActiveMode(false);
                }
            };
            scheduleRunnable(mStopScanningRunnable, mActiveScanPeriodOnScreenStateSwitch);
            scanLeDevice(true);
        }
    }

    @Override
    public void onScreenOn() {
        launchBackgroundScanning();
    }

    @Override
    public void onScreenOff() {
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
