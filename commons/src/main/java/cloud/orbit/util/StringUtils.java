/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package cloud.orbit.util;

public class StringUtils
{
    private StringUtils()
    {
        // Placating HideUtilityClassConstructorCheck
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public static boolean equals(final CharSequence s1, final CharSequence s2)
    {
        return (s1 == s2) || (s1 != null && s2 != null && s1.equals(s2));
    }

    public static boolean isNotBlank(final CharSequence s)
    {
        if (s != null)
        {
            int len = s.length();
            for (int i = 0; i < len; i++)
            {
                if (!Character.isWhitespace(s.charAt(i)))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isBlank(final CharSequence s)
    {
        if (s != null)
        {
            int len = s.length();
            for (int i = 0; i < len; i++)
            {
                if (!Character.isWhitespace(s.charAt(i)))
                {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isEmpty(final CharSequence s)
    {
        return s == null || s.length() == 0;
    }

    public static boolean isNotEmpty(final CharSequence s)
    {
        return s != null && s.length() != 0;
    }

    public static CharSequence uncapitalize(CharSequence s)
    {
        if (isNotBlank(s) && Character.isUpperCase(s.charAt(0)))
        {
            final StringBuilder sb = new StringBuilder(s);
            sb.setCharAt(0, Character.toLowerCase(s.charAt(0)));
            return sb.toString();
        }
        return s;
    }

    public static CharSequence capitalize(CharSequence s)
    {
        if (isNotBlank(s) && !Character.isUpperCase(s.charAt(0)))
        {
            final StringBuilder sb = new StringBuilder(s);
            sb.setCharAt(0, Character.toUpperCase(s.charAt(0)));
            return sb.toString();
        }

        return s;
    }

    public static boolean equalsIgnoreCase(String str1, String str2)
    {
        if (str1 == null)
        {
            return (str2 == null);
        }
        return str1.equalsIgnoreCase(str2);
    }

    public static String defaultIfBlank(final String str, final String def)
    {
        return isBlank(str) ? def : str;
    }
}
