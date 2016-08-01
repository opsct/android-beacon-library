package org.altbeacon.beacon.service.scanner;

import android.os.Parcel;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.scanner.optimizer.CycledMonitorNotifier;
import org.altbeacon.beacon.service.scanner.optimizer.GroupOptimizerScanPeriods;
import org.altbeacon.beacon.service.scanner.optimizer.OptimizerScanPeriods;
import org.altbeacon.beacon.service.scanner.optimizer.CountRegions;
import org.altbeacon.beacon.service.scanner.screenstate.ScreenStateListener;

import java.security.acl.Group;
import java.util.ArrayList;
import java.util.List;

/**
 * The idea behind the <code>CycledLeScannerScreenStateOptimizer</code> is to adapt the scanning frequency to the situation.
 *
 * If the mobile phone is inside a beacon region, and there is many entry and exit of regions, it means it's interesting to scan more frequently.
 * At contrary, if the screen is Off and the mobile is not inside a beacon region, the <code>CycledLeScannerScreenStateOptimizer</code> will scan at a low frequency
 * until the mobile screen goes On where it will scan at high frequency for a predefined period.
 * If an entry or exit of region is detected during this period, next the scan frequency will stay at a good frequency.
 * If no entry or exit of region is detected, the frequency will slowly go down.
 *
 *
 * To achieve this, the <code>CycledLeScannerScreenStateOptimizer</code> manages a list of <code>GroupOptimizerScanPeriods</code>.
 * The <code>GroupOptimizerScanPeriods</code> permits to define various <code>ScanPeriods</code> depending of the situation:
 * <ul>
 *     <li>Is the screen on ? is the screen Off ?</li>
 *     <li>Are there nearby beacons ?</li>
 * </ul>
 *
 * The list of <code>GroupOptimizerScanPeriods</code> permits to adapt the scan frequency depending of the frequency of entry and exit regions.
 * The first <code>GroupOptimizerScanPeriods</code> of the list get an higher scan frequency than the last elements.
 *
 *
 * Created by Connecthings on 17/06/16.
 */
