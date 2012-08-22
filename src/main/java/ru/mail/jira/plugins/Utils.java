/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import javax.servlet.http.HttpServletRequest;

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
