package cn.teleinfo.idpointer.sdk.core.stream.util;

import java.text.ParseException;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 
 * A thread-safe and high-performance class for formatting and parsing dates.
 * FastDateFormat uses a proleptic Gregorian calendar with a zero year.
 * Most ISO8601 date-times are parseable; fractional hours and minutes are not supported.
 * A sensible subset of ISO8601 date-times can be produced by the format engine.
 * 
 * Thread-safety is guaranteed; UTC-only methods are free-threaded; when a TimeZone is involved its calculation of DST offset is synchronized in formatting, and in parsing date representations without explicit time zone.
 * 
 * Format methods referring to "Now" will automatically format System.currentTimeMillis() and will cache much of the calculation to increase performance.
 */
public class FastDateFormat {
    private final TimeZone zone;
    private final FormatSpec defaultFormatSpec;
    private volatile Status status = new Status();
    
    private static class Date {
        int year, month=1, day=1, hour, minute, second, millisecond;
        boolean hasZone=true, negativeZone;
        int zoneHour, zoneMinute;
        @Override
        public String toString() {
            return "Date [year=" + year + ", month=" + month + ", day=" + day + ", hour=" + hour + ", minute=" + minute + ", second=" + second
                    + ", millisecond=" + millisecond + ", hasZone=" + hasZone + ", negativeZone=" + negativeZone + ", zoneHour=" + zoneHour + ", zoneMinute="
                    + zoneMinute + "]";
        }
    }
    
    /** Display components of date as of a specific minute */
    private static class Status {
        String yearString, monthString, dayString;
        String hourString, minuteString;
        String zoneHour;
        String zoneMinute;
        boolean isZ;
        long lastUpdate = -1;
    }
    
    private static String pad2(int s) {
        if(s>=10) return String.valueOf(s);
        else return "0" + s;
    }

    private static String pad3(int s) {
        if(s>=100) return String.valueOf(s);
        else if(s>=10) return "0" + s;
        else return "00" + s;
    }

    private static String pad4(int s) {
        if(s>=1000) return String.valueOf(s);
        else if(s>=100) return "0" + s;
        else if(s>=10) return "00" + s;
        else if(s>=0) return "000" + s;
        else if(s>-10) return "-000" + (-s);
        else if(s>-100) return "-00" + (-s);
        else if(s>-1000) return "-0" + (-s);
        else return "-" + (-s);
    }

    private static Status getStatus(Date date) {
        Status newStatus = new Status();
        newStatus.yearString = pad4(date.year);
        newStatus.monthString = pad2(date.month);
        newStatus.dayString = pad2(date.day);
        newStatus.hourString = pad2(date.hour);
        newStatus.minuteString = pad2(date.minute);
        newStatus.isZ = date.zoneHour==0 && date.zoneMinute==0;
        newStatus.zoneHour = (date.negativeZone ? "-" : "+") + pad2(date.zoneHour);
        newStatus.zoneMinute = pad2(date.zoneMinute);
        return newStatus;
    }
    
    private Status getStatus(Date date, long time, boolean isNow) {
        Status currStatus;
        if(isNow) {
            currStatus = status;
            int timeSeconds = (int)(time % 60000);
            if(timeSeconds < 0) timeSeconds = 60000 - timeSeconds;
            long timeMinute = time - timeSeconds;
            if(currStatus.lastUpdate!=timeMinute) {
                currStatus = getStatus(date);
                currStatus.lastUpdate = timeMinute;
                if(status.lastUpdate!=timeMinute) status = currStatus;
            }
        } else {
            currStatus = getStatus(date);
        }
        return currStatus;
    }

    /**
     * Constructs a FastDateFormat using the specified time zone, or UTC if given null.
     * Default formatting is as {@link FastDateFormat.FormatSpec#ISO8601_MS}.
     * For UTC and the default time zone, prefer {@link #getUtcFormat()} and {@link #getLocalFormat()}.
     * 
     * @param zone a TimeZone object, or null to use UTC.
     */
    public FastDateFormat(TimeZone zone) {
        this(FormatSpec.ISO8601_MS,zone);
    }

