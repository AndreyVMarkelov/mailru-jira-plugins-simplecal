/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

/**
 * Plug-In data interface.
 * 
 * @author Andrey Markelov
 */
public interface MailRuCalCfg
{
    /**
     * Get user preferences.
     */
    UserCalPref getUserCalPref(String user);

    /**
     * Get stored user data.
     */
    UserCalData getUserData(String user);

    /**
     * Put user preferences.
     */
    void putUserCalPref(String user, UserCalPref userPref);

    /**
     * Put user data.
     */
    void putUserData(String user, UserCalData userData);
}
