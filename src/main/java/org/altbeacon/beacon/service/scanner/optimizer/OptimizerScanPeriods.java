package org.altbeacon.beacon.service.scanner.optimizer;

import org.altbeacon.beacon.service.scanner.ScanPeriods;

/**
 * Created by Connecthings on 22/06/16.
 */
public class OptimizerScanPeriods {

    private long periodDuration;

    private long maxDelayToSwitchToPreviousPeriod;

    private ScanPeriods scanPeriodsReference, scanPeriodsScreenOff, getScanPeriodsScreenOnNoBeacon;


    public OptimizerScanPeriods(long periodDuration, long maxDelayToSwitchToPreviousPeriod,
                                float pourcentageOfAdditionalPauseWhenScreenOff, float pourcentageOfAdditionalPauseWhenScreenOnNoBeacon,
                                long scanPeriod, long betweenScanPeriod) {
        this.periodDuration = periodDuration;
        this.maxDelayToSwitchToPreviousPeriod = maxDelayToSwitchToPreviousPeriod;
        this.scanPeriodsReference = new ScanPeriods(scanPeriod, betweenScanPeriod);
        this.scanPeriodsScreenOff = new ScanPeriods(scanPeriod, (long) (betweenScanPeriod * (1.0+pourcentageOfAdditionalPauseWhenScreenOff)));
        this.getScanPeriodsScreenOnNoBeacon = new ScanPeriods(scanPeriod, (long) (betweenScanPeriod * (1.0+pourcentageOfAdditionalPauseWhenScreenOnNoBeacon)));
    }

    public long getPeriodDuration() {
        return periodDuration;
    }

    public long getMaxDelayToSwitchToPreviousPeriod() {
        return maxDelayToSwitchToPreviousPeriod;
    }

    public ScanPeriods selectScanPeriods(boolean isScreenOn, boolean beaconsInRegion){
        if(isScreenOn) {
            if(beaconsInRegion) {
                return scanPeriodsReference;
            }else{
                return getScanPeriodsScreenOnNoBeacon;
            }
        }else{
            return scanPeriodsScreenOff;
        }
    }

}
