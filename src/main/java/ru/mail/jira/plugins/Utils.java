/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.HashSet;
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

/**
 * This class contains utility methods.
 * 
 * @author Andrey Markelov
 */
public class Utils
{
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
