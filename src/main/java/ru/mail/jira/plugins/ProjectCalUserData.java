/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

/**
 * This structure keeps information about stored calendar.
 * 
 * @author Andrey Markelov
 */
public class ProjectCalUserData
{
    private static final int PROJECT_TYPE = 0;
    private static final int JCL_TYPE = 1;

    private static final int IDD = 0;
    private static final int DATE_POINT = 1;
    private static final int DATE_RANGE = 2;

    public static final String PROJECT_TYPE_STR = "project";
    public static final String JCL_TYPE_STR = "jcl";

    public static final String IDD_STR = "idd";
    public static final String DATE_POINT_STR = "cdp";
    public static final String DATE_RANGE_STR = "cdr";

    /**
     * Calendar color.
     */
    private String color;

    /**
     * Creation time.
     */
    private long cTime;

    /**
     * Calendar description.
     */
    private String descr;

    /**
     * Field type.
     */
    private int fieldType;

    /**
     * Calendar name.
     */
    private String name;

    /**
     * Target: project or JCL.
     */
    private String target;

    /**
     * Start custom field.
     */
    private String startPoint;

    /**
     * End custom field.
     */
    private String endPoint;

    /**
     * Type.
     */
    private int type;

    /**
     * Is calendar active?
     */
    private boolean isActive;

    /**
     * Constructor.
     */
    public ProjectCalUserData(
        String name,
        String descr,
        String color,
        String type,
        String target,
        String fieldType,
        String startPoint,
        String endPoint,
        boolean isActive)
    {
        this.name = name;
        this.descr = descr;
        this.color = color;
        this.type = type.equals(PROJECT_TYPE_STR) ? PROJECT_TYPE : JCL_TYPE;
        this.target = target;
        initFieldType(fieldType);
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.isActive = isActive;
        this.cTime = System.currentTimeMillis();
    }

    public String getColor()
    {
        return color;
    }

    public long getcTime()
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

    public String getName()
    {
        return name;
    }

    public String getStartPoint()
    {
        return startPoint;
    }

    public String getTarget()
    {
        return target;
    }

    private void initFieldType(String fieldType)
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

    public boolean isActive()
    {
        return isActive;
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

    public void setActive(boolean isActive)
    {
        this.isActive = isActive;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public void setDescr(String descr)
    {
        this.descr = descr;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return "ProjectCalUserData[color=" + color + ", cTime=" + cTime
            + ", descr=" + descr + ", fieldType=" + fieldType + ", name="
            + name + ", target=" + target + ", startPoint=" + startPoint
            + ", endPoint=" + endPoint + ", type=" + type + ", isActive=" + isActive + "]";
    }
}
