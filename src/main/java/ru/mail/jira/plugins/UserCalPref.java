/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
     * User colors for calendars.
     */
    private Map<Long, String> colors;

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
     * Get user colors.
     */
    public Map<Long, String> getColors()
    {
        return colors;
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
     * Get user color for calendar.
     */
    public String getUserColor(Long calId)
    {
        return colors.get(calId);
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
     * Is user color for calendar?
     */
    public boolean isUserColor(Long calId)
    {
        if (colors == null)
        {
            return false;
        }
        return colors.containsKey(calId);
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
     * Set user colors.
     */
    public void setColors(Map<Long, String> colors)
    {
        this.colors = colors;
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

    /**
     * Add user color for calendar.
     */
    public void storeUserColor(Long calId, String color)
    {
        if (colors == null)
        {
            colors = new HashMap<Long, String>();
        }
        colors.put(calId, color);
    }

    @Override
    public String toString()
    {
        return "UserCalPref[defaultView=" + defaultView + ", shadowCalendars=" +
            shadowCalendars + ", colors=" + colors + "]";
    }
}
