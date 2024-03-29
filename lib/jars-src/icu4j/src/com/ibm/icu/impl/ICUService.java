/**
 *******************************************************************************
 * Copyright (C) 2001-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/ICUService.java,v $
 * $Date: 2003/06/03 18:49:32 $
 * $Revision: 1.18 $
 *
 *******************************************************************************
 */
package com.ibm.icu.impl;

//import com.ibm.icu.text.Collator;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <p>A Service provides access to service objects that implement a
 * particular service, e.g. transliterators.  Users provide a String
 * id (for example, a locale string) to the service, and get back an
 * object for that id.  Service objects can be any kind of object.
 * The service object is cached and returned for later queries, so
 * generally it should not be mutable, or the caller should clone the
 * object before modifying it.</p>
 *
 * <p>Services 'canonicalize' the query id and use the canonical id to
 * query for the service.  The service also defines a mechanism to
 * 'fallback' the id multiple times.  Clients can optionally request
 * the actual id that was matched by a query when they use an id to
 * retrieve a service object.</p>
 *
 * <p>Service objects are instantiated by Factory objects registered with
 * the service.  The service queries each Factory in turn, from most recently
 * registered to earliest registered, until one returns a service object.
 * If none responds with a service object, a fallback id is generated,
 * and the process repeats until a service object is returned or until
 * the id has no further fallbacks.</p>
 *
 * <p>Factories can be dynamically registered and unregistered with the
 * service.  When registered, a Factory is installed at the head of
 * the factory list, and so gets 'first crack' at any keys or fallback
 * keys.  When unregistered, it is removed from the service and can no
 * longer be located through it.  Service objects generated by this
 * factory and held by the client are unaffected.</p>
 *
 * <p>ICUService uses Keys to query factories and perform
 * fallback.  The Key defines the canonical form of the id, and
 * implements the fallback strategy.  Custom Keys can be defined that
 * parse complex IDs into components that Factories can more easily
 * use.  The Key can cache the results of this parsing to save
 * repeated effort.  ICUService provides convenience APIs that
 * take Strings and generate default Keys for use in querying.</p>
 *
 * <p>ICUService provides API to get the list of ids publicly
 * supported by the service (although queries aren't restricted to
 * this list).  This list contains only 'simple' IDs, and not fully
 * unique ids.  Factories are associated with each simple ID and
 * the responsible factory can also return a human-readable localized
 * version of the simple ID, for use in user interfaces.  ICUService
 * can also provide a sorted collection of the all the localized visible
 * ids.</p>
 *
 * <p>ICUService implements ICUNotifier, so that clients can register
 * to receive notification when factories are added or removed from
 * the service.  ICUService provides a default EventListener subinterface,
 * ServiceListener, which can be registered with the service.  When
 * the service changes, the ServiceListener's serviceChanged method
 * is called, with the service as the only argument.</p>
 *
 * <p>The ICUService API is both rich and generic, and it is expected
 * that most implementations will statically 'wrap' ICUService to
 * present a more appropriate API-- for example, to declare the type
 * of the objects returned from get, to limit the factories that can
 * be registered with the service, or to define their own listener
 * interface with a custom callback method.  They might also customize
 * ICUService by overriding it, for example, to customize the Key and
 * fallback strategy.  ICULocaleService is a customized service that
 * uses Locale names as ids and uses Keys that implement the standard
 * resource bundle fallback strategy.<p>
 */
public class ICUService extends ICUNotifier {
    /**
     * Name used for debugging.
     */
    protected final String name;

    /**
     * Constructor.
     */
    public ICUService() {
        name = "";
    }

    /**
     * Construct with a name (useful for debugging).
     */
    public ICUService(String name) {
        this.name = name;
    }

    /**
     * Access to factories is protected by a read-write lock.  This is
     * to allow multiple threads to read concurrently, but keep
     * changes to the factory list atomic with respect to all readers.
     */
    private final ICURWLock factoryLock = new ICURWLock();

