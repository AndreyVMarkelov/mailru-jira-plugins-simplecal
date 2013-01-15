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
     * Fields.
     */
    private Map<Long, Set<String>> fields;

    /**
     * Hide weekends.
     */
    private boolean hideWeekend;

    /**
     * Show time.
     */
    private boolean showTime;

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
     * Get fields for the calendar.
     */
    public Set<String> getCalendarFields(Long calId)
    {
        if (fields == null)
        {
            return null;
        }

        return fields.get(calId);
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
     * Fields for every calendar.
     */
    public Map<Long, Set<String>> getFields()
    {
        return fields;
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

    public boolean isHideWeekend()
    {
        return hideWeekend;
    }

    public boolean isShowTime()
    {
        return showTime;
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
     * Set fields for the calendar.
     */
    public void setCalendarFields(Long calId, Set<String> calFields)
    {
        if (fields == null)
        {
            fields = new HashMap<Long, Set<String>>();
        }

        Set<String> storedFields = fields.get(calId);
        if (storedFields == null)
        {
            storedFields = new HashSet<String>(calFields);
        }
        else
        {
            storedFields.clear();
            storedFields.addAll(calFields);
        }

        fields.put(calId, storedFields);
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
     * Set calendar fields.
     */
    public void setFields(Map<Long, Set<String>> fields)
    {
        this.fields = fields;
    }

    public void setHideWeekend(boolean hideWeekend)
    {
        this.hideWeekend = hideWeekend;
    }

    /**
     * Set hide calendars.
     */
    public void setShadowCalendars(Set<Long> shadowCalendars)
    {
        this.shadowCalendars = shadowCalendars;
    }

    public void setShowTime(boolean showTime)
    {
        this.showTime = showTime;
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
        return "UserCalPref[colors=" + colors + ", defaultView=" + defaultView
            + ", fields=" + fields + ", hideWeekend=" + hideWeekend
            + ", shadowCalendars=" + shadowCalendars + ", showTime=" + showTime + "]";
    }
}
