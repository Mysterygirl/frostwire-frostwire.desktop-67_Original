package com.ibm.icu.dev.test.normalizer;

import java.util.Hashtable;

/**
 * Integer hash table. Uses Java Hashtable for now.
 * @author Mark Davis
 */
 
public class IntHashtable {
    static final String copyright = "Copyright � 1998-1999 Unicode, Inc.";
    
    public IntHashtable (int defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public void put(int key, int value) {
        if (value == defaultValue) {
            table.remove(new Integer(key));
        } else {
            table.put(new Integer(key), new Integer(value));
        }
    }
    
    public int get(int key) {
        Object value = table.get(new Integer(key));
        if (value == null) return defaultValue;
        return ((Integer)value).intValue();
    }
    
    private int defaultValue;
    private Hashtable table = new Hashtable();
}