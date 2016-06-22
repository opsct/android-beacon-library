package org.altbeacon.beacon.service.scanner.optimizer;

import org.altbeacon.beacon.service.scanner.ScanPeriods;

/**
 * Created by Connecthings on 22/06/16.
 */
public class OptimizerScanPeriods {

    private long periodDuration;

    private long maxDelayToSwitchToPreviousPeriod;

    private ScanPeriods scanPeriodsScreenOn, scanPeriodsScreenOff;


    public OptimizerScanPeriods(long periodDuration, long maxDelayToSwitchToPreviousPeriod, float pourcentageOfAdditionalPauseWhenScreenOff,
                                long scanPeriod, long betweenScanPeriod) {
        this.periodDuration = periodDuration;
        this.maxDelayToSwitchToPreviousPeriod = maxDelayToSwitchToPreviousPeriod;
        this.scanPeriodsScreenOn = new ScanPeriods(scanPeriod, betweenScanPeriod);
        this.scanPeriodsScreenOff = new ScanPeriods(scanPeriod, (long) (betweenScanPeriod * (1.0+pourcentageOfAdditionalPauseWhenScreenOff)));
    }

    public long getPeriodDuration() {
        return periodDuration;
    }

    public long getMaxDelayToSwitchToPreviousPeriod() {
        return maxDelayToSwitchToPreviousPeriod;
    }

    public ScanPeriods getScanPeriods(boolean isScreenOn){
        if(isScreenOn) {
            return scanPeriodsScreenOn;
        }else{
            return scanPeriodsScreenOff;
        }
    }

}
