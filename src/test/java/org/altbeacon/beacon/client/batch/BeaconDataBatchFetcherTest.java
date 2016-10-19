package org.altbeacon.beacon.client.batch;

import android.os.SystemClock;

import org.altbeacon.beacon.Beacon;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Connecthings on 04/10/16.
 */
@Config(sdk = 18)
@RunWith(RobolectricTestRunner.class)
public class BeaconDataBatchFetcherTest {


    @Test
    public void testEphemeralIdsToFetchIfNotEnable(){
        BeaconDataBatchFetcher<BeaconContentSimple> batchFetcher = new BeaconDataBatchFetcher<>(new BatchProviderSucceedThread(false), 50, 3000);
        Beacon beacon = new Beacon.Builder().setEphemeralId1("12").setEphemeralId2("5").setEphemeralId3("6").build();
        batchFetcher.updateContentOrAddToFetch(beacon);
        assertTrue("The list of beacons to fetch is not empty", batchFetcher.getBeaconsToFetch().size() == 0);
    }

    @Test
    public void testEphemeralIdsToFetchIfEnable(){
        BeaconDataBatchFetcher<BeaconContentSimple> batchFetcher = new BeaconDataBatchFetcher<>(new BatchProviderSucceedThread(true), 50, 3000);
        Beacon beacon = new Beacon.Builder().setEphemeralId1("12").setEphemeralId2("5").setEphemeralId3("6").build();
        batchFetcher.updateContentOrAddToFetch(beacon);
        assertTrue("The list of beacons to fetch is empty", batchFetcher.getBeaconsToFetch().size() == 1);
    }

    @Test
    public void testIdsToFetchIfEphemeralEnable(){
        BeaconDataBatchFetcher<BeaconContentSimple> batchFetcher = new BeaconDataBatchFetcher<>(new BatchProviderSucceedThread(true), 50, 3000);
        Beacon beacon = new Beacon.Builder().setId1("12").setId2("5").setId3("6").build();
        batchFetcher.updateContentOrAddToFetch(beacon);
        assertTrue("The list of beacons to fetch is empty", batchFetcher.getBeaconsToFetch().size() == 1);
    }

    @Test
    public void testIdsToFetchIfEphemeralDisable(){
        BeaconDataBatchFetcher<BeaconContentSimple> batchFetcher = new BeaconDataBatchFetcher<>(new BatchProviderSucceedThread(false), 50, 3000);
        Beacon beacon = new Beacon.Builder().setId1("12").setId2("5").setId3("6").build();
        batchFetcher.updateContentOrAddToFetch(beacon);
        assertTrue("The list of beacons to fetch is empty", batchFetcher.getBeaconsToFetch().size() == 1);
    }

    @Test
    public void testIdsToFetchIfDuplicate(){
        BeaconDataBatchFetcher<BeaconContentSimple> batchFetcher = new BeaconDataBatchFetcher<>(new BatchProviderSucceedThread(false), 50, 3000);
        Beacon beacon1 = new Beacon.Builder().setId1("12").setId2("5").setId3("6").build();
        batchFetcher.updateContentOrAddToFetch(beacon1);
        Beacon beacon2 = new Beacon.Builder().setId1("12").setId2("5").setId3("6").build();
        batchFetcher.updateContentOrAddToFetch(beacon2);
        assertTrue("The list of beacons to fetch is not 1", batchFetcher.getBeaconsToFetch().size() == 1);
    }


    @Test
    public void testFindContentSuccess() {
        BeaconDataBatchFetcher<BeaconContentSimple> batchFetcher = new BeaconDataBatchFetcher<>(new BatchProviderSucceed(), 5000, 200);
        List<Beacon> beacons = new ArrayList<>();
        Beacon beacon1 = new Beacon.Builder().setId1("1").setId2("1").setId3("3").build();
        beacons.add(beacon1);
        batchFetcher.updateContentOrAddToFetch(beacon1);
        Beacon beacon2 = new Beacon.Builder().setEphemeralId1("12").setEphemeralId2("5").setEphemeralId3("6").build();
        beacons.add(beacon2);
        batchFetcher.updateContentOrAddToFetch(beacon2);
        //first fetch will launch task to get the content
        batchFetcher.fetch();


        BeaconContentFetchInfo<BeaconContentSimple> contentFetchInfo = null;
        for(int i = 0;i<beacons.size();i++){
            contentFetchInfo = batchFetcher.getFetchInfo(beacons.get(i));
            assertTrue( "Status is "  + contentFetchInfo.getStatus() + " for the beacon " + i + " while it's expected to be SUCCESS", contentFetchInfo.getStatus() == BeaconContentFetchStatus.SUCCESS);
            assertNotNull("BeaconContent is null", contentFetchInfo.getContent());
        }

        BeaconBatchFetchInfo<BeaconContentSimple> fetchInfo = batchFetcher.updateContentOrAddToFetch(beacons);
        assertEquals("Test status that must be success", BeaconContentFetchStatus.SUCCESS, fetchInfo.getFetchStatus());
        assertEquals("test size ", beacons.size(), fetchInfo.getContents().size());


    }