    /**
     * Constructs a FastDateFormat using the specified default format spec and the specified time zone, or UTC if given null.
     * For UTC and the default time zone, prefer {@link #getUtcFormat()} and {@link #getLocalFormat()}.
     * 
     * @param formatSpec a {@link FastDateFormat.FormatSpec} specifying the default formatting
     * @param zone a TimeZone object, or null to use UTC.
     */
    public FastDateFormat(FormatSpec formatSpec,TimeZone zone) {
        this.defaultFormatSpec = formatSpec;
        this.zone = zone;
    }

    
    /** Specification of how to format a date. */
    public static class FormatSpec {
        String dateSep,dateTimeSep,timeSep,zoneSep,decimalPoint;
        boolean useMs,useZ;
        /**
         * Constructs a format specification.  
         * 
         * @param dateSep separator between date and time, typically "-" or "".
         * @param dateTimeSep separator between date and time, typically "T" or " ".
         * @param timeSep separator between date and time, typically ":" or "".
         * @param zoneSep separator between hours and minutes of zone offset, typically ":" or "".
         * @param decimalPoint separator between seconds and milliseconds, typically ".", ",", or "".
         * @param useMs whether to include milliseconds.
         * @param useZ whether to specify the time zone +00:00 as "Z".
         */ 
        public FormatSpec(String dateSep, String dateTimeSep, String timeSep, String zoneSep, String decimalPoint, boolean useMs, boolean useZ) {
            this.dateSep = dateSep;
            this.dateTimeSep = dateTimeSep;
            this.timeSep = timeSep;
            this.zoneSep = zoneSep;
            this.decimalPoint = decimalPoint;
            this.useMs = useMs;
            this.useZ = useZ;
        }
        
        /** {@code new FormatSpec("-","T",":",":",".",true,true);}, e.g. 2012-11-02T23:39:05.346-05:00 */
        public static final FormatSpec ISO8601_MS = new FormatSpec("-","T",":",":",".",true,true);
        /** {@code new FormatSpec("-","T",":",":",".",false,true);}, e.g. 2012-11-02T23:39:05-05:00 */
        public static final FormatSpec ISO8601_NO_MS = new FormatSpec("-","T",":",":",".",false,true);
    }
    
    /**
     * Formats the current time using the default format spec, caching format calculations for increased performance over many calls. 
     */
    public String formatNow() {
        return formatNow(defaultFormatSpec);
    }

    /**
     * Formats a given time using the default format spec.  
     * 
     * @param time the time to format.
     * @return
     */
    public String format(long time) {
        return format(defaultFormatSpec,time);
    }

    /**
     * Formats a given time using the default format spec.  
     * 
     * @param time the time to format.
     * @param isNow whether to cache formatting information over multiple calls.  Should be set true when consecutive calls are close in time.
     * @return
     */
    public String format(long time, boolean isNow) {
        return format(defaultFormatSpec,time,isNow);
    }
    
    /**
     * Formats the current time in a profile of ISO8601, caching format calculations for increased performance over many calls. 
     * 
     * @param formatSpec specification of how to format.
     * @return
     */
    public String formatNow(FormatSpec formatSpec) {
        long time = System.currentTimeMillis();
        Date date = zoneDate(time,zone);
        Status currStatus = getStatus(date,time,true);
        return formatDateAndStatus(date,currStatus,formatSpec);
    }

    /**
     * Formats a given time in a profile of ISO8601.  
     * 
     * @param formatSpec specification of how to format.
     * @param time the time to format.
     * @return
     */
    public String format(FormatSpec formatSpec, long time) {
        Date date = zoneDate(time,zone);
        Status currStatus = getStatus(date,time,false);
        return formatDateAndStatus(date,currStatus,formatSpec);
    }

