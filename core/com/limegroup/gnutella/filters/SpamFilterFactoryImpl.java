package com.limegroup.gnutella.filters;

import java.util.Vector;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.settings.FilterSettings;

@Singleton
public class SpamFilterFactoryImpl implements SpamFilterFactory {
    
    private final Provider<MutableGUIDFilter> mutableGUIDFilter;
    private final Provider<HostileFilter> hostileFilter;
    private final Provider<LocalIPFilter> ipFilter;

    @Inject
    public SpamFilterFactoryImpl(Provider<MutableGUIDFilter> mutableGUIDFilter, 
            Provider<HostileFilter> hostileFilter,
            Provider<LocalIPFilter> ipFilter) {
        this.mutableGUIDFilter = mutableGUIDFilter;
        this.hostileFilter = hostileFilter;
        this.ipFilter = ipFilter;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.filters.SpamFilterFactory#createPersonalFilter()
     */
    public SpamFilter createPersonalFilter() {
        
        Vector<SpamFilter> buf=new Vector<SpamFilter>();

        //1. IP-based techniques.
        LocalIPFilter ipFilter = this.ipFilter.get();
        if(ipFilter.hasBlacklistedHosts())
            buf.add(ipFilter);

        //2. Keyword-based techniques.
        String[] badWords = FilterSettings.BANNED_WORDS.getValue();

        boolean filterAdult = FilterSettings.FILTER_ADULT.getValue();
        boolean filterVbs = FilterSettings.FILTER_VBS.getValue();
        boolean filterHtml = FilterSettings.FILTER_HTML.getValue();
        boolean filterWMVASF = FilterSettings.FILTER_WMV_ASF.getValue();
        
        if (badWords.length!=0 || filterAdult || filterVbs || filterHtml) {
            KeywordFilter kf=new KeywordFilter();
            //kf.disallow("LimeWireWin");
	    kf.disallow("has shaking orgasm");
	    kf.disallow("teenfuns xxx");
            for (int i=0; i<badWords.length; i++)
                kf.disallow(badWords[i]);
            if (filterAdult)
                kf.disallowAdult();

            if (filterVbs)
                kf.disallowVbs();
            if (filterHtml)
                kf.disallowHtml();
            if (filterWMVASF)
                kf.disallowWMVASF();
            buf.add(kf);
        }

        //3. Spammy Replies
        SpamReplyFilter spf=new SpamReplyFilter();
        buf.add(spf);
        
        //4. Mutable GUID-based filters.
        buf.add(mutableGUIDFilter.get());

        return compose(buf);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.filters.SpamFilterFactory#createRouteFilter()
     */
    public SpamFilter createRouteFilter() {
        //Assemble spam filters. Order matters a little bit.
        
        Vector<SpamFilter>  buf=new Vector<SpamFilter> ();

        //1. Eliminate old LimeWire requeries.
        buf.add(new RequeryFilter());        

        //1b. Eliminate runaway Qtrax queries.
        buf.add(new GUIDFilter());

        //2. Duplicate-based techniques.
        if (FilterSettings.FILTER_DUPLICATES.getValue())
            buf.add(new DuplicateFilter());

        //3. Greedy queries.  Yes, this is a route filter issue.
        if (FilterSettings.FILTER_GREEDY_QUERIES.getValue())
            buf.add(new GreedyQueryFilter());

        //4. Queries containing hash urns.
        if (FilterSettings.FILTER_HASH_QUERIES.getValue())
            buf.add(new HashFilter());
        
        //4. BearShare high-bit queries.
        // if (FilterSettings.FILTER_HIGHBIT_QUERIES.getValue())
        //     buf.add(new BearShareFilter());
        
        // always filter hostiles
        buf.add(hostileFilter.get());

        return compose(buf);
    }

    /**
     * Returns a composite filter of the given filters.
     * @param filters a Vector of SpamFilter.
     */
    private static SpamFilter compose(Vector<? extends SpamFilter>  filters) {
        //As a minor optimization, we avoid a few method calls in
        //special cases.
        if (filters.size()==0)
            return new AllowFilter();
        else if (filters.size()==1)
            return filters.get(0);
        else {
            SpamFilter[] delegates=new SpamFilter[filters.size()];
            filters.copyInto(delegates);
            return new CompositeFilter(delegates);
        }
    }

}
