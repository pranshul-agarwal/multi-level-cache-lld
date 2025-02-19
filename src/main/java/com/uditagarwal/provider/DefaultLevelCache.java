package com.uditagarwal.provider;

import com.uditagarwal.model.LevelCacheData;
import com.uditagarwal.model.ReadResponse;
import com.uditagarwal.model.WriteResponse;
import com.uditagarwal.single.cache.SingleCacheProvider;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;

//MultiLevelCacheService -> L1 -> L2 -> L3...Ln -> null
//                C1    C2    ...
@AllArgsConstructor
public class DefaultLevelCache<Key, Value> implements ILevelCache<Key, Value> {
    private final LevelCacheData levelCacheData;
    private final SingleCacheProvider<Key, Value> singleCacheProvider;

    @NonNull
    private final ILevelCache<Key, Value> next;

    @NonNull
    public WriteResponse set(Key key, Value value) {
        Double curTime = 0.0;
        Value curLevelValue = singleCacheProvider.get(key);
        curTime += levelCacheData.getReadTime();
        if (!value.equals(curLevelValue)) {
            singleCacheProvider.set(key, value);
            curTime += levelCacheData.getWriteTime();
        }

        curTime += next.set(key, value).getTimeTaken();
        return new WriteResponse(curTime);
    }

    @NonNull
    public ReadResponse<Value> get(Key key) {
        Double curTime = 0.0;
        Value curLevelValue = singleCacheProvider.get(key);
        curTime += levelCacheData.getReadTime();

        // L1 -> L2 -> L3 -> L4
        if (curLevelValue == null) {
            ReadResponse<Value> nextResponse = next.get(key);
            curTime += nextResponse.getTotalTime();
            curLevelValue = nextResponse.getValue();
            if (curLevelValue != null) {
                singleCacheProvider.set(key, curLevelValue);
                curTime += levelCacheData.getWriteTime();
            }
        }

        return new ReadResponse<>(curLevelValue, curTime);
    }

    @NonNull
    public List<Double> getUsages() {
        final List<Double> usages;
        if (next == null) {
            usages = Collections.emptyList();
        } else {
            usages = next.getUsages();
        }

        usages.add(0, singleCacheProvider.getCurrentUsage());
        return usages;
    }
}
