package org.altbeacon.beacon.client.batch;

import android.os.Parcelable;

import org.altbeacon.beacon.Identifier;

import java.util.List;

/**
 * Created by Connecthings on 28/09/16.
 */
public interface BeaconIdentifiers {


    List<Identifier> getStaticIdentifiers();

    List<Identifier> getEphemeralIdentifiers();

    boolean hasEphemeralIdentifiers();

    boolean hasStaticIdentifiers();

}
