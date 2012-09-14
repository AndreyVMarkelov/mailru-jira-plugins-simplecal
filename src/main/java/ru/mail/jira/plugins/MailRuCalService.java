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
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
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
     * Project role manager.
     */
    private final ProjectRoleManager projectRoleManager;

    /**
     * Custom field manager.
     */
    private final CustomFieldManager cfMgr;

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
        xstream = new XStream(new JsonHierarchicalStreamDriver()
        {
            public HierarchicalStreamWriter createWriter(Writer writer)
            {
                return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE);
            }
        });
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

        List<ProjCreateIssueData> pcids = new ArrayList<ProjCreateIssueData>();
        UserCalData usrData = mailCfg.getUserData(user.getName());
        for (ProjectCalUserData pcud : usrData.getProjs())
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
            pcid.setCtime(pcud.getcTime());
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

        long ctime = Counter.getVal();

        Set<String> shUsers = Utils.getSharedUsers(groups, projRoles, groupMgr, prMgr, projectRoleManager, user.getName());
        for (String shUser : shUsers)
        {
            UserCalData usrData = mailCfg.getUserData(shUser);
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
                groups,
                projRoles,
                ctime));
            mailCfg.putUserData(shUser, usrData);
        }

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
                    en.setKey(issue.getKey());
                    en.setAssignee(issue.getAssigneeUser().getDisplayName());
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
                    en.setKey(issue.getKey());
                    en.setAssignee(issue.getAssigneeUser().getDisplayName());
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
                    en.setKey(issue.getKey());
                    en.setAssignee(issue.getAssigneeUser().getDisplayName());
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
                    en.setKey(issue.getKey());
                    en.setAssignee(issue.getAssigneeUser().getDisplayName());
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
                        en.setKey(issue.getKey());
                        en.setAssignee(issue.getAssigneeUser().getDisplayName());
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
                        en.setKey(issue.getKey());
                        en.setAssignee(issue.getAssigneeUser().getDisplayName());
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
        CustomField startCf,
        CustomField endCf)
    {
        if (pcud.isIDD())
        {
            return createIddEvent(pcud, issue, startDate, endDate, baseUrl);
        }
        else if (pcud.isDatePoint())
        {
            return createDatePointEntity(pcud, issue, startDate, endDate, baseUrl, startCf);
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
            en.setKey(issue.getKey());
            en.setAssignee(issue.getAssigneeUser().getDisplayName());
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
        ProjectCalUserData pcud = usrData.getProjectCalUserData(name, ctime);
        if (pcud != null)
        {
            Set<String> shUsers = Utils.getSharedUsers(pcud.getGroups(), pcud.getProjRoles(), groupMgr, prMgr, projectRoleManager, user.getName());
            for (String shUser : shUsers)
            {
                UserCalData shUsrData = mailCfg.getUserData(shUser);
                ProjectCalUserData shPcud = shUsrData.getProjectCalUserData(name, ctime);
                shUsrData.removeProjectCalUserData(shPcud);
                mailCfg.putUserData(shUser, shUsrData);
            }
        }

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
                    EventEntity entity = createEventEntity(pcud, issue, startDate, endDate, baseUrl, startCf, endCf);
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
                EventEntity entity = createEventEntity(pcud, issue, startDate, endDate, baseUrl, startCf, endCf);
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
                params.put("allGroups", groupMgr.getGroupsForUser(user.getName()));
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

        UserCalData usrData = mailCfg.getUserData(user.getName());
        ProjectCalUserData pcud = usrData.getProjectCalUserData(name, ctime);
        if (pcud != null)
        {
            if (pcud.getCreator() != null && pcud.getCreator().equals(user.getName()))
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

                Set<String> newShUsers = Utils.getSharedUsers(groups, projRoles, groupMgr, prMgr, projectRoleManager, user.getName());
                Set<String> shUsers = Utils.getSharedUsers(pcud.getGroups(), pcud.getProjRoles(), groupMgr, prMgr, projectRoleManager, user.getName());
                for (String shUser : shUsers)
                {
                    if (newShUsers.contains(shUser))
                    {
                        newShUsers.remove(shUser);
                        UserCalData shUsrData = mailCfg.getUserData(shUser);
                        ProjectCalUserData shPcud = shUsrData.getProjectCalUserData(name, ctime);
                        if (shPcud != null)
                        {
                            shPcud.setName(newname);
                            shPcud.setColor(color);
                            shPcud.setDescr(descr);
                            shPcud.setTarget(mainsel);
                            shPcud.setType(display);
                            shPcud.setFieldType(showfld);
                            shPcud.setStartPoint(start);
                            shPcud.setEndPoint(end);
                            shPcud.setGroups(groups);
                            shPcud.setProjRoles(projRoles);
                            mailCfg.putUserData(shUser, shUsrData);
                        }
                    }
                    else
                    {
                        UserCalData shUsrData = mailCfg.getUserData(shUser);
                        ProjectCalUserData shPcud = shUsrData.getProjectCalUserData(name, ctime);
                        shUsrData.removeProjectCalUserData(shPcud);
                        mailCfg.putUserData(shUser, shUsrData);
                    }
                }

                for (String newShUser : newShUsers)
                {
                    UserCalData shUsrData = mailCfg.getUserData(newShUser);
                    usrData.add(new ProjectCalUserData(
                        newname,
                        descr,
                        color,
                        display,
                        mainsel,
                        showfld,
                        start,
                        end,
                        true,
                        user.getName(),
                        groups,
                        projRoles,
                        ctime));
                    mailCfg.putUserData(newShUser, shUsrData);
                }
            }
            else
            {
                pcud.setDescr(descr);
                pcud.setColor(color);
                mailCfg.putUserData(user.getName(), usrData);
            }
        }

        String baseUrl = Utils.getBaseUrl(request);
        return Response.seeOther(URI.create(baseUrl + "/plugins/servlet/mailrucal/view")).build();
    }
}
