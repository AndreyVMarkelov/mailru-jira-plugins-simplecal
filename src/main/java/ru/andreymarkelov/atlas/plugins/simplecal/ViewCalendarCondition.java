package ru.andreymarkelov.atlas.plugins.simplecal;

import java.util.List;
import java.util.Map;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.plugin.webfragment.JiraWebInterfaceManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

public class ViewCalendarCondition implements Condition {
    private final MailRuCalCfg cfg;
    private final GroupManager grpMgr;

    public ViewCalendarCondition(MailRuCalCfg cfg, GroupManager grpMgr) {
        this.cfg = cfg;
        this.grpMgr = grpMgr;
    }

    private String getCurrentUser(Map<String, Object> context) {
        String username;
        Object userObj = context.get(JiraWebInterfaceManager.CONTEXT_KEY_USER);
        if (userObj != null) {
            if (userObj instanceof com.opensymphony.user.User) {
                username = ((com.opensymphony.user.User)userObj).getName();
            } else if (userObj instanceof com.atlassian.crowd.embedded.api.User) {
                username = ((com.atlassian.crowd.embedded.api.User)userObj).getName();
            } else {
                username = null;
            }
        } else {
            username = (String) context.get(JiraWebInterfaceManager.CONTEXT_KEY_USERNAME);
        }
        return username;
    }

    private UserUtil getUserUtil() {
        return ComponentManager.getComponentInstanceOfType(UserUtil.class);
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException {}

    @Override
    public boolean shouldDisplay(Map<String, Object> context) {
        String username = getCurrentUser(context);
        if (username != null) {
            List<String> groups = cfg.getCalendarGroups();
            if (groups != null && !groups.isEmpty()) {
                for (String group : groups) {
                    if (grpMgr.groupExists(group) && getUserUtil().userExists(username)) {
                        if (grpMgr.isUserInGroup(getUserUtil().getUserObject(username), grpMgr.getGroupObject(group))) return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }
}
