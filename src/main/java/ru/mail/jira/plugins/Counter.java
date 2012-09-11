/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

/**
 * Counter.
 * 
 * @author Andrey Markelov
 */
public class Counter
{
    /**
     * Initial val.
     */
    private static long val = System.currentTimeMillis();

    /**
     * Get unique value.
     */
    public synchronized static long getVal()
    {
        return val++;
    }

    /**
     * Private constructor.
     */
    private Counter() {}
}
