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
import java.util.Set;
import java.util.TreeMap;
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
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.sharing.SharePermission;
import com.atlassian.jira.sharing.SharePermissionUtils;
import com.atlassian.jira.sharing.SharedEntity.SharePermissions;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
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
     * Custom field manager.
     */
    private final CustomFieldManager cfMgr;

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
     * Project role manager.
     */
    private final ProjectRoleManager projectRoleManager;

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
     * Xstream.
     */
    private XStream xstream;

    /**
     * Constructor.
     */
    public MailRuCalService(
        MailRuCalCfg mailCfg,
        PermissionManager permMgr,
        ProjectManager prMgr,
        SearchRequestService srMgr,
        UserUtil userUtil,
        GroupManager groupMgr,
        ProjectRoleManager projectRoleManager,
        CustomFieldManager cfMgr)
    {
        this.mailCfg = mailCfg;
        this.permMgr = permMgr;
        this.prMgr = prMgr;
        this.srMgr = srMgr;
        this.userUtil = userUtil;
        this.groupMgr = groupMgr;
        this.projectRoleManager = projectRoleManager;
        this.cfMgr = cfMgr;
        this.sdf = new SimpleDateFormat("yyyy-MM-dd");
        this.xstream = new XStream(new JsonHierarchicalStreamDriver()
        {
            public HierarchicalStreamWriter createWriter(Writer writer)
            {
                return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE);
            }
        });
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/addcalendar")
    public Response addCalendar(@Context HttpServletRequest request)
    throws Exception
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
        String shares_data = request.getParameter("shares_data");

        List<String> groups = new ArrayList<String>();
        List<ProjRole> projRoles = new ArrayList<ProjRole>();
        try
        {
            JSONArray jsonObj = new JSONArray(shares_data);
            for (int i = 0; i < jsonObj.length(); i++)
            {
                JSONObject obj = jsonObj.getJSONObject(i);
                String type = obj.getString("type");
                if (type.equals("G"))
                {
                    groups.add(obj.getString("group"));
                }
                else
                {
                    ProjRole pr = new ProjRole(obj.getString("proj"), obj.getString("role"));
                    projRoles.add(pr);
                }
            }
        }
        catch (JSONException e)
        {
            log.error("MailRuCalService::addCalendar - Incorrect parameters", e);
            return Response.status(500).build();
        }

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

        if (display.equals(ProjectCalUserData.JCL_TYPE_STR))
        {
            JSONArray perms = new JSONArray();
            JiraServiceContext jsCtx = new JiraServiceContextImpl(user);
            SearchRequest sr = srMgr.getFilter(jsCtx, Long.parseLong(mainsel));
            SharePermissions oldSharePerms = sr.getPermissions();
            for (SharePermission sp : oldSharePerms.getPermissionSet())
            {
                JSONObject obj = new JSONObject();
                obj.put("type", sp.getType().get());
                obj.put("param1", sp.getParam1());
                obj.put("param2", sp.getParam2());
                perms.put(obj);
            }

            for (String group : groups)
            {
                JSONObject obj = new JSONObject();
                obj.put("type", "group");
                obj.put("param1", group);
                perms.put(obj);
            }

            for (ProjRole projRole : projRoles)
            {
                if (projRole.getRole().isEmpty())
                {
                    Collection<ProjectRole> roles = projectRoleManager.getProjectRoles();
                    if (roles != null)
                    {
                        for (ProjectRole role : roles)
                        {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "project");
                            obj.put("param1", projRole.getProject());
                            obj.put("param2", role.getId());
                            perms.put(obj);
                        }
                    }
                }
                else
                {
                    JSONObject obj = new JSONObject();
                    obj.put("type", "project");
                    obj.put("param1", projRole.getProject());
                    obj.put("param2", projRole.getRole());
                    perms.put(obj);
                }
            }

            SharePermissions sharePerms = SharePermissionUtils.fromJsonArray(perms);
            sr.setPermissions(sharePerms);
            srMgr.updateFilter(jsCtx, sr);
        }

        long calId = Counter.getVal();

        ProjectCalUserData pcud = new ProjectCalUserData(
            calId,
            name,
            descr,
            color,
            display,
            mainsel,
            showfld,
            start,
            end,
            user.getName(),
            groups,
            projRoles,
            System.currentTimeMillis());
        mailCfg.storeProjectCalUserData(pcud);

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

        String ctimestr = request.getParameter("ctime");
        String mode = request.getParameter("mode");

        //--> checks
        if (!Utils.isStr(ctimestr))
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

        UserCalPref userPref = mailCfg.getUserCalPref(user.getName());
        if (userPref == null)
        {
            userPref = new UserCalPref();
        }

        if (Utils.isStr(mode) &&
            (Boolean.parseBoolean(mode) || mode.equalsIgnoreCase("checked")))
        {
            userPref.removeshadowCalendar(ctime);
        }
        else
        {
            userPref.addshadowCalendar(ctime);
        }
        mailCfg.putUserCalPref(user.getName(), userPref);

        return Response.ok().build();
    }

    /**
     * Create date point entity.
     */
    private EventEntity createDatePointEntity(
        ProjectCalUserData pcud,
        Issue issue,
        String baseUrl,
        CustomField datePointCf,
        String color)
    {
        if (datePointCf != null)
        {
            Object val = issue.getCustomFieldValue(datePointCf);
            if (val != null && val instanceof Timestamp)
            {
                Timestamp ts = (Timestamp)val;
                return createEventEntityObj(issue, baseUrl, color, sdf.format(ts), null);
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
        String baseUrl,
        CustomField startCf,
        CustomField endCf,
        String color)
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
                return createEventEntityObj(issue, baseUrl, color, sdf.format(startTs), sdf.format(endTs));
            }
            else if (startVal != null && startVal instanceof Timestamp)
            {
                Timestamp ts = (Timestamp)startVal;
                return createEventEntityObj(issue, baseUrl, color, sdf.format(ts), null);
            }
            else if (endVal != null && endVal instanceof Timestamp)
            {
                Timestamp ts = (Timestamp)endVal;
                return createEventEntityObj(issue, baseUrl, color, sdf.format(ts), null);
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
                    return createEventEntityObj(issue, baseUrl, color, sdf.format(ts), null);
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
                    return createEventEntityObj(issue, baseUrl, color, sdf.format(ts), null);
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
        String baseUrl,
        CustomField startCf,
        CustomField endCf,
        String color)
    {
        if (pcud.isIDD())
        {
            return createIddEvent(pcud, issue, baseUrl, color);
        }
        else if (pcud.isDatePoint())
        {
            return createDatePointEntity(pcud, issue, baseUrl, startCf, color);
        }
        else
        {
            return createDateRangeEntity(pcud, issue, baseUrl, startCf, endCf, color);
        }
    }

    /**
     * Create entity object.
     */
    private EventEntity createEventEntityObj(
        Issue issue,
        String baseUrl,
        String color,
        String start,
        String end)
    {
        EventEntity en= new EventEntity();
        en.setId(issue.getKey());
        //--> summary
        en.setTitle(issue.getSummary());
        en.setStart(start);
        en.setEnd(end);
        en.setColor(color);
        en.setAllDay(true);
        en.setUrl(baseUrl + "/browse/" + issue.getKey());
        //--> key
        en.setKey(issue.getKey());
        //--> status
        en.setStatus(issue.getStatusObject().getName());
        //--> assignee
        if (issue.getAssigneeUser() != null)
        {
            en.setAssignee(issue.getAssigneeUser().getDisplayName());
        }
        //--> reporter
        if (issue.getReporterUser() != null)
        {
            en.setReporter(issue.getReporterUser().getDisplayName());
        }
        //--> custom fields
        List<CustomField> cfs = cfMgr.getCustomFieldObjects(issue);
        if (cfs != null && !cfs.isEmpty())
        {
            Map<String, String> cfMap = new HashMap<String, String>();
            for (CustomField cf : cfs)
            {
                Object cfVal = cf.getValue(issue);
                if (cfVal != null)
                cfMap.put(cf.getName(), cfVal.toString());
            }
            en.setCustomFields(cfMap);
        }
        //--> labels
        Set<Label> labels = issue.getLabels();
        if (labels != null && !labels.isEmpty())
        {
            List<String> labelList = new ArrayList<String>();
            for (Label label : labels)
            {
                labelList.add(label.getLabel());
            }
            en.setLabels(labelList);
        }
        //--> components
        Collection<ProjectComponent> comps = issue.getComponentObjects();
        if (comps != null && !comps.isEmpty())
        {
            List<String> pcs = new ArrayList<String>();
            for (ProjectComponent pc : comps)
            {
                pcs.add(pc.getName());
            }
            en.setComponents(pcs);
        }
        //--> due
        if (issue.getDueDate() != null)
        {
            en.setDue(issue.getDueDate().toString());
        }
        //--> environment
        en.setEnvironment(issue.getEnvironment());
        //--> priority
        if (issue.getPriorityObject() != null)
        {
            en.setPriority(issue.getPriorityObject().getName());
        }
        //--> resolution
        if (issue.getResolutionObject() != null)
        {
            en.setResolution(issue.getResolutionObject().getName());
        }
        //--> affect versions
        Collection<Version> vers = issue.getAffectedVersions();
        if (vers != null && !vers.isEmpty())
        {
            List<String> versList = new ArrayList<String>();
            for (Version ver : vers)
            {
                versList.add(ver.getName());
            }
            en.setAffectVersions(versList);
        }
        //--> fix versions
        vers = issue.getFixVersions();
        if (vers != null && !vers.isEmpty())
        {
            List<String> versList = new ArrayList<String>();
            for (Version ver : vers)
            {
                versList.add(ver.getName());
            }
            en.setFixVersions(versList);
        }
        //--> created
        en.setCreated(issue.getCreated().toString());
        //--> updated
        en.setUpdated(issue.getUpdated().toString());

        return en;
    }

    /**
     * Create "Issue Due Date" entity.
     */
    private EventEntity createIddEvent(
        ProjectCalUserData pcud,
        Issue issue,
        String baseUrl,
        String color)
    {
        Timestamp dueDate = issue.getDueDate();
        return createEventEntityObj(issue, baseUrl, color, sdf.format(dueDate), null);
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

        mailCfg.deleteCalendar(ctime);

        return Response.ok().build();
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

        List<ProjectCalUserData> datas = mailCfg.getCalendarsData();
        Iterator<ProjectCalUserData> iter = datas.iterator();
        while (iter.hasNext())
        {
            ProjectCalUserData pcud = iter.next();

            if (!Utils.isCalendarVisiable(pcud, user, groupMgr, prMgr, projectRoleManager))
            {
                iter.remove();
            }
        }

        UserCalPref userPref = mailCfg.getUserCalPref(user.getName());
        if (userPref == null)
        {
            userPref = new UserCalPref();
        }

        List<EventEntity> eventObjs = new ArrayList<EventEntity>();
        for (ProjectCalUserData pcud : datas)
        {
            if (userPref.isCalendarShadow(pcud.getCalId()))
            {
                continue;
            }

            //--> set user color
            String color = pcud.getColor();
            if (userPref.isUserColor(pcud.getCalId()))
            {
                color = userPref.getUserColor(pcud.getCalId());
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
                localeventObjs = getProjectIssues(id, user, startLong, endLong, pcud, Utils.getBaseUrl(request), color);
            }
            else
            {
                localeventObjs = getJclIssues(id, user, startLong, endLong, pcud, Utils.getBaseUrl(request), color);
            }

            if (localeventObjs != null && !localeventObjs.isEmpty())
            {
                eventObjs.addAll(localeventObjs);
            }
        }

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
        long startDate,
        long endDate,
        ProjectCalUserData pcud,
        String baseUrl,
        String color)
    throws SearchException
    {
        SearchRequest search = getSearchRequest(fltId, user);
        if (search != null)
        {
            //--> result
            List<EventEntity> entities = new ArrayList<EventEntity>();

            CustomField startCf = null;
            CustomField endCf = null;
            if (pcud.isDatePoint())
            {
                if (pcud.getStartPoint().startsWith("customfield_"))
                {
                    startCf = cfMgr.getCustomFieldObject(pcud.getStartPoint());
                }
                else
                {
                    startCf = cfMgr.getCustomFieldObjectByName(pcud.getStartPoint());
                }
            }
            else if (pcud.isDateRange())
            {
                if (pcud.getStartPoint().startsWith("customfield_"))
                {
                    startCf = cfMgr.getCustomFieldObject(pcud.getStartPoint());
                }
                else
                {
                    startCf = cfMgr.getCustomFieldObjectByName(pcud.getStartPoint());
                }

                if (pcud.getEndPoint().startsWith("customfield_"))
                {
                    endCf = cfMgr.getCustomFieldObject(pcud.getEndPoint());
                }
                else
                {
                    endCf = cfMgr.getCustomFieldObjectByName(pcud.getEndPoint());
                }
            }

            JqlQueryBuilder builder = JqlQueryBuilder.newBuilder(search.getQuery());
            JqlClauseBuilder jcb = JqlQueryBuilder.newClauseBuilder();
            if (pcud.isIDD())
            {
                jcb.and().dueBetween(formatDate(startDate), formatDate(endDate));
            }
            else if (pcud.isDatePoint())
            {
                JqlClauseBuilder jcb2 = JqlQueryBuilder.newClauseBuilder();
                jcb2.customField(startCf.getIdAsLong()).gtEq(formatDate(startDate)).and().customField(startCf.getIdAsLong()).ltEq(formatDate(endDate));
                jcb.and().addClause(jcb2.buildClause());
            }
            else if (pcud.isDateRange())
            {
                if (startCf != null && endCf != null)
                {
                    JqlClauseBuilder jcb2 = JqlQueryBuilder.newClauseBuilder();
                    JqlClauseBuilder jcb3 = JqlQueryBuilder.newClauseBuilder().not().addClause(JqlQueryBuilder.newClauseBuilder().customField(startCf.getIdAsLong()).gt(formatDate(endDate)).buildClause());
                    JqlClauseBuilder jcb4 = JqlQueryBuilder.newClauseBuilder().not().addClause(JqlQueryBuilder.newClauseBuilder().customField(endCf.getIdAsLong()).lt(formatDate(startDate)).buildClause());
                    jcb3.and().addClause(jcb4.buildClause());
                    jcb2.addClause(jcb3.buildClause());
                    jcb.and().addClause(jcb2.buildClause());
                }
            }
            builder.where().and().addClause(jcb.buildClause());

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
                    EventEntity entity = createEventEntity(pcud, issue, baseUrl, startCf, endCf, color);
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
        long startDate,
        long endDate,
        ProjectCalUserData pcud,
        String baseUrl,
        String color)
    throws SearchException
    {
        List<EventEntity> entities = new ArrayList<EventEntity>();

        CustomField startCf = null;
        CustomField endCf = null;
        if (pcud.isDatePoint())
        {
            if (pcud.getStartPoint().startsWith("customfield_"))
            {
                startCf = cfMgr.getCustomFieldObject(pcud.getStartPoint());
            }
            else
            {
                startCf = cfMgr.getCustomFieldObjectByName(pcud.getStartPoint());
            }
        }
        else if (pcud.isDateRange())
        {
            if (pcud.getStartPoint().startsWith("customfield_"))
            {
                startCf = cfMgr.getCustomFieldObject(pcud.getStartPoint());
            }
            else
            {
                startCf = cfMgr.getCustomFieldObjectByName(pcud.getStartPoint());
            }

            if (pcud.getEndPoint().startsWith("customfield_"))
            {
                endCf = cfMgr.getCustomFieldObject(pcud.getEndPoint());
            }
            else
            {
                endCf = cfMgr.getCustomFieldObjectByName(pcud.getEndPoint());
            }
        }

        JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
        JqlClauseBuilder jcb = JqlQueryBuilder.newClauseBuilder();
        jcb.project(prId);
        if (pcud.isIDD())
        {
            jcb.and().dueBetween(formatDate(startDate), formatDate(endDate));
        }
        else if (pcud.isDatePoint())
        {
            JqlClauseBuilder jcb2 = JqlQueryBuilder.newClauseBuilder();
            jcb2.customField(startCf.getIdAsLong()).gtEq(formatDate(startDate)).and().customField(startCf.getIdAsLong()).ltEq(formatDate(endDate));
            jcb.and().addClause(jcb2.buildClause());
        }
        else if (pcud.isDateRange())
        {
            if (startCf != null && endCf != null)
            {
                JqlClauseBuilder jcb2 = JqlQueryBuilder.newClauseBuilder();
                JqlClauseBuilder jcb3 = JqlQueryBuilder.newClauseBuilder().not().addClause(JqlQueryBuilder.newClauseBuilder().customField(startCf.getIdAsLong()).gt(formatDate(endDate)).buildClause());
                JqlClauseBuilder jcb4 = JqlQueryBuilder.newClauseBuilder().not().addClause(JqlQueryBuilder.newClauseBuilder().customField(endCf.getIdAsLong()).lt(formatDate(startDate)).buildClause());
                jcb3.and().addClause(jcb4.buildClause());
                jcb2.addClause(jcb3.buildClause());
                jcb.and().addClause(jcb2.buildClause());
            }
        }
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
                EventEntity entity = createEventEntity(pcud, issue, baseUrl, startCf, endCf, color);
                if (entity != null)
                {
                    entities.add(entity);
                }
            }
        }

        return entities;
    }

    /**
     * Format date.
     */
    private synchronized String formatDate(long date)
    {
        return sdf.format(date);
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

        Map<Long, String> roleProjs = new TreeMap<Long, String>();
        Collection<ProjectRole> roles = projectRoleManager.getProjectRoles();
        if (roles != null)
        {
            for (ProjectRole role : roles)
            {
                roleProjs.put(role.getId(), role.getName());
            }
        }

        Map<String, String> cfs = new TreeMap<String, String>();
        for (CustomField cf : cfMgr.getCustomFieldObjects())
        {
            String key = cf.getCustomFieldType().getKey();
            if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:datepicker") ||
                key.equals("com.atlassian.jira.plugin.system.customfieldtypes:datetime"))
            {
                cfs.put(cf.getId(), cf.getName());
            }
        }

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
        params.put("allGroups", groupMgr.getGroupsForUser(user.getName()));
        params.put("roleProjs", roleProjs);
        params.put("cfs", cfs);

        return Response.ok(new HtmlEntity(ComponentAccessor.getVelocityManager().getBody("templates/", "addcalendar.vm", params))).build();
    }

    @POST
    @Produces ({ MediaType.APPLICATION_JSON})
    @Path("/initcreatedlg")
    public Response initCreateDlg(@Context HttpServletRequest request)
    throws VelocityException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("MailRuCalService::initCreateDlg - User is not logged");
            return Response.status(401).build();
        }

        String dateStr = request.getParameter("date");
        long dateLong;
        try
        {
            dateLong = Long.parseLong(dateStr);
        }
        catch (NumberFormatException nex)
        {
            log.error("MailRuCalService::initCreateDlg - Required parameters are not set");
            return Response.status(500).build();
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("i18n", authCtx.getI18nHelper());
        params.put("baseUrl", Utils.getBaseUrl(request));
        params.put("user", user.getName());
        params.put("date", authCtx.getOutlookDate().formatDatePicker(new Date(dateLong)));

        List<ProjectCalUserData> datas = mailCfg.getCalendarsData();
        Iterator<ProjectCalUserData> iter = datas.iterator();
        while (iter.hasNext())
        {
            ProjectCalUserData pcud = iter.next();

            if (!Utils.isCalendarVisiable(pcud, user, groupMgr, prMgr, projectRoleManager))
            {
                iter.remove();
            }
        }

        List<ProjCreateIssueData> pcids = new ArrayList<ProjCreateIssueData>();
        for (ProjectCalUserData pcud : datas)
        {
            if (!pcud.isProjectType())
            {
                continue;
            }

            Project proj = getProject(Long.valueOf(pcud.getTarget()));
            if (proj == null)
            {
                continue;
            }

            String target;
            if (pcud.isIDD())
            {
                target = "duedate";
            }
            else
            {
                CustomField cf = cfMgr.getCustomFieldObjectByName(pcud.getStartPoint());
                if (cf == null)
                {
                    continue;
                }

                target = cf.getId();
            }

            ProjCreateIssueData pcid = new ProjCreateIssueData();
            pcid.setCalName(pcud.getName());
            pcid.setCtime(pcud.getCalId());
            pcid.setProjId(proj.getId());
            pcid.setKey(proj.getKey());
            pcid.setName(proj.getName());
            pcid.setTargetName(target);

            IssueTypeSchemeManager issueTypeSchemeManager = ComponentManager.getInstance().getIssueTypeSchemeManager();
            Collection<IssueType> its = issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(proj);
            for (IssueType it : its)
            {
                pcid.addIssueType(it.getId(), it.getName());
            }

            pcids.add(pcid);
        }
        params.put("pcids", pcids);

        return Response.ok(new HtmlEntity(ComponentAccessor.getVelocityManager().getBody("templates/", "initCreateIssue.vm", params))).build();
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON})
    @Path("/infocaldlg")
    public Response initInfoDialog(@Context HttpServletRequest request)
    throws VelocityException, JSONException
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance().getJiraAuthenticationContext();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("MailRuCalService::initInfoDialog - User is not logged");
            return Response.status(401).build();
        }

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

        ProjectCalUserData pcud = mailCfg.getCalendarData(ctime);
        if (pcud != null && Utils.isCalendarVisiable(pcud, user, groupMgr, prMgr, projectRoleManager))
        {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("i18n", authCtx.getI18nHelper());
            params.put("baseUrl", Utils.getBaseUrl(request));
            params.put("pcud", pcud);
            params.put("createtime", authCtx.getOutlookDate().format(new Date(pcud.getCalId())));
            params.put("creator", Utils.getDisplayUser(userUtil, pcud.getCreator()));
            params.put("allGroups", groupMgr.getGroupsForUser(user.getName()));

            //--> set user color
            UserCalPref userPref = mailCfg.getUserCalPref(user.getName());
            if (userPref == null)
            {
                userPref = new UserCalPref();
            }
            String color = pcud.getColor();
            if (userPref.isUserColor(pcud.getCalId()))
            {
                color = userPref.getUserColor(pcud.getCalId());
            }
            params.put("usercolor", color);

            if (pcud.getCreator() != null && pcud.getCreator().equals(user.getName()))
            {
                params.put("isOwner", pcud.getCreator().equals(user.getName()));
            }
            else
            {
                params.put("isOwner", false);
            }

            if (pcud.isProjectType())
            {
                Project proj = getProject(Long.valueOf(pcud.getTarget()));
                if (proj == null)
                {
                    return Response.ok(new HtmlEntity("NO_PROJECT")).build();
                }
                params.put("targetName", proj.getName());
            }
            else
            {
                SearchRequest sr = getSearchRequest(Long.valueOf(pcud.getTarget()), user);
                if (sr == null)
                {
                    return Response.ok(new HtmlEntity("NO_FILTER")).build();
                }
                params.put("targetName", sr.getName());
            }

            JSONArray storedShares = new JSONArray();
            if (pcud.getGroups() != null)
            {
                for (String group : pcud.getGroups())
                {
                    JSONObject obj = new JSONObject();
                    obj.put("id", "group" + group);
                    obj.put("type", "G");
                    obj.put("group", group);
                    storedShares.put(obj);
                }
            }
            if (pcud.getProjRoles() != null)
            {
                for (ProjRole pr : pcud.getProjRoles())
                {
                    JSONObject obj = new JSONObject();
                    obj.put("id", "project" + pr.getProject() + "role" + pr.getRole());
                    obj.put("type", "P");
                    obj.put("proj", pr.getProject());
                    obj.put("role", pr.getRole());
                    storedShares.put(obj);
                }
            }

            //--> available projects
            List<DataPair> projPairs = new ArrayList<DataPair>();
            Map<String, String> projMap = new HashMap<String, String>();
            List<Project> projects = prMgr.getProjectObjects();
            if (projects != null)
            {
                for (Project project : projects)
                {
                    if (permMgr.hasPermission(Permissions.BROWSE, project, user))
                    {
                        DataPair pair = new DataPair(project.getId(), project.getName());
                        projMap.put(Long.toString(project.getId()), project.getName());
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

            Map<Long, String> roleProjs = new TreeMap<Long, String>();
            Map<String, String> roleProjMap = new TreeMap<String, String>();
            Collection<ProjectRole> roles = projectRoleManager.getProjectRoles();
            if (roles != null)
            {
                for (ProjectRole role : roles)
                {
                    roleProjMap.put(Long.toString(role.getId()), role.getName());
                    roleProjs.put(role.getId(), role.getName());
                }
            }

            Map<String, String> cfs = new TreeMap<String, String>();
            for (CustomField cf : cfMgr.getCustomFieldObjects())
            {
                String key = cf.getCustomFieldType().getKey();
                if (key.equals("com.atlassian.jira.plugin.system.customfieldtypes:datepicker") ||
                    key.equals("com.atlassian.jira.plugin.system.customfieldtypes:datetime"))
                {
                    cfs.put(cf.getId(), cf.getName());
                }
            }

            params.put("cfs", cfs);
            params.put("roleProjs", roleProjs);
            params.put("aProj", projPairs);
            params.put("aSearch", filterPairs);
            params.put("projMap", projMap);
            params.put("roleProjMap", roleProjMap);
            params.put("storedShares", storedShares.toString());

            return Response.ok(new HtmlEntity(ComponentAccessor.getVelocityManager().getBody("templates/", "infocalendar.vm", params))).build();
        }

        return Response.ok(new HtmlEntity("NO_CALENDAR")).build();
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
    throws JSONException
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
        String shares_data = request.getParameter("shares_data");
        String display = request.getParameter("display");
        String mainsel = request.getParameter("mainsel");
        String showfld = request.getParameter("showfld");
        String startpoint = request.getParameter("startpoint");
        String endpoint = request.getParameter("endpoint");
        String cdpinput = request.getParameter("cdpinput");

        List<String> groups = new ArrayList<String>();
        List<ProjRole> projRoles = new ArrayList<ProjRole>();
        try
        {
            JSONArray jsonObj = new JSONArray(shares_data);
            for (int i = 0; i < jsonObj.length(); i++)
            {
                JSONObject obj = jsonObj.getJSONObject(i);
                String type = obj.getString("type");
                if (type.equals("G"))
                {
                    groups.add(obj.getString("group"));
                }
                else
                {
                    ProjRole pr = new ProjRole(obj.getString("proj"), obj.getString("role"));
                    projRoles.add(pr);
                }
            }
        }
        catch (JSONException e)
        {
            log.error("MailRuCalService::updateCalendar - Incorrect input parameters", e);
            return Response.status(500).build();
        }

        Long ctime;
        try
        {
            ctime = Long.valueOf(ctimestr);
        }
        catch (NumberFormatException nfex)
        {
            log.error("MailRuCalService::updateCalendar - Incorrect input parameters", nfex);
            return Response.status(500).build();
        }

        //--> checks
        if (!Utils.isStr(name) ||
            !Utils.isStr(color))
        {
            log.error("MailRuCalService::updateCalendar - Required parameters are not set");
            return Response.status(500).build();
        }

        if (display.equals(ProjectCalUserData.JCL_TYPE_STR))
        {
            JSONArray perms = new JSONArray();
            JiraServiceContext jsCtx = new JiraServiceContextImpl(user);
            SearchRequest sr = srMgr.getFilter(jsCtx, Long.parseLong(mainsel));
            SharePermissions oldSharePerms = sr.getPermissions();
            for (SharePermission sp : oldSharePerms.getPermissionSet())
            {
                JSONObject obj = new JSONObject();
                obj.put("type", sp.getType().get());
                obj.put("param1", sp.getParam1());
                obj.put("param2", sp.getParam2());
                perms.put(obj);
            }

            for (String group : groups)
            {
                JSONObject obj = new JSONObject();
                obj.put("type", "group");
                obj.put("param1", group);
                perms.put(obj);
            }

            for (ProjRole projRole : projRoles)
            {
                if (projRole.getRole().isEmpty())
                {
                    Collection<ProjectRole> roles = projectRoleManager.getProjectRoles();
                    if (roles != null)
                    {
                        for (ProjectRole role : roles)
                        {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "project");
                            obj.put("param1", projRole.getProject());
                            obj.put("param2", role.getId());
                            perms.put(obj);
                        }
                    }
                }
                else
                {
                    JSONObject obj = new JSONObject();
                    obj.put("type", "project");
                    obj.put("param1", projRole.getProject());
                    obj.put("param2", projRole.getRole());
                    perms.put(obj);
                }
            }

            SharePermissions sharePerms = SharePermissionUtils.fromJsonArray(perms);
            sr.setPermissions(sharePerms);
            srMgr.updateFilter(jsCtx, sr);
        }

        ProjectCalUserData pcud = mailCfg.getCalendarData(ctime);
        if (pcud != null && Utils.isCalendarVisiable(pcud, user, groupMgr, prMgr, projectRoleManager))
        {
            if (pcud.getCreator().equals(user.getName()))
            {
                //--> checks
                if (!Utils.isStr(newname) ||
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

                pcud.setName(newname);
                pcud.setColor(color);
                pcud.setDescr(descr);
                pcud.setTarget(mainsel);
                pcud.setType(display);
                pcud.setFieldType(showfld);
                pcud.setStartPoint(start);
                pcud.setEndPoint(end);
                pcud.setGroups(groups);
                pcud.setProjRoles(projRoles);
                mailCfg.storeProjectCalUserData(pcud);
            }
            else
            {
                UserCalPref userPref = mailCfg.getUserCalPref(user.getName());
                if (userPref == null)
                {
                    userPref = new UserCalPref();
                }
                userPref.storeUserColor(pcud.getCalId(), color);
                mailCfg.putUserCalPref(user.getName(), userPref);
            }
        }

        String baseUrl = Utils.getBaseUrl(request);
        return Response.seeOther(URI.create(baseUrl + "/plugins/servlet/mailrucal/view")).build();
    }
}
