/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

/**
 * Plug-In stater.
 */
public class Starter
    implements LifecycleAware
{
    /**
     * Calendars data.
     */
    private static ConcurrentMap<Long, ProjectCalUserData> calDatas = new ConcurrentLinkedHashMap.Builder<Long, ProjectCalUserData>().maximumWeightedCapacity(1500).build();

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

    /**
     * Get calendars data.
     */
    public synchronized static ConcurrentMap<Long, ProjectCalUserData> getCalendarsData()
    {
        return calDatas;
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
