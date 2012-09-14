/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

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
     * Plug-In Jira db key.
     */
    private final String PLUGIN_KEY = "MailRuCalendar";

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
    public UserCalPref getUserCalPref(String user)
    {
        String xmlData = (String)pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(prefKey(user));
        if (xmlData != null && !xmlData.isEmpty())
        {
            try
            {
                return (UserCalPref)xstream.fromXML(xmlData);
            }
            catch (XStreamException xsex)
            {
                return null;
            }
        }
        return null;
    }

    @Override
    public UserCalData getUserData(String user)
    {
        String xmlData = (String)pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(user);
        if (xmlData != null && !xmlData.isEmpty())
        {
            try
            {
                return (UserCalData)xstream.fromXML(xmlData);
            }
            catch (XStreamException xsex)
            {
                return new UserCalData();
            }
        }
        return new UserCalData();
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
        pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).put(prefKey(user), xmlData);
    }

    @Override
    public void putUserData(String user, UserCalData userData)
    {
        String xmlData = "";
        if (userData != null)
        {
            xmlData = xstream.toXML(userData);
        }

        if (xmlData != null)
        {
            pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).put(user, xmlData);
        }
    }
}
