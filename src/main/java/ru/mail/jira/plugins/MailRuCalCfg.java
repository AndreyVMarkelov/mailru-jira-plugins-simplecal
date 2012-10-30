/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.List;

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
     * Get list of all stored calendars.
     */
    List<Long> getCalendars();

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
    void saveCalendars(List<Long> cals);

    /**
     * Store calendar.
     */
    void storeProjectCalUserData(ProjectCalUserData pcud);
}
