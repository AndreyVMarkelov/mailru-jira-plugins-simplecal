/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.crowd.embedded.api.User;
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
