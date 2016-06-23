package org.altbeacon.beacon.service.scanner.optimizer;

import org.altbeacon.beacon.Region;

/**
 * Created by Connecthings on 23/06/16.
 */
public interface CycledMonitorNotifier {


    /**
     * Called when at least one beacon in a <code>Region</code> is visible.
     * @param region a Region that defines the criteria of beacons to look for
     */
    void didEnterRegion(Region region);

    /**
     * Called when no beacons in a <code>Region</code> are visible.
     * @param region a Region that defines the criteria of beacons to look for
     */
    void didExitRegion(Region region);

    /**
     * Called when a region contain a beacon
     * It can't be a new entry
     * @param region a Region that defines the criteria of beacons to look for
     */
    void regionWithBeaconInside(Region region);

}