    /**
     * All the factories registered with this service.
     */
    private final List factories = new ArrayList();

    /**
     * Record the default number of factories for this service.
     * Can be set by markDefault.
     */
    private int defaultSize = 0;

    /**
     * Keys are used to communicate with factories to generate an
     * instance of the service.  Keys define how ids are
     * canonicalized, provide both a current id and a current
     * descriptor to use in querying the cache and factories, and
     * determine the fallback strategy.</p>
     *
     * <p>Keys provide both a currentDescriptor and a currentID.
     * The descriptor contains an optional prefix, followed by '/'
     * and the currentID.  Factories that handle complex keys,
     * for example number format factories that generate multiple
     * kinds of formatters for the same locale, use the descriptor
     * to provide a fully unique identifier for the service object,
     * while using the currentID (in this case, the locale string),
     * as the visible IDs that can be localized.
     *
     * <p> The default implementation of Key has no fallbacks and
     * has no custom descriptors.</p>
     */
    public static class Key {
        private final String id;

        /**
         * Construct a key from an id.
         */
        public Key(String id) {
            this.id = id;
        }

        /**
         * Return the original ID used to construct this key.
         */
        public final String id() {
            return id;
        }

        /**
         * Return the canonical version of the original ID.  This implementation
         * returns the original ID unchanged.
         */
        public String canonicalID() {
            return id;
        }

        /**
         * Return the (canonical) current ID.  This implementation
         * returns the canonical ID.
         */
        public String currentID() {
            return canonicalID();
        }

        /**
         * Return the current descriptor.  This implementation returns
         * the current ID.  The current descriptor is used to fully
         * identify an instance of the service in the cache.  A
         * factory may handle all descriptors for an ID, or just a
         * particular descriptor.  The factory can either parse the
         * descriptor or use custom API on the key in order to
         * instantiate the service.
         */
        public String currentDescriptor() {
            return "/" + currentID();
        }

        /**
         * If the key has a fallback, modify the key and return true,
         * otherwise return false.  The current ID will change if there
         * is a fallback.  No currentIDs should be repeated, and fallback
         * must eventually return false.  This implmentation has no fallbacks
         * and always returns false.
         */
        public boolean fallback() {
            return false;
        }

        /**
         * If a key created from id would eventually fallback to match the
         * canonical ID of this key, return true.
         */
        public boolean isFallbackOf(String id) {
            return canonicalID().equals(id);
        }
    }

    /**
     * Factories generate the service objects maintained by the
     * service.  A factory generates a service object from a key,
     * updates id->factory mappings, and returns the display name for
     * a supported id.
     */
    public static interface Factory {

        /**
         * Create a service object from the key, if this factory
         * supports the key.  Otherwise, return null.
         *
         * <p>If the factory supports the key, then it can call
         * the service's getKey(Key, String[], Factory) method
         * passing itself as the factory to get the object that
         * the service would have created prior to the factory's
         * registration with the service.  This can change the
         * key, so any information required from the key should
         * be extracted before making such a callback.
         */
        public Object create(Key key, ICUService service);

        /**
         * Update the result IDs (not descriptors) to reflect the IDs
         * this factory handles.  This function and getDisplayName are
         * used to support ICUService.getDisplayNames.  Basically, the
         * factory has to determine which IDs it will permit to be
         * available, and of those, which it will provide localized
         * display names for.  In most cases this reflects the IDs that
         * the factory directly supports.
         */
        public void updateVisibleIDs(Map result);

        /**
         * Return the display name for this id in the provided locale.
         * This is an localized id, not a descriptor.  If the id is
         * not visible or not defined by the factory, return null.
         * If locale is null, return id unchanged.
         */
        public String getDisplayName(String id, Locale locale);
    }

