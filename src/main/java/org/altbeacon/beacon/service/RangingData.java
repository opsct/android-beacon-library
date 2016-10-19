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
package org.altbeacon.beacon.service;

import java.util.ArrayList;
import java.util.Collection;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.client.batch.BeaconBatchFetchInfo;
import org.altbeacon.beacon.client.batch.BeaconContentFetchStatus;
import org.altbeacon.beacon.client.batch.BeaconIdentifiers;
import org.altbeacon.beacon.logging.LogManager;

import android.os.Parcel;
import android.os.Parcelable;

public class RangingData<BeaconContent extends BeaconIdentifiers> implements Parcelable {
    private static final String TAG = "RangingData";
    private final Collection<Beacon> beacons;
    private BeaconBatchFetchInfo<BeaconContent> batchFetchInfo;
    private final Region region;


    public RangingData (Collection<Beacon> beacons, BeaconBatchFetchInfo<BeaconContent> batchFetchInfo, Region region) {
        synchronized (beacons) {
            this.beacons =  beacons;
        }
        this.batchFetchInfo = batchFetchInfo;
        this.region = region;
    }

    public Collection<Beacon> getBeacons() {
        return beacons;
    }
    public Region getRegion() {
        return region;
    }

    public BeaconBatchFetchInfo<BeaconContent> getBatchFetchInfo() {
        return batchFetchInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }
    public void writeToParcel(Parcel out, int flags) {
        LogManager.d(TAG, "writing RangingData");
        out.writeParcelable(region, flags);
        out.writeParcelableArray(beacons.toArray(new Parcelable[0]), flags);
        out.writeParcelable(batchFetchInfo, flags);
        LogManager.d(TAG, "done writing RangingData");

    }

    RangingData(Parcel in) {
        LogManager.d(TAG, "parsing RangingData");
        region = in.readParcelable(Region.class.getClassLoader());
        LogManager.d(TAG, "parsing rd beacons start");
        Parcelable[] parcelables  = in.readParcelableArray(Beacon.class.getClassLoader());
        beacons = new ArrayList<Beacon>(parcelables.length);
        for (int i = 0; i < parcelables.length; i++) {
            beacons.add((Beacon)parcelables[i]);
        }
        LogManager.d(TAG, "parsing batch fetch info");
        batchFetchInfo = in.readParcelable(BeaconBatchFetchInfo.class.getClassLoader());
        LogManager.d(TAG, "parsing batch fetch info");
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
