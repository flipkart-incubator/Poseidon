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

import com.flipkart.poseidon.datasources.AbstractNonBlockingDataSource;
import com.flipkart.poseidon.model.annotations.Description;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;
import com.flipkart.poseidon.sample.datatypes.PostDataType;
import com.flipkart.poseidon.sample.datatypes.PostsDataType;
import com.flipkart.poseidon.serviceclients.sampleSC.v1.Post;
import com.flipkart.poseidon.serviceclients.sampleSC.v1.Posts;
import com.flipkart.poseidon.serviceclients.sampleSC.v1.SampleServiceClient;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;
import flipkart.lego.concurrency.api.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Name("PostsNBDS")
@Version(major = 1, minor = 0, patch = 0)
@Description("Fetches all posts for a given user id in a non blocking way")
public class PostsNonBlockingDataSource extends AbstractNonBlockingDataSource<Posts, PostsDataType> {
    private static final Logger logger = LoggerFactory.getLogger(PostsNonBlockingDataSource.class);

    public PostsNonBlockingDataSource(LegoSet legoset, Request request) {
        super(legoset, request);
    }

    @Override
    public Promise<Posts> callAsync() throws Exception {
        logger.info("Thread executing callAsync - {}", Thread.currentThread().getName());

        String userId = request.getAttribute("userId");
        SampleServiceClient serviceClient = (SampleServiceClient) legoset.getServiceClient("sampleSC_1.0.0");
        return serviceClient.getAllPostsOfUser(userId);
    }

    @Override
    public PostsDataType map(Posts posts) {
        logger.info("Thread executing map - {}", Thread.currentThread().getName());

        PostsDataType postsDataType = new PostsDataType();
        List<PostDataType> postDataTypeList = new ArrayList<>();
        for (Post post : posts) {
            PostDataType postDataType = new PostDataType();
            postDataType.setUserId(post.getUserId());
            postDataType.setPostId(post.getId());
            postDataType.setPostBody(post.getTitle());
            postDataType.setPostTitle(post.getTitle());
            postDataTypeList.add(postDataType);
        }
        postsDataType.setPosts(postDataTypeList);
        return postsDataType;
    }
}
