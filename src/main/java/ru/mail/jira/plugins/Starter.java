package ru.mail.jira.plugins;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

public class Starter
    implements LifecycleAware
{
    /**
     * Calendars.
     */
    private static CopyOnWriteArrayList<Long> calendars = new CopyOnWriteArrayList<Long>();

    /**
     * User cache.
     */
    private static ConcurrentMap<String, UserCalPref> userCache = new ConcurrentLinkedHashMap.Builder<String, UserCalPref>().maximumWeightedCapacity(1000).build();

    /**
     * Get calendars.
     */
    public synchronized static CopyOnWriteArrayList<Long> getCalendars()
    {
        return calendars;
    }

    public static ConcurrentMap<String, UserCalPref> getUserCache()
    {
        return userCache;
    }

    /**
     * Calendar manager.
     */
    private final MailRuCalCfg calMrg;

    /**
     * Constructor.
     */
    public Starter(
        MailRuCalCfg calMrg)
    {
        this.calMrg = calMrg;
    }

    @Override
    public void onStart()
    {
        calendars.addAll(calMrg.getCalendars());
    }
}