    /**
     * A default implementation of factory.  This provides default
     * implementations for subclasses, and implements a singleton
     * factory that matches a single id  and returns a single
     * (possibly deferred-initialized) instance.  This implements
     * updateVisibleIDs to add a mapping from its ID to itself
     * if visible is true, or to remove any existing mapping
     * for its ID if visible is false.
     */
    public static class SimpleFactory implements Factory {
        protected Object instance;
        protected String id;
        protected boolean visible;

        /**
         * Convenience constructor that calls SimpleFactory(Object, String, boolean)
         * with visible true.
         */
        public SimpleFactory(Object instance, String id) {
            this(instance, id, true);
        }

        /**
         * Construct a simple factory that maps a single id to a single
         * service instance.  If visible is true, the id will be visible.
         * Neither the instance nor the id can be null.
         */
        public SimpleFactory(Object instance, String id, boolean visible) {
            if (instance == null || id == null) {
                throw new IllegalArgumentException("Instance or id is null");
            }
            this.instance = instance;
            this.id = id;
            this.visible = visible;
        }

        /**
         * Return the service instance if the factory's id is equal to
         * the key's currentID.  Service is ignored.
         */
        public Object create(Key key, ICUService service) {
            if (id.equals(key.currentID())) {
                return instance;
            }
            return null;
        }

        /**
         * If visible, adds a mapping from id -> this to the result,
         * otherwise removes id from result.
         */
        public void updateVisibleIDs(Map result) {
            if (visible) {
                result.put(id, this);
            } else {
                result.remove(id);
            }
        }

        /**
         * If this.id equals id, returns id regardless of locale,
         * otherwise returns null.  (This default implementation has
         * no localized id information.)
         */
        public String getDisplayName(String id, Locale locale) {
            return (visible && this.id.equals(id)) ? id : null;
        }

        /**
         * For debugging.
         */
        public String toString() {
            StringBuffer buf = new StringBuffer(super.toString());
            buf.append(", id: ");
            buf.append(id);
            buf.append(", visible: ");
            buf.append(visible);
            return buf.toString();
        }
    }

    /**
     * Convenience override for get(String, String[]). This uses
     * createKey to create a key for the provided descriptor.
     */
    public Object get(String descriptor) {
        return getKey(createKey(descriptor), null);
    }

    /**
     * Convenience override for get(Key, String[]).  This uses
     * createKey to create a key from the provided descriptor.
     */
    public Object get(String descriptor, String[] actualReturn) {
        if (descriptor == null) {
            throw new NullPointerException("descriptor must not be null");
        }
        return getKey(createKey(descriptor), actualReturn);
    }

    /**
     * Convenience override for get(Key, String[]).
     */
    public Object getKey(Key key) {
        return getKey(key, null);
    }

    /**
     * <p>Given a key, return a service object, and, if actualReturn
     * is not null, the descriptor with which it was found in the
     * first element of actualReturn.  If no service object matches
     * this key, return null, and leave actualReturn unchanged.</p>
     *
     * <p>This queries the cache using the key's descriptor, and if no
     * object in the cache matches it, tries the key on each
     * registered factory, in order.  If none generates a service
     * object for the key, repeats the process with each fallback of
     * the key, until either one returns a service object, or the key
     * has no fallback.</p>
     *
     * <p>If key is null, just returns null.</p>
     */
    public Object getKey(Key key, String[] actualReturn) {
        return getKey(key, actualReturn, null);
    }


