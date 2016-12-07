package org.altbeacon.beacon.client.batch;

import org.altbeacon.beacon.Identifier;

import java.util.List;

/**
 * Interface that must implement the BeaconContent (The content attached to the beacon)
 *
 * Created by Connecthings on 28/09/16.
 */
public interface BeaconIdentifiers {


    List<Identifier> getStaticIdentifiers();

    List<Identifier> getEphemeralIdentifiers();

    boolean hasEphemeralIdentifiers();

    boolean hasStaticIdentifiers();

}
