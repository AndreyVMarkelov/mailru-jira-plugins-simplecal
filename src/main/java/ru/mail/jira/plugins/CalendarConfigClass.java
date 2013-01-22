/*
 * Created by Andrey Markelov 01-12-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

/**
 * Calendar configuration action.
 *
 * @author Andrey Markelov
 */
public class CalendarConfigClass
    extends JiraWebActionSupport
{
    /**
     * Unique ID.
     */
    private static final long serialVersionUID = 1225110307136619908L;

    /**
     * Application properties.
     */
    private final ApplicationProperties applicationProperties;

    /**
     * Plug-In configuration.
     */
    private final MailRuCalCfg cfg;

    /**
     * Group manager.
     */
    private final GroupManager grpMgr;

    /**
     * Is saved?
     */
    private boolean isSaved = false;

    /**
     * Saved groups.
     */
    private List<String> savedGroups;

    /**
     * Selected groups.
     */
    private String[] selectedGroups = new String[0];

    /**
     * Constructor.
     */
    public CalendarConfigClass(
        MailRuCalCfg cfg,
        GroupManager grpMgr,
        ApplicationProperties applicationProperties)
    {
        this.cfg = cfg;
        this.grpMgr = grpMgr;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String doDefault()
    throws Exception
    {
        List<String> groups = cfg.getCalendarGroups();
        if (groups != null && !groups.isEmpty())
        {
            selectedGroups = groups.toArray(new String[groups.size()]);
            savedGroups = Arrays.asList(selectedGroups);
        }

        return SUCCESS;
    }

    @Override
    @com.atlassian.jira.security.xsrf.RequiresXsrfCheck
    protected String doExecute()
    throws Exception
    {
        cfg.setCalendarGroups(Utils.arrayToList(selectedGroups));
        if (selectedGroups != null)
        {
            savedGroups = cfg.getCalendarGroups();
        }

        setSaved(true);
        return getRedirect("CalendarConfigClass!default.jspa?saved=true");
    }

    /**
     * Get context path.
     */
    public String getBaseUrl()
    {
        return applicationProperties.getBaseUrl();
    }

    /**
     * Get all groups.
     */
    public Collection<Group> getGroups()
    {
        return grpMgr.getAllGroups();
    }

    public List<String> getSavedGroups()
    {
        return savedGroups;
    }

    public String[] getSelectedGroups()
    {
        return selectedGroups;
    }

    /**
     * Check administration permissions.
     */
    public boolean hasAdminPermission()
    {
        User user = getLoggedInUser();
        if (user == null)
        {
            return false;
        }

        return getPermissionManager().hasPermission(Permissions.ADMINISTER, getLoggedInUser());
    }

    public boolean isSaved()
    {
        return isSaved;
    }

    public void setSaved(boolean isSaved)
    {
        this.isSaved = isSaved;
    }

    public void setSavedGroups(List<String> savedGroups)
    {
        this.savedGroups = savedGroups;
    }

    public void setSelectedGroups(String[] selectedGroups)
    {
        this.selectedGroups = selectedGroups;
    }
}
