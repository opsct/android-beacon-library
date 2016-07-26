package org.altbeacon.beacon.service.scanner.optimizer;

import org.altbeacon.beacon.service.scanner.ScanPeriods;

import java.util.HashMap;

/**
 * Created by Connecthings on 22/06/16.
 * Permits to manage a set of <code>OptimizerScanPeriods</code> in order to select the <code>OptimizerScanPeriods</code> that fits the situation
 */
public class GroupOptimizerScanPeriods {

    HashMap<String, OptimizerScanPeriods> scanPeriodsMap;
    private OptimizerScanPeriods defaultScanPeriods;

    public GroupOptimizerScanPeriods(OptimizerScanPeriods defaultScanPeriods) {
       scanPeriodsMap = new HashMap<>();
        this.defaultScanPeriods = defaultScanPeriods;
    }

    /**
     * Permits to register an <code>OptimizerScanPeriods</code> in relation with the situation;
     * @param isScreenOn : true if the screen is on
     * @param beaconsInRegion : true if there are nearby beacons
     * @param optimizerScanPeriods : the <code>OptimizerScanPeriods</code> to use in that situation
     */
    public void addOptimizerScanPeriods(boolean isScreenOn, boolean beaconsInRegion, OptimizerScanPeriods optimizerScanPeriods){
        scanPeriodsMap.put(generateKey(isScreenOn, beaconsInRegion), optimizerScanPeriods);
    }

    private String generateKey(boolean isScreenOn, boolean beaconsInRegion){
        return new StringBuilder().append(isScreenOn).append('|').append(beaconsInRegion).toString();
    }

    /**
     * Select the right scanning periods depending of the situation
     * @param isScreenOn - is the screen on
     * @param beaconsInRegion - true if any beacon dected in the regino
     * @return the <code>OptimizerScanPeriods</code> in relation with the the screen and beacons status
     */
    public OptimizerScanPeriods selectScanPeriods(boolean isScreenOn, boolean beaconsInRegion){
        OptimizerScanPeriods scanPeriods = scanPeriodsMap.get(generateKey(isScreenOn, beaconsInRegion));
        if(scanPeriods == null){
            scanPeriods = defaultScanPeriods;
        }
        return scanPeriods;
    }

}
