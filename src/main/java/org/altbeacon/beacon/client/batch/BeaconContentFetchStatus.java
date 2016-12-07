package org.altbeacon.beacon.client.batch;

/**
 * The various status that can be associated to beacon while fetching a content associated to
 * Created by Connecthings on 27/09/16.
 */
public enum BeaconContentFetchStatus {
    /**
     * The content associated to the beacon is in progress
     */
    IN_PROGRESS,
    /**
     * There is no content associated to the beacon
     */
    NO_CONTENT,
    /**
     * The content has been sucessfully retrieved
     */
    SUCCESS,
    /**
     * The content can't be downloaded because of backend error
     */
    BACKEND_ERROR,
    /**
     * The content can't be downloaded because none network is available
     */
    NETWORK_ERROR,
    /**
     * The content can't be found because of an error meet while querying a local DB
     */
    DB_ERROR
}
