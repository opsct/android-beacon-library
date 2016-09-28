package org.altbeacon.beacon.client.batch;

import org.altbeacon.beacon.Identifier;

import java.util.List;

/**
 * Created by Connecthings on 28/09/16.
 */
public interface BeaconEphemeralIdentifier {

    List<Identifier> getEphemeralIdentifier();

}
