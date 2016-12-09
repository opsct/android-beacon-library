package com.connecthings.altbeacon.beacon.client.batch;

import com.connecthings.altbeacon.beacon.Beacon;
import android.support.annotation.NonNull;


/**
 * Can be implemented by a BeaconContent (the content attached to a beacon)
 * Can be used for example to update information about RSSI and distance
 *
 * Created by Connecthings on 19/10/16.
 */
public interface UpdateBeacon {

    public void updateBeacon(@NonNull Beacon beacon);

}
