package com.ibm.icu.dev.test.normalizer;

import java.util.Hashtable;

/**
 * Hashtable storing ints addressed by longs. Used
 * for storing of composition data. Uses Java Hashtable
 * for now.
 * @author Vladimir Weinstein
 */
public class LongHashtable {
    static final String copyright = "Copyright � 2002 Unicode, Inc.";
    
    public LongHashtable (int defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public void put(long key, int value) {
        if (value == defaultValue) {
            table.remove(new Long(key));
        } else {
            table.put(new Long(key), new Integer(value));
        }
    }
    
    public int get(long key) {
        Object value = table.get(new Long(key));
        if (value == null) return defaultValue;
        return ((Integer)value).intValue();
    }
    
    private int defaultValue;
    private Hashtable table = new Hashtable();

}
