package org.altbeacon.beacon.service.scanner.optimizer;

import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Connecthings on 22/06/16.
 */
public class CountRegions extends CountRegion{

    private int count;
    private long lastInt;
    private long lastOut;
    private List<CountRegion> countRegions;

    public CountRegions(){
        countRegions = new ArrayList<>();
        countRegions.add(new CountRegionIndividual(1));
        countRegions.add(new CountRegionIndividual(2));
        countRegions.add(new CountRegionIndividual(3));
    }

    public boolean isOutFromR23SinceLessThan(long delay){
        int size = countRegions.size();
        for(int i = 1;i<size; i++){
            if(countRegions.get(i).isOutForLessThan(delay)){
                return true;
            }
        }
        return false;
    }

    public boolean checkRegion(Region region){
        return true;
    }

    public boolean add(Region region){
        for(CountRegion countRegion : countRegions){
            countRegion.add(region);
        }
        return super.add(region);
    }

    public boolean remove(Region region){
        for(CountRegion countRegion : countRegions){
            countRegion.remove(region);
        }
        return super.remove(region);
    }



}

