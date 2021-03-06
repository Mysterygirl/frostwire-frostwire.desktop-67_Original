/**
 *******************************************************************************
 * Copyright (C) 2001-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/util/ICUListResourceBundleTest.java,v $
 * $Date: 2003/10/07 17:24:18 $
 * $Revision: 1.7 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.ResourceBundle;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.Utility;


public final class ICUListResourceBundleTest extends TestFmwk 
{
    public static void main(String args[]) throws Exception {
		ICUListResourceBundleTest test = new ICUListResourceBundleTest();
	    test.run(args);

    }

    public void TestReferences() {
		ResourceBundle rb = ICULocaleData.getLocaleElements("th");
		logln("got rb:" + rb);
	
		byte[] binaryData = null;//(byte[])rb.getObject("%%CollationBin");
		Object colElem = rb.getObject("CollationElements");
        if(colElem instanceof Object[][]){
            Object[][] colElemArr = (Object[][])colElem;
            if(((String)colElemArr[0][0]).equals("%%CollationBin")){   
	           binaryData = (byte[]) colElemArr[0][1];
            }
        }
        logln("got binaryData: " + binaryData + " length: " + (binaryData == null ? 0 : binaryData.length));
		Object[] stringArrayData = (Object[])rb.getObject("CollationElements");
		//String[] collationData = new String[] {
		 //   (String)stringArrayData[0],
		 //   (String)stringArrayData[0]
		//};
		logln("got stringData: " + stringArrayData + " length: " + stringArrayData.length);
		logln("got stringDataElement: " + stringArrayData[0] + " length: " + stringArrayData.length);
		//System.out.println("got sdee: " + collationData[0]);
		//	System.out.println("char data length: " + stringArrayData.length());
    }
   
   String simpleAlias   = "Open";

   Object[][] zoneTests =  new Object[][]{
                               {
                                    "zoneAlias",
                                    new String[] { 
                                                    "PST",
                                                    "Pacific Standard Time",
                                                    "PST",
                                                    "Pacific Daylight Time",
                                                    "PDT",
                                                    "Los Angeles",
                                                },
                               },
                               {
                                   "zoneAlias1",
                                   new String[] { 
                                                   "America/Denver",
                                                   "Mountain Standard Time",
                                                   "MST",
                                                   "Mountain Daylight Time",
                                                   "MDT",
                                                   "Denver",
                        
                                               },
                               },
                               {
                                   "zoneAlias2",
                                   "America/Denver",
                               },
                           };
    Object[] zoneStrings = new Object[] {
                                new String[] { 
                                    
                                    "PST",
                                    "Pacific Standard Time",
                                    "PST",
                                    "Pacific Daylight Time",
                                    "PDT",
                                    "Los Angeles",
                                
                                },
                                new String[] { 
                                    "America/Los_Angeles",
                                    "Pacific Standard Time",
                                    "PST",
                                    "Pacific Daylight Time",
                                    "PDT",
                                    "Los Angeles",
                                
                                },
                            };
    Object[] testGetStringByIndexAliasing =  new String[] { 
                                                    "PST",
                                                    "Pacific Standard Time",
                                                    "PDT",
                                                    "Los Angeles",
                                                };
    Object[][] testGetStringByKeyAliasing = new Object[][]{
                                                {
                                                    "KeyAlias0PST",
                                                    "PST",
                                                },
                                                {
                                                    "KeyAlias1PacificStandardTime",
                                                    "Pacific Standard Time",
                                                },
                                                {
                                                    "KeyAlias2PDT",
                                                    "PDT",
                                                },
                                                {
                                                    "KeyAlias3LosAngeles",
                                                    "Los Angeles",
                                                },
                                            };                                          
    private boolean arrayEquals(Object[][] s1, Object[][] s2){
        boolean isEqual = true;
        if(s1.length != s2.length){
            return false;
        }
        for (int i = 0; i<s1.length; i++){
            if(s1[i] instanceof Object[][] &&
               s2[i] instanceof Object[][]){
                if(!arrayEquals((Object[][])s1[i],(Object[][]) s2[i])){
                   isEqual = false;
                }
            }else if (s1[i] instanceof Object[] &&
                      s2[i] instanceof Object[]){
                if(!Utility.arrayEquals(s1[i],s2[i])){
                    isEqual = false;
                }
           }else if(s1[i] instanceof Object && s2[i] instanceof Object){
               if(!s1[i].equals(s2[i])){
                   isEqual = false;
               }
           }else{
               isEqual = false;
               break;
           }
        }
        return isEqual;
    }
    public void TestAliases(){
        ResourceBundle rb = ICULocaleData.getResourceBundle("com.ibm.icu.dev.data","TestDataElements","testaliases");
        //rb.getObject("CollationElements");
        String s1 = rb.getString("simplealias");
        if(s1.equals(simpleAlias)){
            logln("Alias mechanism works for simplealias");
        }else{
            errln("Did not get the expected output for simplealias");
        }
        {
            // test aliasing through another alias
            s1 = rb.getString("referencingalias");
            ResourceBundle uk = ICULocaleData.getResourceBundle("com.ibm.icu.impl.data","LocaleElements","uk");
            Object o = uk.getObject("CollationElements");
            if(o instanceof Object[][]){
                Object[][] val = (Object[][]) o;
                if(s1.equals(val[1][1])){
                    logln("Alias mechanism works for referencingalias");
                }else{
                    errln("Did not get the expected result for referencingalias");
                }
            }else{
                errln("Did not get the expected result for CollationElements resource of uk bundle");
            }
            Object anotheralias = rb.getObject("anotheralias");
            if(anotheralias instanceof Object[][]&& o instanceof Object[][]){
                if(arrayEquals((Object[][])o, (Object[][])anotheralias)){
                    logln("Alias mechanism works for anotheralias");
                }else{
                    errln("Did not get the expected output for anotheralias");
                }
            
            }else{
                errln("Alias mechanism failed for anotheralias in TestAlias");
            }
            Object o1 = rb.getObject("CollationElements");
            if(o1 instanceof Object[][]&& o instanceof Object[][]){
                if(arrayEquals((Object[][])o, (Object[][])o1)){
                    logln("Alias mechanism works for CollationElements");
                }else{
                    errln("Did not get the expected output for CollationElements");
                }
            
            }else{
                errln("Alias mechanism failed for CollationElements in TestAlias");
            }
    
        }
                        
        Object o1 = rb.getObject("zoneTests");
        if(o1 instanceof Object[][]){
            if(arrayEquals(zoneTests, (Object[][])o1)){
                logln("Alias mechanism works for zoneTests");
            }else{
                errln("Did not get the expected output for zoneTests");
            }
            
        }else{
            errln("Alias mechanism failed for zoneTest in TestAlias");
        }
        o1 = rb.getObject("zoneStrings");
        if(o1 instanceof Object[]){
            if(Utility.arrayEquals(zoneStrings, (Object[])o1)){
                logln("Alias mechanism works for zoneStrings");
            }else{
                errln("Did not get the expected output for zoneStrings");
            }
            
        }else{
            errln("Alias mechanism failed for zoneStrings in TestAlias");
        }
        o1 = rb.getObject("testGetStringByIndexAliasing");
        if(o1 instanceof Object[]){
            if(Utility.arrayEquals(testGetStringByIndexAliasing, (Object[])o1)){
                logln("Alias mechanism works for testGetStringByIndexAliasing");
            }else{
                errln("Did not get the expected output for testGetStringByIndexAliasing");
            }
            
        }else{
            errln("Alias mechanism failed for testGetStringByIndexAliasing in TestAlias");
        }
        
        o1 = rb.getObject("testGetStringByKeyAliasing");
        if(o1 instanceof Object[][]){
            if(arrayEquals(testGetStringByKeyAliasing, (Object[][])o1)){
                logln("Alias mechanism works for testGetStringByKeyAliasing");
            }else{
                errln("Did not get the expected output for testGetStringByKeyAliasing");
            }
            
        }else{
            errln("Alias mechanism failed for testGetStringByKeyAliasing in TestAlias");
        }
        // should not get an exception
        rb = ICULocaleData.getResourceBundle("com.ibm.icu.impl.data","LocaleElements","fr_BE");
        Object o = rb.getObject("SpelloutRules");
        if(o instanceof String){
            logln("Alias mechanism works");
        }else{
            errln("Alias mechanism failed for fr_BE SpelloutRules");
        }
        rb = ICULocaleData.getResourceBundle("com.ibm.icu.impl.data","LocaleElements","zh_TW");
        o = rb.getObject("CollationElements");
        if(o instanceof Object[][]){
            Object[][] arr = (Object[][])o;
            if(((String)arr[0][0])== "%%CollationBin"){
                logln("Alias mechanism works");
            }else{
                errln("Alias mechanism failed for fr_BE SpelloutRules");
            }
        }
        
    }
    
    public void TestCircularAliases(){
        try{
	        ResourceBundle rb = ICULocaleData.getResourceBundle("com.ibm.icu.dev.data","TestDataElements","testcircularalias");
	       /* Object o =*/ rb.getObject("aaa");
        }catch(java.util.MissingResourceException e){
            if(e.toString().indexOf("ircular")==-1){
                errln("Did not get the expected Exception for circular aliases");
            }
            return;
        }
        errln("Did not get the expected Exception for circular aliases");
    }

}
