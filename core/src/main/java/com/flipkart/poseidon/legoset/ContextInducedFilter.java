/*
 * Copyright 2015 Flipkart Internet, pvt ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.poseidon.legoset;

import flipkart.lego.api.entities.Filter;
import flipkart.lego.api.entities.Request;
import flipkart.lego.api.entities.Response;
import flipkart.lego.api.exceptions.BadRequestException;
import flipkart.lego.api.exceptions.InternalErrorException;
import flipkart.lego.api.exceptions.ProcessingException;

import java.util.List;

/*
 * Induces all request contexts (like contexts used by Hystrix, Brave's DT, our own RequestContext)
 * into DataSource threads from Jetty threads
 */
public class ContextInducedFilter extends ContextInducedBlock implements Filter {

    private final Filter filter;

    public ContextInducedFilter(Filter filter) {
        super(filter);
        this.filter = filter;
    }

    @Override
    public void filterRequest(Request request, Response response) throws InternalErrorException, BadRequestException, ProcessingException {
        try {
            // We don't want to trace requests in filter traces. Hence pass null for request.
            initAllContext(null);
            filter.filterRequest(request, response);
            success = true;
        } finally {
            shutdownAllContext();
        }
    }

    @Override
    public void filterResponse(Request request, Response response) throws InternalErrorException, BadRequestException, ProcessingException {
        try {
            // We don't want to trace responses in filter traces. Hence pass null for request.
            initAllContext(null);
            filter.filterResponse(request, response);
            success = true;
        } finally {
            shutdownAllContext();
        }
    }

    @Override
    public String getId() throws UnsupportedOperationException {
        return filter.getId();
    }

    @Override
    public String getName() throws UnsupportedOperationException {
        return filter.getName();
    }

    @Override
    public List<Integer> getVersion() throws UnsupportedOperationException {
        return filter.getVersion();
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof ContextInducedFilter && o.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        return filter.hashCode();
    }

    @Override
    public String getShortDescription() {
        return filter.getShortDescription();
    }

    @Override
    public String getDescription() {
        return filter.getDescription();
    }
}
