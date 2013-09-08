/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.andreymarkelov.atlas.plugins.simplecal;

import java.util.Map;
import java.util.TreeMap;

/**
 * 
 * 
 * @author Andrey Markelov
 */
public class ProjCreateIssueData
{
    /**
     * Issue types.
     */
    private Map<String, String> issueType;

    private long ctime;

    private String calName;

    /**
     * Porject key.
     */
    private String key;

    /**
     * Project key.
     */
    private String name;

    /**
     * Project ID.
     */
    private long projId;

    /**
     * Target name.
     */
    private String targetName;

    /**
     * Constructor.
     */
    public ProjCreateIssueData()
    {
        this.issueType = new TreeMap<String, String>();
    }

    public void addIssueType(String id, String name)
    {
        this.issueType.put(id, name);
    }

    public Map<String, String> getIssueType()
    {
        return issueType;
    }

    public String getKey()
    {
        return key;
    }

    public String getName()
    {
        return name;
    }

    public long getCtime() {
		return ctime;
	}

	public void setCtime(long ctime) {
		this.ctime = ctime;
	}

	public String getCalName() {
		return calName;
	}

	public void setCalName(String calName) {
		this.calName = calName;
	}

	public long getProjId()
    {
        return projId;
    }

    public String getTargetName()
    {
        return targetName;
    }

    public void setIssueType(Map<String, String> issueType)
    {
        this.issueType = issueType;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setProjId(long projId)
    {
        this.projId = projId;
    }

    public void setTargetName(String targetName)
    {
        this.targetName = targetName;
    }

    @Override
    public String toString()
    {
        return "ProjCreateIssueData[projId=" + projId + ", key=" + key
            + ", name=" + name + ", targetName=" + targetName
            + ", issueType=" + issueType + "]";
    }
}
