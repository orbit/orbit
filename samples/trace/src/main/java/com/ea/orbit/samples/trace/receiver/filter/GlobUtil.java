package com.ea.orbit.samples.trace.receiver.filter;

//source: http://stackoverflow.com/questions/1247772/is-there-an-equivalent-of-java-util-regex-for-glob-type-patterns
//license: http://creativecommons.org/licenses/by-sa/3.0/
//author: http://stackoverflow.com/users/3333/paul-tomblin

public class GlobUtil
{

    public static String convertGlobToRegEx(String line)
    {
        line = line.trim();
        int strLen = line.length();
        StringBuilder sb = new StringBuilder(strLen);
        // Remove beginning and ending * globs because they're useless
        if (line.startsWith("*"))
        {
            line = line.substring(1);
            strLen--;
        }
        if (line.endsWith("*"))
        {
            line = line.substring(0, strLen - 1);
            strLen--;
        }
        boolean escaping = false;
        int inCurlies = 0;
        for (char currentChar : line.toCharArray())
        {
            switch (currentChar)
            {
                case '*':
                    if (escaping)
                    {
                        sb.append("\\*");
                    }
                    else
                    {
                        sb.append(".*");
                    }
                    escaping = false;
                    break;
                case '?':
                    if (escaping)
                    {
                        sb.append("\\?");
                    }
                    else
                    {
                        sb.append('.');
                    }
                    escaping = false;
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    sb.append('\\');
                    sb.append(currentChar);
                    escaping = false;
                    break;
                case '\\':
                    if (escaping)
                    {
                        sb.append("\\\\");
                        escaping = false;
                    }
                    else
                    {
                        escaping = true;
                    }
                    break;
                case '{':
                    if (escaping)
                    {
                        sb.append("\\{");
                    }
                    else
                    {
                        sb.append('(');
                        inCurlies++;
                    }
                    escaping = false;
                    break;
                case '}':
                    if (inCurlies > 0 && !escaping)
                    {
                        sb.append(')');
                        inCurlies--;
                    }
                    else if (escaping)
                    {
                        sb.append("\\}");
                    }
                    else
                    {
                        sb.append("}");
                    }
                    escaping = false;
                    break;
                case ',':
                    if (inCurlies > 0 && !escaping)
                    {
                        sb.append('|');
                    }
                    else if (escaping)
                    {
                        sb.append("\\,");
                    }
                    else
                    {
                        sb.append(",");
                    }
                    break;
                default:
                    escaping = false;
                    sb.append(currentChar);
            }
        }
        return sb.toString();
    }

}
