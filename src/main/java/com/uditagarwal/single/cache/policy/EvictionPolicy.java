package com.uditagarwal.single.cache.policy;

public interface EvictionPolicy<Key> {
    void keyAccessed(Key key);
    Key evictKey();
}
