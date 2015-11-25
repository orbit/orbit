package com.ea.orbit.actors.extensions;

public class NamedPipelineExtension implements PipelineExtension
{
    private String name;
    private String beforeHandlerName;
    private String afterHandlerName;

    public NamedPipelineExtension()
    {
    }

    public NamedPipelineExtension(final String name)
    {
        this.name = name;
    }

    public NamedPipelineExtension(final String name, final String beforeHandlerName, final String afterHandlerName)
    {
        this.name = name;
        this.beforeHandlerName = beforeHandlerName;
        this.afterHandlerName = afterHandlerName;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public String getBeforeHandlerName()
    {
        return beforeHandlerName;
    }

    public void setBeforeHandlerName(final String beforeHandlerName)
    {
        this.beforeHandlerName = beforeHandlerName;
    }

    public String getAfterHandlerName()
    {
        return afterHandlerName;
    }

    public void setAfterHandlerName(final String afterHandlerName)
    {
        this.afterHandlerName = afterHandlerName;
    }
}
