package org.altbeacon.beacon.client.batch;

import org.altbeacon.beacon.Beacon;

import java.util.List;

/**
 * Created by Connecthings on 27/09/16.
 */
public interface BeaconDataBatchProvider<BeaconContent extends BeaconIdentifiers> {

    public void fetch(List<Beacon> beacons, BeaconDataBatchNotifier<BeaconContent> beaconDataBatchNotifier);

}
