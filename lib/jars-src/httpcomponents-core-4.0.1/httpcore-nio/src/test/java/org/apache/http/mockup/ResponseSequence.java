/*
 * $HeadURL: https://svn.apache.org/repos/asf/httpcomponents/httpcore/tags/4.0.1/httpcore-nio/src/test/java/org/apache/http/mockup/ResponseSequence.java $
 * $Revision: 749050 $
 * $Date: 2009-03-01 17:01:15 +0100 (Sun, 01 Mar 2009) $
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.mockup;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;

public class ResponseSequence {

    private final List<HttpResponse> data;
    
    public ResponseSequence() {
        super();
        this.data = new ArrayList<HttpResponse>();
    }
    
    public void addResponse(final HttpResponse response) {
        this.data.add(response);
    }
    
    public int size() {
        return this.data.size();
    }

    public HttpResponse getResponse(int index) {
        return this.data.get(index);
    }
    
}
