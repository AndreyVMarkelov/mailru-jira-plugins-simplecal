/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.sharing.SharePermissionUtils;
import com.atlassian.jira.sharing.SharedEntity.SharePermissions;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
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
     * Project manager.
     */
    private final ProjectManager prMgr;

    /**
     * Shares data.
     */
    private String shares_data;

    /**
     * Project role manager.
     */
    private final ProjectRoleManager projectRoleManager;

    /**
     * Field type.
     */
    private String showfld;

    /**
     * Search request service.
     */
    private final SearchRequestService srMgr;

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
        ProjectManager prMgr,
        ProjectRoleManager projectRoleManager)
    {
        this.applicationProperties = applicationProperties;
        this.groupManager = groupManager;
        this.mailCfg = mailCfg;
        this.srMgr = srMgr;
        this.prMgr = prMgr;
        this.projectRoleManager = projectRoleManager;
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

        List<String> groups = new ArrayList<String>();
        List<ProjRole> projRoles = new ArrayList<ProjRole>();
        try
        {
            JSONArray jsonObj = new JSONArray(shares_data);
            for (int i = 0; i < jsonObj.length(); i++)
            {
                JSONObject obj = jsonObj.getJSONObject(i);
                String type = obj.getString("type");
                if (type.equals("G"))
                {
                    groups.add(obj.getString("group"));
                }
                else
                {
                    ProjRole pr = new ProjRole(obj.getString("proj"), obj.getString("role"));;
                    projRoles.add(pr);
                }
            }
        }
        catch (JSONException e)
        {
            //impossible
        }

        if (display.equals(ProjectCalUserData.JCL_TYPE_STR))
        {
            JiraServiceContext jsCtx = new JiraServiceContextImpl(getLoggedInUser());
            SearchRequest sr = srMgr.getFilter(jsCtx, Long.parseLong(mainsel));

            JSONArray perms = new JSONArray();
            for (String group : groups)
            {
                JSONObject obj = new JSONObject();
                obj.put("type", "group");
                obj.put("param1", group);
                perms.put(obj);
            }

            for (ProjRole projRole : projRoles)
            {
                if (projRole.getRole().isEmpty())
                {
                    Collection<ProjectRole> roles = projectRoleManager.getProjectRoles();
                    if (roles != null)
                    {
                        for (ProjectRole role : roles)
                        {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "project");
                            obj.put("param1", projRole.getProject());
                            obj.put("param2", role.getId());
                            perms.put(obj);
                        }
                    }
                }
                else
                {
                    JSONObject obj = new JSONObject();
                    obj.put("type", "project");
                    obj.put("param1", projRole.getProject());
                    obj.put("param2", projRole.getRole());
                    perms.put(obj);
                }
            }

            SharePermissions sharePerms = SharePermissionUtils.fromJsonArray(perms);
            sr.setPermissions(sharePerms);
            srMgr.updateFilter(jsCtx, sr);
        }

        long ctime = Counter.getVal();

        Set<String> shUsers = Utils.getSharedUsers(groups, projRoles, groupManager, prMgr, projectRoleManager, getLoggedInUser().getName());
        for (String shUser : shUsers)
        {
            UserCalData usrData = mailCfg.getUserData(shUser);
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
                getLoggedInUser().getName(),
                groups,
                projRoles,
                ctime));
            mailCfg.putUserData(shUser, usrData);
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

        try
        {
            new JSONArray(shares_data);
        }
        catch (JSONException e)
        {
            addErrorMessage(getText("mailrucal.shares.error"));
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

    public List<DataPair> getFilters()
    {
        List<DataPair> filterPairs = new ArrayList<DataPair>();
        Collection<SearchRequest> searches = srMgr.getOwnedFilters(getLoggedInUser());
        if (searches != null)
        {
            for (SearchRequest search : searches)
            {
                DataPair pair = new DataPair(search.getId(), search.getName());
                filterPairs.add(pair);
            }
        }
        Collections.sort(filterPairs);

        return filterPairs;
    }

    public String getMainsel()
    {
        return mainsel;
    }

    public Map<Long, String> getProjectRoles()
    {
        Map<Long, String> roleProjs = new TreeMap<Long, String>();
        Collection<ProjectRole> roles = projectRoleManager.getProjectRoles();
        if (roles != null)
        {
            for (ProjectRole role : roles)
            {
                roleProjs.put(role.getId(), role.getName());
            }
        }

        return roleProjs;
    }

    public List<DataPair> getProjects()
    {
        List<DataPair> projPairs = new ArrayList<DataPair>();
        List<Project> projects = prMgr.getProjectObjects();
        if (projects != null)
        {
            for (Project project : projects)
            {
                DataPair pair = new DataPair(project.getId(), project.getName());
                projPairs.add(pair);
            }
        }
        Collections.sort(projPairs);

        return projPairs;
    }

    public String getShares_data()
    {
        return shares_data;
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

    public void setSaved(boolean isSaved)
    {
        this.isSaved = isSaved;
    }

    public void setShares_data(String shares_data)
    {
        this.shares_data = shares_data;
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
