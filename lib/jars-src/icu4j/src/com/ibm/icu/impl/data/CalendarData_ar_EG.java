/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/CalendarData_ar_EG.java,v $
 * $Date: 2002/08/13 23:36:19 $
 * $Revision: 1.3 $
 *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

// Arabic, Egypt
public class CalendarData_ar_EG extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            { "Weekend",
                new String[] { // Friday:Saturday
                    "6", "0", // onset dow, millis in day
                    "1", "0"  // cease dow, millis in day
                }
            },
        };
    }
}
