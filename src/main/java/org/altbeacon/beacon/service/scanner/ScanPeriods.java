package org.altbeacon.beacon.service.scanner;

/**
 */
public class ScanPeriods {

    private long scanPeriod;
    private long betweenScanPeriod;

    public ScanPeriods(long betweenScanPeriod, long scanPeriod) {
        this.betweenScanPeriod = betweenScanPeriod;
        this.scanPeriod = scanPeriod;
    }

    public long getBetweenScanPeriod() {
        return betweenScanPeriod;
    }

    public long getScanPeriod() {
        return scanPeriod;
    }
}
