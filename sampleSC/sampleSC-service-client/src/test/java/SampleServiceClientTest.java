package com.flipkart.poseidon.serviceclients.sampleSC.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.poseidon.serviceclients.FutureTaskResultToDomainObjectPromiseWrapper;
import flipkart.lego.api.exceptions.ElementNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedList;
import java.util.List;
import com.flipkart.poseidon.serviceclients.sampleSC.model.v1.*;
import com.flipkart.poseidon.serviceclients.sampleSC.service.client.v1.*;

/** This is just a sample test and provides a usage of models and service clients
 * to better test the implementation
 * */
public class SampleServiceClientTest {
    private SampleServiceClient serviceClient;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void test() {
        if (serviceClient == null)
            return;

        ObjectMapper mapper = new ObjectMapper();
        try {
            Integer userId = 1;
            List<String> userIdList = new LinkedList<>();
            userIdList.add(userId.toString());
            FutureTaskResultToDomainObjectPromiseWrapper<UserList>
                    userPromise = serviceClient.getUser(true, userIdList, "request-id-1");

            List<String> postIdList = new LinkedList<>();
            Integer postId = 1;
            postIdList.add(postId.toString());
            Post post = new Post();
            post.setUserId(userId);
            post.setTitle("Test Title");
            post.setBody("Test Body");
            if (!userPromise.isRealized()) {
                logger.info("Creating Post before realizing User promise");
            }
            post = serviceClient.createPost("request-id-2", true, post).get();
            logger.info("Created Post: {}", mapper.writeValueAsString(post));
            logger.info("User: {}", mapper.writeValueAsString(userPromise.get()));

            post.setTitle("Updated Test Title");
            post.setBody("Updated Test Body");
            post = serviceClient.updatePost(postIdList, "request-id-3", post).get();
            logger.info("Updated Post: {}", mapper.writeValueAsString(post));

            Posts posts =
                    serviceClient.getPostsOfUser(userIdList, new String[]{"dummy1", "dummy2"}, "request-id-4").get();
            logger.info("Posts count of User: " + posts.size());
            logger.info("Posts of User: {}", mapper.writeValueAsString(posts));

            posts = serviceClient.getAllPostsOfUser(userIdList, "request-id-4").get();
            logger.info("All posts count of User: " + posts.size());
            logger.info("All posts of User: {}", mapper.writeValueAsString(posts));
        } catch (SampleServiceFailureException failureException) {
            logger.error("500 response from server", failureException);
        } catch (SampleServiceException serviceException) {
            logger.error("Some other exception from server", serviceException);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}