    public Object getKey(Key key, String[] actualReturn, Factory factory) {
        if (factories.size() == 0) {
            return handleDefault(key, actualReturn);
        }

        boolean debug = false;
        if (debug) System.out.println("Service: " + name + " key: " + key);

        CacheEntry result = null;
        if (key != null) {
            try {
                // The factory list can't be modified until we're done,
                // otherwise we might update the cache with an invalid result.
                // The cache has to stay in synch with the factory list.
                factoryLock.acquireRead();

                Map cache = null;
                SoftReference cref = cacheref; // copy so we don't need to sync on this
                if (cref != null) {
                    cache = (Map)cref.get();
                }
                if (cache == null) {
                    // synchronized since additions and queries on the cache must be atomic
                    // they can be interleaved, though
                    cache = Collections.synchronizedMap(new HashMap());
                    cref = new SoftReference(cache);
                }

                String currentDescriptor = null;
                ArrayList cacheDescriptorList = null;
                boolean putInCache = false;

                int NDebug = 0;

                int startIndex = 0;
                int limit = factories.size();
                boolean cacheResult = true;
                if (factory != null) {
                    for (int i = 0; i < limit; ++i) {
                        if (factory == factories.get(i)) {
                            startIndex = i + 1;
                            break;
                        }
                    }
                    if (startIndex == 0) {
                        throw new InternalError("Factory " + factory + "not registered with service: " + this);
                    }
                    cacheResult = false;
                }

            outer:
                do {
                    currentDescriptor = key.currentDescriptor();
                    if (debug) System.out.println(name + "[" + NDebug++ + "] looking for: " + currentDescriptor);
                    result = (CacheEntry)cache.get(currentDescriptor);
                    if (result != null) {
                        if (debug) System.out.println(name + " found with descriptor: " + currentDescriptor);
                        break outer;
                    } else {
                        if (debug) System.out.println("did not find: " + currentDescriptor + " in cache");
                    }

                    // first test of cache failed, so we'll have to update
                    // the cache if we eventually succeed-- that is, if we're
                    // going to update the cache at all.
                    putInCache = cacheResult;

                    //  int n = 0;
                    int index = startIndex;
                    while (index < limit) {
                        Factory f = (Factory)factories.get(index++);
                        if (debug) System.out.println("trying factory[" + (index-1) + "] " + f.toString());
                        Object service = f.create(key, this);
                        if (service != null) {
                            result = new CacheEntry(currentDescriptor, service);
                            if (debug) System.out.println(name + " factory supported: " + currentDescriptor + ", caching");
                            break outer;
                        } else {
                            if (debug) System.out.println("factory did not support: " + currentDescriptor);
                        }
                    }

                    // prepare to load the cache with all additional ids that
                    // will resolve to result, assuming we'll succeed.  We
                    // don't want to keep querying on an id that's going to
                    // fallback to the one that succeeded, we want to hit the
                    // cache the first time next goaround.
                    if (cacheDescriptorList == null) {
                        cacheDescriptorList = new ArrayList(5);
                    }
                    cacheDescriptorList.add(currentDescriptor);

                } while (key.fallback());

                if (result != null) {
                    if (putInCache) {
                        cache.put(result.actualDescriptor, result);
                        if (cacheDescriptorList != null) {
                            Iterator iter = cacheDescriptorList.iterator();
                            while (iter.hasNext()) {
                                String desc = (String)iter.next();
                                if (debug) System.out.println(name + " adding descriptor: '" + desc + "' for actual: '" + result.actualDescriptor + "'");

                                cache.put(desc, result);
                            }
                        }
                        // Atomic update.  We held the read lock all this time
                        // so we know our cache is consistent with the factory list.
                        // We might stomp over a cache that some other thread
                        // rebuilt, but that's the breaks.  They're both good.
                        cacheref = cref;
                    }

                    if (actualReturn != null) {
                        // strip null prefix
                        if (result.actualDescriptor.indexOf("/") == 0) {
                            actualReturn[0] = result.actualDescriptor.substring(1);
                        } else {
                            actualReturn[0] = result.actualDescriptor;
                        }
                    }

                    if (debug) System.out.println("found in service: " + name);

                    return result.service;
                }
            }
            finally {
                factoryLock.releaseRead();
            }
        }

        if (debug) System.out.println("not found in service: " + name);

        return handleDefault(key, actualReturn);
    }
    private SoftReference cacheref;

    // Record the actual id for this service in the cache, so we can return it
    // even if we succeed later with a different id.
    private static final class CacheEntry {
        final String actualDescriptor;
        final Object service;
        CacheEntry(String actualDescriptor, Object service) {
            this.actualDescriptor = actualDescriptor;
            this.service = service;
        }
    }


