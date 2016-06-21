package org.altbeacon.beacon.service.scanner;

import android.content.Context;

import org.altbeacon.beacon.service.scanner.optimizer.ScreenStateListener;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

/**
 * Created by Connecthings on 17/06/16.
 */
public class CycledLeScannerScreenState extends CycledLeScanner implements ScreenStateListener{

    public static final int DELAY=40*1000;

    private Runnable mCancelScanningRun;

    public CycledLeScannerScreenState(Context context, ScanPeriods activePeriods, ScanPeriods passivePeriods, boolean backgroundFlag, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        super(context, activePeriods, passivePeriods, backgroundFlag, cycledLeScanCallback, crashResolver);
    }

    public void updateApplicationStatus(boolean backgroundFlag) {
        cancelRunnableCancelScanning();
        super.updateApplicationStatus(backgroundFlag);
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

    private void cancelRunnableCancelScanning(){
        cancelRunnable(mCancelScanningRun);
    }

    private void launchBackgroundScanning(){
        if(getBackgroundFlag()){
            cancelNextCycledRunnable();
            cancelRunnableCancelScanning();
            this.updateActiveMode(true);
            mCancelScanningRun = new Runnable() {
                @Override
                public void run() {
                    updateActiveMode(false);
                }
            };
            scheduleRunnable(mCancelScanningRun, DELAY);
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
}
