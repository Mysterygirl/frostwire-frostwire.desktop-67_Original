/**
 *******************************************************************************
 * Copyright (C) 2000-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/timezone/TimeZoneRegression.java,v $
 * $Date: 2003/06/03 18:49:31 $
 * $Revision: 1.10 $
 *
 *******************************************************************************
 */

/**
 * @test 1.18 99/09/21
 * @bug 4052967 4073209 4073215 4084933 4096952 4109314 4126678 4151406 4151429
 * @bug 4154525 4154537 4154542 4154650 4159922 4162593 4173604 4176686 4184229 4208960
 */

package com.ibm.icu.dev.test.timezone;
import com.ibm.icu.util.*;
import java.io.*;
import com.ibm.icu.text.*;
import com.ibm.icu.dev.test.*;
import java.util.Date;
import java.util.Locale;

public class TimeZoneRegression extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new TimeZoneRegression().run(args);
    }

    public void Test4052967() {
        logln("*** CHECK TIMEZONE AGAINST HOST OS SETTING ***");
	String id = TimeZone.getDefault().getID();
        logln("user.timezone: " + System.getProperty("user.timezone", "<not set>"));
	logln("TimeZone.getDefault().getID(): " + id);
        logln(new Date().toString());
        logln("*** THE RESULTS OF THIS TEST MUST BE VERIFIED MANUALLY ***");
    }

    public void Test4073209() {
        TimeZone z1 = TimeZone.getTimeZone("PST");
        TimeZone z2 = TimeZone.getTimeZone("PST");
        if (z1 == z2) errln("Fail: TimeZone should return clones");
    }

    public void Test4073215() {
        SimpleTimeZone z = (SimpleTimeZone) TimeZone.getTimeZone("GMT");
        if (z.useDaylightTime())
            errln("Fail: Fix test to start with non-DST zone");
        z.setStartRule(Calendar.FEBRUARY, 1, Calendar.SUNDAY, 0);
        z.setEndRule(Calendar.MARCH, -1, Calendar.SUNDAY, 0);
        if (!z.useDaylightTime())
            errln("Fail: DST not active");
        java.util.Calendar tempcal = java.util.Calendar.getInstance();
        tempcal.clear();
        tempcal.set(1997, Calendar.JANUARY, 31);
        Date d1 = tempcal.getTime();
        tempcal.set(1997, Calendar.MARCH, 1);
        Date d2 = tempcal.getTime();
        tempcal.set(1997, Calendar.MARCH, 31);
        Date d3 = tempcal.getTime();
        if (z.inDaylightTime(d1) || !z.inDaylightTime(d2) ||
            z.inDaylightTime(d3)) {
            errln("Fail: DST not working as expected");
        }
    }

    /**
     * The expected behavior of TimeZone around the boundaries is:
     * (Assume transition time of 2:00 AM)
     *    day of onset 1:59 AM STD  = display name 1:59 AM ST
     *                 2:00 AM STD  = display name 3:00 AM DT
     *    day of end   0:59 AM STD  = display name 1:59 AM DT
     *                 1:00 AM STD  = display name 1:00 AM ST
     */
    public void Test4084933() {
        TimeZone tz = TimeZone.getTimeZone("PST");

        long offset1 = tz.getOffset(1,
            1997, Calendar.OCTOBER, 26, Calendar.SUNDAY, (2*60*60*1000));
        long offset2 = tz.getOffset(1,
            1997, Calendar.OCTOBER, 26, Calendar.SUNDAY, (2*60*60*1000)-1);

        long offset3 = tz.getOffset(1,
            1997, Calendar.OCTOBER, 26, Calendar.SUNDAY, (1*60*60*1000));
        long offset4 = tz.getOffset(1,
            1997, Calendar.OCTOBER, 26, Calendar.SUNDAY, (1*60*60*1000)-1);

        /*
         *  The following was added just for consistency.  It shows that going *to* Daylight
         *  Savings Time (PDT) does work at 2am.
         */

        long offset5 = tz.getOffset(1,
            1997, Calendar.APRIL, 6, Calendar.SUNDAY, (2*60*60*1000));
        long offset6 = tz.getOffset(1,
            1997, Calendar.APRIL, 6, Calendar.SUNDAY, (2*60*60*1000)-1);

        long offset7 = tz.getOffset(1,
            1997, Calendar.APRIL, 6, Calendar.SUNDAY, (1*60*60*1000));
        long offset8 = tz.getOffset(1,
            1997, Calendar.APRIL, 6, Calendar.SUNDAY, (1*60*60*1000)-1);

        long SToffset = -8 * 60*60*1000L;
        long DToffset = -7 * 60*60*1000L;
        if (offset1 != SToffset || offset2 != SToffset ||
            offset3 != SToffset || offset4 != DToffset ||
            offset5 != DToffset || offset6 != SToffset ||
            offset7 != SToffset || offset8 != SToffset)
            errln("Fail: TimeZone misbehaving");
    }

    public void Test4096952() {
        String[] ZONES = { "GMT", "MET", "IST" };
        boolean pass = true;
        try {
            for (int i=0; i<ZONES.length; ++i) {
                TimeZone zone = TimeZone.getTimeZone(ZONES[i]);
                if (!zone.getID().equals(ZONES[i]))
                    errln("Fail: Test broken; zones not instantiating");

                ByteArrayOutputStream baos;
                ObjectOutputStream ostream =
                    new ObjectOutputStream(baos = new 
                                           ByteArrayOutputStream());
                ostream.writeObject(zone);
                ostream.close();
                baos.close();
                ObjectInputStream istream =
                    new ObjectInputStream(new 
                                          ByteArrayInputStream(baos.toByteArray()));
                TimeZone frankenZone = (TimeZone) istream.readObject();
                //logln("Zone:        " + zone);
                //logln("FrankenZone: " + frankenZone);
                if (!zone.equals(frankenZone)) {
                    logln("TimeZone " + zone.getID() +
                          " not equal to serialized/deserialized one");
                    pass = false;
                }
            }
            if (!pass) errln("Fail: TimeZone serialization/equality bug");
        }
        catch (IOException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
    }

    public void Test4109314() {
        GregorianCalendar testCal = (GregorianCalendar)Calendar.getInstance(); 
        TimeZone PST = TimeZone.getTimeZone("PST");
        java.util.Calendar tempcal = java.util.Calendar.getInstance();
        tempcal.clear();
        tempcal.set(1998,Calendar.APRIL,4,22,0);
        Date d1 = tempcal.getTime();
        tempcal.set(1998,Calendar.APRIL,5,6,0);
        Date d2 = tempcal.getTime();
        tempcal.set(1998,Calendar.OCTOBER,24,22,0);
        Date d3 = tempcal.getTime();
        tempcal.set(1998,Calendar.OCTOBER,25,6,0);
        Date d4 = tempcal.getTime();
        Object[] testData = {
            PST, d1, d2,
            PST, d3, d4,
        };
        boolean pass=true;
        for (int i=0; i<testData.length; i+=3) {
            testCal.setTimeZone((TimeZone) testData[i]);
            long t = ((Date)testData[i+1]).getTime();
            Date end = (Date) testData[i+2];
            while (t < end.getTime()) { 
                testCal.setTime(new Date(t));
                if (!checkCalendar314(testCal, (TimeZone) testData[i]))
                    pass = false;
                t += 60*60*1000L;
            } 
        }
        if (!pass) errln("Fail: TZ API inconsistent");
    } 

    boolean checkCalendar314(GregorianCalendar testCal, TimeZone testTZ) { 
        // GregorianCalendar testCal = (GregorianCalendar)aCal.clone(); 

        final int ONE_DAY = 24*60*60*1000;

        int tzOffset, tzRawOffset; 
        Float tzOffsetFloat,tzRawOffsetFloat; 
        // Here is where the user made an error.  They were passing in the value of
        // the MILLSECOND field; you need to pass in the millis in the day in STANDARD
        // time.
        int millis = testCal.get(Calendar.MILLISECOND) +
            1000 * (testCal.get(Calendar.SECOND) +
                    60 * (testCal.get(Calendar.MINUTE) +
                          60 * (testCal.get(Calendar.HOUR_OF_DAY)))) -
            testCal.get(Calendar.DST_OFFSET);

        /* Fix up millis to be in range.  ASSUME THAT WE ARE NOT AT THE
         * BEGINNING OR END OF A MONTH.  We must add this code because
         * getOffset() has been changed to be more strict about the parameters
         * it receives -- it turns out that this test was passing in illegal
         * values. */
        int date = testCal.get(Calendar.DATE);
        int dow  = testCal.get(Calendar.DAY_OF_WEEK);
        while (millis < 0) {
            millis += ONE_DAY;
            --date;
            dow = Calendar.SUNDAY + ((dow - Calendar.SUNDAY + 6) % 7);
        }
        while (millis >= ONE_DAY) {
            millis -= ONE_DAY;
            ++date;
            dow = Calendar.SUNDAY + ((dow - Calendar.SUNDAY + 1) % 7);
        }

        tzOffset = testTZ.getOffset(testCal.get(Calendar.ERA), 
                                    testCal.get(Calendar.YEAR), 
                                    testCal.get(Calendar.MONTH), 
                                    date, 
                                    dow, 
                                    millis); 
        tzRawOffset = testTZ.getRawOffset(); 
        tzOffsetFloat = new Float((float)tzOffset/(float)3600000); 
        tzRawOffsetFloat = new Float((float)tzRawOffset/(float)3600000); 

        Date testDate = testCal.getTime(); 

        boolean inDaylightTime = testTZ.inDaylightTime(testDate); 
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm"); 
        sdf.setCalendar(testCal); 
        String inDaylightTimeString; 

        boolean passed; 

        if (inDaylightTime) 
        { 
            inDaylightTimeString = " DST "; 
            passed = (tzOffset == (tzRawOffset + 3600000));
        } 
        else 
        { 
            inDaylightTimeString = "     "; 
            passed = (tzOffset == tzRawOffset);
        } 

        String output = testTZ.getID() + " " + sdf.format(testDate) +
            " Offset(" + tzOffsetFloat + ")" +
            " RawOffset(" + tzRawOffsetFloat + ")" + 
            " " + millis/(float)3600000 + " " +
            inDaylightTimeString; 

        if (passed) 
            output += "     "; 
        else 
            output += "ERROR"; 

        if (passed) logln(output); else errln(output);
        return passed;
    } 

    /**
     * CANNOT REPRODUDE
     *
     * Yet another _alleged_ bug in TimeZone.getOffset(), a method that never
     * should have been made public.  It's simply too hard to use correctly.
     *
     * The original test code failed to do the following:
     * (1) Call Calendar.setTime() before getting the fields!
     * (2) Use the right millis (as usual) for getOffset(); they were passing
     *     in the MILLIS field, instead of the STANDARD MILLIS IN DAY.
     * When you fix these two problems, the test passes, as expected.
     */
    public void Test4126678() {
	// Note: this test depends on the PST time zone.
	TimeZone initialZone = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance();
        TimeZone tz = TimeZone.getTimeZone("PST");
	TimeZone.setDefault(tz);
        cal.setTimeZone(tz);

        java.util.Calendar tempcal = java.util.Calendar.getInstance();
        tempcal.clear();
        tempcal.set(1998, Calendar.APRIL, 5, 10, 0);
        Date dt = tempcal.getTime();
	// the dt value is local time in PST.
        if (!tz.inDaylightTime(dt))
            errln("We're not in Daylight Savings Time and we should be.\n");

        cal.setTime(dt);
        int era = cal.get(Calendar.ERA);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DATE);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int millis = cal.get(Calendar.MILLISECOND) +
            (cal.get(Calendar.SECOND) +
             (cal.get(Calendar.MINUTE) +
              (cal.get(Calendar.HOUR) * 60) * 60) * 1000) -
            cal.get(Calendar.DST_OFFSET);

        long offset = tz.getOffset(era, year, month, day, dayOfWeek, millis);
        long raw_offset = tz.getRawOffset();
        if (offset == raw_offset)
            errln("Offsets should not match when in DST");

	// restore the initial time zone so that this test case
	// doesn't affect the others.
	TimeZone.setDefault(initialZone);
    }
    
    /**
     * TimeZone.getAvailableIDs(int) throws exception for certain values,
     * due to a faulty constant in TimeZone.java.
     */
    public void Test4151406() {
        int max = 0;
        for (int h=-28; h<=30; ++h) {
            // h is in half-hours from GMT; rawoffset is in millis
            int rawoffset = h * 1800000;
            int hh = (h<0) ? -h : h;
            String hname = ((h<0) ? "GMT-" : "GMT+") +
                ((hh/2 < 10) ? "0" : "") +
                (hh/2) + ':' +
                ((hh%2==0) ? "00" : "30");
            try {
                String[] ids = TimeZone.getAvailableIDs(rawoffset);
                if (ids.length > max) max = ids.length;
                logln(hname + ' ' + ids.length +
                      ((ids.length > 0) ? (" e.g. " + ids[0]) : ""));
            } catch (Exception e) {
                errln(hname + ' ' + "Fail: " + e);
            }
        }
        logln("Maximum zones per offset = " + max);
    }

    public void Test4151429() {
        try {
            TimeZone tz = TimeZone.getTimeZone("GMT");
            /*String name =*/ tz.getDisplayName(true, Integer.MAX_VALUE,
                                            Locale.getDefault());
            errln("IllegalArgumentException not thrown by TimeZone.getDisplayName()");
        } catch(IllegalArgumentException e) {
            System.out.print("");
        }
    }

    /**
     * SimpleTimeZone accepts illegal DST savings values.  These values
     * must be non-zero.  There is no upper limit at this time.
     */
    public void Test4154525() {
        final int GOOD = 1, BAD = 0;
        int[] DATA = {
            1, GOOD,
            0, BAD,
            -1, BAD,
            60*60*1000, GOOD,
            Integer.MIN_VALUE, BAD,
            // Integer.MAX_VALUE, ?, // no upper limit on DST savings at this time
        };
        for (int i=0; i<DATA.length; i+=2) {
            int savings = DATA[i];
            boolean valid = DATA[i+1] == GOOD;
            String method = null;
            for (int j=0; j<2; ++j) {
                try {
                    switch (j) {
                    case 0:
                        method = "constructor";
                        SimpleTimeZone z = new SimpleTimeZone(0, "id",
                            Calendar.JANUARY, 1, 0, 0,
                            Calendar.MARCH, 1, 0, 0,
                            savings); // <- what we're interested in
                        break;
                    case 1:
                        method = "setDSTSavings()";
                        z = new SimpleTimeZone(0, "GMT");
                        z.setDSTSavings(savings);
                        break;
                    }
                    if (valid) {
                        logln("Pass: DST savings of " + savings + " accepted by " + method);
                    } else {
                        errln("Fail: DST savings of " + savings + " accepted by " + method);
                    }
                } catch (IllegalArgumentException e) {
                    if (valid) {
                        errln("Fail: DST savings of " + savings + " to " + method + " gave " + e);
                    } else {
                        logln("Pass: DST savings of " + savings + " to " + method + " gave " + e);
                    }               
                }
            }
        }
    }

    /**
     * SimpleTimeZone.hasSameRules() doesn't work for zones with no DST
     * and different DST parameters.
     */
    public void Test4154537() {
        // tz1 and tz2 have no DST and different rule parameters
        SimpleTimeZone tz1 = new SimpleTimeZone(0, "1", 0, 0, 0, 0, 2, 0, 0, 0);
        SimpleTimeZone tz2 = new SimpleTimeZone(0, "2", 1, 0, 0, 0, 3, 0, 0, 0);
        // tza and tzA have the same rule params
        SimpleTimeZone tza = new SimpleTimeZone(0, "a", 0, 1, 0, 0, 3, 2, 0, 0);
        SimpleTimeZone tzA = new SimpleTimeZone(0, "A", 0, 1, 0, 0, 3, 2, 0, 0);
        // tzb differs from tza
        SimpleTimeZone tzb = new SimpleTimeZone(0, "b", 0, 1, 0, 0, 3, 1, 0, 0);
        if (tz1.useDaylightTime() || tz2.useDaylightTime() ||
            !tza.useDaylightTime() || !tzA.useDaylightTime() ||
            !tzb.useDaylightTime()) {
            errln("Test is broken -- rewrite it");
        }
        if (!tza.hasSameRules(tzA) || tza.hasSameRules(tzb)) {
            errln("Fail: hasSameRules() broken for zones with rules");
        }
        if (!tz1.hasSameRules(tz2)) {
            errln("Fail: hasSameRules() returns false for zones without rules");
            errln("zone 1 = " + tz1);
            errln("zone 2 = " + tz2);
        }
    }

    /**
     * SimpleTimeZone constructors, setStartRule(), and setEndRule() don't
     * check for out-of-range arguments.
     */
    public void Test4154542() {
        final int GOOD = 1;
        final int BAD  = 0;

        final int GOOD_MONTH       = Calendar.JANUARY;
        final int GOOD_DAY         = 1;
        final int GOOD_DAY_OF_WEEK = Calendar.SUNDAY;
        final int GOOD_TIME        = 0;

        int[] DATA = {
            GOOD, Integer.MIN_VALUE,    0,  Integer.MAX_VALUE,   Integer.MIN_VALUE,
            GOOD, Calendar.JANUARY,    -5,  Calendar.SUNDAY,     0,
            GOOD, Calendar.DECEMBER,    5,  Calendar.SATURDAY,   24*60*60*1000-1,
            BAD,  Calendar.DECEMBER,    5,  Calendar.SATURDAY,   24*60*60*1000,
            BAD,  Calendar.DECEMBER,    5,  Calendar.SATURDAY,  -1,
            BAD,  Calendar.JANUARY,    -6,  Calendar.SUNDAY,     0,
            BAD,  Calendar.DECEMBER,    6,  Calendar.SATURDAY,   24*60*60*1000,
            GOOD, Calendar.DECEMBER,    1,  0,                   0,
            GOOD, Calendar.DECEMBER,   31,  0,                   0,
            BAD,  Calendar.APRIL,      31,  0,                   0,
            BAD,  Calendar.DECEMBER,   32,  0,                   0,
            BAD,  Calendar.JANUARY-1,   1,  Calendar.SUNDAY,     0,
            BAD,  Calendar.DECEMBER+1,  1,  Calendar.SUNDAY,     0,
            GOOD, Calendar.DECEMBER,   31, -Calendar.SUNDAY,     0,
            GOOD, Calendar.DECEMBER,   31, -Calendar.SATURDAY,   0,
            BAD,  Calendar.DECEMBER,   32, -Calendar.SATURDAY,   0,
            BAD,  Calendar.DECEMBER,  -32, -Calendar.SATURDAY,   0,
            BAD,  Calendar.DECEMBER,   31, -Calendar.SATURDAY-1, 0,
        };
        SimpleTimeZone zone = new SimpleTimeZone(0, "Z");
        for (int i=0; i<DATA.length; i+=5) {
            boolean shouldBeGood = (DATA[i] == GOOD);
            int month     = DATA[i+1];
            int day       = DATA[i+2];
            int dayOfWeek = DATA[i+3];
            int time      = DATA[i+4];

            Exception ex = null;
            try {
                zone.setStartRule(month, day, dayOfWeek, time);
            } catch (IllegalArgumentException e) {
                ex = e;
            }
            if ((ex == null) != shouldBeGood) {
                errln("setStartRule(month=" + month + ", day=" + day +
                      ", dayOfWeek=" + dayOfWeek + ", time=" + time +
                      (shouldBeGood ? (") should work but throws " + ex)
                       : ") should fail but doesn't"));
            }

            ex = null;
            try {
                zone.setEndRule(month, day, dayOfWeek, time);
            } catch (IllegalArgumentException e) {
                ex = e;
            }
            if ((ex == null) != shouldBeGood) {
                errln("setEndRule(month=" + month + ", day=" + day +
                      ", dayOfWeek=" + dayOfWeek + ", time=" + time +
                      (shouldBeGood ? (") should work but throws " + ex)
                       : ") should fail but doesn't"));
            }

            ex = null;
            try {
                /*SimpleTimeZone temp =*/ new SimpleTimeZone(0, "Z",
                        month, day, dayOfWeek, time,
                        GOOD_MONTH, GOOD_DAY, GOOD_DAY_OF_WEEK, GOOD_TIME);
            } catch (IllegalArgumentException e) {
                ex = e;
            }
            if ((ex == null) != shouldBeGood) {
                errln("SimpleTimeZone(month=" + month + ", day=" + day +
                      ", dayOfWeek=" + dayOfWeek + ", time=" + time +
                      (shouldBeGood ? (", <end>) should work but throws " + ex)
                       : ", <end>) should fail but doesn't"));
            }            

            ex = null;
            try {
                /*SimpleTimeZone temp = */new SimpleTimeZone(0, "Z",
                        GOOD_MONTH, GOOD_DAY, GOOD_DAY_OF_WEEK, GOOD_TIME,
                        month, day, dayOfWeek, time);
               // temp = null;
            } catch (IllegalArgumentException e) {
                ex = e;
            }
            if ((ex == null) != shouldBeGood) {
                errln("SimpleTimeZone(<start>, month=" + month + ", day=" + day +
                      ", dayOfWeek=" + dayOfWeek + ", time=" + time +
                      (shouldBeGood ? (") should work but throws " + ex)
                       : ") should fail but doesn't"));
            }            
        }
    }

    /**
     * SimpleTimeZone.getOffset accepts illegal arguments.
     */
    public void Test4154650() {
        final int GOOD=1, BAD=0;
        final int GOOD_ERA=GregorianCalendar.AD, GOOD_YEAR=1998, GOOD_MONTH=Calendar.AUGUST;
        final int GOOD_DAY=2, GOOD_DOW=Calendar.SUNDAY, GOOD_TIME=16*3600000;
        int[] DATA = {
            GOOD, GOOD_ERA, GOOD_YEAR, GOOD_MONTH, GOOD_DAY, GOOD_DOW, GOOD_TIME,

            GOOD, GregorianCalendar.BC, GOOD_YEAR, GOOD_MONTH, GOOD_DAY, GOOD_DOW, GOOD_TIME,
            GOOD, GregorianCalendar.AD, GOOD_YEAR, GOOD_MONTH, GOOD_DAY, GOOD_DOW, GOOD_TIME,
            BAD,  GregorianCalendar.BC-1, GOOD_YEAR, GOOD_MONTH, GOOD_DAY, GOOD_DOW, GOOD_TIME,
            BAD,  GregorianCalendar.AD+1, GOOD_YEAR, GOOD_MONTH, GOOD_DAY, GOOD_DOW, GOOD_TIME,

            GOOD, GOOD_ERA, GOOD_YEAR, Calendar.JANUARY, GOOD_DAY, GOOD_DOW, GOOD_TIME,
            GOOD, GOOD_ERA, GOOD_YEAR, Calendar.DECEMBER, GOOD_DAY, GOOD_DOW, GOOD_TIME,
            BAD,  GOOD_ERA, GOOD_YEAR, Calendar.JANUARY-1, GOOD_DAY, GOOD_DOW, GOOD_TIME,
            BAD,  GOOD_ERA, GOOD_YEAR, Calendar.DECEMBER+1, GOOD_DAY, GOOD_DOW, GOOD_TIME,
            
            GOOD, GOOD_ERA, GOOD_YEAR, Calendar.JANUARY, 1, GOOD_DOW, GOOD_TIME,
            GOOD, GOOD_ERA, GOOD_YEAR, Calendar.JANUARY, 31, GOOD_DOW, GOOD_TIME,
            BAD,  GOOD_ERA, GOOD_YEAR, Calendar.JANUARY, 0, GOOD_DOW, GOOD_TIME,
            BAD,  GOOD_ERA, GOOD_YEAR, Calendar.JANUARY, 32, GOOD_DOW, GOOD_TIME,

            GOOD, GOOD_ERA, GOOD_YEAR, GOOD_MONTH, GOOD_DAY, Calendar.SUNDAY, GOOD_TIME,
            GOOD, GOOD_ERA, GOOD_YEAR, GOOD_MONTH, GOOD_DAY, Calendar.SATURDAY, GOOD_TIME,
            BAD,  GOOD_ERA, GOOD_YEAR, GOOD_MONTH, GOOD_DAY, Calendar.SUNDAY-1, GOOD_TIME,
            BAD,  GOOD_ERA, GOOD_YEAR, GOOD_MONTH, GOOD_DAY, Calendar.SATURDAY+1, GOOD_TIME,

            GOOD, GOOD_ERA, GOOD_YEAR, GOOD_MONTH, GOOD_DAY, GOOD_DOW, 0,
            GOOD, GOOD_ERA, GOOD_YEAR, GOOD_MONTH, GOOD_DAY, GOOD_DOW, 24*3600000-1,
            BAD,  GOOD_ERA, GOOD_YEAR, GOOD_MONTH, GOOD_DAY, GOOD_DOW, -1,
            BAD,  GOOD_ERA, GOOD_YEAR, GOOD_MONTH, GOOD_DAY, GOOD_DOW, 24*3600000,
        };

        TimeZone tz = TimeZone.getDefault();
        for (int i=0; i<DATA.length; i+=7) {
            boolean good = DATA[i] == GOOD;
            IllegalArgumentException e = null;
            try {
                /*int offset =*/ tz.getOffset(DATA[i+1], DATA[i+2], DATA[i+3],
                                          DATA[i+4], DATA[i+5], DATA[i+6]); 
                //offset = 0;
           } catch (IllegalArgumentException ex) {
                e = ex;
            }
            if (good != (e == null)) {
                errln("Fail: getOffset(" +
                      DATA[i+1] + ", " + DATA[i+2] + ", " + DATA[i+3] + ", " +
                      DATA[i+4] + ", " + DATA[i+5] + ", " + DATA[i+6] +
                      (good ? (") threw " + e) : ") accepts invalid args"));
            }
        }
    }

    /**
     * TimeZone constructors allow null IDs.
     */
    public void Test4159922() {
        TimeZone z = null;

        // TimeZone API.  Only hasSameRules() and setDefault() should
        // allow null.
        try {
            z = TimeZone.getTimeZone(null);
            errln("FAIL: Null allowed in getTimeZone");
        } catch (NullPointerException e) {
            System.out.print("");
        }
        z = TimeZone.getTimeZone("GMT");
        try {
            z.getDisplayName(false, TimeZone.SHORT, null);
            errln("FAIL: Null allowed in getDisplayName(3)");
        } catch (NullPointerException e) {
            System.out.print("");
        }
        try {
            z.getDisplayName(null);
            errln("FAIL: Null allowed in getDisplayName(1)");
        } catch (NullPointerException e) {
            System.out.print("");
        }
        try {
            if (z.hasSameRules(null)) {
                errln("FAIL: hasSameRules returned true");
            }
        } catch (NullPointerException e) {
            errln("FAIL: Null NOT allowed in hasSameRules");
        }
        try {
            z.inDaylightTime(null);
            errln("FAIL: Null allowed in inDaylightTime");
        } catch (NullPointerException e) {
            System.out.print("");
        }
        try {
            z.setID(null);
            errln("FAIL: Null allowed in setID");
        } catch (NullPointerException e) {
            System.out.print("");
        }

        TimeZone save = TimeZone.getDefault();
        try {
            TimeZone.setDefault(null);
        } catch (NullPointerException e) {
            errln("FAIL: Null NOT allowed in setDefault");
        } finally {
            TimeZone.setDefault(save);
        }

        // SimpleTimeZone API
        SimpleTimeZone s = null;
        try {
            s = new SimpleTimeZone(0, null);
            errln("FAIL: Null allowed in SimpleTimeZone(2)");
        } catch (NullPointerException e) {
            System.out.print("");
        }
        try {
            s = new SimpleTimeZone(0, null, 0, 1, 0, 0, 0, 1, 0, 0);
            errln("FAIL: Null allowed in SimpleTimeZone(10)");
        } catch (NullPointerException e) {
            System.out.print("");
        }
        try {
            s = new SimpleTimeZone(0, null, 0, 1, 0, 0, 0, 1, 0, 0, 1000);
            errln("FAIL: Null allowed in SimpleTimeZone(11)");
        } catch (NullPointerException e) {
            System.out.print("");
        }
        if(s!=null){
            errln("FAIL: Did not get the expected Exception");
        }
    }

    /**
     * TimeZone broken at midnight.  The TimeZone code fails to handle
     * transitions at midnight correctly.
     */
    public void Test4162593() {
        SimpleDateFormat fmt = new SimpleDateFormat("z", Locale.US);
        final int ONE_HOUR = 60*60*1000;
	TimeZone initialZone = TimeZone.getDefault();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm z"); 

        SimpleTimeZone asuncion = new SimpleTimeZone(-4*ONE_HOUR, "America/Asuncion" /*PY%sT*/,
            Calendar.OCTOBER, 1, 0 /*DOM*/, 0*ONE_HOUR,
            Calendar.MARCH, 1, 0 /*DOM*/, 0*ONE_HOUR, 1*ONE_HOUR);

        /* Zone
         * Starting time
         * Transition expected between start+1H and start+2H
         */
        Object[] DATA = {
            new SimpleTimeZone(2*ONE_HOUR, "Asia/Damascus" /*EE%sT*/,
                Calendar.APRIL, 1, 0 /*DOM*/, 0*ONE_HOUR,
                Calendar.OCTOBER, 1, 0 /*DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
            new int[] {98, Calendar.SEPTEMBER, 30, 22, 0},
            Boolean.TRUE,

            asuncion,
            new int[] {100, Calendar.FEBRUARY, 28, 22, 0},
            Boolean.FALSE,

            asuncion,
            new int[] {100, Calendar.FEBRUARY, 29, 22, 0},
            Boolean.TRUE,
        };
        
        String[] zone = new String[4];
        
        for (int j=0; j<DATA.length; j+=3) {
            TimeZone tz = (TimeZone)DATA[j];
            TimeZone.setDefault(tz);
            fmt.setTimeZone(tz);
            sdf.setTimeZone(tz);

            // Must construct the Date object AFTER setting the default zone
            int[] p = (int[])DATA[j+1];
            java.util.Calendar tempcal = java.util.Calendar.getInstance();
            tempcal.clear();
            tempcal.set(p[0] + 1900, p[1], p[2], p[3], p[4]);
            Date d = tempcal.getTime();
            boolean transitionExpected = ((Boolean)DATA[j+2]).booleanValue();

            logln(tz.getID() + ":");
            for (int i=0; i<4; ++i) {
                zone[i] = fmt.format(d);
                logln("" + i + ": " + sdf.format(d) + " => " + zone[i]);
                d = new Date(d.getTime() + ONE_HOUR);
            }
            if (zone[0].equals(zone[1]) &&
                (zone[1].equals(zone[2]) != transitionExpected) &&
                zone[2].equals(zone[3])) {
                logln("Ok: transition " + transitionExpected);
            } else {
                errln("Fail: boundary transition incorrect");
            }
        }

	// restore the initial time zone so that this test case
	// doesn't affect the others.
	TimeZone.setDefault(initialZone);
    }

    /**
     * TimeZone broken in last hour of year
     */
    public void Test4173604() {
        SimpleTimeZone pst = (SimpleTimeZone)TimeZone.getTimeZone("PST");
        int o22 = pst.getOffset(1, 1998, 11, 31, Calendar.THURSDAY, 22*60*60*1000);
        int o23 = pst.getOffset(1, 1998, 11, 31, Calendar.THURSDAY, 23*60*60*1000);
        int o00 = pst.getOffset(1, 1999, 0, 1, Calendar.FRIDAY, 0);
        if (o22 != o23 || o22 != o00) {
            errln("Offsets should be the same (for PST), but got: " +
                  "12/31 22:00 " + o22 +
                  ", 12/31 23:00 " + o23 +
                  ", 01/01 00:00 " + o00);
        }

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeZone(pst);
        cal.clear();
        cal.set(1998, Calendar.JANUARY, 1);
        int lastDST = cal.get(Calendar.DST_OFFSET);
        int transitions = 0;
        int delta = 5;
        while (cal.get(Calendar.YEAR) < 2000) {
            cal.add(Calendar.MINUTE, delta);
            if (cal.get(Calendar.DST_OFFSET) != lastDST) {
                ++transitions;
                Calendar t = (Calendar)cal.clone();
                t.add(Calendar.MINUTE, -delta);
                logln(t.getTime() + "  " + t.get(Calendar.DST_OFFSET));
                logln(cal.getTime() + "  " + (lastDST=cal.get(Calendar.DST_OFFSET)));
            }
        }
        if (transitions != 4) {
            errln("Saw " + transitions + " transitions; should have seen 4");
        }
    }

    /**
     * getDisplayName doesn't work with unusual savings/offsets.
     */
    public void Test4176686() {
        // Construct a zone that does not observe DST but
        // that does have a DST savings (which should be ignored).
        int offset = 90 * 60000; // 1:30
        SimpleTimeZone z1 = new SimpleTimeZone(offset, "_std_zone_");
        z1.setDSTSavings(45 * 60000); // 0:45

        // Construct a zone that observes DST for the first 6 months.
        SimpleTimeZone z2 = new SimpleTimeZone(offset, "_dst_zone_");
        z2.setDSTSavings(45 * 60000); // 0:45
        z2.setStartRule(Calendar.JANUARY, 1, 0);
        z2.setEndRule(Calendar.JULY, 1, 0);

        // Also check DateFormat
        DateFormat fmt1 = new SimpleDateFormat("z");
        fmt1.setTimeZone(z1); // Format uses standard zone
        DateFormat fmt2 = new SimpleDateFormat("z");
        fmt2.setTimeZone(z2); // Format uses DST zone
        java.util.Calendar tempcal = java.util.Calendar.getInstance();
        tempcal.clear();
        tempcal.set(1970, Calendar.FEBRUARY, 1);
        Date dst = tempcal.getTime(); // Time in DST
        tempcal.set(1970, Calendar.AUGUST, 1);
        Date std = tempcal.getTime(); // Time in standard

        // Description, Result, Expected Result
        String[] DATA = {
            "getDisplayName(false, SHORT)/std zone",
            z1.getDisplayName(false, TimeZone.SHORT), "GMT+01:30",
            "getDisplayName(false, LONG)/std zone",
            z1.getDisplayName(false, TimeZone.LONG ), "GMT+01:30",
            "getDisplayName(true, SHORT)/std zone",
            z1.getDisplayName(true, TimeZone.SHORT), "GMT+01:30",
            "getDisplayName(true, LONG)/std zone",
            z1.getDisplayName(true, TimeZone.LONG ), "GMT+01:30",
            "getDisplayName(false, SHORT)/dst zone",
            z2.getDisplayName(false, TimeZone.SHORT), "GMT+01:30",
            "getDisplayName(false, LONG)/dst zone",
            z2.getDisplayName(false, TimeZone.LONG ), "GMT+01:30",
            "getDisplayName(true, SHORT)/dst zone",
            z2.getDisplayName(true, TimeZone.SHORT), "GMT+02:15",
            "getDisplayName(true, LONG)/dst zone",
            z2.getDisplayName(true, TimeZone.LONG ), "GMT+02:15",
            "DateFormat.format(std)/std zone", fmt1.format(std), "GMT+01:30",
            "DateFormat.format(dst)/std zone", fmt1.format(dst), "GMT+01:30",
            "DateFormat.format(std)/dst zone", fmt2.format(std), "GMT+01:30",
            "DateFormat.format(dst)/dst zone", fmt2.format(dst), "GMT+02:15",
        };

        for (int i=0; i<DATA.length; i+=3) {
            if (!DATA[i+1].equals(DATA[i+2])) {
                errln("FAIL: " + DATA[i] + " -> " + DATA[i+1] + ", exp " + DATA[i+2]);
            }
        }
    }

    /**
     * SimpleTimeZone allows invalid DOM values.
     */
    public void Test4184229() {
        SimpleTimeZone zone = null;
        try {
            zone = new SimpleTimeZone(0, "A", 0, -1, 0, 0, 0, 0, 0, 0);
            errln("Failed. No exception has been thrown for DOM -1 startDay");
        } catch(IllegalArgumentException e) {
            logln("(a) " + e.getMessage());
        }
        try {
            zone = new SimpleTimeZone(0, "A", 0, 0, 0, 0, 0, -1, 0, 0);
            errln("Failed. No exception has been thrown for DOM -1 endDay");
        } catch(IllegalArgumentException e) {
            logln("(b) " + e.getMessage());
        }
        try {
            zone = new SimpleTimeZone(0, "A", 0, -1, 0, 0, 0, 0, 0, 0, 1000);
            errln("Failed. No exception has been thrown for DOM -1 startDay +savings");
        } catch(IllegalArgumentException e) {
            logln("(c) " + e.getMessage());
        }
        try {
            zone = new SimpleTimeZone(0, "A", 0, 0, 0, 0, 0, -1, 0, 0, 1000);
            errln("Failed. No exception has been thrown for DOM -1 endDay +savings");
        } catch(IllegalArgumentException e) {
            logln("(d) " + e.getMessage());
        }
        // Make a valid constructor call for subsequent tests.
        zone = new SimpleTimeZone(0, "A", 0, 1, 0, 0, 0, 1, 0, 0);
        try {
            zone.setStartRule(0, -1, 0, 0);
            errln("Failed. No exception has been thrown for DOM -1 setStartRule +savings");
        } catch(IllegalArgumentException e) {
            logln("(e) " + e.getMessage());
        }
        try {
            zone.setStartRule(0, -1, 0);
            errln("Failed. No exception has been thrown for DOM -1 setStartRule");
        } catch(IllegalArgumentException e) {
            logln("(f) " + e.getMessage());
        }
        try {
            zone.setEndRule(0, -1, 0, 0);
            errln("Failed. No exception has been thrown for DOM -1 setEndRule +savings");
        } catch(IllegalArgumentException e) {
            logln("(g) " + e.getMessage());
        }
        try {
            zone.setEndRule(0, -1, 0);
            errln("Failed. No exception has been thrown for DOM -1 setEndRule");
        } catch(IllegalArgumentException e) {
            logln("(h) " + e.getMessage());
        }
    }

    /**
     * SimpleTimeZone.getOffset() throws IllegalArgumentException when to get
     * of 2/29/1996 (leap day).
     */
    public void Test4208960 () {
	SimpleTimeZone tz = (SimpleTimeZone)TimeZone.getTimeZone("PST");
	try {
	    /*int offset =*/ tz.getOffset(GregorianCalendar.AD, 1996, Calendar.FEBRUARY, 29, 
				      Calendar.THURSDAY, 0);
        //offset = 0;
	} catch (IllegalArgumentException e) {
	    errln("FAILED: to get TimeZone.getOffset(2/29/96)");
	}
	try {
	    /*int offset =*/ tz.getOffset(GregorianCalendar.AD, 1997, Calendar.FEBRUARY, 29, 
				      Calendar.THURSDAY, 0);
	    //offset = 0;
	    errln("FAILED: TimeZone.getOffset(2/29/97) expected to throw Exception.");
	} catch (IllegalArgumentException e) {
	    logln("got IllegalArgumentException");
	}
    }

    /**
     * Test to see if DateFormat understands zone equivalency groups.  It
     * might seem that this should be a DateFormat test, but it's really a
     * TimeZone test -- the changes to DateFormat are minor.
     *
     * We use two known, stable zones that shouldn't change much over time
     * -- America/Vancouver and America/Los_Angeles.  However, they MAY
     * change at some point -- if that happens, replace them with any two
     * zones in an equivalency group where one zone has localized name
     * data, and the other doesn't, in some locale.
     */
    public void TestJ449() {
        // not used String str;

        // Modify the following three as necessary.  The two IDs must
        // specify two zones in the same equivalency group.  One must have
        // locale data in 'loc'; the other must not.
        String idWithLocaleData = "America/Los_Angeles";
        String idWithoutLocaleData = "America/Vancouver";
        Locale loc = new Locale("en", "", "");

        TimeZone zoneWith = TimeZone.getTimeZone(idWithLocaleData);
        TimeZone zoneWithout = TimeZone.getTimeZone(idWithoutLocaleData);
        // Make sure we got valid zones
        if (!(zoneWith.getID().equals(idWithLocaleData) &&
              zoneWithout.getID().equals(idWithoutLocaleData))) {
            errln("Fail: Unable to create zones");
        } else {
            GregorianCalendar calWith = new GregorianCalendar(zoneWith);
            GregorianCalendar calWithout = new GregorianCalendar(zoneWithout);
            SimpleDateFormat fmt =
                new SimpleDateFormat("MMM d yyyy hh:mm a zzz", loc);
            Date date = new Date(0L);
            fmt.setCalendar(calWith);
            String strWith = fmt.format(date);
            fmt.setCalendar(calWithout);
            String strWithout = fmt.format(date);
            if (strWith.equals(strWithout)) {
                logln("Ok: " + idWithLocaleData + " -> " +
                      strWith + "; " + idWithoutLocaleData + " -> " +
                      strWithout);
            } else {
                errln("FAIL: " + idWithLocaleData + " -> " +
                      strWith + "; " + idWithoutLocaleData + " -> " +
                      strWithout);
            }
        }
    }
}

//eof
