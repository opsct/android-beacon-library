/**
 * Radius Networks, Inc.
 * http://www.radiusnetworks.com
 *
 * @author David G. Young
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.connecthings.altbeacon.beacon.service;


import android.os.Parcel;
import android.os.Parcelable;

import com.connecthings.altbeacon.beacon.Beacon;
import com.connecthings.altbeacon.beacon.Region;
import com.connecthings.altbeacon.beacon.client.batch.BeaconContentFetchStatus;
import com.connecthings.altbeacon.beacon.logging.LogManager;

import java.util.ArrayList;
import java.util.Collection;


public class RangingData<BeaconContent extends Parcelable> implements Parcelable {

    private static final String TAG = "RangingData";
    private final Collection<Beacon> beacons;
    private final Collection<BeaconContent> contents;
    private final BeaconContentFetchStatus status;
    private final Region region;

    public RangingData (Collection<Beacon> beacons, Collection<BeaconContent> contents, BeaconContentFetchStatus status, Region region) {
        synchronized (beacons) {
            this.beacons =  beacons;
        }
        this.contents = contents;
        this.status = status;
        this.region = region;
    }

    public Collection<Beacon> getBeacons() {
        return beacons;
    }
    public Region getRegion() {
        return region;
    }

    public BeaconContentFetchStatus getStatus() {
        return status;
    }

    public Collection<BeaconContent> getContents() {
        return contents;
    }

    @Override
    public int describeContents() {
        return 0;
    }
    public void writeToParcel(Parcel out, int flags) {
        LogManager.d(TAG, "writing RangingData");
        out.writeParcelable(region, flags);
        out.writeString(status.toString());
        out.writeParcelableArray(beacons.toArray(new Parcelable[0]), flags);
        out.writeParcelableArray(contents.toArray(new Parcelable[0]), flags);
        LogManager.d(TAG, "done writing RangingData");

    }

    RangingData(Parcel in) {
        LogManager.d(TAG, "parsing RangingData");
        region = in.readParcelable(Region.class.getClassLoader());
        status = BeaconContentFetchStatus.valueOf(in.readString());
        LogManager.d(TAG, "parsing rd beacons start");
        Parcelable[] parcelables  = in.readParcelableArray(Beacon.class.getClassLoader());
        beacons = new ArrayList<Beacon>(parcelables.length);
        for (int i = 0; i < parcelables.length; i++) {
            beacons.add((Beacon)parcelables[i]);
        }
        LogManager.d(TAG, "parsing rd beacons end");
        parcelables  = in.readParcelableArray(Parcelable.class.getClassLoader());
        contents = new ArrayList<BeaconContent>(parcelables.length);
        for (int i = 0; i < parcelables.length; i++) {
            contents.add((BeaconContent) parcelables[i]);
        }
        LogManager.d(TAG, "parsing rd contents end");
    }

    public static final Parcelable.Creator<RangingData> CREATOR
            = new Parcelable.Creator<RangingData>() {
        public RangingData createFromParcel(Parcel in) {
            return new RangingData(in);
        }

        public RangingData[] newArray(int size) {
            return new RangingData[size];
        }
    };

}
