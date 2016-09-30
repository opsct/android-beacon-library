package com.connecthings.altbeacon.beacon.client.batch;

import com.connecthings.altbeacon.beacon.Identifier;

import java.util.List;

/**
 * Created by Connecthings on 28/09/16.
 */
public interface BeaconIdentifiers {


    List<Identifier> getStaticIdentifiers();

    List<Identifier> getEphemeralIdentifiers();

    boolean hasEphemeralIdentifiers();

}
