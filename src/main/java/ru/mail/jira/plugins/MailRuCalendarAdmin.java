/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

/**
 * Administration page action.
 * 
 * @author Andrey Markelov
 */
public class MailRuCalendarAdmin
    extends JiraWebActionSupport
{
    /**
     * Unique ID.
     */
    private static final long serialVersionUID = -6299473301220059325L;

    /**
     * Application properties.
     */
    private final ApplicationProperties applicationProperties;

    /**
     * Project manager.
     */
    private final ProjectManager prMgr;

    /**
     * Search request service.
     */
    private final SearchRequestService srMgr;

    /**
     * Calendar color.
     */
    private String calcolor;

    /**
     * Calendar description.
     */
    private String caldescr;

    /**
     * Calendar name.
     */
    private String calname;

    /**
     * Date point.
     */
    private String cdpinput;

    /**
     * Target type.
     */
    private String display;

    /**
     * End point.
     */
    private String endpoint;

    /**
     * Group manager.
     */
    private final GroupManager groupManager;

    /**
     * Is saved?
     */
    private boolean isSaved = false;

    /**
     * Mail.Ru calendar plug-In data.
     */
    private final MailRuCalCfg mailCfg;

    /**
     * Target.
     */
    private String mainsel;

    /**
     * User.
     */
    private String pgadmin;

    /**
     * Selected groups.
     */
    private String[] selectedGroups;

    /**
     * Field type.
     */
    private String showfld;

    /**
     * Start point.
     */
    private String startpoint;

    /**
     * Constructor.
     */
    public MailRuCalendarAdmin(
        ApplicationProperties applicationProperties,
        GroupManager groupManager,
        MailRuCalCfg mailCfg,
        SearchRequestService srMgr,
        ProjectManager prMgr)
    {
        this.applicationProperties = applicationProperties;
        this.groupManager = groupManager;
        this.mailCfg = mailCfg;
        this.srMgr = srMgr;
        this.prMgr = prMgr;
    }

    @Override
    protected String doExecute()
    throws Exception
    {
        String start = "";
        String end = "";
        if (showfld.equals(ProjectCalUserData.DATE_POINT_STR))
        {
            start = cdpinput;
        }
        else if (showfld.equals(ProjectCalUserData.DATE_RANGE_STR))
        {
            start = startpoint;
            end = endpoint;
        }

        Set<String> userSet = new HashSet<String>();
        if (Utils.isStr(pgadmin))
        {
            UserCalData usrData = mailCfg.getUserData(pgadmin);
            if (usrData == null)
            {
                usrData = new UserCalData();
            }

            usrData.add(new ProjectCalUserData(
                calname,
                caldescr,
                calcolor,
                display,
                mainsel,
                showfld,
                start,
                end,
                true,
                getLoggedInUser().getName()));
            mailCfg.putUserData(pgadmin, usrData);
            userSet.add(pgadmin);
        }

        if (Utils.isArray(selectedGroups))
        {
            for (String group : selectedGroups)
            {
                Collection<User> users = groupManager.getUsersInGroup(group);
                if (users != null)
                {
                    for (User user : users)
                    {
                        if (userSet.contains(user.getName()))
                        {
                            continue;
                        }

                        UserCalData usrData = mailCfg.getUserData(user.getName());
                        if (usrData == null)
                        {
                            usrData = new UserCalData();
                        }

                        usrData.add(new ProjectCalUserData(
                            calname,
                            caldescr,
                            calcolor,
                            display,
                            mainsel,
                            showfld,
                            start,
                            end,
                            true,
                            getLoggedInUser().getName()));
                        mailCfg.putUserData(user.getName(), usrData);
                        userSet.add(user.getName());
                    }
                }
            }
        }

        setSaved(true);
        return getRedirect("ViewMailRuCalendarAdmin!default.jspa?saved=true");
    }

    @Override
    protected void doValidation()
    {
        super.doValidation();

        if (!Utils.isStr(calname))
        {
            addErrorMessage(getText("mailrucal.calname.error"));
        }

        if (!Utils.isStr(calcolor))
        {
            addErrorMessage(getText("mailrucal.calcolor.error"));
        }

        if (!Utils.isStr(display))
        {
            addErrorMessage(getText("mailrucal.caldisplay.error"));
        }

        if (!Utils.isStr(mainsel))
        {
            addErrorMessage(getText("mailrucal.target.error"));
        }

        if (!Utils.isStr(showfld))
        {
            addErrorMessage(getText("mailrucal.showfilter.error"));
        }

        if (!display.equals(ProjectCalUserData.PROJECT_TYPE_STR) &&
            !display.equals(ProjectCalUserData.JCL_TYPE_STR))
        {
            addErrorMessage(getText("mailrucal.displaytype.error"));
        }

        if (!showfld.equals(ProjectCalUserData.IDD_STR) &&
            !showfld.equals(ProjectCalUserData.DATE_POINT_STR) &&
            !showfld.equals(ProjectCalUserData.DATE_RANGE_STR))
        {
            addErrorMessage(getText("mailrucal.showfiltertype.error"));
        }

        if (showfld.equals(ProjectCalUserData.DATE_POINT_STR))
        {
            if (!Utils.isStr(cdpinput))
            {
                addErrorMessage(getText("mailrucal.customdatepoint.error"));
            }
        }
        else if (showfld.equals(ProjectCalUserData.DATE_RANGE_STR))
        {
            if (!Utils.isStr(startpoint) || !Utils.isStr(endpoint))
            {
                addErrorMessage(getText("mailrucal.daterange.error"));
            }
        }

        if (!Utils.isArray(selectedGroups) &&
            !Utils.isStr(pgadmin))
        {
            addErrorMessage(getText("mailrucal.usergroup.error"));
        }
    }

    /**
     * Get all Jira groups.
     */
    public Collection<Group> getAllGroups()
    {
        return groupManager.getAllGroups();
    }

    /**
     * Get context path.
     */
    public String getBaseUrl()
    {
        return applicationProperties.getBaseUrl();
    }

    public String getCalcolor()
    {
        return calcolor;
    }

    public String getCaldescr()
    {
        return caldescr;
    }

    public String getCalname()
    {
        return calname;
    }

    public String getCdpinput()
    {
        return cdpinput;
    }

    public String getDisplay()
    {
        return display;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public Map<String, String> getFilters()
    {
        Map<String, String> aSearch = new TreeMap<String, String>();
        Collection<SearchRequest> searches = srMgr.getOwnedFilters(getLoggedInUser());
        if (searches != null)
        {
            for (SearchRequest search : searches)
            {
                aSearch.put(search.getId().toString(), search.getName());
            }
        }

        return aSearch;
    }

    public String getMainsel()
    {
        return mainsel;
    }

    public String getPgadmin()
    {
        return pgadmin;
    }

    public Map<String, String> getProjects()
    {
        Map<String, String> aProj = new TreeMap<String, String>();
        List<Project> projects = prMgr.getProjectObjects();
        if (projects != null)
        {
            for (Project project : projects)
            {
                aProj.put(project.getId().toString(), project.getName());
            }
        }

        return aProj;
    }

    public String[] getSelectedGroups()
    {
        return selectedGroups;
    }

    public String getShowfld()
    {
        return showfld;
    }

    public String getStartpoint()
    {
        return startpoint;
    }

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

    public void setCalcolor(String calcolor)
    {
        this.calcolor = calcolor;
    }

    public void setCaldescr(String caldescr)
    {
        this.caldescr = caldescr;
    }

    public void setCalname(String calname)
    {
        this.calname = calname;
    }

    public void setCdpinput(String cdpinput)
    {
        this.cdpinput = cdpinput;
    }

    public void setDisplay(String display)
    {
        this.display = display;
    }

    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

    public void setMainsel(String mainsel)
    {
        this.mainsel = mainsel;
    }

    public void setPgadmin(String pgadmin)
    {
        this.pgadmin = pgadmin;
    }

    public void setSaved(boolean isSaved)
    {
        this.isSaved = isSaved;
    }

    public void setSelectedGroups(String[] selectedGroups)
    {
        this.selectedGroups = selectedGroups;
    }

    public void setShowfld(String showfld)
    {
        this.showfld = showfld;
    }
 
    public void setStartpoint(String startpoint)
    {
        this.startpoint = startpoint;
    }
}
