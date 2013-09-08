/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.andreymarkelov.atlas.plugins.simplecal;

import java.util.List;
import java.util.Set;

/**
 * Plug-In data interface.
 * 
 * @author Andrey Markelov
 */
public interface MailRuCalCfg
{
    /**
     * Delete calendar.
     */
    void deleteCalendar(Long id);

    /**
     * Get calendar data.
     */
    ProjectCalUserData getCalendarData(Long id);

    /**
     * Get calendar groups.
     */
    List<String> getCalendarGroups();

    /**
     * Get list of all stored calendars.
     */
    Set<Long> getCalendars();

    /**
     * Get calendars data.
     */
    List<ProjectCalUserData> getCalendarsData();

    /**
     * Get user preferences.
     */
    UserCalPref getUserCalPref(String user);

    /**
     * Put user preferences.
     */
    void putUserCalPref(String user, UserCalPref userPref);

    /**
     * Store all calendars.
     */
    void saveCalendars(Set<Long> cals);

    /**
     * Set calendar groups.
     */
    void setCalendarGroups(List<String> groups);

    /**
     * Store calendar.
     */
    void storeProjectCalUserData(ProjectCalUserData pcud);

    /**
     * Update calendar.
     */
    void updateProjectCalUserData(ProjectCalUserData pcud);
}