    /**
     * Formats a given time in a profile of ISO8601.  
     * 
     * @param formatSpec specification of how to format.
     * @param time the time to format.
     * @param isNow whether to cache formatting information over multiple calls.  Should be set true when consecutive calls are close in time.
     * @return
     */
    public String format(FormatSpec formatSpec, long time, boolean isNow) {
        Date date = zoneDate(time,zone);
        Status currStatus = getStatus(date,time,isNow);
        return formatDateAndStatus(date,currStatus,formatSpec);
    }
    
    /**
     * Formats a given time in the default format spec (2012-11-02T23:39:05.346Z), using UTC. 
     * 
     * @param time the time to format.
     * @return
     */    
    public static String formatUtc(long time) {
        return formatUtc(FormatSpec.ISO8601_MS,time);
    }
    
    /**
     * Formats a given time in the default format spec (2012-11-02T23:39:05.346-05:00), using a specified time zone.
     * 
     * @param time the time to format.
     * @param zone the time zone for the formatting.
     * @return
     */  
    public static String format(long time, TimeZone zone) {
        return format(FormatSpec.ISO8601_MS,time,zone);
    }

    /**
     * Formats a given time in a profile of ISO8601, using UTC. 
     * 
     * @param formatSpec specification of how to format.
     * @param time the time to format.
     * @return
     */    
    public static String formatUtc(FormatSpec formatSpec, long time) {
        return format(formatSpec,time,null);
    }
    
    /**
     * Formats a given time in a profile of ISO8601, using a specified time zone.
     * 
     * @param formatSpec specification of how to format.
     * @param time the time to format.
     * @param zone the time zone for the formatting.
     * @return
     */  
    public static String format(FormatSpec formatSpec, long time, TimeZone zone) {
        Date date = zoneDate(time,zone);
        Status status = getStatus(date);
        return formatDateAndStatus(date,status,formatSpec);
    }

    private static String formatDateAndStatus(Date date, Status status, FormatSpec formatSpec) {
        StringBuilder sb = new StringBuilder();
        sb.append(status.yearString).append(formatSpec.dateSep).append(status.monthString).append(formatSpec.dateSep).append(status.dayString);
        sb.append(formatSpec.dateTimeSep);
        sb.append(status.hourString).append(formatSpec.timeSep).append(status.minuteString).append(formatSpec.timeSep).append(pad2(date.second));
        if(formatSpec.useMs) sb.append(formatSpec.decimalPoint).append(pad3(date.millisecond));
        if(formatSpec.useZ && status.isZ) sb.append("Z");
        else sb.append(status.zoneHour).append(formatSpec.zoneSep).append(status.zoneMinute);
        return sb.toString();
    }
    
    private static class ZFormatHolder {
        static FastDateFormat format = new FastDateFormat(null);
    }

    private static class LocalFormatHolder {
        static FastDateFormat format = new FastDateFormat(TimeZone.getDefault());
    }
    
    /**
     * Returns a FastDateFormat which uses UTC.
     */
    public static FastDateFormat getUtcFormat() {
        return ZFormatHolder.format;
    }

    /**
     * Returns a FastDateFormat which uses the local time zone, TimeZone.getDefault().
     */
    public static FastDateFormat getLocalFormat() {
        return LocalFormatHolder.format;
    }

    private static final long MILLISECONDS_IN_FOUR_CENTURIES = 12622780800000L;
    static final int DAYS_FROM_YEAR_ZERO_TO_1970 = 719528;
    private static final long DAYS_IN_FOUR_CENTURIES = 146097L;  // needs to be long to prevent overflow
    private static final int DAYS_IN_CENTURY = 36524;
    private static final int DAYS_IN_FOUR_YEARS = 1461;
    
    private static boolean isLeapYear(int year) {
        return year % 400 == 0 || (year%4 == 0 && year%100!=0);
    }
    
