package org.altbeacon.beacon.service.scanner;

import android.os.Parcel;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.scanner.optimizer.OptimizerScanPeriods;
import org.altbeacon.beacon.service.scanner.optimizer.CountRegions;
import org.altbeacon.beacon.service.scanner.screenstate.ScreenStateListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Connecthings on 17/06/16.
 */
public class CycledLeScannerScreenStateOptimizer extends CycledLeScanner implements ScreenStateListener, MonitorNotifier{

    private static final String TAG = "CycledScreenStateOptimizer";
    private static final int DELAY_ACTION_ENTER_EXIT_REGION = 1000;

    private Runnable mStopScanningRunnable, mNextScanPeriodRunnable, mOnEnterExitRegionRunnable;
    private final int mActiveScanPeriodOnScreenStateSwitch;
    private final CountRegions countRegions = new CountRegions();
    private boolean activeFromScreenState = false;
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
            LogManager.d(TAG, "nextScanDelay - ScreenUpdate - scan start immediatly");
            return 0;
        }else if(countRegions.isIn()){
            LogManager.d(TAG, "nextScanDelay - region in - used the current scanPeriod %s", currentScanPeriodsPosition);
            return super.calculateNextScanLeDeviceDelayBackground();
        }else{
            LogManager.d(TAG, "nextScanDelay - no beacon - lock the scan");
            return 1000;
        }
    }

    protected long calculateNextStopCyleDelayBackground(){
        if(activeFromScreenState) {
            LogManager.d(TAG, "nextStopCycleDelay - ScreenUpdate - stop cycle is locked");
            return 1000;
        } else if(countRegions.isIn()){
            LogManager.d(TAG, "nextStopCycleDelay - region in - used the current scanPeriod %s", currentScanPeriodsPosition);
            return super.calculateNextStopCyleDelayBackground();
        }else{
            LogManager.d(TAG, "nextStopCycleDelay - no beacon - stop cycle immediatly");
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
        LogManager.d(TAG, "planNextOptimizerScanPeriods - currentScanPeriodsPosition: %s", currentScanPeriodsPosition);
        if(currentScanPeriodsPosition < optimizerScanPeriodsList.size() -1){
            OptimizerScanPeriods nextPeriods = optimizerScanPeriodsList.get(currentScanPeriodsPosition + 1);
            mNextScanPeriodRunnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (lockScanPeriodsPosition) {
                        LogManager.d(TAG, "nextScanPeriodRunnable - currentScanPeriodsPosition: %s", currentScanPeriodsPosition);
                        if (currentScanPeriodsPosition < optimizerScanPeriodsList.size() - 1) {
                            currentScanPeriodsPosition++;
                            LogManager.d(TAG, "nextScanPeriodRunnable - update currentScanPosition: %s", currentScanPeriodsPosition);
                            updateMode(optimizerScanPeriodsList.get(currentScanPeriodsPosition).getScanPeriods(screenOn), false);
                            planNextOptimizerScanPeriods();
                        }
                    }
                }
            };
            long nextTime = nextPeriods.getPeriodDuration();
            LogManager.d(TAG, "planNextOptimizerScanPeriods schedule next ScanPeriodRunnable - next time %s", nextTime);
            scheduleRunnable(mNextScanPeriodRunnable, nextTime);
        }
    }

    private void launchActiveBackgroundScanning(){
        LogManager.d(TAG, "launchActiveBackgroundScanning - background flag %s", getBackgroundFlag());
        if(getBackgroundFlag()){
            activeFromScreenState = true;
            cancelScanPeriodClockRunnable();
            cancelNextCycledRunnable();
            cancelStopScanningRunnable();
            this.updateMode(true);
            mStopScanningRunnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (lockScanPeriodsPosition) {
                        LogManager.d(TAG, "stopScanningRunnable");
                        activeFromScreenState = false;
                        if(countRegions.isIn()){
                            LogManager.d(TAG, "still one beacon region active with currentScanPeriodsPosition %s", currentScanPeriodsPosition);
                            if (countRegions.isLastUpdateForLessThen(mActiveScanPeriodOnScreenStateSwitch)) {
                                LogManager.d(TAG, "an update region has occured while the active scan was launched -> currentScanPeriodsPosition back to 0");
                                currentScanPeriodsPosition = 0;
                            } else {
                                currentScanPeriodsPosition = Math.min(currentScanPeriodsPosition, backClockPositionOnScreenStateSwitch);
                                LogManager.d(TAG, "no update region -> currentScanPeriodsPosition back to %s", currentScanPeriodsPosition);
                            }
                            OptimizerScanPeriods optimizerScanPeriods = optimizerScanPeriodsList.get(currentScanPeriodsPosition);
                            updateMode(optimizerScanPeriodsList.get(currentScanPeriodsPosition).getScanPeriods(screenOn), false);
                            planNextOptimizerScanPeriods();
                        }else{
                            currentScanPeriodsPosition = optimizerScanPeriodsList.size() - 1;
                            LogManager.d(TAG, "not inside region -> the scanning will be locked - currentScanPeriodsPosition %s", currentScanPeriodsPosition);
                            updateMode(optimizerScanPeriodsList.get(currentScanPeriodsPosition).getScanPeriods(screenOn), false);
                        }
                    }
                }
            };
            LogManager.d(TAG, "schedule an active scan period");
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
        LogManager.d(TAG, "actionOnEnterOrExitRegion - background %s", getBackgroundFlag());
        if(getBackgroundFlag()) {
            cancelOnEnterExitRegionRunnable();
            mOnEnterExitRegionRunnable = new Runnable() {
                @Override
                public void run() {
                    LogManager.d(TAG, "onEnterRegion - backegound: %s, in: %s", getBackgroundFlag(), countRegions.isIn());
                    if (getBackgroundFlag() && countRegions.isIn()) {
                        synchronized (lockScanPeriodsPosition) {
                            LogManager.d(TAG, "onEnterRegion - currentScanPeriodPosition %s", currentScanPeriodsPosition);
                            int scanPeriodPosition = Math.min(currentScanPeriodsPosition, backClockPositionOnNewRegionEntry);
                            LogManager.d(TAG, "onEnterRegion - min scanPeriodPosition %s", scanPeriodPosition);
                            if (scanPeriodPosition < optimizerScanPeriodsList.size()) {
                                if (scanPeriodPosition == currentScanPeriodsPosition) {
                                    OptimizerScanPeriods optimizerScanPeriods = optimizerScanPeriodsList.get(currentScanPeriodsPosition);
                                    if (countRegions.isOneIntervalLessThen(optimizerScanPeriods.getMaxDelayToSwitchToPreviousPeriod()) && currentScanPeriodsPosition > 0) {
                                        scanPeriodPosition--;
                                        LogManager.d(TAG, "onEnterRegion - back previous interval: %s", scanPeriodPosition);
                                    }
                                }
                                currentScanPeriodsPosition = scanPeriodPosition;
                                updateMode(optimizerScanPeriodsList.get(currentScanPeriodsPosition).getScanPeriods(screenOn), false);
                                LogManager.d(TAG, "onEnterRegion - update currentScanPeriodPosition: %s", currentScanPeriodsPosition);
                                cancelScanPeriodClockRunnable();
                                planNextOptimizerScanPeriods();
                            }
                        }
                    }
                }
            };
            LogManager.d(TAG, "schedule an enter exit region");
            scheduleRunnable(mOnEnterExitRegionRunnable, DELAY_ACTION_ENTER_EXIT_REGION);
        }
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
