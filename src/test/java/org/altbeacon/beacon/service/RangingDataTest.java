package org.altbeacon.beacon.service;

import android.os.Parcel;

import org.altbeacon.beacon.AltBeacon;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.client.batch.BeaconContentFetchStatus;
import org.altbeacon.beacon.client.batch.BeaconContentSimple;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Connecthings on 18/10/16.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class RangingDataTest {

    @Test
    public void testCanSerializeParcelable() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Parcel parcel = Parcel.obtain();
        List<Beacon> beacons = new ArrayList<>();
        beacons.add(new AltBeacon.Builder().setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6).setBluetoothName("xx")
                .setBluetoothAddress("1:2:3:4:5:6").setDataFields(Arrays.asList(100l)).build());
        beacons.add(new AltBeacon.Builder().setId1("2").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6).setBluetoothName("xx")
                .setBluetoothAddress("1:2:3:4:5:6").setDataFields(Arrays.asList(100l)).build());
        beacons.add(new AltBeacon.Builder().setId1("3").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6).setBluetoothName("xx")
                .setBluetoothAddress("1:2:3:4:5:6").setDataFields(Arrays.asList(100l)).build());
        List<BeaconContentSimple> beaconContentSimples = new ArrayList<>();
        for(Beacon<BeaconContentSimple> beacon : beacons){
            beaconContentSimples.add(new BeaconContentSimple(beacon.getStaticIdentifiers()));
        }
        RangingData<BeaconContentSimple> rangingData1 = new RangingData<>(beacons, beaconContentSimples, BeaconContentFetchStatus.SUCCESS, new Region("my region", null, null, null));

        rangingData1.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        RangingData<BeaconContentSimple> rangingData2 = new RangingData<>(parcel);
        assertEquals("Right number of identifiers after deserialization: ", 3, rangingData2.getBeacons().size());
        assertEquals("Right number of identifiers after deserialization: ", 3, rangingData2.getContents().size());
        assertEquals("Status ", BeaconContentFetchStatus.SUCCESS, rangingData2.getStatus());
        assertEquals("Region identifier ", "my region", rangingData2.getRegion().getUniqueId());

        for(BeaconContentSimple beaconContentSimple : beaconContentSimples) {
            assertTrue(rangingData2.getContents().contains(beaconContentSimple));
        }

        for(Beacon<BeaconContentSimple> beacon : beacons) {
            assertTrue("contains ", rangingData2.getBeacons().contains(beacon));
        }
    }
}
