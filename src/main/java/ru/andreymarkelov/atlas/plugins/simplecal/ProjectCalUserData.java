/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.andreymarkelov.atlas.plugins.simplecal;

import java.util.List;

/**
 * This structure keeps information about stored calendar.
 * 
 * @author Andrey Markelov
 */
public class ProjectCalUserData
{
    private static final int IDD = 0;
    private static final int DATE_POINT = 1;
    private static final int DATE_RANGE = 2;

    public static final String IDD_STR = "idd";
    public static final String DATE_POINT_STR = "cdp";
    public static final String DATE_RANGE_STR = "cdr";

    private static final int PROJECT_TYPE = 0;
    private static final int JCL_TYPE = 1;

    public static final String PROJECT_TYPE_STR = "project";
    public static final String JCL_TYPE_STR = "jcl";

    /**
     * Calendar ID.
     */
    private long calId;

    /**
     * Calendar color.
     */
    private String color;

    /**
     * Calendar creator.
     */
    private String creator;

    /**
     * Creation time.
     */
    private long cTime;

    /**
     * Calendar description.
     */
    private String descr;

    /**
     * End custom field.
     */
    private String endPoint;

    /**
     * Field type.
     */
    private int fieldType;

    /**
     * Groups.
     */
    private List<String> groups;

    /**
     * Calendar name.
     */
    private String name;

    /**
     * Project roles.
     */
    private List<ProjRole> projRoles;

    /**
     * Start custom field.
     */
    private String startPoint;

    /**
     * Target: project or JCL.
     */
    private String target;

    /**
     * Type.
     */
    private int type;

    /**
     * Constructor.
     */
    public ProjectCalUserData(
        long calId,
        String name,
        String descr,
        String color,
        String type,
        String target,
        String fieldType,
        String startPoint,
        String endPoint,
        String creator,
        List<String> groups,
        List<ProjRole> projRoles,
        long cTime)
    {
        this.calId = calId;
        this.name = name;
        this.descr = descr;
        this.color = color;
        setType(type);
        this.target = target;
        setFieldType(fieldType);
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.creator = creator;
        this.groups = groups;
        this.projRoles = projRoles;
        this.cTime = cTime;
    }

    public long getCalId()
    {
        return calId;
    }

    public String getColor()
    {
        return color;
    }

    public String getCreator()
    {
        return creator;
    }

    public long getCTime()
    {
        return cTime;
    }

    public String getDescr()
    {
        return descr;
    }

    public String getEndPoint()
    {
        return endPoint;
    }

    public List<String> getGroups()
    {
        return groups;
    }

    public String getName()
    {
        return name;
    }

    public List<ProjRole> getProjRoles()
    {
        return projRoles;
    }

    public String getStartPoint()
    {
        return startPoint;
    }

    public String getTarget()
    {
        return target;
    }

    public boolean isDatePoint()
    {
    	return fieldType == DATE_POINT;
    }

    public boolean isDateRange()
    {
    	return fieldType == DATE_RANGE;
    }

    public boolean isIDD()
    {
    	return fieldType == IDD;
    }

    public boolean isJclType()
    {
        return type == JCL_TYPE;
    }

    public boolean isProjectType()
    {
        return type == PROJECT_TYPE;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public void setDescr(String descr)
    {
        this.descr = descr;
    }

    public void setEndPoint(String endPoint)
    {
        this.endPoint = endPoint;
    }

    public void setFieldType(String fieldType)
    {
        if (fieldType.equals(IDD_STR))
        {
            this.fieldType = IDD;
        }
        else if (fieldType.equals(DATE_POINT_STR))
        {
            this.fieldType = DATE_POINT;
        }
        else
        {
            this.fieldType = DATE_RANGE;
        }
    }

    public void setGroups(List<String> groups)
    {
        this.groups = groups;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setProjRoles(List<ProjRole> projRoles)
    {
        this.projRoles = projRoles;
    }

    public void setStartPoint(String startPoint)
    {
        this.startPoint = startPoint;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    public void setType(String type)
    {
        this.type = type.equals(PROJECT_TYPE_STR) ? PROJECT_TYPE : JCL_TYPE;
    }

    @Override
    public String toString()
    {
        return "ProjectCalUserData[color=" + color + ", cTime=" + cTime
            + ", descr=" + descr + ", fieldType=" + fieldType + ", name="
            + name + ", target=" + target + ", startPoint=" + startPoint
            + ", endPoint=" + endPoint + ", type=" + type + ", calId=" +
            calId + ", creator=" + creator + ", projRoles=" + projRoles  + "]";
    }
}