    @Test
    public void testNoContentWithUpdate() {
        BeaconDataBatchFetcher<BeaconContentSimple> batchFetcher = new BeaconDataBatchFetcher<>(new BatchProviderSucceed(), 5000, 200);
        List<Beacon> beacons = generateBeaconList();
        BeaconBatchFetchInfo<BeaconContentSimple> fetchInfo = batchFetcher.updateContentOrAddToFetch(beacons);
        assertEquals("Test status that must be in progress", BeaconContentFetchStatus.IN_PROGRESS, fetchInfo.getFetchStatus());
        assertEquals("test size ", 0, fetchInfo.getContents().size());
        //first fetch will launch task to get the content
        batchFetcher.fetch();
        testBeacons(batchFetcher, beacons, 2,2,2);
        fetchInfo = batchFetcher.updateContentOrAddToFetch(beacons);
        assertEquals("Test status that must be success", BeaconContentFetchStatus.SUCCESS, fetchInfo.getFetchStatus());
        assertEquals("test size ", 2, fetchInfo.getContents().size());
        testBeacons(batchFetcher, beacons, 2,2,2);
    }

    @Test
    public void testFindContentError() {
        BeaconDataBatchFetcher<BeaconContentSimple> batchFetcher = new BeaconDataBatchFetcher<>(new BatchProviderError(), 5000, 200);
        List<Beacon> beacons = new ArrayList<>();
        Beacon beacon1 = new Beacon.Builder().setId1("1").setId2("1").setId3("3").build();
        beacons.add(beacon1);
        batchFetcher.updateContentOrAddToFetch(beacon1);
        Beacon beacon2 =new Beacon.Builder().setEphemeralId1("12").setEphemeralId2("5").setEphemeralId3("6").build();
        beacons.add(beacon2);
        batchFetcher.updateContentOrAddToFetch(beacon2);
        //first fetch will launch task to get the content
        batchFetcher.fetch();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BeaconContentFetchInfo<BeaconContentSimple> contentFetchInfo = null;
        for(int i = 0;i<beacons.size();i++){
            contentFetchInfo = batchFetcher.getFetchInfo(beacons.get(i));
            assertTrue( "Status is "  + contentFetchInfo.getStatus() + " for the beacon " + i + " while it's expected to be ERROR", contentFetchInfo.getStatus() == BeaconContentFetchStatus.BACKEND_ERROR);
            assertNull("BeaconContent is null", contentFetchInfo.getContent());
        }

        BeaconBatchFetchInfo<BeaconContentSimple> fetchInfo = batchFetcher.updateContentOrAddToFetch(beacons);
        assertEquals("Test status that must be backendError", BeaconContentFetchStatus.BACKEND_ERROR, fetchInfo.getFetchStatus());
        assertEquals("test size ", 0, fetchInfo.getContents().size());
    }



    @Test
    public void testFindContentSuccessAndUnresolved(){
        testFindContentSuccessAndUnresolved(new BatchProviderSucceed());
    }

    @Test
    public void testFindContentSuccessAndUnresolvedThread(){
        testFindContentSuccessAndUnresolved(new BatchProviderSucceedThread());
    }

    @Test
    public void testFindContentSuccessAndUnresolvedList(){
        testFindContentSuccessAndUnresolvedList(new BatchProviderSucceed());
    }

