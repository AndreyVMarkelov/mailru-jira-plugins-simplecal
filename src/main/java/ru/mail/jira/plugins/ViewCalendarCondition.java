/*
 * Created by Andrey Markelov 01-12-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.List;
import java.util.Map;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.plugin.webfragment.JiraWebInterfaceManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

/**
 * Calendar condition.
 *
 * @author Andrey Markelov
 */
public class ViewCalendarCondition
    implements Condition
{
    /**
     * Plug-In configuration.
     */
    private final MailRuCalCfg cfg;

    /**
     * Group manager.
     */
    private final GroupManager grpMgr;

    /**
     * Constructor.
     */
    public ViewCalendarCondition(
        MailRuCalCfg cfg,
        GroupManager grpMgr)
    {
        this.cfg = cfg;
        this.grpMgr = grpMgr;
    }

    /**
     * Get current user.
     */
    private String getCurrentUser(
        Map<String, Object> context)
    {
        String username;
        Object userObj = context.get(JiraWebInterfaceManager.CONTEXT_KEY_USER);
        if (userObj != null)
        {
            if (userObj instanceof com.opensymphony.user.User)
            {
                username = ((com.opensymphony.user.User)userObj).getName();
            }
            else if (userObj instanceof com.atlassian.crowd.embedded.api.User)
            {
                username = ((com.atlassian.crowd.embedded.api.User)userObj).getName();
            }
            else
            {
                username = null;
            }
        }
        else
        {
            username = (String) context.get(JiraWebInterfaceManager.CONTEXT_KEY_USERNAME);
        }

        return username;
    }

    /**
     * Get user utils.
     */
    private UserUtil getUserUtil()
    {
        return ComponentManager.getComponentInstanceOfType(UserUtil.class);
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException {}

    @Override
    public boolean shouldDisplay(
        Map<String, Object> context)
    {
        String username = getCurrentUser(context);

        if (username != null)
        {
            List<String> groups = cfg.getCalendarGroups();
            if (groups != null && !groups.isEmpty())
            {
                for (String group : groups)
                {
                    if (grpMgr.groupExists(group) && getUserUtil().userExists(username))
                    {
                        if (grpMgr.isUserInGroup(getUserUtil().getUserObject(username), grpMgr.getGroupObject(group)))
                        {
                            return true;
                        }
                    }
                }
            }
            else
            {
                return true;
            }
        }

        return false;
    }
}
