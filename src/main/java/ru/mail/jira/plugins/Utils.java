/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleActors;
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
     * Get all group and project users.
     */
    public static Set<String> getSharedUsers(
        List<String> groups,
        List<ProjRole> projRoles,
        GroupManager groupMgr,
        ProjectManager prMgr,
        ProjectRoleManager projectRoleManager,
        String... sUsers)
    {
        Set<String> users = new TreeSet<String>();

        //--> add from groups
        if (groups != null)
        {
            for (String group : groups)
            {
                Collection<User> grUsers = groupMgr.getUsersInGroup(group);
                if (users == null)
                {
                    continue;
                }

                for (User grUser : grUsers)
                {
                    users.add(grUser.getName());
                }
            }
        }

        //--> from project roles
        if (projRoles != null)
        {
            for (ProjRole projRole : projRoles)
            {
                Project proj = prMgr.getProjectObj(Long.parseLong(projRole.getProject()));
                if (projRole.getRole().isEmpty())
                {
                    Collection<ProjectRole> roles = projectRoleManager.getProjectRoles();
                    if (roles != null)
                    {
                        for (ProjectRole role : roles)
                        {
                            ProjectRole pr = projectRoleManager.getProjectRole(role.getId());
                            ProjectRoleActors pra = projectRoleManager.getProjectRoleActors(pr, proj);
                            Set<com.opensymphony.user.User> prUsers = pra.getUsers();
                            if (prUsers != null)
                            {
                                for (com.opensymphony.user.User prUser : prUsers)
                                {
                                    users.add(prUser.getName());
                                }
                            }
                        }
                    }
                }
                else
                {
                    ProjectRole pr = projectRoleManager.getProjectRole(Long.parseLong(projRole.getRole()));
                    ProjectRoleActors pra = projectRoleManager.getProjectRoleActors(pr, proj);
                    Set<com.opensymphony.user.User> prUsers = pra.getUsers();
                    if (prUsers != null)
                    {
                        for (com.opensymphony.user.User prUser : prUsers)
                        {
                            users.add(prUser.getName());
                        }
                    }
                }
            }
        }

        if (sUsers != null)
        {
            for (String sUser : sUsers)
            {
                users.add(sUser);
            }
        }

        return users;
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
     * Private constructor.
     */
    private Utils() {}
}
