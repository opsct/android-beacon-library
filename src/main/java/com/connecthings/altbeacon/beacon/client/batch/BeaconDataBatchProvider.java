package com.connecthings.altbeacon.beacon.client.batch;

import com.connecthings.altbeacon.beacon.Beacon;
import android.support.annotation.NonNull;

import java.util.Collection;

/**
 * Created by Connecthings on 27/09/16.
 * A BatchProvider permits to download from a server or find in local DB content attached to beacons
 *
 * An application that would like to use the
 */
public interface BeaconDataBatchProvider<BeaconContent extends BeaconIdentifiers> {

    /**
     *
     * @return true if the provider manage the ephemeral ids
     */
    public boolean fetchEphemeralIds();

    /**
     * retrieve the content associated to the beacons
     * @param beacons the beacons for which the provider must find a content
     * @param beaconDataBatchNotifier callback to notify when the contents have been retrieved
     */
    public void fetch(@NonNull Collection<Beacon> beacons, @NonNull BeaconDataBatchNotifier<BeaconContent> beaconDataBatchNotifier);

}
