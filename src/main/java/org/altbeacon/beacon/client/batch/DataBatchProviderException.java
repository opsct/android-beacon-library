package org.altbeacon.beacon.client.batch;

/**
 * Utility Class to manage the potentiel batch exception
 *
 * Created by Connecthings on 19/10/16.
 */
public class DataBatchProviderException extends Exception {

    private BeaconContentFetchStatus status;

    public DataBatchProviderException(BeaconContentFetchStatus status) {
        super();
        this.status = status;
    }
    public DataBatchProviderException(BeaconContentFetchStatus status, String msg) {
        super(msg);
        this.status = status;
    }
    public DataBatchProviderException(BeaconContentFetchStatus status, String msg, Throwable t) {
        super(msg, t);
        this.status = status;
    }

    public BeaconContentFetchStatus getStatus() {
        return status;
    }
}
