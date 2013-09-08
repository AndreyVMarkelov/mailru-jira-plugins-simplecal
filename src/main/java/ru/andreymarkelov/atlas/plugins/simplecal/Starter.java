package ru.andreymarkelov.atlas.plugins.simplecal;

import java.util.concurrent.ConcurrentMap;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

public class Starter implements LifecycleAware {
    private static ConcurrentMap<Long, ProjectCalUserData> calDatas = new ConcurrentLinkedHashMap.Builder<Long, ProjectCalUserData>().maximumWeightedCapacity(1500).build();
    private static ConcurrentMap<String, UserCalPref> userCache = new ConcurrentLinkedHashMap.Builder<String, UserCalPref>().maximumWeightedCapacity(1000).build();

    public synchronized static ConcurrentMap<Long, ProjectCalUserData> getCalendarsData() {
        return calDatas;
    }

    public static ConcurrentMap<String, UserCalPref> getUserCache() {
        return userCache;
    }

    public Starter() {}

    @Override
    public void onStart() {}
}
