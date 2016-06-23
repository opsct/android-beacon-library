package org.altbeacon.beacon.service.scanner;

import android.os.Parcel;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.scanner.optimizer.CycledMonitorNotifier;
import org.altbeacon.beacon.service.scanner.optimizer.OptimizerScanPeriods;
import org.altbeacon.beacon.service.scanner.optimizer.CountRegions;
import org.altbeacon.beacon.service.scanner.screenstate.ScreenStateListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Connecthings on 17/06/16.
 */
public class CycledLeScannerScreenStateOptimizer extends CycledLeScanner implements ScreenStateListener, CycledMonitorNotifier{

    private static final String TAG = "CycledScreenStateOptimizer";
    private static final int DELAY_ACTION_ENTER_EXIT_REGION = 1000;

    private final int mActiveScanPeriodOnScreenStateSwitch;
    private final CountRegions countRegions = new CountRegions();
    private boolean activeFromScreenState = false;
    private boolean screenOn;
    private List<OptimizerScanPeriods> optimizerScanPeriodsList;
    private int currentScanPeriodsPosition = 0;
    private final Object lockScanPeriodsPosition = new Object();
    private int backClockPositionOnScreenStateSwitch;
    private int backClockPositionOnNewRegionEntry;

    private final Runnable mStopScanningRunnable = new Runnable() {
        @Override
        public void run() {
            stopActiveScanning();
        }
    };

    private final Runnable mNextScanPeriodRunnable = new Runnable() {
        @Override
        public void run() {
            goNextScanPeriods();
        }
    };