public class
CycledLeScannerScreenStateOptimizer extends CycledLeScanner implements ScreenStateListener, CycledMonitorNotifier{

    private static final String TAG = "CycledScreenStateOptimizer";
    private static final int DELAY_ACTION_ENTER_EXIT_REGION = 1000;

    private final int mActiveScanPeriodOnScreenStateSwitch;
    private final CountRegions countRegions = new CountRegions();
    private boolean activeFromScreenState = false;
    private boolean screenOn;
    private List<GroupOptimizerScanPeriods> optimizerScanPeriodsList;
    private int optimizerScanPeriodsSizeMinus;
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
    private boolean planEnterExitRegion;

    public CycledLeScannerScreenStateOptimizer() {
        this(new ScanPeriods(BeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD, BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD),
                CycledLeScannerScreenState.DELAY, null, 3, 2);
    }


    public CycledLeScannerScreenStateOptimizer(ScanPeriods activePeriods,
                                                int activeScanPeriodOnScreenStateSwitch,
                                                int backClockPositionOnScreenStateSwitch,
                                                int backClockPositionOnNewRegionEntry
                                                ) {
        this(activePeriods, activeScanPeriodOnScreenStateSwitch, null, backClockPositionOnScreenStateSwitch, backClockPositionOnScreenStateSwitch);
    }

    private CycledLeScannerScreenStateOptimizer(ScanPeriods activePeriods,
                                               int activeScanPeriodOnScreenStateSwitch,
                                               List<GroupOptimizerScanPeriods> clockScanPeriodsList,
                                               int backClockPositionOnScreenStateSwitch,
                                               int backClockPositionOnNewRegionEntry) {
        super(activePeriods,  new ScanPeriods(BeaconManager.DEFAULT_BACKGROUND_SCAN_PERIOD, BeaconManager.DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD));
        mActiveScanPeriodOnScreenStateSwitch = activeScanPeriodOnScreenStateSwitch;
        this.optimizerScanPeriodsList = clockScanPeriodsList == null ? initDefaultClockScanPeriods():clockScanPeriodsList;
        optimizerScanPeriodsSizeMinus = optimizerScanPeriodsList.size() - 1;
        this.backClockPositionOnScreenStateSwitch = backClockPositionOnScreenStateSwitch;
        this.backClockPositionOnNewRegionEntry = backClockPositionOnNewRegionEntry;
        this.planEnterExitRegion = false;
    }

    private List<GroupOptimizerScanPeriods> initDefaultClockScanPeriods(){
        ArrayList<GroupOptimizerScanPeriods> clockScanPeriodsList = new ArrayList<>();
        /**
         * Period 1
         */
        OptimizerScanPeriods defaultScanPeriods1 = new OptimizerScanPeriods(60 * 1000 * 2, 2 * 1000 * 60, 30 * 1000 * 60, 10 * 1000);
        GroupOptimizerScanPeriods groupOptimizerScanPeriods1 = new GroupOptimizerScanPeriods(defaultScanPeriods1);
        groupOptimizerScanPeriods1.addOptimizerScanPeriods(true, false, new OptimizerScanPeriods(2 * 1000 * 60, 2 * 1000 * 60, 30 * 1000, 20 * 1000 ));
        groupOptimizerScanPeriods1.addOptimizerScanPeriods(false, true, new OptimizerScanPeriods(2 * 1000 * 60, 2 * 1000 * 60, 30 * 1000, 20 * 1000 ));
        groupOptimizerScanPeriods1.addOptimizerScanPeriods(false, false, new OptimizerScanPeriods(2 * 1000 * 60, 2 * 1000 * 60, 30 * 1000, 30 * 1000 ));
        /**
         * Period 2
         */
        OptimizerScanPeriods defaultScanPeriods2 = new OptimizerScanPeriods(60 * 1000 * 2, 2 * 1000 * 60, 30 * 1000 * 60, 20 * 1000 * 60);
        GroupOptimizerScanPeriods groupOptimizerScanPeriods2 = new GroupOptimizerScanPeriods(defaultScanPeriods2);
        groupOptimizerScanPeriods1.addOptimizerScanPeriods(true, false, new OptimizerScanPeriods(2 * 1000 * 60, 2 * 1000 * 60, 30 * 1000, 30 * 1000 ));
        groupOptimizerScanPeriods1.addOptimizerScanPeriods(false, true, new OptimizerScanPeriods(2 * 1000 * 60, 2 * 1000 * 60, 30 * 1000, 30 * 1000 ));
        groupOptimizerScanPeriods1.addOptimizerScanPeriods(false, false, new OptimizerScanPeriods(2 * 1000 * 60, 2 * 1000 * 60, 30 * 1000, 40 * 1000 ));
        /**
         * Period 3
         */
        OptimizerScanPeriods defaultScanPeriods3 = new OptimizerScanPeriods(60 * 1000 * 2, 2 * 1000 * 60, 30 * 1000 * 60, 30 * 1000 * 60);
        GroupOptimizerScanPeriods groupOptimizerScanPeriods3 = new GroupOptimizerScanPeriods(defaultScanPeriods3);
        groupOptimizerScanPeriods1.addOptimizerScanPeriods(true, false, new OptimizerScanPeriods(2 * 1000 * 60, 2 * 1000 * 60, 20 * 1000, 40 * 1000 ));
        groupOptimizerScanPeriods1.addOptimizerScanPeriods(false, true, new OptimizerScanPeriods(2 * 1000 * 60, 2 * 1000 * 60, 20 * 1000, 40 * 1000 ));
        groupOptimizerScanPeriods1.addOptimizerScanPeriods(false, false, new OptimizerScanPeriods(2 * 1000 * 60, 2 * 1000 * 60, 20 * 1000, 50 * 1000 ));
        /**
         * Period 4
         */
        OptimizerScanPeriods defaultScanPeriods4 = new OptimizerScanPeriods(60 * 1000 * 2, 2 * 1000 * 60, 30 * 1000 * 60, 40 * 1000 * 60);
        GroupOptimizerScanPeriods groupOptimizerScanPeriods4 = new GroupOptimizerScanPeriods(defaultScanPeriods4);
        groupOptimizerScanPeriods1.addOptimizerScanPeriods(true, false, new OptimizerScanPeriods(2 * 1000 * 60, 2 * 1000 * 60, 20 * 1000, 1 * 1000 * 60));
        groupOptimizerScanPeriods1.addOptimizerScanPeriods(false, true, new OptimizerScanPeriods(2 * 1000 * 60, 2 * 1000 * 60, 20 * 1000, 1 * 1000 * 60));
        groupOptimizerScanPeriods1.addOptimizerScanPeriods(false, false, new OptimizerScanPeriods(2 * 1000 * 60, 2 * 1000 * 60, 20 * 1000, 2 * 1000 * 60));
        /**/
        clockScanPeriodsList.add(groupOptimizerScanPeriods1);
        clockScanPeriodsList.add(groupOptimizerScanPeriods2);
        clockScanPeriodsList.add(groupOptimizerScanPeriods3);
        clockScanPeriodsList.add(groupOptimizerScanPeriods4);
        return clockScanPeriodsList;
    }

    public void updateCycledParameter(CycledParameter cycledParameter) {
        cancelOnEnterExitRegionRunnable();
        cancelNextScanPeriodRunnable();
        cancelStopScanningRunnable();
        super.updateCycledParameter(cycledParameter);
        if(cycledParameter.getBackgroundFlag()){
            stopActiveScanning();
        }
    }


    protected long calculateNextScanLeDeviceDelayBackground(){
        if(activeFromScreenState) {
            LogManager.d(TAG, "nextScanDelay - ScreenUpdate - scan start immediatly");
            return 0;
        }else {
            LogManager.d(TAG, "nextScanDelay - region in or screen on - used the current scanPeriod %s / %s", currentScanPeriodsPosition, optimizerScanPeriodsSizeMinus);
            return super.calculateNextScanLeDeviceDelayBackground();
        }/*else if(countRegions.isIn() || screenOn){
            LogManager.d(TAG, "nextScanDelay - region in or screen on - used the current scanPeriod %s / %s", currentScanPeriodsPosition, optimizerScanPeriodsSizeMinus);
            return super.calculateNextScanLeDeviceDelayBackground();
        }else{
            LogManager.d(TAG, "nextScanDelay - no beacon - lock the scan");
            return 1000;
        }*/
    }

    protected long calculateNextStopCycleDelayBackground(){
        if(activeFromScreenState) {
            LogManager.d(TAG, "nextStopCycleDelay - ScreenUpdate - stop cycle is locked");
            return 1000;
        } else {
            LogManager.d(TAG, "nextStopCycleDelay - used the current scanPeriod %s / %s", currentScanPeriodsPosition, optimizerScanPeriodsSizeMinus);
            return super.calculateNextStopCycleDelayBackground();
        }/* else if(countRegions.isIn() || screenOn){
            LogManager.d(TAG, "nextStopCycleDelay - region in or screen on - used the current scanPeriod %s / %s", currentScanPeriodsPosition, optimizerScanPeriodsSizeMinus);
            return super.calculateNextStopCycleDelayBackground();
        }else{
            LogManager.d(TAG, "nextStopCycleDelay - no beacon - stop cycle immediatly");
            return 0;
        }*/
    }

    private void cancelOnEnterExitRegionRunnable(){
        planEnterExitRegion = false;
        cancelRunnable(mOnEnterExitRegionRunnable);
    }

    private void cancelNextScanPeriodRunnable(){
        cancelRunnable(mNextScanPeriodRunnable);
    }

    private void cancelStopScanningRunnable(){
        activeFromScreenState = false;
        cancelRunnable(mStopScanningRunnable);
    }


    private void calculateScanPeriodsPosition(){
        if (getBackgroundFlag()) {
            int backClockPosition = 0;
            if(activeFromScreenState && planEnterExitRegion){
                LogManager.d(TAG, "calculateScanPeriodsPosition - screenState and planEnterExit");
                backClockPosition = Math.min(backClockPositionOnScreenStateSwitch, backClockPositionOnNewRegionEntry);
            }else if(activeFromScreenState){
                LogManager.d(TAG, "calculateScanPeriodsPosition - screenState");
                backClockPosition = backClockPositionOnScreenStateSwitch;
            }else if(planEnterExitRegion){
                LogManager.d(TAG, "calculateScanPeriodsPosition - planEnterExit");
                backClockPosition = backClockPositionOnScreenStateSwitch;
            }
            cancelOnEnterExitRegionRunnable();
            LogManager.d(TAG, "calculateScanPeriodsPosition - before lock with backClock: %s", backClockPosition);
            synchronized (lockScanPeriodsPosition) {
                if(countRegions.isIn() || screenOn) {
                    LogManager.d(TAG, "calculateScanPeriodsPosition - currentScanPeriodPosition %s / %s", currentScanPeriodsPosition, optimizerScanPeriodsSizeMinus);
                    int scanPeriodPosition = Math.min(currentScanPeriodsPosition, backClockPosition);
                    LogManager.d(TAG, "calculateScanPeriodsPosition - min scanPeriodPosition %s / %s", scanPeriodPosition, optimizerScanPeriodsList.size());
                    if (scanPeriodPosition < optimizerScanPeriodsList.size()) {
                        if (scanPeriodPosition == currentScanPeriodsPosition) {
                            OptimizerScanPeriods optimizerScanPeriods = optimizerScanPeriodsList.get(currentScanPeriodsPosition).selectScanPeriods(screenOn, countRegions.isIn());
                            if (countRegions.isOneIntervalLessThen(optimizerScanPeriods.getMaxDelayToSwitchToPreviousPeriod()) && currentScanPeriodsPosition > 0) {
                                scanPeriodPosition--;
                                LogManager.d(TAG, "doObackScanPeriodsPositionnEnterOrExitRegion - back previous interval: %s / %s", scanPeriodPosition, optimizerScanPeriodsSizeMinus);
                            }
                        }
                        currentScanPeriodsPosition = scanPeriodPosition;
                        updateMode(optimizerScanPeriodsList.get(currentScanPeriodsPosition).selectScanPeriods(screenOn, countRegions.isIn()), false);
                        LogManager.d(TAG, "calculateScanPeriodsPosition - update currentScanPeriodPosition: %s / %s", currentScanPeriodsPosition, optimizerScanPeriodsSizeMinus);
                        cancelNextScanPeriodRunnable();
                        planNextOptimizerScanPeriods();
                    }
                }else{
                    LogManager.d(TAG, "calculateScanPeriodsPosition - no beacons in - update scan periods: %s / %s", currentScanPeriodsPosition, optimizerScanPeriodsSizeMinus);
                    updateMode(optimizerScanPeriodsList.get(currentScanPeriodsPosition).selectScanPeriods(screenOn, countRegions.isIn()), false);
                    cancelNextScanPeriodRunnable();
                    planNextOptimizerScanPeriods();
                }
            }
        }
    }

    private void stopActiveScanning(){
        LogManager.d(TAG, "stopActiveScanning and calculate new scan periods...");
        calculateScanPeriodsPosition();
        activeFromScreenState = false;
        this.updateMode(false);
    }

    private void goNextScanPeriods(){
        synchronized (lockScanPeriodsPosition) {
            LogManager.d(TAG, "goNextScanPeriods - currentScanPeriodsPosition: %s / %s", currentScanPeriodsPosition, optimizerScanPeriodsSizeMinus);
            if (currentScanPeriodsPosition < optimizerScanPeriodsSizeMinus) {
                currentScanPeriodsPosition++;
                LogManager.d(TAG, "goNextScanPeriods - update currentScanPosition: %s / %s", currentScanPeriodsPosition, optimizerScanPeriodsSizeMinus);
                updateMode(optimizerScanPeriodsList.get(currentScanPeriodsPosition).selectScanPeriods(screenOn, countRegions.isIn()), false);
                planNextOptimizerScanPeriods();
            }
        }
    }

    private synchronized void planNextOptimizerScanPeriods(){
        LogManager.d(TAG, "planNextOptimizerScanPeriods - currentScanPeriodsPosition: %s / %s", currentScanPeriodsPosition, optimizerScanPeriodsSizeMinus);
        if(currentScanPeriodsPosition < optimizerScanPeriodsList.size()){
            OptimizerScanPeriods nextPeriods = optimizerScanPeriodsList.get(currentScanPeriodsPosition).selectScanPeriods(screenOn, countRegions.isIn());
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
        calculateScanPeriodsPosition();
        planEnterExitRegion = false;
    }

    public void actionOnEnterOrExitRegion(){
        LogManager.d(TAG, "actionOnEnterOrExitRegion - background: %s, activeFromScreenState: %s", getBackgroundFlag(), activeFromScreenState);
        if(getBackgroundFlag() && !activeFromScreenState && !planEnterExitRegion) {
            LogManager.d(TAG, "schedule an enter/exit region");
            planEnterExitRegion=true;
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
