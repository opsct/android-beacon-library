package org.altbeacon.beacon.client.batch;

import android.os.Parcel;
import android.os.Parcelable;

import org.altbeacon.beacon.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Connecthings on 30/09/16.
 */

public class BeaconContentSimple implements BeaconIdentifiers, Parcelable {


    private List<Identifier> staticIdentifiers;
    private List<Identifier> ephemeralIdentifiers;

    public BeaconContentSimple(List<Identifier> ephemeralIdentifiers, List<Identifier> staticIdentifiers) {
        this.ephemeralIdentifiers = ephemeralIdentifiers==null?new ArrayList<Identifier>():ephemeralIdentifiers;
        this.staticIdentifiers = staticIdentifiers;
    }

    public BeaconContentSimple(List<Identifier> staticIdentifiers){
        this(null, staticIdentifiers);
    }

    private BeaconContentSimple(Parcel in){
        int staticSize = in.readInt();
        this.staticIdentifiers = new ArrayList<Identifier>(staticSize);
        for (int i = 0; i < staticSize; i++) {
            staticIdentifiers.add(Identifier.parse(in.readString()));
        }
    }

    @Override
    public List<Identifier> getStaticIdentifiers() {
        return staticIdentifiers;
    }

    @Override
    public List<Identifier> getEphemeralIdentifiers() {
        return ephemeralIdentifiers;
    }

    @Override
    public boolean hasEphemeralIdentifiers() {
        return ephemeralIdentifiers.size() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(staticIdentifiers.size());
        for (Identifier identifier: staticIdentifiers) {
            dest.writeString(identifier == null ? null : identifier.toString());
        }
    }

    public static final Parcelable.Creator<BeaconContentSimple> CREATOR
            = new Parcelable.Creator<BeaconContentSimple>() {
        public BeaconContentSimple createFromParcel(Parcel in) {
            return new BeaconContentSimple(in);
        }

        public BeaconContentSimple[] newArray(int size) {
            return new BeaconContentSimple[size];
        }
    };

}
