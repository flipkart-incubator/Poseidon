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

package com.flipkart.poseidon.sample.datasources;

import com.flipkart.poseidon.datasources.AbstractDataSource;
import com.flipkart.poseidon.exception.DataSourceException;
import com.flipkart.poseidon.model.annotations.Description;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;
import com.flipkart.poseidon.sample.datatypes.PostDataType;
import com.flipkart.poseidon.sample.datatypes.PostsDataType;
import com.flipkart.poseidon.serviceclients.sampleSC.v5.*;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Name("PostsDS")
@Version(major = 1, minor = 0, patch = 0)
@Description("Fetches all posts for a given user id")
public class PostsDataSource extends AbstractDataSource<PostsDataType> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PostsDataSource(LegoSet legoset, Request request) {
        super(legoset, request);
    }

    @Override
    public PostsDataType call() throws Exception {
        try {
            String userId = request.getAttribute("userId");
            SampleServiceClient serviceClient = (SampleServiceClient) legoset.getServiceClient("sampleSC_5.14.0");
            Posts posts = serviceClient.getAllPostsOfUser(userId).get();
            if (posts == null || posts.isEmpty()) {
                return null;
            }

            PostsDataType postsDataType = new PostsDataType();
            List<PostDataType> postDataTypeList = new ArrayList<>();
            for (Post post: posts) {
                PostDataType postDataType = new PostDataType();
                postDataType.setUserId(post.getUserId());
                postDataType.setPostId(post.getId());
                postDataType.setPostBody(post.getTitle());
                postDataType.setPostTitle(post.getTitle());
                postDataTypeList.add(postDataType);
            }
            postsDataType.setPosts(postDataTypeList);
            return postsDataType;
        } catch (SampleServiceResourceNotFoundException notFoundException) { // 400 from service
            throw new DataSourceException("User not found");
        } catch (SampleServiceFailureException failureException) { // 500 from service
            throw new DataSourceException("Failed to fetch user details due to service failure");
        } catch (SampleServiceException serviceException) { // Some other errors like timeouts etc
            logger.error("Error fetching user details", serviceException);
            throw new DataSourceException("Failed to fetch user details");
        }
    }
}
