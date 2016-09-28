package org.altbeacon.beacon.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class FixSizeCache<K,V> extends LinkedHashMap<K, V>{

    private int maxEntries;

    public FixSizeCache(int maxEntries){
        super(maxEntries);
        this.maxEntries = maxEntries;
    }

    protected boolean removeEldestEntry(final Map.Entry<K, V> eldest){
        return super.size() > maxEntries;
    }


}