    @Test
    public void testFinContentSuccessAgain(){
        BeaconDataBatchFetcher<BeaconContentSimple> batchFetcher = new BeaconDataBatchFetcher<>(new BatchProviderSucceedThread(true), 50, 3000);
        List<Beacon> beacons = generateBeaconList();
        batchFetcher.updateContentOrAddToFetch(beacons);
        //first fetch will launch task to get the content
        batchFetcher.fetch();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testBeacons(batchFetcher, beacons, 2,2,2);


        beacons = generateBeaconList();
        batchFetcher.updateContentOrAddToFetch(beacons);
        assertTrue("The list of beacons to fetch is not empty",batchFetcher.getBeaconsToFetch().size() == 0);
        testBeacons(batchFetcher, beacons, 2,2,2);

        SystemClock.setCurrentTimeMillis(3200);
        long t = SystemClock.elapsedRealtime();
        BeaconContentFetchInfo<BeaconContentSimple> contentFetchInfo = null;
        for(int i = 0;i<beacons.size();i++){
            contentFetchInfo = batchFetcher.getFetchInfo(beacons.get(i));
            assertTrue("FetchInfo - content is not out of date ", contentFetchInfo.isTimeToUpdate());
        }

        beacons = generateBeaconList();
        BeaconBatchFetchInfo<BeaconContentSimple> fetchInfo = batchFetcher.updateContentOrAddToFetch(beacons);
        assertTrue("The list of beacons to fetch is empty while beacons are out of date",batchFetcher.getBeaconsToFetch().size() == beacons.size());
        testBeacons(batchFetcher, beacons, 2,2,2);
        assertEquals("status is in progress", BeaconContentFetchStatus.SUCCESS, fetchInfo.getFetchStatus());
        assertEquals("2 beacons content must be found", 2, fetchInfo.getContents().size());


    }

    private void testBeacons(BeaconDataBatchFetcher<BeaconContentSimple> batchFetcher, List<Beacon> beacons, int expectedCountSuccess, int expectedCountNoContent, int exepectedCountContent){
        int countSuccess = 0;
        int countContent = 0;
        int countNoContent = 0;
        BeaconContentFetchInfo<BeaconContentSimple> contentFetchInfo = null;
        for(Beacon beacon : beacons){
            contentFetchInfo = batchFetcher.getFetchInfo(beacon);
            if(contentFetchInfo.getContent() != null){
                countContent++;
            }
            if(contentFetchInfo.getStatus() == BeaconContentFetchStatus.SUCCESS){
                countSuccess++;
            }
            if(contentFetchInfo.getStatus() == BeaconContentFetchStatus.NO_CONTENT){
                countNoContent++;
            }
        }
        assertTrue("CountContent is wrong ",countContent == exepectedCountContent);
        assertTrue("CountNoContent is wrong", countNoContent == countNoContent);
        assertTrue("CountSuccess is wrong", countSuccess == countSuccess);
    }


    private void testFindContentSuccessAndUnresolved(BeaconDataBatchProvider<BeaconContentSimple> batchProvider) {
        BeaconDataBatchFetcher<BeaconContentSimple> batchFetcher = new BeaconDataBatchFetcher<>(batchProvider, 5000, 200);
        List<Beacon> beacons = new ArrayList<>();
        Beacon beacon1 = new Beacon.Builder().setId1("11").setId2("1").setId3("3").build();
        beacons.add(beacon1);
        batchFetcher.updateContentOrAddToFetch(beacon1);
        Beacon beacon2 = new Beacon.Builder().setId1("12").setId2("5").setId3("6").build();
        beacons.add(beacon2);
        batchFetcher.updateContentOrAddToFetch(beacon2);
        Beacon beacon3 = new Beacon.Builder().setId1("13").setId2("7").setId3("8").build();
        beacons.add(beacon3);
        batchFetcher.updateContentOrAddToFetch(beacon3);
        Beacon beacon4 = new Beacon.Builder().setId1("14").setId2("7").setId3("8").build();
        beacons.add(beacon4);
        batchFetcher.updateContentOrAddToFetch(beacon4);
        //first fetch will launch task to get the content
        batchFetcher.fetch();

        if(batchProvider instanceof  BatchProviderSucceedThread) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        BeaconContentFetchInfo<BeaconContentSimple> contentFetchInfo = null;
        for(int i = 0;i<beacons.size();i++){
            contentFetchInfo = batchFetcher.getFetchInfo(beacons.get(i));
            if(i<=1) {
                assertTrue("Status is " + contentFetchInfo.getStatus() + " for the beacon " + i + " while it's expected to be SUCCESS", contentFetchInfo.getStatus() == BeaconContentFetchStatus.SUCCESS);
                assertNotNull("BeaconContent is null", contentFetchInfo.getContent());
            }else{
                assertTrue("Status is " + contentFetchInfo.getStatus() + " for the beacon " + i + " while it's expected to be NO_CONTENT", contentFetchInfo.getStatus() == BeaconContentFetchStatus.NO_CONTENT);
                assertNull("BeaconContent is null", contentFetchInfo.getContent());
            }
        }

    }

