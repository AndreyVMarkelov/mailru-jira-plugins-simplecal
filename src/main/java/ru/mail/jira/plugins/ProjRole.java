/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

/**
 * This structure keeps project role.
 * 
 * @author Andrey Markelov
 */
public class ProjRole
{
    /**
     * Project ID.
     */
    private String project;

    /**
     * Role ID.
     */
    private String role;

    /**
     * Constructor.
     */
    public ProjRole(
        String project,
        String role)
    {
        this.project = project;
        this.role = role;
    }

    public String getProject()
    {
        return project;
    }

    public String getRole()
    {
        return role;
    }

    @Override
    public String toString()
    {
        return "ProjRoles[project=" + project + ", role=" + role + "]";
    }
}
