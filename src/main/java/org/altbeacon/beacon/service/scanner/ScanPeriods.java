package org.altbeacon.beacon.service.scanner;

/**
 */
public class ScanPeriods {

    private long scanPeriod;
    private long betweenScanPeriod;

    public ScanPeriods(long scanPeriod,long betweenScanPeriod) {
        this.scanPeriod = scanPeriod;
        this.betweenScanPeriod = betweenScanPeriod;
    }

    public long getBetweenScanPeriod() {
        return betweenScanPeriod;
    }

    public long getScanPeriod() {
        return scanPeriod;
    }
}
