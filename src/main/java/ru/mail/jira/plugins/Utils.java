/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.I18nHelper;

/**
 * This class contains utility methods.
 * 
 * @author Andrey Markelov
 */
public class Utils
{
    /**
     * Convert array to set.
     */
    public static List<String> arrayToList(String[] array)
    {
        if (array == null)
        {
            return null;
        }
        else
        {
            List<String> list = new ArrayList<String>();
            for (String item : array)
            {
                list.add(item);
            }
            return list;
        }
    }

    public static Map<String, String> getStoredFields(
        Set<String> fields,
        I18nHelper i18n)
    {
        Map<String, String> fMap = new LinkedHashMap<String, String>();

        if (fields != null)
        {
            for (String field : fields)
            {
                if(field.equals("issuestatus"))
                {
                    fMap.put("issuestatus", i18n.getText("mailrucal.statusview"));
                }
                else if(field.equals("assignee"))
                {
                    fMap.put("assignee", i18n.getText("mailrucal.assigneeview"));
                }
                else if(field.equals("reporter"))
                {
                    fMap.put("reporter", i18n.getText("mailrucal.reporter"));
                }
                else if(field.equals("labels"))
                {
                    fMap.put("labels", i18n.getText("mailrucal.labels"));
                }
                else if(field.equals("components"))
                {
                    fMap.put("components", i18n.getText("mailrucal.components"));
                }
                else if(field.equals("duedate"))
                {
                    fMap.put("duedate", i18n.getText("mailrucal.duedate"));
                }
                else if(field.equals("environment"))
                {
                    fMap.put("environment", i18n.getText("mailrucal.environment"));
                }
                else if(field.equals("priority"))
                {
                    fMap.put("priority", i18n.getText("mailrucal.priority"));
                }
                else if(field.equals("resolution"))
                {
                    fMap.put("resolution", i18n.getText("mailrucal.resolution"));
                }
                else if(field.equals("affect"))
                {
                    fMap.put("affect", i18n.getText("mailrucal.affect"));
                }
                else if(field.equals("fixed"))
                {
                    fMap.put("fixed", i18n.getText("mailrucal.fixes"));
                }
                else if(field.equals("created"))
                {
                    fMap.put("created", i18n.getText("mailrucal.created"));
                }
                else if(field.equals("updated"))
                {
                    fMap.put("updated", i18n.getText("mailrucal.updated"));
                }
                else
                {
                    fMap.put(field, field);
                }
            }
        }

        return fMap;
    }

    /**
     * Get base URL from HTTP request.
     */
    public static String getBaseUrl(HttpServletRequest req)
    {
        return (req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath());
    }

    /**
     * Return display name of user if user exists in JIRA.
     */
    public static String getDisplayUser(UserUtil userUtil, String user)
    {
        User userObj = userUtil.getUserObject(user);
        if (userObj != null)
        {
            return userObj.getDisplayName();
        }
        else
        {
            return user;
        }
    }

    /**
     * Check that array is not null and is not empty.
     */
    public static boolean isArray(Object[] arr)
    {
        if (arr == null || arr.length == 0)
        {
            return false;
        }

        return true;
    }

    /**
     * Check if calendar is visible for user.
     */
    public static boolean isCalendarVisiable(
        ProjectCalUserData pcud,
        User user,
        GroupManager grMgr,
        ProjectManager prMgr,
        ProjectRoleManager projectRoleManager)
    {
        if (pcud.getCreator().equals(user.getName()))
        {
            return true;
        }

        if (pcud.getGroups() != null)
        {
            for (String groupStr : pcud.getGroups())
            {
                Group group;
                if ((group = grMgr.getGroupObject(groupStr)) != null)
                {
                    if (grMgr.isUserInGroup(user, group))
                    {
                        return true;
                    }
                }
            }
        }

        if (pcud.getProjRoles() != null)
        {
            for (ProjRole pr : pcud.getProjRoles())
            {
                if (pr.getRole().equals(""))
                {
                    Collection<ProjectRole> realRoles = projectRoleManager.getProjectRoles();
                    if (realRoles != null)
                    {
                        for (ProjectRole realRole : realRoles)
                        {
                            Project realProject = prMgr.getProjectObj(Long.valueOf(pr.getProject()));
                            if (realRole != null &&
                                realProject != null &&
                                projectRoleManager.isUserInProjectRole(user, realRole, realProject))
                            {
                                return true;
                            }
                        }
                    }
                }
                else
                {
                    ProjectRole realRole = projectRoleManager.getProjectRole(Long.valueOf(pr.getRole()));
                    Project realProject = prMgr.getProjectObj(Long.valueOf(pr.getProject()));
                    if (realRole != null &&
                        realProject != null &&
                        projectRoleManager.isUserInProjectRole(user, realRole, realProject))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check that string is not null and is not empty.
     */
    public static boolean isStr(String str)
    {
        if (str == null || str.length() == 0)
        {
            return false;
        }

        return true;
    }

    /**
     * Convert list to string for entity view.
     */
    public static String listEntityView(List<String> list)
    {
        StringBuilder sb = new StringBuilder("(");
        if (list != null)
        {
            for (int i = 0; i < list.size(); i++)
            {
                if (i > 0)
                {
                    sb.append(", ");
                }
                sb.append(list.get(i));
            }
        }
        sb.append(")");

        return sb.toString();
    }

    /**
     * Convert list of long to string.
     */
    public static String listLongsToStr(Set<Long> list)
    {
        StringBuilder sb = new StringBuilder();

        if (list != null && !list.isEmpty())
        {
            for (Number l : list)
            {
                sb.append(l.toString()).append("&");
            }
        }

        return sb.toString();
    }

    /**
     * Convert string to long list.
     */
    public static Set<Long> strToListLongs(String str)
    {
        Set<Long> list = new HashSet<Long>();

        if (str == null || str.isEmpty())
        {
            return list;
        }

        StringTokenizer st = new StringTokenizer(str, "&");
        while (st.hasMoreTokens())
        {
            try
            {
                list.add(Long.valueOf(st.nextToken()));
            }
            catch (NumberFormatException nex)
            {
                //--> ignore
            }
        }

        return list;
    }

    /**
     * Private constructor.
     */
    private Utils() {}
}
