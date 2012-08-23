/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

/**
 * Data pair.
 * 
 * @author Andrey Markelov
 */
public class DataPair
    implements Comparable<DataPair>
{
    /**
     * ID.
     */
    private Long id;

    /**
     * Name.
     */
    private String name;

    /**
     * Constructor.
     */
    public DataPair(Long id, String name)
    {
        this.id = id;
        this.name = name;
    }

    @Override
    public int compareTo(DataPair o)
    {
        return name.compareTo(o.getName());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        DataPair other = (DataPair) obj;
        if (id == null)
        {
            if (other.id != null)
            {
                return false;
            }
        }
        else if (!id.equals(other.id))
        {
            return false;
        }

        if (name == null)
        {
            if (other.name != null)
            {
                return false;
            }
        }
        else if (!name.equals(other.name))
        {
            return false;
        }

        return true;
    }

    public Long getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());

        return result;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return "DataPair[id=" + id + ", name=" + name + "]";
    }
}
