package com.connecthings.altbeacon.beacon.client.batch;

import com.connecthings.altbeacon.beacon.Beacon;

import java.util.List;

/**
 * Created by Connecthings on 27/09/16.
 */
public interface BeaconDataBatchProvider<BeaconContent extends BeaconContentIdentifier> {

    public void fetch(List<Beacon> beacons, BeaconDataBatchNotifier<BeaconContent> beaconDataBatchNotifier);

}
