/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.io.Writer;
import java.net.URI;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.exception.VelocityException;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.web.bean.PagerFilter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * Mail.Ru calendar REST service.
 * 
 * @author Andrey Markelov
 */
@Path("/mailcalsrv")
public class MailRuCalService
{
    /**
     * Logger.
     */
    private static Log log = LogFactory.getLog(MailRuCalService.class);

    /**
     * Max fetch size.
     */
    private static final int MAX_FETCH = 1500;

    /**
     * Group manager.
     */
    private final GroupManager groupMgr;

    /**
     * Mail.Ru calendar plug-In data.
     */
    private final MailRuCalCfg mailCfg;

    /**
     * Permission manager.
     */
    private final PermissionManager permMgr;

    /**
     * Project manager.
     */
    private final ProjectManager prMgr;

    /**
     * Date formatter.
     */
    private final SimpleDateFormat sdf;

    /**
     * Search request service.
     */
    private final SearchRequestService srMgr;

    /**
     * Utility for work with JIRA users.
     */
    private final UserUtil userUtil;

    /**
     * Constructor.
     */
    public MailRuCalService(
        MailRuCalCfg mailCfg,
        PermissionManager permMgr,
        ProjectManager prMgr,
        SearchRequestService srMgr,
        UserUtil userUtil,
        GroupManager groupMgr)
    {
        this.mailCfg = mailCfg;
        this.permMgr = permMgr;
        this.prMgr = prMgr;
        this.srMgr = srMgr;
        this.userUtil = userUtil;
        this.groupMgr = groupMgr;
        this.sdf = new SimpleDateFormat("yyyy-MM-dd");
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/addcalendar")
    public Response addCalendar(@Context HttpServletRequest request)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("MailRuCalService::addCalendar - User is not logged");
            return Response.status(401).build();
        }

        String name = request.getParameter("calname");
        String descr = request.getParameter("caldescr");
        String color = request.getParameter("calcolor");
        String display = request.getParameter("display");
        String mainsel = request.getParameter("mainsel");
        String showfld = request.getParameter("showfld");
        String startpoint = request.getParameter("startpoint");
        String endpoint = request.getParameter("endpoint");
        String cdpinput = request.getParameter("cdpinput");

        //--> checks
        if (!Utils.isStr(name) ||
            !Utils.isStr(color) ||
            !Utils.isStr(display) ||
            !Utils.isStr(mainsel) ||
            !Utils.isStr(showfld))
        {
            log.error("MailRuCalService::addCalendar - Required parameters are not set");
            return Response.status(500).build();
        }

        if (!display.equals(ProjectCalUserData.PROJECT_TYPE_STR) &&
            !display.equals(ProjectCalUserData.JCL_TYPE_STR))
        {
            log.error("MailRuCalService::addCalendar - Incorrect parameters");
            return Response.status(500).build();
        }

        if (!showfld.equals(ProjectCalUserData.IDD_STR) &&
            !showfld.equals(ProjectCalUserData.DATE_POINT_STR) &&
            !showfld.equals(ProjectCalUserData.DATE_RANGE_STR))
        {
            log.error("MailRuCalService::addCalendar - Incorrect parameters");
            return Response.status(500).build();
        }

        String start = "";
        String end = "";
        if (showfld.equals(ProjectCalUserData.DATE_POINT_STR))
        {
            if (!Utils.isStr(cdpinput))
            {
                log.error("MailRuCalService::addCalendar - Incorrect parameters");
                return Response.status(500).build();
            }
            start = cdpinput;
        }
        else if (showfld.equals(ProjectCalUserData.DATE_RANGE_STR))
        {
            if (!Utils.isStr(startpoint) || !Utils.isStr(endpoint))
            {
                log.error("MailRuCalService::addCalendar - Incorrect parameters");
                return Response.status(500).build();
            }
            start = startpoint;
            end = endpoint;
        }
        //<--

        UserCalData usrData = mailCfg.getUserData(user.getName());
        if (usrData == null)
        {
            usrData = new UserCalData();
        }

