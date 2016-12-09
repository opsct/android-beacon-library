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
package com.connecthings.altbeacon.beacon;


import com.connecthings.altbeacon.beacon.client.batch.BeaconContentFetchStatus;
import com.connecthings.altbeacon.beacon.client.batch.BeaconIdentifiers;
import android.support.annotation.NonNull;


import java.util.Collection;

/**
 * This interface is implemented by classes that receive beacon ranging notifications with the content associated to the beacons
 *
 * @see BeaconManager#addRangeContentNotifier(RangeContentNotifier)
 * @see BeaconManager#startRangingBeaconsInRegion(Region region)
 * @see Region
 * @see Beacon
 *
 * @author Connecthings
 *
 */
public interface RangeContentNotifier<BeaconContent extends BeaconIdentifiers> {

    /**
     *
     * @param contents
     * @param noContentAssociateToBeacons
     * @param fetchStatus
     * @param region
     */
    public void didRangeBeaconsInRegion(@NonNull Collection<Beacon> beacons, @NonNull Collection<BeaconContent> contents, @NonNull Collection<Beacon> noContentAssociateToBeacons, @NonNull BeaconContentFetchStatus fetchStatus, @NonNull Region region);
}
