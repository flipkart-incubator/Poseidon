/*
 * Copyright 2016 Flipkart Internet, pvt ltd.
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

package com.flipkart.poseidon.sample.api.filters;

import com.flipkart.poseidon.constants.RequestConstants;
import com.flipkart.poseidon.core.PoseidonRequest;
import com.flipkart.poseidon.core.RequestContext;
import com.flipkart.poseidon.filters.AbstractFilter;
import com.flipkart.poseidon.model.annotations.Description;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;
import flipkart.lego.api.entities.Response;
import flipkart.lego.api.exceptions.BadRequestException;
import flipkart.lego.api.exceptions.InternalErrorException;
import flipkart.lego.api.exceptions.ProcessingException;

import java.util.Map;
import java.util.Optional;

@Name("BotFilter")
@Version(major = 1, minor = 0, patch = 0)
@Description("Detects and filters out bots in a dumb way")
public class BotFilter extends AbstractFilter {
    public BotFilter(LegoSet legoset) {
        super(legoset);
    }

    @Override
    public void filterRequest(Request request, Response response) throws InternalErrorException, BadRequestException, ProcessingException {
        String userAgent = ((PoseidonRequest) request).getHeader("User-Agent");
        if (userAgent != null && userAgent.contains("bot")) {
            Map<String, Object> endpointAnnotations = RequestContext.get(RequestConstants.API_ANNOTATIONS);
            String message = (String) Optional.ofNullable(endpointAnnotations.get("errorMessage")).orElse("Bots are not allowed");
            throw new BadRequestException(message);
        }
    }

    @Override
    public void filterResponse(Request request, Response response) throws InternalErrorException, BadRequestException, ProcessingException {
    }
}
