package org.limewire.listener;

import org.apache.commons.logging.Log;
import org.limewire.listener.EventListenerList.EventListenerListContext;


/**
 * A Multicaster that caches the last event it handles or broadcasts.
 *
 * The cached event is used for two purposes:
 *
 * 1. New listeners who are added will have their handleEvent(E event)
 * method called with the cached event
 *
 * 2. When broadcast(E event) and handleEvent(E event) are called
 * on the CacheingEventMulticaster, the event is only broadcast
 * if it is not equal to the cached event.  Event classes should override equals()
 * for this to provide any meaningful implementation
 *
 * @param <E>
 */
public class CachingEventMulticasterImpl<E> implements CachingEventMulticaster<E> {
    
	private final EventListenerListContext listenerContext;
    private final EventMulticaster<E> multicaster;
    private final BroadcastPolicy broadcastPolicy;
    private final Object LOCK = new Object();
    
    private volatile E cachedEvent;
    
    public CachingEventMulticasterImpl() {
        this(BroadcastPolicy.ALWAYS, new EventMulticasterImpl<E>());    
    }
    
    public CachingEventMulticasterImpl(Log log) {
        this(BroadcastPolicy.ALWAYS, log);
    }
    
    public CachingEventMulticasterImpl(BroadcastPolicy broadcastPolicy) {
        this(broadcastPolicy, new EventMulticasterImpl<E>());
    }
    
    public CachingEventMulticasterImpl(BroadcastPolicy broadcastPolicy, Log log) {
        this(broadcastPolicy, new EventMulticasterImpl<E>(log));
    }
    
    public CachingEventMulticasterImpl(BroadcastPolicy broadcastPolicy, EventMulticaster<E> multicaster) {
        this.broadcastPolicy = broadcastPolicy;
        this.multicaster = multicaster;
        this.listenerContext = multicaster.getListenerContext();
    }

    @Override
    /**
     * Adds a listener and calls its handleEvent() method with the
     * most recent Event, if any.
     */
    public void addListener(EventListener<E> eEventListener) {
        E copy = cachedEvent;
        if(copy != null) {
            // An alternate way to do this would be to add some kind of notifyListener(EventListener, Event)                                                                                                                                
            // method/interface, similar to EventListenerList#notifyListener, that would internally                                                                                                                                         
            // use the context.  This would remove the need to pass a context to this class,                                                                                                                                                
            // but would require a more difficult interface be implemented by multicasters.                                                                                                                                                 
            // Overall it's probably the better option to do it via notifyListener, because that would                                                                                                                                      
            // also allow the multicaster to control how the event is broadcast, but harder to fit                                                                                                                                          
            // into the existing multicaster impls.                                                                                                                                                                                         
            EventListenerList.dispatch(eEventListener, copy, listenerContext);
        }
    	
//    	if(cachedEvent != null) {
//            EventListenerList.dispatch(eEventListener, cachedEvent);
//        }
        multicaster.addListener(eEventListener);
    }

    @Override
    public boolean removeListener(EventListener<E> eEventListener) {
        return multicaster.removeListener(eEventListener);
    }

    @Override
    public void handleEvent(E event) {
        broadcast(event);
    }

    @Override
    public void broadcast(E event) {
        assert eventConsistentWithBroadcastPolicy(event);
        
        boolean broadcast = false;
        synchronized(LOCK) {
            if(cachedEvent == null ||
                    broadcastPolicy == BroadcastPolicy.ALWAYS ||
                    !cachedEvent.equals(event)) {
                cachedEvent = event;
                broadcast = true;             
            }
        }
        
        if(broadcast) {
            multicaster.broadcast(event);
        }
    }

    private boolean eventConsistentWithBroadcastPolicy(E event) {
        return broadcastPolicy == BroadcastPolicy.ALWAYS ||
                event.getClass().isEnum() ||
                System.identityHashCode(event) != event.hashCode(); // other case caching won't work
    }

    @Override
    public E getLastEvent() {
        return cachedEvent;
    }

	@Override
	public EventListenerListContext getListenerContext() {
		return listenerContext;
	}
}
