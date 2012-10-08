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

    @Override
    public UserCalData getUserData(String user)
    {
        UserCalData ucd = Starter.getCache().get(user);
        if (ucd != null)
        {
            return ucd;
        }

        String xmlData = (String)pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(user);
        if (xmlData != null && !xmlData.isEmpty())
        {
            try
            {
                ucd = (UserCalData)xstream.fromXML(xmlData);
                Starter.getCache().put(user, ucd);
                return ucd;
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

        if (xmlData != null)
        {
            Starter.getUserCache().put(user, userPref);
            pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).put(prefKey(user), xmlData);
        }
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
            Starter.getCache().put(user, userData);
            pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).put(user, xmlData);
        }
    }
}