        usrData.add(new ProjectCalUserData(
            name,
            descr,
            color,
            display,
            mainsel,
            showfld,
            start,
            end,
            true,
            user.getName(),
            null));
        mailCfg.putUserData(user.getName(), usrData);

        String baseUrl = Utils.getBaseUrl(request);
        return Response.seeOther(URI.create(baseUrl + "/plugins/servlet/mailrucal/view")).build();
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/changecalmode")
    public Response changeCalendarMode(@Context HttpServletRequest request)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("MailRuCalService::changeCalendarMode - User is not logged");
            return Response.status(401).build();
        }

        String name = request.getParameter("name");
        String ctimestr = request.getParameter("ctime");
        String mode = request.getParameter("mode");

        //--> checks
        if (!Utils.isStr(name) ||
            !Utils.isStr(mode))
        {
            log.error("MailRuCalService::changeCalendarMode - Required parameters are not set");
            return Response.status(500).build();
        }

        Long ctime;
        try
        {
            ctime = Long.valueOf(ctimestr);
        }
        catch (NumberFormatException nfex)
        {
            log.error("MailRuCalService::changeCalendarMode - Incorrect input parameters");
            return Response.status(500).build();
        }

        UserCalData usrData = mailCfg.getUserData(user.getName());
        if (usrData != null)
        {
            Iterator<ProjectCalUserData> iter = usrData.getProjs().iterator();
            while (iter.hasNext())
            {
                ProjectCalUserData pcud = iter.next();
                if (pcud.getName().equals(name) && pcud.getcTime() == ctime.longValue())
                {
                    pcud.setActive(Boolean.parseBoolean(mode));
                }
            }
            mailCfg.putUserData(user.getName(), usrData);
        }

        return Response.ok().build();
    }

    /**
     * Create date point entity.
     */
    private EventEntity createDatePointEntity(
        ProjectCalUserData pcud,
        Issue issue,
        Date startDate,
        Date endDate,
        String baseUrl,
        CustomField datePointCf)
    {
        if (datePointCf != null)
        {
            Object val = issue.getCustomFieldValue(datePointCf);
            if (val != null && val instanceof Timestamp)
            {
                Timestamp ts = (Timestamp)val;
                if (ts.after(startDate) && ts.before(endDate))
                {
                    EventEntity en= new EventEntity();
                    en.setId(issue.getKey());
                    en.setTitle(issue.getSummary());
                    en.setStart(sdf.format(ts));
                    en.setColor(pcud.getColor());
                    en.setAllDay(true);
                    en.setUrl(baseUrl + "/browse/" + issue.getKey());
                    return en;
                }
            }
        }

        return null;
    }

    /**
     * Create date range entity.
     */
    private EventEntity createDateRangeEntity(
        ProjectCalUserData pcud,
        Issue issue,
        Date startDate,
        Date endDate,
        String baseUrl,
        CustomField startCf,
        CustomField endCf)
    {
        if (startCf != null && endCf != null)
        {
            Object startVal = issue.getCustomFieldValue(startCf);
            Object endVal = issue.getCustomFieldValue(endCf);

            if (startVal != null && startVal instanceof Timestamp &&
                endVal != null && endVal instanceof Timestamp)
            {
                Timestamp startTs = (Timestamp)startVal;
                Timestamp endTs = (Timestamp)endVal;
                if (endTs.after(startDate) && startTs.before(endDate))
                {
                    EventEntity en= new EventEntity();
                    en.setId(issue.getKey());
                    en.setTitle(issue.getSummary());
                    en.setStart(sdf.format(startTs));
                    en.setEnd(sdf.format(endTs));
                    en.setColor(pcud.getColor());
                    en.setAllDay(true);
                    en.setUrl(baseUrl + "/browse/" + issue.getKey());
                    return en;
                }
            }
            else if (startVal != null && startVal instanceof Timestamp)
            {
                Timestamp ts = (Timestamp)startVal;
                if (ts.after(startDate) && ts.before(endDate))
                {
                    EventEntity en= new EventEntity();
                    en.setId(issue.getKey());
                    en.setTitle(issue.getSummary());
                    en.setStart(sdf.format(ts));
                    en.setColor(pcud.getColor());
                    en.setAllDay(true);
                    en.setUrl(baseUrl + "/browse/" + issue.getKey());
                    return en;
                }
            }
            else if (endVal != null && endVal instanceof Timestamp)
            {
                Timestamp ts = (Timestamp)endVal;
                if (ts.after(startDate) && ts.before(endDate))
                {
                    EventEntity en= new EventEntity();
                    en.setId(issue.getKey());
                    en.setTitle(issue.getSummary());
                    en.setStart(sdf.format(ts));
                    en.setColor(pcud.getColor());
                    en.setAllDay(true);
                    en.setUrl(baseUrl + "/browse/" + issue.getKey());
                    return en;
                }
            }
        }
        else if (startCf == null && endCf != null)
        {
            if (endCf != null)
            {
                Object val = issue.getCustomFieldValue(endCf);
                if (val != null && val instanceof Timestamp)
                {
                    Timestamp ts = (Timestamp)val;
                    if (ts.after(startDate) && ts.before(endDate))
                    {
                        EventEntity en= new EventEntity();
                        en.setId(issue.getKey());
                        en.setTitle(issue.getSummary());
                        en.setStart(sdf.format(ts));
                        en.setColor(pcud.getColor());
                        en.setAllDay(true);
                        en.setUrl(baseUrl + "/browse/" + issue.getKey());
                        return en;
                    }
                }
            }
        }
        else if (startCf != null && endCf == null)
        {
            if (startCf != null)
            {
                Object val = issue.getCustomFieldValue(startCf);
                if (val != null && val instanceof Timestamp)
                {
                    Timestamp ts = (Timestamp)val;
                    if (ts.after(startDate) && ts.before(endDate))
                    {
                        EventEntity en= new EventEntity();
                        en.setId(issue.getKey());
                        en.setTitle(issue.getSummary());
                        en.setStart(sdf.format(ts));
                        en.setColor(pcud.getColor());
                        en.setAllDay(true);
                        en.setUrl(baseUrl + "/browse/" + issue.getKey());
                        return en;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Create event entity.
     */
    private EventEntity createEventEntity(
        ProjectCalUserData pcud,
        Issue issue,
        Date startDate,
        Date endDate,
        String baseUrl,
        CustomField datePointCf,
        CustomField startCf,
        CustomField endCf)
    {
        if (pcud.isIDD())
        {
            return createIddEvent(pcud, issue, startDate, endDate, baseUrl);
        }
        else if (pcud.isDatePoint())
        {
            return createDatePointEntity(pcud, issue, startDate, endDate, baseUrl, datePointCf);
        }
        else
        {
            return createDateRangeEntity(pcud, issue, startDate, endDate, baseUrl, startCf, endCf);
        }
    }

    /**
     * Create "Issue Due Date" entity.
     */
    private EventEntity createIddEvent(
        ProjectCalUserData pcud,
        Issue issue,
        Date startDate,
        Date endDate,
        String baseUrl)
    {
        Timestamp dueDate = issue.getDueDate();
        if (dueDate != null && dueDate.after(startDate) && dueDate.before(endDate))
        {
            EventEntity en= new EventEntity();
            en.setId(issue.getKey());
            en.setTitle(issue.getSummary());
            en.setStart(sdf.format(dueDate));
            en.setColor(pcud.getColor());
            en.setAllDay(true);
            en.setUrl(baseUrl + "/browse/" + issue.getKey());
            return en;
        }

        return null;
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/deletecalendar")
    public Response deleteCalendar(@Context HttpServletRequest request)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("MailRuCalService::deleteCalendar - User is not logged");
            return Response.status(401).build();
        }

        String name = request.getParameter("origcalname");
        String ctimestr = request.getParameter("calctime");

        Long ctime;
        try
        {
            ctime = Long.valueOf(ctimestr);
        }
        catch (NumberFormatException nfex)
        {
            log.error("MailRuCalService::deleteCalendar - Incorrect input parameters");
            return Response.status(500).build();
        }

        UserCalData usrData = mailCfg.getUserData(user.getName());
        if (usrData != null)
        {
            Iterator<ProjectCalUserData> iter = usrData.getProjs().iterator();
            while (iter.hasNext())
            {
                ProjectCalUserData pcud = iter.next();
                if (pcud.getName().equals(name) && pcud.getcTime() == ctime.longValue())
                {
                    iter.remove();
                }
            }
            mailCfg.putUserData(user.getName(), usrData);
        }

        String baseUrl = Utils.getBaseUrl(request);
        return Response.seeOther(URI.create(baseUrl + "/plugins/servlet/mailrucal/view")).build();
    }

    /**
     * Get all Jira groups.
     */
    private Collection<Group> getAllGroups()
    {
        return groupMgr.getAllGroups();
    }

    @GET
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/events")
    public Response getEvents(@Context HttpServletRequest request)
    throws SearchException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("MailRuCalService::getEvents - User is not logged");
            return Response.status(401).build();
        }

        String start = request.getParameter("start");
        String end = request.getParameter("end");

        long startLong;
        long endLong;
        try
        {
            startLong = Long.parseLong(start) * 1000;
            endLong = Long.parseLong(end) * 1000;
        }
        catch (NumberFormatException nfex)
        {
            log.error("MailRuCalService::getEvents - Incorrect input values", nfex);
            return Response.status(500).build();
        }

        Date startDate = new Date(startLong);
        Date endDate = new Date(endLong);

        UserCalData usrData = mailCfg.getUserData(user.getName());
        if (usrData == null)
        {
            return Response.ok().build();
        }

        List<EventEntity> eventObjs = new ArrayList<EventEntity>();
        for (ProjectCalUserData pcud : usrData.getProjs())
        {
            if (!pcud.isActive())
            {
                continue;
            }

            Long id;
            try
            {
                id = Long.valueOf(pcud.getTarget());
            }
            catch (NumberFormatException nex)
            {
                continue;
            }

            List<EventEntity> localeventObjs = null;
            if (pcud.isProjectType())
            {
                localeventObjs = getProjectIssues(id, user, startDate, endDate, pcud, Utils.getBaseUrl(request));
            }
            else
            {
                localeventObjs = getJclIssues(id, user, startDate, endDate, pcud, Utils.getBaseUrl(request));
            }

            if (localeventObjs != null && !localeventObjs.isEmpty())
            {
                eventObjs.addAll(localeventObjs);
            }
        }

        XStream xstream = new XStream(new JsonHierarchicalStreamDriver()
        {
            public HierarchicalStreamWriter createWriter(Writer writer)
            {
                return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE);
            }
        });

        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(xstream.toXML(eventObjs)).cacheControl(cc).build();
    }

    /**
     * Get query issues.
     */
    private List<EventEntity> getJclIssues(
        Long fltId,
        User user,
        Date startDate,
        Date endDate,
        ProjectCalUserData pcud,
        String baseUrl)
    throws SearchException
    {
        SearchRequest search = getSearchRequest(fltId, user);
        if (search != null)
        {
            //--> result
            List<EventEntity> entities = new ArrayList<EventEntity>();

            CustomField datePointCf = null;
            CustomField startCf = null;
            CustomField endCf = null;
            if (pcud.isDatePoint())
            {
                datePointCf = ComponentManager.getInstance().getCustomFieldManager().getCustomFieldObjectByName(pcud.getStartPoint());
            }
            else if (pcud.isDateRange())
            {
                startCf = ComponentManager.getInstance().getCustomFieldManager().getCustomFieldObjectByName(pcud.getStartPoint());
                endCf = ComponentManager.getInstance().getCustomFieldManager().getCustomFieldObjectByName(pcud.getEndPoint());
            }

            int start = 0;
            int count = (int)ComponentManager.getInstance().getSearchService().searchCount(user, search.getQuery());
            while (start < count)
            {
                List<Issue> issues = ComponentManager.getInstance().getSearchService().search(
                    user,
                    search.getQuery(),
                    PagerFilter.newPageAlignedFilter(start, MAX_FETCH)).getIssues();
                start += issues.size();
                for (Issue issue : issues)
                {
                    EventEntity entity = createEventEntity(pcud, issue, startDate, endDate, baseUrl, datePointCf, startCf, endCf);
                    if (entity != null)
                    {
                        entities.add(entity);
                    }
                }
            }

            return entities;
        }

        return null;
    }

    /**
     * Get project by ID.
     */
    private Project getProject(Long prId)
    {
        return prMgr.getProjectObj(prId);
    }

    /**
     * Get project issues.
     */
    private List<EventEntity> getProjectIssues(
        Long prId,
        User user,
        Date startDate,
        Date endDate,
        ProjectCalUserData pcud,
        String baseUrl)
    throws SearchException
    {
        List<EventEntity> entities = new ArrayList<EventEntity>();

        CustomField datePointCf = null;
        CustomField startCf = null;
        CustomField endCf = null;
        if (pcud.isDatePoint())
        {
            datePointCf = ComponentManager.getInstance().getCustomFieldManager().getCustomFieldObjectByName(pcud.getStartPoint());
        }
        else if (pcud.isDateRange())
        {
            startCf = ComponentManager.getInstance().getCustomFieldManager().getCustomFieldObjectByName(pcud.getStartPoint());
            endCf = ComponentManager.getInstance().getCustomFieldManager().getCustomFieldObjectByName(pcud.getEndPoint());
        }

        JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
        JqlClauseBuilder jcb = JqlQueryBuilder.newClauseBuilder();
        jcb.project(prId);
        builder.where().addClause(jcb.buildClause());

        int start = 0;
        int count = (int)ComponentManager.getInstance().getSearchService().searchCount(user, builder.buildQuery());
        while (start < count)
        {
            List<Issue> issues = ComponentManager.getInstance().getSearchService().search(
                user,
                builder.buildQuery(),
                PagerFilter.newPageAlignedFilter(start, MAX_FETCH)).getIssues();
            start += issues.size();
            for (Issue issue : issues)
            {
                EventEntity entity = createEventEntity(pcud, issue, startDate, endDate, baseUrl, datePointCf, startCf, endCf);
                if (entity != null)
                {
                    entities.add(entity);
                }
            }
        }

        return entities;
    }

    /**
     * Return <code>SearchRequest</code>.
     */
    private SearchRequest getSearchRequest(
        Long id,
        User user)
    {
        JiraServiceContext jsCtx = new JiraServiceContextImpl(user);
        return srMgr.getFilter(jsCtx, id);
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON})
    @Path("/addcaldlg")
    public Response initAddDialog(@Context HttpServletRequest request)
    throws VelocityException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("MailRuCalService::initAddDialog - User is not logged");
            return Response.status(401).build();
        }

        //--> available projects
        List<DataPair> projPairs = new ArrayList<DataPair>();
        List<Project> projects = prMgr.getProjectObjects();
        if (projects != null)
        {
            for (Project project : projects)
            {
                if (permMgr.hasPermission(Permissions.BROWSE, project, user))
                {
                    DataPair pair = new DataPair(project.getId(), project.getName());
                    projPairs.add(pair);
                }
            }
        }
        Collections.sort(projPairs);

        //--> available searches
        List<DataPair> filterPairs = new ArrayList<DataPair>();
        Collection<SearchRequest> searches = srMgr.getOwnedFilters(user);
        if (searches != null)
        {
            for (SearchRequest search : searches)
            {
                DataPair pair = new DataPair(search.getId(), search.getName());
                filterPairs.add(pair);
            }
        }
        Collections.sort(filterPairs);

        

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("i18n", authCtx.getI18nHelper());
        params.put("baseUrl", Utils.getBaseUrl(request));
        params.put("aProj", projPairs);
        params.put("aSearch", filterPairs);
        params.put("allGroups", getAllGroups());

        return Response.ok(new HtmlEntity(ComponentAccessor.getVelocityManager().getBody("templates/", "addcalendar.vm", params))).build();
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON})
    @Path("/infocaldlg")
    public Response initInfoDialog(@Context HttpServletRequest request)
    throws VelocityException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("MailRuCalService::initInfoDialog - User is not logged");
            return Response.status(401).build();
        }

        String name = request.getParameter("name");
        String ctimestr = request.getParameter("ctime");

        Long ctime;
        try
        {
            ctime = Long.valueOf(ctimestr);
        }
        catch (NumberFormatException nfex)
        {
            log.error("MailRuCalService::initInfoDialog - Incorrect input parameters");
            return Response.status(500).build();
        }

        UserCalData usrData = mailCfg.getUserData(user.getName());
        if (usrData == null)
        {
            return Response.ok().build();
        }

        for (ProjectCalUserData pcud : usrData.getProjs())
        {
            if (pcud.getName().equals(name) && pcud.getcTime() == ctime.longValue())
            {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("i18n", authCtx.getI18nHelper());
                params.put("baseUrl", Utils.getBaseUrl(request));
                params.put("pcud", pcud);
                params.put("createtime", authCtx.getOutlookDate().format(new Date(pcud.getcTime())));
                params.put("creator", Utils.getDisplayUser(userUtil, pcud.getCreator()));
                params.put("allGroups", getAllGroups());

                if (pcud.isProjectType())
                {
                    params.put("targetName", getProject(Long.valueOf(pcud.getTarget())).getName());
                }
                else
                {
                    params.put("targetName", getSearchRequest(Long.valueOf(pcud.getTarget()), user).getName());
                }

                return Response.ok(new HtmlEntity(ComponentAccessor.getVelocityManager().getBody("templates/", "infocalendar.vm", params))).build();
            }
        }

        return Response.ok().build();
    }

    private void s()
    {
        List<Project> projs = prMgr.getProjectObjects();
        if (projs != null)
        {
            for (Project proj : projs)
            {
                Collection<Group> projGroups = permMgr.getAllGroups(Permissions.BROWSE, proj);
                
            }
        }
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/setuserprefview")
    public Response setUserPrefView(@Context HttpServletRequest request)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("MailRuCalService::setUserPrefView - User is not logged");
            return Response.status(401).build();
        }

        String view = request.getParameter("view");

        UserCalPref userPref = mailCfg.getUserCalPref(user.getName());
        if (userPref == null)
        {
            userPref = new UserCalPref();
        }
        userPref.setDefaultView(view);
        mailCfg.putUserCalPref(user.getName(), userPref);

        return Response.ok().build();
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/updatecalendar")
    public Response updateCalendar(@Context HttpServletRequest request)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("MailRuCalService::updateCalendar - User is not logged");
            return Response.status(401).build();
        }

        String name = request.getParameter("origcalname");
        String ctimestr = request.getParameter("calctime");
        String newname = request.getParameter("calname");
        String descr = request.getParameter("caldescr");
        String color = request.getParameter("calcolor");

        Long ctime;
        try
        {
            ctime = Long.valueOf(ctimestr);
        }
        catch (NumberFormatException nfex)
        {
            log.error("MailRuCalService::updateCalendar - Incorrect input parameters");
            return Response.status(500).build();
        }

        //--> checks
        if (!Utils.isStr(name) ||
            !Utils.isStr(color))
        {
            log.error("MailRuCalService::updateCalendar - Required parameters are not set");
            return Response.status(500).build();
        }

        UserCalData usrData = mailCfg.getUserData(user.getName());
        if (usrData != null)
        {
            Iterator<ProjectCalUserData> iter = usrData.getProjs().iterator();
            while (iter.hasNext())
            {
                ProjectCalUserData pcud = iter.next();
                if (pcud.getName().equals(name) && pcud.getcTime() == ctime.longValue())
                {
                    pcud.setName(newname);
                    pcud.setDescr(descr);
                    pcud.setColor(color);
                }
            }
            mailCfg.putUserData(user.getName(), usrData);
        }

        String baseUrl = Utils.getBaseUrl(request);
        return Response.seeOther(URI.create(baseUrl + "/plugins/servlet/mailrucal/view")).build();
    }
}