    private static Date zoneDate(long time, TimeZone zone) {
        if(zone==null) return utcDate(time,0);
        int offsetMillisAndUp;
        synchronized(zone) { offsetMillisAndUp = zone.getOffset(time); }
        boolean negative = offsetMillisAndUp < 0;
        if(negative) offsetMillisAndUp *= -1;
        int zoneMillis = offsetMillisAndUp % 60000;
        int offsetMinutesAndUp = offsetMillisAndUp / 60000;
        if(zoneMillis >= 30000) offsetMinutesAndUp++;
        long offset = (negative ? -60000L : 60000L) * offsetMinutesAndUp;
        Date date = utcDate(time, offset);
        date.negativeZone = negative;
        date.zoneMinute = offsetMinutesAndUp % 60;
        date.zoneHour = (offsetMinutesAndUp / 60);
        return date;
    }
    
    private static Date utcDate(long aTime, long offset) {
        long time = aTime;
        Date date = new Date();
        int adjustedQuadCents = 0;
        if(time < 0 || (offset < 0 && time + offset < 0)) {
            // add 4-century blocks to ensure time is positive
            // add an extra to cover any negative offset
            adjustedQuadCents = 1 - (int)(time / MILLISECONDS_IN_FOUR_CENTURIES);
            time += MILLISECONDS_IN_FOUR_CENTURIES * adjustedQuadCents;
        } else if(offset > 0 && Long.MAX_VALUE - offset < time){
            // subtract 4-century blocks to prevent overflow
            adjustedQuadCents = -1;
            time -= MILLISECONDS_IN_FOUR_CENTURIES;
        }
        time += offset;
        date.millisecond = (int)(time%1000);
        time /= 1000;
        date.second = (int)(time%60);
        time /= 60;
        date.minute = (int)(time%60);
        time /= 60;
        date.hour = (int)(time%24);
        time /= 24;
        time += DAYS_FROM_YEAR_ZERO_TO_1970;
        date.year = (int)(time / DAYS_IN_FOUR_CENTURIES) * 400;
        int dayInQuadCent = (int)(time % DAYS_IN_FOUR_CENTURIES);
        int dayInYear;
        if(dayInQuadCent<366) {
            dayInYear = dayInQuadCent;
        } else {
            // skip first year, which is the centurial leap year
            dayInQuadCent -= 366;
            date.year++;
            // now count forward centuries
            date.year += (dayInQuadCent / DAYS_IN_CENTURY) * 100;
            int dayInCent = dayInQuadCent % DAYS_IN_CENTURY;
            date.year += (dayInCent / DAYS_IN_FOUR_YEARS) * 4;
            dayInYear = dayInCent % DAYS_IN_FOUR_YEARS;
            int yearsInFour = dayInYear / 365;
            if(yearsInFour==4) yearsInFour = 3; // handle final leap year
            date.year += yearsInFour;
            dayInYear -= 365 * yearsInFour;
        }
        setDayAndMonth(date,dayInYear);
        
        if(adjustedQuadCents!=0) date.year -= 400 * adjustedQuadCents;
        return date;
    }

    private static void setDayAndMonth(Date date, int dayInYear) {
        date.day = dayInYear + 1;
        date.month = 1;
        if(date.day > 31) { date.day -= 31; date.month++; } else return;
        int febDays = isLeapYear(date.year) ? 29 : 28;
        if(date.day > febDays) { date.day -= febDays; date.month++; } else return;  
        if(date.day > 31) { date.day -= 31; date.month++; } else return;
        if(date.day > 30) { date.day -= 30; date.month++; } else return;
        if(date.day > 31) { date.day -= 31; date.month++; } else return;
        if(date.day > 30) { date.day -= 30; date.month++; } else return;
        if(date.day > 31) { date.day -= 31; date.month++; } else return;
        if(date.day > 31) { date.day -= 31; date.month++; } else return;
        if(date.day > 30) { date.day -= 30; date.month++; } else return;
        if(date.day > 31) { date.day -= 31; date.month++; } else return;
        if(date.day > 30) { date.day -= 30; date.month++; }
    }
    
