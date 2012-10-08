package ru.mail.jira.plugins;

import java.util.concurrent.ConcurrentMap;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.atlassian.sal.api.lifecycle.LifecycleAware;

public class Starter
    implements LifecycleAware
{
    /**
     * Calendar cache.
     */
    private static ConcurrentMap<String, UserCalData> cache = new ConcurrentLinkedHashMap.Builder<String, UserCalData>().maximumWeightedCapacity(1000).build();

    /**
     * User cache.
     */
    private static ConcurrentMap<String, UserCalPref> userCache = new ConcurrentLinkedHashMap.Builder<String, UserCalPref>().maximumWeightedCapacity(1000).build();

    public static ConcurrentMap<String, UserCalData> getCache()
    {
        return cache;
    }

    public static ConcurrentMap<String, UserCalPref> getUserCache()
    {
        return userCache;
    }

    @Override
    public void onStart()
    {
        //--> nothing
    }
}
