/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.HashSet;
import java.util.Set;

/**
 * User preferences.
 * 
 * @author Andrey Markelov
 */
public class UserCalPref
{
    /**
     * Default view screen.
     */
    private String defaultView;

    /**
     * Hide calendars.
     */
    private Set<Long> shadowCalendars;

    /**
     * Constructor.
     */
    public UserCalPref() {}

    public void addshadowCalendar(Long calId)
    {
        if (shadowCalendars == null)
        {
            shadowCalendars = new HashSet<Long>();
        }
        shadowCalendars.add(calId);
    }

    public String getDefaultView()
    {
        return defaultView;
    }

    public Set<Long> getShadowCalendars()
    {
        return shadowCalendars;
    }

    public boolean isCalendarShadow(Long calId)
    {
        if (shadowCalendars == null)
        {
            return false;
        }
        return !shadowCalendars.contains(calId);
    }

    public void removeshadowCalendar(Long calId)
    {
        if (shadowCalendars == null)
        {
            return;
        }
        shadowCalendars.remove(calId);
    }

    public void setDefaultView(String defaultView)
    {
        this.defaultView = defaultView;
    }

    public void setShadowCalendars(Set<Long> shadowCalendars)
    {
        this.shadowCalendars = shadowCalendars;
    }

    @Override
    public String toString()
    {
        return "UserCalPref[defaultView=" + defaultView + ", shadowCalendars=" + shadowCalendars + "]";
    }
}
