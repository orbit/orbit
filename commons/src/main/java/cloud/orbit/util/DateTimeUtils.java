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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class DateTimeUtils
{
    private DateTimeUtils()
    {
    }

    /**
     * Given the format value parse the date time from the string.
     *
     * @param value
     * @param format
     * @return Date
     * @throws ParseException
     */
    public static Date parseExact(final String value, final String format) throws ParseException
    {
        final DateFormat formatter = new SimpleDateFormat(format, Locale.getDefault());
        return formatter.parse(value);
    }

    /**
     * Takes a date and a date format string and returns a string
     * of the date formatted according to the passed in arguments
     *
     * @param date             - date to turn into a string
     * @param dateFormatString - string specifying how the date is to be
     *                         formatted into a string
     * @return String - the date argument in a string representation according
     * to the passed in dateFormatString
     */
    public static String getStringFromDate(final Date date, final String dateFormatString)
    {
        final SimpleDateFormat sdf = new SimpleDateFormat(dateFormatString, Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Takes a date in string format and a date format string and returns
     * a Date
     *
     * @param dateString       - the date in a string representation
     * @param dateFormatString - the format of how the date is represented in the
     *                         dateString argument
     * @return Date - returns a date object as specified by the dateString argument
     */
    public static Date getDateFromString(final String dateString, final String dateFormatString)
    {
        try
        {
            return parseExact(dateString, dateFormatString);
        }
        catch (final ParseException e)
        {
            throw new RuntimeException("Date could not be parsed in the specified format: "
                    + dateFormatString + " - " + e.getMessage());
        }
    }

    /**
     * Returns weather the date is within a given range.
     *
     * @return boolean
     */
    public static boolean isDateInRange(final Date dateToCheck, final Date startDate, final Date endDate)
    {
        return !((startDate != null && dateToCheck.before(startDate)) || (endDate != null && dateToCheck.after(endDate)));
    }

    /**
     * Builds and returns a date that is a specified amount of time away from now (earlier or later).
     */
    public static Date getDateRelativeToNow(TimeUnit timeUnit, long amount)
    {
        return new Date(System.currentTimeMillis() + timeUnit.toMillis(amount));
    }

}
