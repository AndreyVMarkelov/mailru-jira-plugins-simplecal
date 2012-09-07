/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

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
     * Constructor.
     */
    public UserCalPref() {}

    public String getDefaultView()
    {
        return defaultView;
    }

    public void setDefaultView(String defaultView)
    {
        this.defaultView = defaultView;
    }

    @Override
    public String toString()
    {
        return "UserCalPref[defaultView=" + defaultView + "]";
    }
}