    private List<Beacon> generateBeaconList(){
        List<Beacon> beacons = new ArrayList<>();
        Beacon beacon1 = new Beacon.Builder().setId1("11").setId2("1").setId3("3").build();
        beacons.add(beacon1);
        Beacon beacon2 = new Beacon.Builder().setEphemeralId1("12").setEphemeralId2("5").setEphemeralId3("6").build();
        beacons.add(beacon2);
        Beacon beacon3 = new Beacon.Builder().setId1("13").setId2("7").setId3("8").build();
        beacons.add(beacon3);

        Beacon beacon4 = new Beacon.Builder().setEphemeralId1("14").setEphemeralId2("5").setEphemeralId3("6").build();
        beacons.add(beacon4);
        return beacons;
    }

    private void testFindContentSuccessAndUnresolvedList(BeaconDataBatchProvider<BeaconContentSimple> batchProvider) {
        BeaconDataBatchFetcher<BeaconContentSimple> batchFetcher = new BeaconDataBatchFetcher<>(batchProvider, 5000, 200);
        List<Beacon> beacons = generateBeaconList();
        batchFetcher.updateContentOrAddToFetch(beacons);
        //first fetch will launch task to get the content
        batchFetcher.fetch();

        if(batchProvider instanceof  BatchProviderSucceedThread) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

       testBeacons(batchFetcher, beacons, 2, 2, 2);

    }


    private class BatchProviderError implements BeaconDataBatchProvider<BeaconContentSimple>{

        @Override
        public boolean fetchEphemeralIds() {
            return true;
        }

        @Override
        public void fetch(final Collection<Beacon> beacons, final BeaconDataBatchNotifier<BeaconContentSimple> beaconDataBatchNotifier) {
            beaconDataBatchNotifier.onBatchError(beacons, new DataBatchProviderException(BeaconContentFetchStatus.BACKEND_ERROR));
        }
    }

    private class BatchProviderSucceed implements BeaconDataBatchProvider<BeaconContentSimple>{

        @Override
        public boolean fetchEphemeralIds() {
            return true;
        }

        @Override
        public void fetch(final Collection<Beacon> beacons, final BeaconDataBatchNotifier<BeaconContentSimple> beaconDataBatchNotifier) {
            List<BeaconContentSimple> contentSimples = new ArrayList<>();
            List<Beacon> unresolved = new ArrayList<>();
            int size = beacons.size();
            int i = 0;
            for(Beacon beacon : beacons){
                if(i<=1) {
                    contentSimples.add(new BeaconContentSimple(beacon.getEphemeralIdentifiers(), beacon.getIdentifiers()));
                }else {
                    unresolved.add(beacon);
                }
                i++;
            }
            beaconDataBatchNotifier.onBatchUpdate(contentSimples, unresolved);
        }
    }

    private class BatchProviderSucceedThread implements BeaconDataBatchProvider<BeaconContentSimple>{

        private boolean fetchEphemeralIds;

        public BatchProviderSucceedThread(boolean fetchEphemeralIds){
            this.fetchEphemeralIds = fetchEphemeralIds;
        }

        public BatchProviderSucceedThread(){
            this(false);
        }

        @Override
        public boolean fetchEphemeralIds() {
            return fetchEphemeralIds;
        }

        @Override
        public void fetch(final Collection<Beacon> beacons, final BeaconDataBatchNotifier<BeaconContentSimple> beaconDataBatchNotifier) {
            Runnable runnable = new Runnable(){

                @Override
                public void run() {
                    List<BeaconContentSimple> contentSimples = new ArrayList<>();
                    List<Beacon> unresolved = new ArrayList<>();
                    int size = beacons.size();
                    int i = 0;
                    for(Beacon beacon : beacons){
                        if(i<=1) {
                            contentSimples.add(new BeaconContentSimple(beacon.getEphemeralIdentifiers(), beacon.getIdentifiers()));
                        }else {
                            unresolved.add(beacon);
                        }
                        i++;
                    }
                    beaconDataBatchNotifier.onBatchUpdate(contentSimples, unresolved);
                }
            };
            new Thread(runnable).start();

        }
    }
}
