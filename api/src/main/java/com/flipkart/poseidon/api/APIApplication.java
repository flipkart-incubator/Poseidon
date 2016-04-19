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

package com.flipkart.poseidon.api;

import com.codahale.metrics.Timer;
import com.flipkart.poseidon.constants.RequestConstants;
import com.flipkart.poseidon.core.PoseidonRequest;
import com.flipkart.poseidon.core.PoseidonResponse;
import com.google.common.net.MediaType;
import flipkart.lego.api.exceptions.BadRequestException;
import flipkart.lego.api.exceptions.ElementNotFoundException;
import flipkart.lego.api.exceptions.InternalErrorException;
import flipkart.lego.api.exceptions.ProcessingException;
import flipkart.lego.engine.Lego;
import org.springframework.http.HttpMethod;

import java.util.concurrent.ExecutorService;

import static com.flipkart.poseidon.constants.RequestConstants.TIMER_CONTEXT;
import static com.google.common.net.MediaType.JSON_UTF_8;

/**
 * Created by mohan.pandian on 05/11/15.
 */
public class APIApplication implements Application {
    private final APIManager apiManager;
    protected final APILegoSet legoSet;
    private Lego lego;

    public APIApplication(APIManager apiManager, APILegoSet legoSet) {
        this.apiManager = apiManager;
        this.legoSet = legoSet;
    }

    @Override
    public void init(ExecutorService datasourceTPE, ExecutorService filterTPE) {
        lego = new Lego(legoSet, datasourceTPE, filterTPE);
        legoSet.setDataSourceExecutor(datasourceTPE);
        apiManager.init();
    }

    @Override
    public void handleRequest(PoseidonRequest request, PoseidonResponse response) throws ElementNotFoundException, BadRequestException, ProcessingException, InternalErrorException {
        HttpMethod method = request.getAttribute(RequestConstants.METHOD);
        boolean handled = false;
        if (method != null && method.equals(HttpMethod.OPTIONS)) {
            handled = handleOptionsRequest(request, response);
        }

        if (!handled) {
            // Ideally, we have to get the buildable for this request, get
            // API name from the buildable and use it to start a meter here.
            // As getting a buildable could be time consuming (say we use trie
            // instead of a map as in APIBuildable), we start a meter in
            // APILegoSet.getBuildable() and stop it here
            try {
                lego.buildResponse(request, response);
            } finally {
                Object timerContext = request.getAttribute(TIMER_CONTEXT);
                if (timerContext != null && timerContext instanceof Timer.Context) {
                    ((Timer.Context) timerContext).stop();
                }
            }
        }
    }

    /**
     * return true if providing a custom implementation for OPTION calls
     */
    public boolean handleOptionsRequest(PoseidonRequest req, PoseidonResponse resp) throws ElementNotFoundException, BadRequestException, ProcessingException, InternalErrorException {
        return false;
    }

    @Override
    public MediaType getDefaultMediaType() {
        return JSON_UTF_8;
    }
}