    /**
     * Default handler for this service if no factory in the list
     * handled the key.
     */
    protected Object handleDefault(Key key, String[] actualIDReturn) {
        return null;
    }

    /**
     * Convenience override for getVisibleIDs(String) that passes null
     * as the fallback, thus returning all visible IDs.
     */
    public Set getVisibleIDs() {
        return getVisibleIDs(null);
    }

    /**
     * <p>Return a snapshot of the visible IDs for this service.  This
     * set will not change as Factories are added or removed, but the
     * supported ids will, so there is no guarantee that all and only
     * the ids in the returned set are visible and supported by the
     * service in subsequent calls.</p>
     *
     * <p>matchID is passed to createKey to create a key.  If the
     * key is not null, it is used to filter out ids that don't have
     * the key as a fallback.
     */
    public Set getVisibleIDs(String matchID) {
        Set result = getVisibleIDMap().keySet();

        Key fallbackKey = createKey(matchID);

        if (fallbackKey != null) {
            Set temp = new HashSet(result.size());
            Iterator iter = result.iterator();
            while (iter.hasNext()) {
                String id = (String)iter.next();
                if (fallbackKey.isFallbackOf(id)) {
                    temp.add(id);
                }
            }
            result = temp;
        }
        return result;
    }

    /**
     * Return a map from visible ids to factories.
     */
    private Map getVisibleIDMap() {
        Map idcache = null;
        SoftReference ref = idref;
        if (ref != null) {
            idcache = (Map)ref.get();
        }
        while (idcache == null) {
            synchronized (this) { // or idref-only lock?
                if (ref == idref || idref == null) {
                    // no other thread updated idref before we got the lock, so
                    // grab the factory list and update it ourselves
                    try {
                        factoryLock.acquireRead();
                        idcache = new HashMap();
                        ListIterator lIter = factories.listIterator(factories.size());
                        while (lIter.hasPrevious()) {
                            Factory f = (Factory)lIter.previous();
                            f.updateVisibleIDs(idcache);
                        }
                        idcache = Collections.unmodifiableMap(idcache);
                        idref = new SoftReference(idcache);
                    }
                    finally {
                        factoryLock.releaseRead();
                    }
                } else {
                    // another thread updated idref, but gc may have stepped
                    // in and undone its work, leaving idcache null.  If so,
                    // retry.
                    ref = idref;
                    idcache = (Map)ref.get();
                }
            }
        }

        return idcache;
    }
    private SoftReference idref;

    /**
     * Convenience override for getDisplayName(String, Locale) that
     * uses the current default locale.
     */
    public String getDisplayName(String id) {
        return getDisplayName(id, Locale.getDefault());
    }

    /**
     * Given a visible id, return the display name in the requested locale.
     * If there is no directly supported id corresponding to this id, return
     * null.
     */
    public String getDisplayName(String id, Locale locale) {
        Map m = getVisibleIDMap();
        Factory f = (Factory)m.get(id);
        return f != null ? f.getDisplayName(id, locale) : null;
    }

    /**
     * Convenience override of getDisplayNames(Locale, Comparator, String) that 
     * uses the current default Locale as the locale, null as
     * the comparator, and null for the matchID.
     */
    public SortedMap getDisplayNames() {
        Locale locale = Locale.getDefault();
        return getDisplayNames(locale, null, null);
    }

    /**
     * Convenience override of getDisplayNames(Locale, Comparator, String) that
     * uses null for the comparator, and null for the matchID.
     */
    public SortedMap getDisplayNames(Locale locale) {
        return getDisplayNames(locale, null, null);
    }

    /**
     * Convenience override of getDisplayNames(Locale, Comparator, String) that
     * uses null for the matchID, thus returning all display names.
     */
    public SortedMap getDisplayNames(Locale locale, Comparator com) {
        return getDisplayNames(locale, com, null);
    }

