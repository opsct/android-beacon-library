package org.altbeacon.beacon.service.scanner;

import android.os.Parcel;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.scanner.optimizer.OptimizerScanPeriods;
import org.altbeacon.beacon.service.scanner.optimizer.CountRegions;
import org.altbeacon.beacon.service.scanner.screenstate.ScreenStateListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Connecthings on 17/06/16.
 */
public class CycledLeScannerScreenStateOptimizer extends CycledLeScanner implements ScreenStateListener, MonitorNotifier{

    private static final int DELAY_ACTION_ENTER_EXIT_REGION = 1000;

    private Runnable mStopScanningRunnable, mNextScanPeriodRunnable, mOnEnterExitRegionRunnable;
    private final int mActiveScanPeriodOnScreenStateSwitch;
    private final CountRegions countRegions = new CountRegions();
    private boolean activeFromScreenState = false;
    private long lastScreenStateSwitch;
    private long lastRegionSwitch;
    private boolean screenOn;
    private List<OptimizerScanPeriods> optimizerScanPeriodsList;
    private int currentScanPeriodsPosition = 0;
    private final Object lockScanPeriodsPosition = new Object();
    private int backClockPositionOnScreenStateSwitch;
    private int backClockPositionOnNewRegionEntry;

    public CycledLeScannerScreenStateOptimizer() {
        this(new ScanPeriods(BeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD, BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD),
                CycledLeScannerScreenState.DELAY, null, 0.50f, 5, 4);
    }

    public CycledLeScannerScreenStateOptimizer(ScanPeriods activePeriods,
                                                int activeScanPeriodOnScreenStateSwitch,
                                                List<OptimizerScanPeriods> clockScanPeriodsList,
                                                int backClockPositionOnScreenStateSwitch,
                                                int backClockPositionOnNewRegionEntry) {
        this(activePeriods, activeScanPeriodOnScreenStateSwitch, clockScanPeriodsList, 0, backClockPositionOnNewRegionEntry, backClockPositionOnScreenStateSwitch);
    }

    public CycledLeScannerScreenStateOptimizer(ScanPeriods activePeriods,
                                                int activeScanPeriodOnScreenStateSwitch,
                                                int backClockPositionOnScreenStateSwitch,
                                                int backClockPositionOnNewRegionEntry,
                                                float pourcentageOfAdditionalPauseWhenScreenOff) {
        this(activePeriods, activeScanPeriodOnScreenStateSwitch, null,  pourcentageOfAdditionalPauseWhenScreenOff, backClockPositionOnScreenStateSwitch, backClockPositionOnScreenStateSwitch);
    }

    private CycledLeScannerScreenStateOptimizer(ScanPeriods activePeriods,
                                               int activeScanPeriodOnScreenStateSwitch,
                                               List<OptimizerScanPeriods> clockScanPeriodsList, float pourcentageOfAdditionalPauseWhenScreenOff,
                                               int backClockPositionOnScreenStateSwitch,
                                               int backClockPositionOnNewRegionEntry) {
        super(activePeriods,  new ScanPeriods(BeaconManager.DEFAULT_BACKGROUND_SCAN_PERIOD, BeaconManager.DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD));
        mActiveScanPeriodOnScreenStateSwitch = activeScanPeriodOnScreenStateSwitch;
        this.optimizerScanPeriodsList = clockScanPeriodsList == null ? initDefaultClockScanPeriods(pourcentageOfAdditionalPauseWhenScreenOff):clockScanPeriodsList;
        this.backClockPositionOnScreenStateSwitch = backClockPositionOnScreenStateSwitch;
        this.backClockPositionOnNewRegionEntry = backClockPositionOnNewRegionEntry;
    }

    private List<OptimizerScanPeriods> initDefaultClockScanPeriods(float pourcentageOfAdditionalPauseWhenScreenOff){
        ArrayList<OptimizerScanPeriods> clockScanPeriodsList = new ArrayList<>();
        clockScanPeriodsList.add(new OptimizerScanPeriods(0, 0, pourcentageOfAdditionalPauseWhenScreenOff, BeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD, BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD));
        clockScanPeriodsList.add(new OptimizerScanPeriods(2 * 1000 * 60, 1000 * 60, pourcentageOfAdditionalPauseWhenScreenOff, 5 * 1000, 20 * 1000));
        clockScanPeriodsList.add(new OptimizerScanPeriods(3 * 1000 * 60, 2500 * 60, pourcentageOfAdditionalPauseWhenScreenOff, 6500, 20 * 1000));
        clockScanPeriodsList.add(new OptimizerScanPeriods(5 * 1000 * 60, 5000 * 60, pourcentageOfAdditionalPauseWhenScreenOff, 7500, 20 * 1000));
        clockScanPeriodsList.add(new OptimizerScanPeriods(5 * 1000 * 60, 5000 * 60, pourcentageOfAdditionalPauseWhenScreenOff, 10 * 1000, 20 * 1000));
        clockScanPeriodsList.add(new OptimizerScanPeriods(15 * 1000 * 60, 5000 * 60, pourcentageOfAdditionalPauseWhenScreenOff, 20 * 1000, 20 * 1000));
        clockScanPeriodsList.add(new OptimizerScanPeriods(15 * 1000 * 60, 5000 * 60, pourcentageOfAdditionalPauseWhenScreenOff, 30 * 1000, 20 * 1000));
        clockScanPeriodsList.add(new OptimizerScanPeriods(15 * 1000 * 60, 5000 * 60, pourcentageOfAdditionalPauseWhenScreenOff, (long) (2.5 * 60 * 1000), 20 * 1000));
        return clockScanPeriodsList;
    }

