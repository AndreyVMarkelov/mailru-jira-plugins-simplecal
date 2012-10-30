/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.ArrayList;
import java.util.List;
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
     * Calendars.
     */
    private final String CALENDARS = "calendars";

    /**
     * Plug-In Jira db key.
     */
    private final String PLUGIN_KEY = "SimpleCalendar";

    /**
     * Plug-In settings factory.
     */
    private final PluginSettingsFactory pluginSettingsFactory;

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
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.xstream = new XStream();
    }

    @Override
    public void deleteCalendar(Long id)
    {
        List<Long> longs = getCalendars();
        longs.remove(id);
        saveCalendars(longs);
        pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).remove(id + ".data");
    }

    @Override
    public List<Long> getCalendars()
    {
        return Utils.strToListLongs((String)pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(CALENDARS));
    }

    @Override
    public List<ProjectCalUserData> getCalendarsData()
    {
        List<ProjectCalUserData> datas = new ArrayList<ProjectCalUserData>();

        for (Long l : getCalendars())
        {
            String xmlData = (String)pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(l + ".data");
            if (xmlData != null && !xmlData.isEmpty())
            {
                try
                {
                    ProjectCalUserData ucd = (ProjectCalUserData)xstream.fromXML(xmlData);
                    datas.add(ucd);
                }
                catch (XStreamException xsex)
                {
                    //
                }
            }
        }
        return datas;
    }

    @Override
    public UserCalPref getUserCalPref(String user)
    {
        UserCalPref ucp = Starter.getUserCache().get(user);
        if (ucp != null)
        {
            return ucp;
        }

        String xmlData = (String)pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(prefKey(user));
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
        return (user + ".pref");
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
            pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).put(prefKey(user), xmlData);
        }
    }

    @Override
    public void saveCalendars(List<Long> cals)
    {
        pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).put(CALENDARS, Utils.listLongsToStr(cals));
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
            List<Long> longs = getCalendars();
            if (!longs.contains(pcud.getcTime()))
            {
                longs.add(pcud.getcTime());
            }
            saveCalendars(longs);
            pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).put(pcud.getcTime() + ".data", xmlData);
        }
    }

    @Override
    public ProjectCalUserData getCalendarData(Long id)
    {
        String xmlData = (String)pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(id + ".data");
        if (xmlData != null && !xmlData.isEmpty())
        {
            try
            {
                return (ProjectCalUserData)xstream.fromXML(xmlData);
            }
            catch (XStreamException xsex)
            {
                return null;
            }
        }

        return null;
    }
}
