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

import android.os.Parcel;
import android.os.Parcelable;

import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.scanner.CycleParameter;

import java.io.Serializable;

public class StartRMData implements Serializable, Parcelable {
    private Region region;
    private CycleParameter cycleParameter;
    private String callbackPackageName;

    public StartRMData(Region region, String callbackPackageName) {
        this.region = region;
        this.callbackPackageName = callbackPackageName;
    }

    public StartRMData(CycleParameter cycleParameter){
        this.cycleParameter = cycleParameter;
    }

    public StartRMData(Region region, String callbackPackageName, CycleParameter cycleParameter) {
        cycleParameter = this.cycleParameter;
        this.region = region;
        this.callbackPackageName = callbackPackageName;
    }

    @Deprecated
    public StartRMData(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        cycleParameter = new CycleParameter(scanPeriod, betweenScanPeriod, backgroundFlag);
    }

    @Deprecated
    public StartRMData(Region region, String callbackPackageName, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        cycleParameter = new CycleParameter(scanPeriod, betweenScanPeriod, backgroundFlag);
        this.region = region;
        this.callbackPackageName = callbackPackageName;
    }

    public CycleParameter getCycleParameter(){ return cycleParameter; }
    public Region getRegionData() {
        return region;
    }
    public String getCallbackPackageName() {
        return callbackPackageName;
    }
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(region, flags);
        out.writeString(callbackPackageName);
        out.writeParcelable(cycleParameter, flags);
    }

    public static final Parcelable.Creator<StartRMData> CREATOR
            = new Parcelable.Creator<StartRMData>() {
        public StartRMData createFromParcel(Parcel in) {
            return new StartRMData(in);
        }

        public StartRMData[] newArray(int size) {
            return new StartRMData[size];
        }
    };

    private StartRMData(Parcel in) {
        region = in.readParcelable(StartRMData.class.getClassLoader());
        callbackPackageName = in.readString();
        cycleParameter = in.readParcelable(CycleParameter.class.getClassLoader());
    }

}
