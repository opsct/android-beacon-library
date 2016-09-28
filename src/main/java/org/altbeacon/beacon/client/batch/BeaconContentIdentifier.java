package org.altbeacon.beacon.client.batch;

import android.os.Parcelable;

import org.altbeacon.beacon.Identifier;

import java.util.List;

/**
 * Created by Connecthings on 28/09/16.
 */
public abstract class BeaconContentIdentifier implements Parcelable{


    public abstract List<Identifier> getStaticIdentifier();

}
