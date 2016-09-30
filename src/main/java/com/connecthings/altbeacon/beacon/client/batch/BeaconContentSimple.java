package com.connecthings.altbeacon.beacon.client.batch;

import android.os.Parcel;
import android.os.Parcelable;

import com.connecthings.altbeacon.beacon.Identifier;
import com.connecthings.altbeacon.beacon.logging.LogManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Connecthings on 30/09/16.
 */

public class BeaconContentSimple extends BeaconContentIdentifier{


    private List<Identifier> identifiers;

    public BeaconContentSimple(List<Identifier> identifiers){
        this.identifiers = identifiers;
    }

    private BeaconContentSimple(Parcel in){
        int staticSize = in.readInt();
        this.identifiers = new ArrayList<Identifier>(staticSize);
        for (int i = 0; i < staticSize; i++) {
            identifiers.add(Identifier.parse(in.readString()));
        }
    }

    @Override
    public List<Identifier> getStaticIdentifier() {
        return identifiers;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(identifiers.size());
        for (Identifier identifier: identifiers) {
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
