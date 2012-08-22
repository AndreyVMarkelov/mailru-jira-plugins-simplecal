/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.templaterenderer.TemplateRenderer;

/**
 * Main render servlet.
 * 
 * @author Andrey Markelov
 */
public class MailRuCalServlet
    extends HttpServlet
{
    /**
     * Unique ID.
     */
    private static final long serialVersionUID = 1896097245308399163L;

    /**
     * Login URI provider.
     */
    private final LoginUriProvider loginUriProvider;

    /**
     * Mail.Ru calendar plug-In data.
     */
    private final MailRuCalCfg mailCfg;

    /**
     * Template renderer.
     */
    private final TemplateRenderer renderer;

    /**
     * Constructor.
     */
    public MailRuCalServlet(
        MailRuCalCfg mailCfg,
        LoginUriProvider loginUriProvider,
        TemplateRenderer renderer)
    {
        this.mailCfg = mailCfg;
        this.loginUriProvider = loginUriProvider;
        this.renderer = renderer;
    }

    @Override
    protected void doGet(
        HttpServletRequest req,
        HttpServletResponse resp)
    throws ServletException,
           IOException
    {
        JiraAuthenticationContext authenticationContext = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authenticationContext.getLoggedInUser();

        if (user == null)
        {
            redirectToLogin(req, resp);
            return;
        }

        Map<String, Object> parms = new HashMap<String, Object>();
        parms.put("lang", req.getLocale().getLanguage());
        parms.put("baseUrl", Utils.getBaseUrl(req));
        parms.put("i18n", ComponentManager.getInstance().getJiraAuthenticationContext().getI18nHelper());
        parms.put("usrData", mailCfg.getUserData(user.getName()));

        resp.setContentType("text/html;charset=utf-8");
        renderer.render("/templates/mailrucal.vm", parms, resp.getWriter());
    }

    /**
     * Get reuqest URI.
     */
    private URI getUri(HttpServletRequest request)
    {
        StringBuffer builder = request.getRequestURL();
        if (request.getQueryString() != null)
        {
            builder.append("?");
            builder.append(request.getQueryString());
        }
        return URI.create(builder.toString());
    }

    /**
     * Redirect to login page if it's required.
     */
    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response)
    throws IOException
    {
        response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }
}
