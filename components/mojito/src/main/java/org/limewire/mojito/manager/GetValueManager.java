/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.limewire.mojito.manager;

import org.limewire.mojito.Context;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureTask;
import org.limewire.mojito.handler.response.GetValueResponseHandler;
import org.limewire.mojito.result.FindValueResult;


/**
 * GetValueManager is, in the widest sense, equivalent to <code>FindValueManager</code>
 * but the main difference is that it's used to retrieve the value
 * rather than to find it. That means you must know which Node is storing
 * a value.
 */
public class GetValueManager extends AbstractManager<FindValueResult> {

    public GetValueManager(Context context) {
        super(context);
    }
    
    /**
     * Tries to get one or more values from the remote Node.
     */
    public DHTFuture<FindValueResult> get(EntityKey entityKey) {
        
        GetValueResponseHandler handler 
            = new GetValueResponseHandler(context, entityKey);
        GetValueFuture future = new GetValueFuture(handler);
        context.getDHTExecutorService().execute(future);
        return future;
    }
    
    /**
     * A "get value" specific implementation of DHTFuture.
     */
    private class GetValueFuture extends DHTFutureTask<FindValueResult> {

        public GetValueFuture(GetValueResponseHandler callable) {
            super(context, callable);
        }
    }
}