    /**
     * Convenience override of getDisplayNames(Locale, Comparator, String) that
     * uses null for the comparator.
     */
    public SortedMap getDisplayNames(Locale locale, String matchID) {
        return getDisplayNames(locale, null, matchID);
    }

    /**
     * Return a snapshot of the mapping from display names to visible
     * IDs for this service.  This set will not change as factories
     * are added or removed, but the supported ids will, so there is
     * no guarantee that all and only the ids in the returned map will
     * be visible and supported by the service in subsequent calls,
     * nor is there any guarantee that the current display names match
     * those in the set.  The display names are sorted based on the
     * comparator provided.
     */
    public SortedMap getDisplayNames(Locale locale, Comparator com, String matchID) {
        SortedMap dncache = null;
        LocaleRef ref = dnref;

        if (ref != null) {
            dncache = (SortedMap)ref.get(locale, com);
        }

        while (dncache == null) {
            synchronized (this) {
                if (ref == dnref || dnref == null) {
                    dncache = new TreeMap(com); // sorted
                    
                    Map m = getVisibleIDMap();
                    Iterator ei = m.entrySet().iterator();
                    while (ei.hasNext()) {
                        Entry e = (Entry)ei.next();
                        String id = (String)e.getKey();
                        Factory f = (Factory)e.getValue();
                        dncache.put(f.getDisplayName(id, locale), id);
                    }

                    dncache = Collections.unmodifiableSortedMap(dncache);
                    dnref = new LocaleRef(dncache, locale, com);
                } else {
                    ref = dnref;
                    dncache = ref.get(locale, com);
                }
            }
        }

        Key matchKey = createKey(matchID);
        if (matchKey == null) {
            return dncache;
        }

        SortedMap result = new TreeMap(dncache);
        Iterator iter = result.entrySet().iterator();
        while (iter.hasNext()) {
            Entry e = (Entry)iter.next();
            if (!matchKey.isFallbackOf((String)e.getValue())) {
                iter.remove();
            }
        }
        return result;
    }

    // we define a class so we get atomic simultaneous access to the
    // locale, comparator, and corresponding map.
    private static class LocaleRef {
        private final Locale locale;
        private SoftReference ref;
        private Comparator com;

        LocaleRef(Map dnCache, Locale locale, Comparator com) {
            this.locale = locale;
            this.com = com;
            this.ref = new SoftReference(dnCache);
        }


        SortedMap get(Locale locale, Comparator com) {
            SortedMap m = (SortedMap)ref.get();
            if (m != null &&
                this.locale.equals(locale) &&
                (this.com == com || (this.com != null && this.com.equals(com)))) {

                return m;
            }
            return null;
        }
    }
    private LocaleRef dnref;

    /**
     * Return a snapshot of the currently registered factories.  There
     * is no guarantee that the list will still match the current
     * factory list of the service subsequent to this call.
     */
    public final List factories() {
        try {
            factoryLock.acquireRead();
            return new ArrayList(factories);
        }
        finally{
            factoryLock.releaseRead();
        }
    }

    /**
     * A convenience override of registerObject(Object, String, boolean)
     * that defaults visible to true.
     */
    public Factory registerObject(Object obj, String id) {
        return registerObject(obj, id, true);
    }

    /**
     * Register an object with the provided id.  The id will be
     * canonicalized.  The canonicalized ID will be returned by
     * getVisibleIDs if visible is true.
     */
    public Factory registerObject(Object obj, String id, boolean visible) {
        String canonicalID = createKey(id).canonicalID();
        return registerFactory(new SimpleFactory(obj, canonicalID, visible));
    }

    /**
     * Register a Factory.  Returns the factory if the service accepts
     * the factory, otherwise returns null.  The default implementation
     * accepts all factories.
     */
    public final Factory registerFactory(Factory factory) {
        if (factory == null) {
            throw new NullPointerException();
        }
        try {
            factoryLock.acquireWrite();
            factories.add(0, factory);
            clearCaches();
        }
        finally {
            factoryLock.releaseWrite();
        }
        notifyChanged();
        return factory;
    }

