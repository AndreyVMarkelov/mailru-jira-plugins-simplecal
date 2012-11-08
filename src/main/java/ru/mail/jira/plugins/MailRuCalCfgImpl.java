/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

/**
 * IMplementation of <code>MailRuCalCfg</code>.
 * 
 * @author Andrey Markelov
 */
public class MailRuCalCfgImpl
    implements MailRuCalCfg
{
    /**
     * Logger.
     */
    private static Log log = LogFactory.getLog(MailRuCalCfgImpl.class);

    /**
     * Calendars.
     */
    private final String CALENDARS = "calendars";

    /**
     * Plug-In Jira db key.
     */
    private final String PLUGIN_KEY = "SimpleCalendar";

    /**
     * Plug-In settings.
     */
    private final PluginSettings pluginSettings;

    /**
     * XStream.
     */
    private XStream xstream;

    /**
     * Constructor.
     */
    public MailRuCalCfgImpl(
        PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY);
        this.xstream = new XStream();
    }

    /**
     * Key for calendar.
     */
    private String calKey(Long calId)
    {
        return (calId + ".data");
    }

    @Override
    public void deleteCalendar(
        Long id)
    {
        Set<Long> longs = getCalendars();
        longs.remove(id);
        saveCalendars(longs);
        getPluginSettings().remove(calKey(id));
        Starter.getCalendarsData().remove(id);
    }

    @Override
    public ProjectCalUserData getCalendarData(
        Long id)
    {
        if (Starter.getCalendarsData().containsKey(id))
        {
            return Starter.getCalendarsData().get(id);
        }

        String xmlData = (String)getPluginSettings().get(calKey(id));
        if (xmlData != null && !xmlData.isEmpty())
        {
            try
            {
                return (ProjectCalUserData)xstream.fromXML(xmlData);
            }
            catch (XStreamException xsex)
            {
                log.warn("MailRuCalCfgImpl::getCalendarData - XStream error", xsex);
                return null;
            }
        }

        return null;
    }

    @Override
    public Set<Long> getCalendars()
    {
        return Utils.strToListLongs((String)getPluginSettings().get(CALENDARS));
    }

    @Override
    public List<ProjectCalUserData> getCalendarsData()
    {
        List<ProjectCalUserData> datas = new ArrayList<ProjectCalUserData>();

        for (Long id : getCalendars())
        {
            if (Starter.getCalendarsData().containsKey(id))
            {
                datas.add(Starter.getCalendarsData().get(id));
                continue;
            }

            String xmlData = (String)getPluginSettings().get(calKey(id));
            if (xmlData != null && !xmlData.isEmpty())
            {
                try
                {
                    ProjectCalUserData ucd = (ProjectCalUserData)xstream.fromXML(xmlData);
                    datas.add(ucd);
                }
                catch (XStreamException xsex)
                {
                    log.warn("MailRuCalCfgImpl::getCalendarsData - XStream error", xsex);
                }
            }
        }
        return datas;
    }

    private synchronized PluginSettings getPluginSettings()
    {
        return pluginSettings;
    }

    @Override
    public UserCalPref getUserCalPref(
        String user)
    {
        UserCalPref ucp = Starter.getUserCache().get(user);
        if (ucp != null)
        {
            return ucp;
        }

        String xmlData = (String)getPluginSettings().get(prefKey(user));
        if (xmlData != null && !xmlData.isEmpty())
        {
            try
            {
                ucp = (UserCalPref)xstream.fromXML(xmlData);
                Starter.getUserCache().put(user, ucp);
                return ucp;
            }
            catch (XStreamException xsex)
            {
                log.warn("MailRuCalCfgImpl::getUserCalPref - XStream error", xsex);
                return null;
            }
        }
        return null;
    }

    /**
     * Key for preference data.
     */
    private String prefKey(String user)
    {
        return (user + ".userpref");
    }

    @Override
    public void putUserCalPref(String user, UserCalPref userPref)
    {
        String xmlData = "";
        if (userPref != null)
        {
            xmlData = xstream.toXML(userPref);
        }

        if (xmlData != null)
        {
            Starter.getUserCache().put(user, userPref);
            getPluginSettings().put(prefKey(user), xmlData);
        }
    }

    @Override
    public void saveCalendars(Set<Long> cals)
    {
        getPluginSettings().put(CALENDARS, Utils.listLongsToStr(cals));
    }

    @Override
    public void storeProjectCalUserData(ProjectCalUserData pcud)
    {
        String xmlData = "";
        if (pcud != null)
        {
            xmlData = xstream.toXML(pcud);
        }

        if (xmlData != null)
        {
            Set<Long> longs = getCalendars();
            longs.add(pcud.getCalId());
            saveCalendars(longs);
            getPluginSettings().put(calKey(pcud.getCalId()), xmlData);
            Starter.getCalendarsData().put(pcud.getCalId(), pcud);
        }
    }

    @Override
    public void updateProjectCalUserData(ProjectCalUserData pcud)
    {
        String xmlData = "";
        if (pcud != null)
        {
            xmlData = xstream.toXML(pcud);
        }

        if (xmlData != null)
        {
            getPluginSettings().put(calKey(pcud.getCalId()), xmlData);
            Starter.getCalendarsData().put(pcud.getCalId(), pcud);
        }
    }
}
