/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.HashSet;
import java.util.Set;

/**
 * User preferences. This structure contains information about hide calendars
 *  and preferred calendar view.
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

    /**
     * Hide calendar for user.
     */
    public void addshadowCalendar(Long calId)
    {
        if (shadowCalendars == null)
        {
            shadowCalendars = new HashSet<Long>();
        }
        shadowCalendars.add(calId);
    }

    /**
     * Get default calendar view: week, month or day.
     */
    public String getDefaultView()
    {
        return defaultView;
    }

    /**
     * Get set of hide calendars.
     */
    public Set<Long> getShadowCalendars()
    {
        return shadowCalendars;
    }

    /**
     * Check if calendar is hide for user.
     */
    public boolean isCalendarShadow(Long calId)
    {
        if (shadowCalendars == null)
        {
            return false;
        }
        return shadowCalendars.contains(calId);
    }

    /**
     * Show calendar.
     */
    public void removeshadowCalendar(Long calId)
    {
        if (shadowCalendars == null)
        {
            return;
        }
        shadowCalendars.remove(calId);
    }

    /**
     * Set default calendar view.
     */
    public void setDefaultView(String defaultView)
    {
        this.defaultView = defaultView;
    }

    /**
     * Set hide calendars.
     */
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
