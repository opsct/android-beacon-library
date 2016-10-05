package org.altbeacon.beacon.client.batch;

import org.altbeacon.beacon.Beacon;

import java.util.Collection;
import java.util.List;

/**
 * Created by Connecthings on 27/09/16.
 */
public interface BeaconDataBatchProvider<BeaconContent extends BeaconIdentifiers> {

    public boolean fetchEphemeralIds();

    public void fetch(Collection<Beacon<BeaconContent>> beacons, BeaconDataBatchNotifier<BeaconContent> beaconDataBatchNotifier);

}