    private static final String dashDateRegex = "([-+])?+([0-9]{4,}+)-([01][0-9])(?:-?+([0123][0-9])";
    private static final String noDashDateRegex = "([-+])?+([0-9]{4,})([01][0-9])([0123][0-9])";
    private static final String msPointRegex = "[.,]([0-9]{1,4}+)[0-9]*+";
    private static final String msMaybePointRegex = "[.,]?+([0-9]{1,4}+)[0-9]*+";
    private static final String timeRegex = "([012][0-9])(?::?+([0-5][0-9])(?::?+([0-6][0-9])" + "(?:" + msMaybePointRegex + ")?+)?+)?+";
    private static final String zoneRegex = "(?:(Z)|(?:([-+])([012][0-9])(?::?+([0-5][0-9]))?+))?+";
    private static final String dashesRegex = "\\s*+" + dashDateRegex + "(?:\\s*+T?+\\s*+" + timeRegex + ")?+)?+\\s*+" + zoneRegex + "\\s*+";
    private static final Pattern dashesPattern = Pattern.compile(dashesRegex);
    private static final String noDashesAndTimeRegex = "\\s*+" + noDashDateRegex + "(?:\\s*+T?+\\s*+" + timeRegex + ")?+\\s*+" + zoneRegex + "\\s*+";
    private static final Pattern noDashesAndTimePattern = Pattern.compile(noDashesAndTimeRegex);
    private static final String allDigitsWithPointRegex = "\\s*+" + noDashDateRegex + "([012][0-9])([0-5][0-9])([0-6][0-9])" + msPointRegex + "\\s*+" + zoneRegex + "\\s*+";
    private static final Pattern allDigitsWithPointPattern = Pattern.compile(allDigitsWithPointRegex);
    private static final String allDigitsRegex = "\\s*+" + "([-+])?+([0-9]*+)" + "\\s*+" + zoneRegex + "\\s*+";
    private static final Pattern allDigitsPattern = Pattern.compile(allDigitsRegex);
    
    private static int millisOfUpToTenthMillis(String tenthMillis) {
        if(tenthMillis.length()==1) return Integer.parseInt(tenthMillis)*100;
        if(tenthMillis.length()==2) return Integer.parseInt(tenthMillis)*10;
        if(tenthMillis.length()==3) return Integer.parseInt(tenthMillis);
        if(tenthMillis.length()>=4) {
            int res = Integer.parseInt(tenthMillis.substring(0,3));
            int round = Integer.parseInt(tenthMillis.substring(3,4));
            if(round>=5) res++;
            return res;
        }
        return 0;
    }
    
    private static Date parseDate(String dateString) throws ParseException {
        boolean started = false;
        boolean dashes = false;
        boolean sawTime = false;
        boolean sawPoint = false;
        boolean sawGap = false;
        for(int i = 0; i < dateString.length(); i++) {
            char ch = dateString.charAt(i);
            if(!started) {
                if(ch!=' ') started = true;
            } else {
                if(ch=='+' || ch=='Z') break;
                else if(ch=='T') {
                    sawTime = true;
                    break;
                } else if(ch==' ') sawGap = true;
                else if(sawGap && ch>='0' && ch<='9') {
                    sawTime = true;
                    break;
                } else if(ch=='.' || ch==',') {
                    sawPoint = true;
                    break;
                } else if(ch=='-') {
                    if(sawGap) break;
                    if(i+3<dateString.length()) {
                        char chAtPlusThree = dateString.charAt(i+3);
                        if(chAtPlusThree==':' || (chAtPlusThree>='0' && chAtPlusThree<='9')) break;
                    }
                    dashes = true;
                    break;
                }
            }
        }
        if(dashes) return parseDateWithPattern(dateString,dashesPattern);
        else if(sawTime) return parseDateWithPattern(dateString,noDashesAndTimePattern);
        else if(sawPoint) return parseDateWithPattern(dateString,allDigitsWithPointPattern);
        else return parseAllDigits(dateString);
    }
    
    public static void main(String[] args) throws Exception {
        System.out.println(parseDate("71836209-11-05T22:26:04.699Z"));
    }
     
