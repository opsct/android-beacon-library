package com.connecthings.altbeacon.beacon.client.batch;

import com.connecthings.altbeacon.beacon.Identifier;

import java.util.List;

/**
 * Created by Connecthings on 28/09/16.
 */
public interface BeaconEphemeralIdentifier {

    List<Identifier> getEphemeralIdentifier();

}