    public void updateCycledParameter(CycledParameter cycledParameter) {
        cancelOnEnterExitRegionRunnable();
        cancelScanPeriodClockRunnable();
        cancelStopScanningRunnable();
        super.updateCycledParameter(cycledParameter);
    }


    protected long calculateNextScanLeDeviceDelayBackground(){
        if(activeFromScreenState) {
            return 0;
        }else if(countRegions.isIn()){
            return super.calculateNextScanLeDeviceDelayBackground();
        }else{

            return 1000;
        }
    }

    protected long calculateNextStopCyleDelayBackground(){
        if(activeFromScreenState) {
            return 1000;
        } else if(countRegions.isIn()){
            return super.calculateNextStopCyleDelayBackground();
        }else{
            return 0;
        }
    }

    private void cancelOnEnterExitRegionRunnable(){
        cancelRunnable(mOnEnterExitRegionRunnable);
    }

    private void cancelScanPeriodClockRunnable(){
        cancelRunnable(mNextScanPeriodRunnable);
    }

    private void cancelStopScanningRunnable(){
        cancelRunnable(mStopScanningRunnable);
    }

    private synchronized void planNextOptimizerScanPeriods(){
        if(currentScanPeriodsPosition < optimizerScanPeriodsList.size() -1){
            OptimizerScanPeriods nextPeriods = optimizerScanPeriodsList.get(currentScanPeriodsPosition + 1);
            mNextScanPeriodRunnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (lockScanPeriodsPosition) {
                        if (currentScanPeriodsPosition < optimizerScanPeriodsList.size() - 1) {
                            currentScanPeriodsPosition++;
                            updateMode(optimizerScanPeriodsList.get(currentScanPeriodsPosition).getScanPeriods(screenOn), false);
                            planNextOptimizerScanPeriods();
                        }
                    }
                }
            };
            long nextTime = nextPeriods.getPeriodDuration();
            scheduleRunnable(mNextScanPeriodRunnable, nextTime);
        }
    }

    private void launchActiveBackgroundScanning(){
        if(getBackgroundFlag()){
            activeFromScreenState = true;
            cancelScanPeriodClockRunnable();
            cancelNextCycledRunnable();
            cancelStopScanningRunnable();
            this.updateMode(true);
            mStopScanningRunnable = new Runnable() {
                @Override
                public void run() {
                    activeFromScreenState = false;
                    if(countRegions.isIn()){
                        synchronized (lockScanPeriodsPosition) {
                            if (countRegions.isLastUpdateForLessThen(mActiveScanPeriodOnScreenStateSwitch)) {
                                currentScanPeriodsPosition = 0;
                            } else {
                                currentScanPeriodsPosition = Math.min(currentScanPeriodsPosition, backClockPositionOnScreenStateSwitch);
                            }
                            OptimizerScanPeriods optimizerScanPeriods = optimizerScanPeriodsList.get(currentScanPeriodsPosition);
                            updateMode(optimizerScanPeriodsList.get(currentScanPeriodsPosition).getScanPeriods(screenOn), false);
                            planNextOptimizerScanPeriods();
                        }
                    }else{
                        updateMode(optimizerScanPeriodsList.get(optimizerScanPeriodsList.size() - 1).getScanPeriods(screenOn), false);
                    }

                }
            };
            scheduleRunnable(mStopScanningRunnable, mActiveScanPeriodOnScreenStateSwitch);
            scanLeDevice(true);
        }
    }

    @Override
    public void onScreenOn() {
        screenOn = true;
        launchActiveBackgroundScanning();
    }

    @Override
    public void onScreenOff() {
        screenOn = false;
       launchActiveBackgroundScanning();
    }

    public void actionOnEnterOrExitRegion(){
        cancelOnEnterExitRegionRunnable();
        mOnEnterExitRegionRunnable = new Runnable() {
            @Override
            public void run() {
                if(getBackgroundFlag() && countRegions.isIn()) {
                    synchronized (lockScanPeriodsPosition) {
                        int clockPosition = Math.min(currentScanPeriodsPosition, backClockPositionOnNewRegionEntry);
                        if (clockPosition < optimizerScanPeriodsList.size()) {
                            if (clockPosition == currentScanPeriodsPosition) {
                                OptimizerScanPeriods optimizerScanPeriods = optimizerScanPeriodsList.get(currentScanPeriodsPosition);
                                if (countRegions.isInForLessThan(optimizerScanPeriods.getMaxDelayToSwitchToPreviousPeriod())) {
                                    currentScanPeriodsPosition--;
                                }
                            }
                            cancelScanPeriodClockRunnable();
                            planNextOptimizerScanPeriods();
                        }
                    }
                }
            }
        };
        scheduleRunnable(mOnEnterExitRegionRunnable, DELAY_ACTION_ENTER_EXIT_REGION);
    }

    @Override
    public void didEnterRegion(Region region) {
        countRegions.add(region);
        actionOnEnterOrExitRegion();
    }

    @Override
    public void didExitRegion(Region region) {
        countRegions.remove(region);
        actionOnEnterOrExitRegion();
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