    private static Date parseAllDigits(String dateString) throws ParseException {
        Matcher m = allDigitsPattern.matcher(dateString);
        if(!m.matches()) {
            throw new ParseException("Can't parse " + dateString,0);
        }
        boolean negativeYear = "-".equals(m.group(1));
        String digits = m.group(2);
        try {
            Date date = new Date();
            int len = digits.length();
            if(len<=5) {
                date.year = Integer.parseInt(digits);
            } else if(len<=7) {
                date.year = Integer.parseInt(digits.substring(0,len-2));
                date.month = Integer.parseInt(digits.substring(len-2));
            } else if(len<=9) {
                date.year = Integer.parseInt(digits.substring(0,len-4));
                date.month = Integer.parseInt(digits.substring(len-4,len-2));
                date.day = Integer.parseInt(digits.substring(len-2));
            } else if(len<=11) {
                date.year = Integer.parseInt(digits.substring(0,len-6));
                date.month = Integer.parseInt(digits.substring(len-6,len-4));
                date.day = Integer.parseInt(digits.substring(len-4,len-2));
                date.hour = Integer.parseInt(digits.substring(len-2));
            } else if(len<=13) {
                date.year = Integer.parseInt(digits.substring(0,len-8));
                date.month = Integer.parseInt(digits.substring(len-8,len-6));
                date.day = Integer.parseInt(digits.substring(len-6,len-4));
                date.hour = Integer.parseInt(digits.substring(len-4,len-2));
                date.minute = Integer.parseInt(digits.substring(len-2));
            } else if(len<=16) {
                date.year = Integer.parseInt(digits.substring(0,len-10));
                date.month = Integer.parseInt(digits.substring(len-10,len-8));
                date.day = Integer.parseInt(digits.substring(len-8,len-6));
                date.hour = Integer.parseInt(digits.substring(len-6,len-4));
                date.minute = Integer.parseInt(digits.substring(len-4,len-2));
                date.second = Integer.parseInt(digits.substring(len-2));
            } else {
                date.year = Integer.parseInt(digits.substring(0,len-13));
                date.month = Integer.parseInt(digits.substring(len-13,len-11));
                date.day = Integer.parseInt(digits.substring(len-11,len-9));
                date.hour = Integer.parseInt(digits.substring(len-9,len-7));
                date.minute = Integer.parseInt(digits.substring(len-7,len-5));
                date.second = Integer.parseInt(digits.substring(len-5,len-3));
                date.millisecond = Integer.parseInt(digits.substring(len-3));
            }
            if(negativeYear) date.year = -date.year;
            boolean isZ = "Z".equals(m.group(3));
            boolean hasExplicitZone = m.group(4)!=null;
            if(isZ && hasExplicitZone) throw new ParseException("Can't parse " + dateString,0);
            date.hasZone = isZ || hasExplicitZone;
            date.negativeZone = "-".equals(m.group(4));
            if(m.group(5)!=null) date.zoneHour = Integer.parseInt(m.group(5));
            if(m.group(6)!=null) date.zoneMinute = Integer.parseInt(m.group(6));
            return date;
        } catch(ParseException e) {
            throw e;
        } catch(Exception e) {
            ParseException exception = new ParseException("Error parsing " + dateString,0);
            exception.initCause(e);
            throw exception;
        }
    }
    
    private static Date parseDateWithPattern(String dateString,Pattern pattern) throws ParseException {
        Matcher m = pattern.matcher(dateString);
        if(!m.matches()) {
            throw new ParseException("Can't parse " + dateString,0);
        }
        boolean negativeYear = "-".equals(m.group(1));
        String yearString = m.group(2);
        try {
            Date date = new Date();
            date.year = Integer.parseInt(yearString);
            if(negativeYear) date.year = -date.year;
            if(m.group(3)!=null) date.month = Integer.parseInt(m.group(3));
            if(m.group(4)!=null) date.day = Integer.parseInt(m.group(4));
            if(m.group(5)!=null) date.hour = Integer.parseInt(m.group(5));
            if(m.group(6)!=null) date.minute = Integer.parseInt(m.group(6));
            if(m.group(7)!=null) date.second = Integer.parseInt(m.group(7));
            String tenthMillis = "0";
            if(m.group(8)!=null) tenthMillis = m.group(8);
            date.millisecond = millisOfUpToTenthMillis(tenthMillis);
            boolean isZ = "Z".equals(m.group(9));
            boolean hasExplicitZone = m.group(10)!=null;
            if(isZ && hasExplicitZone) throw new ParseException("Can't parse " + dateString,0);
            date.hasZone = isZ || hasExplicitZone;
            date.negativeZone = "-".equals(m.group(10));
            if(m.group(11)!=null) date.zoneHour = Integer.parseInt(m.group(11));
            if(m.group(12)!=null) date.zoneMinute = Integer.parseInt(m.group(12));
            return date;
        } catch(ParseException e) {
            throw e;
        } catch(Exception e) {
            ParseException exception = new ParseException("Error parsing " + dateString,0);
            exception.initCause(e);
            throw exception;
        }
    }

