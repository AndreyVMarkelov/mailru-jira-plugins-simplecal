/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.List;
import java.util.Map;

/**
 * Display event structure.
 * 
 * @author Andrey Markelov
 */
public class EventEntity
{
    private List<String> affectVersions;

    private boolean allDay;

    private String assignee;

    private String color;

    private List<String> components;

    private String created;

    private Map<String, String> customFields;

    private String due;

    private String end;

    private String environment;

    private List<String> fixVersions;

    private String id;

    private String key;

    private List<String> labels;

    private String priority;

    private String reporter;

    private String resolution;

    private String start;

    private String status;

    private String title;

    private String updated;

    private String url;

    /**
     * Constructor.
     */
    public EventEntity() {}

    public List<String> getAffectVersions()
    {
        return affectVersions;
    }

    public String getAssignee()
    {
        return assignee;
    }

    public String getColor()
    {
        return color;
    }

    public List<String> getComponents()
    {
        return components;
    }

    public String getCreated()
    {
        return created;
    }

    public Map<String, String> getCustomFields()
    {
        return customFields;
    }

    public String getDue()
    {
        return due;
    }

    public String getEnd()
    {
        return end;
    }

    public String getEnvironment()
    {
        return environment;
    }

    public List<String> getFixVersions()
    {
        return fixVersions;
    }

    public String getId()
    {
        return id;
    }

    public String getKey()
    {
        return key;
    }

    public List<String> getLabels()
    {
        return labels;
    }

    public String getPriority()
    {
        return priority;
    }

    public String getReporter()
    {
        return reporter;
    }

    public String getResolution()
    {
        return resolution;
    }

    public String getStart()
    {
        return start;
    }

    public String getStatus()
    {
        return status;
    }

    public String getTitle()
    {
        return title;
    }

    public String getUpdated()
    {
        return updated;
    }

    public String getUrl()
    {
        return url;
    }

    public boolean isAllDay()
    {
        return allDay;
    }

    public void setAffectVersions(List<String> affectVersions)
    {
        this.affectVersions = affectVersions;
    }

    public void setAllDay(boolean allDay)
    {
        this.allDay = allDay;
    }

    public void setAssignee(String assignee)
    {
        this.assignee = assignee;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public void setComponents(List<String> components)
    {
        this.components = components;
    }

    public void setCreated(String created)
    {
        this.created = created;
    }

    public void setCustomFields(Map<String, String> customFields)
    {
        this.customFields = customFields;
    }

    public void setDue(String due)
    {
        this.due = due;
    }

    public void setEnd(String end)
    {
        this.end = end;
    }

    public void setEnvironment(String environment)
    {
        this.environment = environment;
    }

	public void setFixVersions(List<String> fixVersions)
    {
        this.fixVersions = fixVersions;
    }

	public void setId(String id)
    {
        this.id = id;
    }

	public void setKey(String key)
    {
        this.key = key;
    }

    public void setLabels(List<String> labels)
    {
        this.labels = labels;
    }

    public void setPriority(String priority)
    {
        this.priority = priority;
    }

    public void setReporter(String reporter)
    {
        this.reporter = reporter;
    }

    public void setResolution(String resolution)
    {
        this.resolution = resolution;
    }

    public void setStart(String start)
    {
        this.start = start;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setUpdated(String updated)
    {
        this.updated = updated;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    @Override
    public String toString()
    {
        return "EventEntity [affectVersions=" + affectVersions + ", allDay="
            + allDay + ", assignee=" + assignee + ", color=" + color
            + ", components=" + components + ", created=" + created
            + ", customFields=" + customFields + ", due=" + due + ", end="
            + end + ", environment=" + environment + ", fixVersions="
            + fixVersions + ", id=" + id + ", key=" + key + ", labels="
            + labels + ", priority=" + priority + ", reporter=" + reporter
            + ", resolution=" + resolution + ", start=" + start
            + ", status=" + status + ", title=" + title + ", updated="
            + updated + ", url=" + url + "]";
    }
}