    /**
     * Unregister a factory.  The first matching registered factory will
     * be removed from the list.  Returns true if a matching factory was
     * removed.
     */
    public final boolean unregisterFactory(Factory factory) {
        if (factory == null) {
            throw new NullPointerException();
        }

        boolean result = false;
        try {
            factoryLock.acquireWrite();
            if (factories.remove(factory)) {
                result = true;
                clearCaches();
            }
        }
        finally {
            factoryLock.releaseWrite();
        }

        if (result) {
            notifyChanged();
        }
        return result;
    }

    /**
     * Reset the service to the default factories.  The factory
     * lock is acquired and then reInitializeFactories is called.
     */
    public final void reset() {
        try {
            factoryLock.acquireWrite();
            reInitializeFactories();
            clearCaches();
        }
        finally {
            factoryLock.releaseWrite();
        }
        notifyChanged();
    }

    /**
     * Reinitialize the factory list to its default state.  By default
     * this clears the list.  Subclasses can override to provide other
     * default initialization of the factory list.  Subclasses must
     * not call this method directly, as it must only be called while
     * holding write access to the factory list.
     */
    protected void reInitializeFactories() {
        factories.clear();
    }

    /**
     * Return true if the service is in its default state.  The default
     * implementation returns true if there are no factories registered.
     */
    public boolean isDefault() {
        return factories.size() == defaultSize;
    }

    /**
     * Set the default size to the current number of registered factories.
     * Used by subclasses to customize the behavior of isDefault.
     */
    protected void markDefault() {
        defaultSize = factories.size();
    }

    /**
     * Create a key from an id.  This creates a Key instance.
     * Subclasses can override to define more useful keys appropriate
     * to the factories they accept.  If id is null, returns null.
     */
    public Key createKey(String id) {
        return id == null ? null : new Key(id);
    }

    /**
     * Clear caches maintained by this service.  Subclasses can
     * override if they implement additional that need to be cleared
     * when the service changes. Subclasses should generally not call
     * this method directly, as it must only be called while
     * synchronized on this.
     */
    protected void clearCaches() {
        // we don't synchronize on these because methods that use them
        // copy before use, and check for changes if they modify the
        // caches.
        cacheref = null;
        idref = null;
        dnref = null;
    }

    /**
     * Clears only the service cache.
     * This can be called by subclasses when a change affects the service
     * cache but not the id caches, e.g., when the default locale changes
     * the resolution of ids changes, but not the visible ids themselves.
     */
    protected void clearServiceCache() {
        cacheref = null;
    }

    /**
     * ServiceListener is the listener that ICUService provides by default.
     * ICUService will notifiy this listener when factories are added to
     * or removed from the service.  Subclasses can provide
     * different listener interfaces that extend EventListener, and modify
     * acceptsListener and notifyListener as appropriate.
     */
    public static interface ServiceListener extends EventListener {
        public void serviceChanged(ICUService service);
    }

    /**
     * Return true if the listener is accepted; by default this
     * requires a ServiceListener.  Subclasses can override to accept
     * different listeners.
     */
    protected boolean acceptsListener(EventListener l) {
        return l instanceof ServiceListener;
    }

    /**
     * Notify the listener, which by default is a ServiceListener.
     * Subclasses can override to use a different listener.
     */
    protected void notifyListener(EventListener l) {
        ((ServiceListener)l).serviceChanged(this);
    }

    /**
     * Return a string describing the statistics for this service.
     * This also resets the statistics. Used for debugging purposes.
     */
    public String stats() {
        ICURWLock.Stats stats = factoryLock.resetStats();
        if (stats != null) {
            return stats.toString();
        }
        return "no stats";
    }

    /**
     * Return the name of this service. This will be the empty string if none was assigned.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the result of super.toString, appending the name in curly braces.
     */
    public String toString() {
        return super.toString() + "{" + name + "}";
    }
}
