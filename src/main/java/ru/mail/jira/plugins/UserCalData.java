/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.ArrayList;
import java.util.List;

/**
 * This structure keeps all calendars for the user.
 * 
 * @author Andrey Markelov
 */
public class UserCalData
{
    /**
     * Calendar's data.
     */
    private List<ProjectCalUserData> projs;

    /**
     * Constructor.
     */
    public UserCalData()
    {
        this.projs = new ArrayList<ProjectCalUserData>();
    }

    /**
     * Add data.
     */
    public void add(ProjectCalUserData projData)
    {
        this.projs.add(projData);
    }

    public List<ProjectCalUserData> getProjs()
    {
        return projs;
    }
}