    private final Runnable mOnEnterExitRegionRunnable = new Runnable() {
        @Override
        public void run() {
            doOnEnterOrExitRegion();
        }
    };

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
        clockScanPeriodsList.add(new OptimizerScanPeriods(2 * 1000 * 60, 1000 * 60, pourcentageOfAdditionalPauseWhenScreenOff, 20 * 1000, 5 * 1000));
        clockScanPeriodsList.add(new OptimizerScanPeriods(3 * 1000 * 60, 2500 * 60, pourcentageOfAdditionalPauseWhenScreenOff, 20 * 1000, 6500));
        clockScanPeriodsList.add(new OptimizerScanPeriods(5 * 1000 * 60, 5000 * 60, pourcentageOfAdditionalPauseWhenScreenOff, 20 * 1000, 7500));
        clockScanPeriodsList.add(new OptimizerScanPeriods(5 * 1000 * 60, 5000 * 60, pourcentageOfAdditionalPauseWhenScreenOff, 20 * 1000, 10 * 1000));
        clockScanPeriodsList.add(new OptimizerScanPeriods(15 * 1000 * 60, 5000 * 60, pourcentageOfAdditionalPauseWhenScreenOff, 20 * 1000, 20 * 1000));
        clockScanPeriodsList.add(new OptimizerScanPeriods(15 * 1000 * 60, 5000 * 60, pourcentageOfAdditionalPauseWhenScreenOff, 20 * 1000, 30

                * 1000));
        clockScanPeriodsList.add(new OptimizerScanPeriods(15 * 1000 * 60, 5000 * 60, pourcentageOfAdditionalPauseWhenScreenOff, (long) (2.5 * 60 * 1000), 20 * 1000));
        return clockScanPeriodsList;
    }

    public void updateCycledParameter(CycledParameter cycledParameter) {
        cancelOnEnterExitRegionRunnable();
        cancelNextScanPeriodRunnable();
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

    protected long calculateNextStopCycleDelayBackground(){
        if(activeFromScreenState) {
            LogManager.d(TAG, "nextStopCycleDelay - ScreenUpdate - stop cycle is locked");
            return 1000;
        } else if(countRegions.isIn()){
            LogManager.d(TAG, "nextStopCycleDelay - region in - used the current scanPeriod %s", currentScanPeriodsPosition);
            return super.calculateNextStopCycleDelayBackground();
        }else{
            LogManager.d(TAG, "nextStopCycleDelay - no beacon - stop cycle immediatly");
            return 0;
        }
    }

    private void cancelOnEnterExitRegionRunnable(){
        cancelRunnable(mOnEnterExitRegionRunnable);
    }

    private void cancelNextScanPeriodRunnable(){
        cancelRunnable(mNextScanPeriodRunnable);
    }

    private void cancelStopScanningRunnable(){
        activeFromScreenState = false;
        cancelRunnable(mStopScanningRunnable);
    }

    private void stopActiveScanning(){
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

    private void goNextScanPeriods(){
        synchronized (lockScanPeriodsPosition) {
            LogManager.d(TAG, "goNextScanPeriods - currentScanPeriodsPosition: %s", currentScanPeriodsPosition);
            if (currentScanPeriodsPosition < optimizerScanPeriodsList.size() - 1) {
                currentScanPeriodsPosition++;
                LogManager.d(TAG, "goNextScanPeriods - update currentScanPosition: %s", currentScanPeriodsPosition);
                updateMode(optimizerScanPeriodsList.get(currentScanPeriodsPosition).getScanPeriods(screenOn), false);
                planNextOptimizerScanPeriods();
            }
        }
    }

    private synchronized void planNextOptimizerScanPeriods(){
        LogManager.d(TAG, "planNextOptimizerScanPeriods - currentScanPeriodsPosition: %s", currentScanPeriodsPosition);
        if(currentScanPeriodsPosition < optimizerScanPeriodsList.size() -1){
            OptimizerScanPeriods nextPeriods = optimizerScanPeriodsList.get(currentScanPeriodsPosition + 1);
            long nextTime = nextPeriods.getPeriodDuration();
            LogManager.d(TAG, "planNextOptimizerScanPeriods schedule next ScanPeriodRunnable - next time %s", nextTime);
            scheduleRunnable(mNextScanPeriodRunnable, nextTime);
        }
    }

    private void launchActiveBackgroundScanning(){
        LogManager.d(TAG, "launchActiveBackgroundScanning - background flag %s", getBackgroundFlag());
        if(getBackgroundFlag()){
            cancelNextScanPeriodRunnable();
            cancelNextCycledRunnable();
            cancelStopScanningRunnable();
            this.updateMode(true);
            LogManager.d(TAG, "schedule an active scan period");
            scheduleRunnable(mStopScanningRunnable, mActiveScanPeriodOnScreenStateSwitch);
            activeFromScreenState = true;
            scanLeDevice(true);
        }
    }

    @Override
    public void onScreenOn() {
        screenOn = true;
        LogManager.d(TAG, "onScreenOn");
        launchActiveBackgroundScanning();
    }

    @Override
    public void onScreenOff() {
        screenOn = false;
        LogManager.d(TAG, "onScreenOff");
       launchActiveBackgroundScanning();
    }

    public void doOnEnterOrExitRegion(){
        LogManager.d(TAG, "doOnEnterOrExitRegion - backegound: %s, in: %s", getBackgroundFlag(), countRegions.isIn());
        if (getBackgroundFlag() && countRegions.isIn()) {
            synchronized (lockScanPeriodsPosition) {
                LogManager.d(TAG, "doOnEnterOrExitRegion - currentScanPeriodPosition %s", currentScanPeriodsPosition);
                int scanPeriodPosition = Math.min(currentScanPeriodsPosition, backClockPositionOnNewRegionEntry);
                LogManager.d(TAG, "doOnEnterOrExitRegion - min scanPeriodPosition %s", scanPeriodPosition);
                if (scanPeriodPosition < optimizerScanPeriodsList.size()) {
                    if (scanPeriodPosition == currentScanPeriodsPosition) {
                        OptimizerScanPeriods optimizerScanPeriods = optimizerScanPeriodsList.get(currentScanPeriodsPosition);
                        if (countRegions.isOneIntervalLessThen(optimizerScanPeriods.getMaxDelayToSwitchToPreviousPeriod()) && currentScanPeriodsPosition > 0) {
                            scanPeriodPosition--;
                            LogManager.d(TAG, "doOnEnterOrExitRegion - back previous interval: %s", scanPeriodPosition);
                        }
                    }
                    currentScanPeriodsPosition = scanPeriodPosition;
                    updateMode(optimizerScanPeriodsList.get(currentScanPeriodsPosition).getScanPeriods(screenOn), false);
                    LogManager.d(TAG, "doOnEnterOrExitRegion - update currentScanPeriodPosition: %s", currentScanPeriodsPosition);
                    cancelNextScanPeriodRunnable();
                    planNextOptimizerScanPeriods();
                }
            }
        }
    }

    public void actionOnEnterOrExitRegion(){
        LogManager.d(TAG, "actionOnEnterOrExitRegion - background: %s, activeFromScreenState: %s", getBackgroundFlag(), activeFromScreenState);
        cancelOnEnterExitRegionRunnable();
        if(getBackgroundFlag() && !activeFromScreenState) {
            LogManager.d(TAG, "schedule an enter exit region");
            scheduleRunnable(mOnEnterExitRegionRunnable, DELAY_ACTION_ENTER_EXIT_REGION);
        }
    }

    @Override
    public void didEnterRegion(Region region) {
        LogManager.d(TAG, "enter region %s", region);
        countRegions.add(region);
        actionOnEnterOrExitRegion();
    }

    @Override
    public void didExitRegion(Region region) {
        LogManager.d(TAG, "exit region %s", region);
        countRegions.remove(region);
        actionOnEnterOrExitRegion();
    }

    @Override
    public void regionWithBeaconInside(Region region) {
        int currentCount = countRegions.getCount();
        countRegions.add(region);
        if(countRegions.getCount()>currentCount){
            LogManager.d(TAG, "this region is dected with beacon inside - region: %s", region);
            actionOnEnterOrExitRegion();
        }
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