    /**
     * Parses a date representation which is generally ISO8601-compliant into milliseconds since the epoch.
     * 
     * @param dateString the date to be parsed.
     * @return the date in milliseconds since 1970-01-01Z.
     * @throws ParseException if the date can not be parsed.
     */
    public long parse(String dateString) throws ParseException {
        return parse(dateString,zone);
    }
    
    /**
     * Parses a date representation which is generally ISO8601-compliant into milliseconds since the epoch, assuming UTC if the time zone is not explicit in the input.
     * 
     * @param dateString the date to be parsed.
     * @return the date in milliseconds since 1970-01-01Z.
     * @throws ParseException if the date can not be parsed.
     */
    public static long parseUtc(String dateString) throws ParseException {
        return parse(dateString,null);
    }

    
    /**
     * Parses a date representation which is generally ISO8601-compliant into milliseconds since the epoch, using a given time zone if the time zone is not explicit in the input.
     * 
     * @param dateString the date to be parsed.
     * @param zone the time zone of the date if an explicit time zone is not given in the input.
     * @return the date in milliseconds since 1970-01-01Z.
     * @throws ParseException if the date can not be parsed.
     */
    public static long parse(String dateString, TimeZone zone) throws ParseException {
        Date date = parseDate(dateString);
        boolean isLeapYear = isLeapYear(date.year);

        int year = date.year;
        long daysSinceYearZero = DAYS_IN_FOUR_CENTURIES * (year / 400);
        year = year % 400;
        if(year >= 1) {
            daysSinceYearZero += 366;
            year--;
        }
        daysSinceYearZero += DAYS_IN_CENTURY * (year / 100);
        year = year % 100;
        daysSinceYearZero += DAYS_IN_FOUR_YEARS * (year / 4);
        year = year % 4;
        daysSinceYearZero += 365 * year;
        
        long daysSince1970 = daysSinceYearZero - DAYS_FROM_YEAR_ZERO_TO_1970;
        
        if(date.month>1) daysSince1970 += 31;
        int febDays = isLeapYear ? 29 : 28;
        if(date.month>2) daysSince1970 += febDays;
        if(date.month>3) daysSince1970 += 31;
        if(date.month>4) daysSince1970 += 30;
        if(date.month>5) daysSince1970 += 31;
        if(date.month>6) daysSince1970 += 30;
        if(date.month>7) daysSince1970 += 31;
        if(date.month>8) daysSince1970 += 31;
        if(date.month>9) daysSince1970 += 30;
        if(date.month>10) daysSince1970 += 31;
        if(date.month>11) daysSince1970 += 30;
        daysSince1970 += date.day - 1;
        
        long res = 86400000L * daysSince1970 + 3600000L * date.hour + 60000L * date.minute + 1000L * date.second + date.millisecond;
        if(date.hasZone) {
            res -= (date.negativeZone ? -1 : 1) * (3600000L * date.zoneHour + 60000L * date.zoneMinute);
        } else if(zone!=null) {
            // do it twice to get right results near DST boundary
            // for ambiguous times (e.g. during "fall back") chooses the earlier time 
            synchronized(zone) {
                long offsetRes = res - zone.getOffset(res);
                res = res - zone.getOffset(offsetRes);
            }
        }
        return res;
    }
}
