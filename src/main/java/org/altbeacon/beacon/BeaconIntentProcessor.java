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
package org.altbeacon.beacon;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.Intent;

import org.altbeacon.beacon.client.batch.BeaconContentFetchStatus;
import org.altbeacon.beacon.client.batch.BeaconIdentifiers;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.MonitoringData;
import org.altbeacon.beacon.service.RangingData;

import java.util.Set;

/**
 * Converts internal intents to notifier callbacks
 */
@TargetApi(3)
public class BeaconIntentProcessor extends IntentService {
    private static final String TAG = "BeaconIntentProcessor";

    public BeaconIntentProcessor() {
        super("BeaconIntentProcessor");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LogManager.d(TAG, "got an intent to process");

        MonitoringData monitoringData = null;
        RangingData rangingData = null;

        if (intent != null && intent.getExtras() != null) {
            monitoringData = (MonitoringData) intent.getExtras().get("monitoringData");
            rangingData = (RangingData) intent.getExtras().get("rangingData");
        }

        if (rangingData != null) {
            LogManager.d(TAG, "got ranging data");
            if (rangingData.getBeacons() == null) {
                LogManager.w(TAG, "Ranging data has a null beacons collection");
            }
            java.util.Collection<Beacon> beacons = rangingData.getBeacons();
            java.util.Collection<?> contents = rangingData.getContents();
            Region region = rangingData.getRegion();
            BeaconContentFetchStatus status = rangingData.getStatus();
            Set<RangeNotifier<? extends BeaconIdentifiers>> notifiers = BeaconManager.getInstanceForApplication(this).getRangingNotifiers();
            if (notifiers != null) {
                for(RangeNotifier notifier : notifiers){
                    notifier.didRangeBeaconsInRegion(beacons, region));
                }
            }
            else {
                LogManager.d(TAG, "but ranging notifier is null, so we're dropping it.");
            }
            Set<RangeContentNotifier<? extends BeaconIdentifiers>> contentNotifiers = BeaconManager.getInstanceForApplication(this).getRangingContentNotifiers();

            if (contentNotifiers != null) {
                for(RangeContentNotifier notifier : contentNotifiers){
                    notifier.didRangeBeaconsInRegion(beacons, contents, status, rangingData.getRegion());
                }
            } else {
                LogManager.d(TAG, "but ranging notifier is null, so we're dropping it.");
            }
            RangeNotifier dataNotifier = BeaconManager.getInstanceForApplication(this).getDataRequestNotifier();
            if (dataNotifier != null) {
                dataNotifier.didRangeBeaconsInRegion(beacons, rangingData.getRegion());
            }
        }

        if (monitoringData != null) {
            LogManager.d(TAG, "got monitoring data");
            Set<MonitorNotifier> notifiers = BeaconManager.getInstanceForApplication(this).getMonitoringNotifiers();
            if (notifiers != null) {
                for(MonitorNotifier notifier : notifiers) {
                    LogManager.d(TAG, "Calling monitoring notifier: %s", notifier);
                    notifier.didDetermineStateForRegion(monitoringData.isInside() ? MonitorNotifier.INSIDE : MonitorNotifier.OUTSIDE, monitoringData.getRegion());
                    if (monitoringData.isInside()) {
                        notifier.didEnterRegion(monitoringData.getRegion());
                    } else {
                        notifier.didExitRegion(monitoringData.getRegion());
                    }
                }
            }
        }
    }
}
