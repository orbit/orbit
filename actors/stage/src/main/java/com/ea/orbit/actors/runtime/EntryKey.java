package com.ea.orbit.actors.runtime;

class EntryKey
{
    private int interfaceId;
    private Object id;

    EntryKey(final int interfaceId, final Object id)
    {
        this.interfaceId = interfaceId;
        this.id = id;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (!(o instanceof EntryKey)) return false;

        final EntryKey entryKey = (EntryKey) o;

        if (interfaceId != entryKey.interfaceId) return false;
        if (id != null ? !id.equals(entryKey.id) : entryKey.id != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = interfaceId;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "EntryKey{" +
                "interfaceId=" + interfaceId +
                ", id=" + id +
                '}';
    }

    public int getInterfaceId()
    {
        return interfaceId;
    }

    public Object getId()
    {
        return id;
    }
}
