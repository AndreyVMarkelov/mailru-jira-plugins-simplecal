/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

/**
 * Display event structure.
 * 
 * @author Andrey Markelov
 */
public class EventEntity
{
    private boolean allDay;

    private String assignee;

    private String color;

    private String end;

    private String id;

    private String key;

    private String start;

    private String status;

    private String title;

    private String url;

    /**
     * Constructor.
     */
    public EventEntity() {}

    public String getAssignee()
    {
        return assignee;
    }

    public String getColor()
    {
        return color;
    }

    public String getEnd()
    {
        return end;
    }

    public String getId()
    {
        return id;
    }

    public String getKey()
    {
        return key;
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

	public String getUrl()
    {
        return url;
    }

	public boolean isAllDay()
    {
        return allDay;
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

    public void setEnd(String end)
    {
        this.end = end;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setKey(String key)
    {
        this.key = key;
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

    public void setUrl(String url)
    {
        this.url = url;
    }

    @Override
    public String toString()
    {
        return "EventEntity[allDay=" + allDay + ", assignee=" + assignee
            + ", color=" + color + ", end=" + end + ", id=" + id + ", key="
            + key + ", start=" + start + ", title=" + title + ", url="
            + url + ", status=" + status + "]";
    }
}
