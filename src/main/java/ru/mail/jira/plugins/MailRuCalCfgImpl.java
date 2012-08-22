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
     * Constructor.
     */
    public MailRuCalCfgImpl(
        PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    public UserCalData getUserData(String user)
    {
        String xmlData = (String)pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(user);
        if (xmlData != null && !xmlData.isEmpty())
        {
            XStream xstream = new XStream();
            try
            {
                return (UserCalData)xstream.fromXML(xmlData);
            }
            catch (XStreamException xsex)
            {
                return null;
            }
        }
        return null;
    }

    @Override
    public void putUserData(String user, UserCalData userData)
    {
        String xmlData = "";
        if (userData != null)
        {
            XStream xstream = new XStream();
            xmlData = xstream.toXML(userData);
        }
        pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).put(user, xmlData);
    }
}
