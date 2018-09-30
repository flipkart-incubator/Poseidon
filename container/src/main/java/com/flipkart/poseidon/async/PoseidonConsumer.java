package com.flipkart.poseidon.async;

import com.flipkart.poseidon.api.Application;
import com.flipkart.poseidon.api.Configuration;
import com.flipkart.poseidon.core.PoseidonAsyncRequest;
import com.flipkart.poseidon.core.PoseidonRequest;
import com.flipkart.poseidon.core.PoseidonResponse;
import com.flipkart.poseidon.core.PoseidonServlet;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;

import static com.flipkart.poseidon.constants.RequestConstants.BODY;
import static com.flipkart.poseidon.constants.RequestConstants.BODY_BYTES;
import static com.flipkart.poseidon.constants.RequestConstants.METHOD;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by shrey.garg on 29/09/18.
 */
public abstract class PoseidonConsumer {
    private static final Logger logger = getLogger(PoseidonConsumer.class);
    private final Application application;
    private final Configuration configuration;

    public PoseidonConsumer(Application application, Configuration configuration) {
        this.application = application;
        this.configuration = configuration;
    }

    public final AsyncConsumerResult consume(AsyncConsumerRequest consumerRequest) {
        PoseidonRequest request = new PoseidonAsyncRequest(consumerRequest.getUrl(), Collections.emptyMap(), Collections.emptyMap(), consumerRequest.getParameters());
        request.setAttribute(METHOD, consumerRequest.getHttpMethod());
        request.setAttribute(BODY_BYTES, consumerRequest.getPayload());

        try {
            PoseidonResponse response = new PoseidonResponse();
            this.application.handleRequest(request, response);
            if (response.getStatusCode() / 100 == 2) {
                return new AsyncConsumerResult(AsyncResultState.SUCCESS);
            } else {
                return new AsyncConsumerResult(AsyncResultState.SIDELINE);
            }
        } catch (Throwable throwable) {
            logger.error("Unexpected exception while consuming async event", throwable);
            return new AsyncConsumerResult(AsyncResultState.FAILURE);
        }
    }
}
