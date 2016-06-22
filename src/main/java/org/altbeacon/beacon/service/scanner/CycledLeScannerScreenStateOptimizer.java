package org.altbeacon.beacon.service.scanner;

import android.os.Parcel;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.scanner.screen.ScreenStateListener;

import java.util.ArrayList;

/**
 * Created by Connecthings on 17/06/16.
 */
public class CycledLeScannerScreenStateOptimizer extends CycledLeScanner implements ScreenStateListener, MonitorNotifier{

    private static final int DELAY=40*1000;

    private Runnable mStopScanningRunnable;
    private final int mActiveScanPeriodOnScreenStateSwitch;
    private final ArrayList<Region> regions = new ArrayList<>();

    public CycledLeScannerScreenStateOptimizer() {
        this(new ScanPeriods(BeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD, BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD),
                new ScanPeriods(BeaconManager.DEFAULT_BACKGROUND_SCAN_PERIOD, BeaconManager.DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD),
                DELAY);
    }

    public CycledLeScannerScreenStateOptimizer(ScanPeriods activePeriods, ScanPeriods passivePeriods, int activeScanPeriodOnScreenStateSwitch) {
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

    private boolean isThereBeaconsInTheArea(){
        return regions.size() != 0;
    }

    @Override
    public void didEnterRegion(Region region) {
        regions.add(region);
    }

    @Override
    public void didExitRegion(Region region) {
        regions.remove(region);
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mActiveScanPeriodOnScreenStateSwitch);
    }

    protected CycledLeScannerScreenStateOptimizer(Parcel in){
        super(in);
        mActiveScanPeriodOnScreenStateSwitch = in.readInt();
    }

    public static final Creator<CycledLeScannerScreenStateOptimizer> CREATOR
            = new Creator<CycledLeScannerScreenStateOptimizer>() {
        public CycledLeScannerScreenStateOptimizer createFromParcel(Parcel in) {
            return new CycledLeScannerScreenStateOptimizer(in);
        }

        public CycledLeScannerScreenStateOptimizer[] newArray(int size) {
            return new CycledLeScannerScreenStateOptimizer[size];
        }
    };
}
