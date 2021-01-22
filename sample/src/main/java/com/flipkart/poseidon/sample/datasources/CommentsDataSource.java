/*
 * Copyright 2018 Flipkart Internet, pvt ltd.
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
import com.flipkart.poseidon.sample.datatypes.CommentDataType;
import com.flipkart.poseidon.sample.datatypes.PostDataType;
import com.flipkart.poseidon.sample.datatypes.PostWithCommentsDataType;
import com.flipkart.poseidon.serviceclients.sampleSC.v5.*;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

@Name("CommentsDS")
@Version(major = 1, minor = 0, patch = 0)
@Description("Fetches all comments for a given post")
public class CommentsDataSource extends AbstractDataSource<PostWithCommentsDataType> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public CommentsDataSource(LegoSet legoset, Request request) {
        super(legoset, request);
    }

    @Override
    public PostWithCommentsDataType call() throws Exception {
        try {
            int index = request.getAttribute("index");
            PostDataType post = request.getAttribute("post");
            logger.info("Index: {}, Post Id: {}", index, post.getPostId());

            // Fetch comments through service client
            SampleServiceClient serviceClient = (SampleServiceClient) legoset.getServiceClient("sampleSC_5.11.1");
            Comments comments = serviceClient.getAllCommentsOfPost(post.getPostId()).get();

            // Map comments to datatype
            PostWithCommentsDataType postWithComments = new PostWithCommentsDataType();
            postWithComments.setPost(post);
            postWithComments.setComments(comments.stream().map(comment -> {
                CommentDataType commentDataType = new CommentDataType();
                commentDataType.setCommentId(comment.getId());
                commentDataType.setPostId(comment.getPostId());
                commentDataType.setName(comment.getName());
                commentDataType.setEmail(comment.getEmail());
                commentDataType.setBody(comment.getBody());
                return commentDataType;
            }).collect(Collectors.toList()));
            return postWithComments;
        } catch (SampleServiceResourceNotFoundException notFoundException) { // 400 from service
            throw new DataSourceException("Post not found");
        } catch (SampleServiceFailureException failureException) { // 500 from service
            throw new DataSourceException("Failed to fetch comments due to service failure");
        } catch (SampleServiceException serviceException) { // Some other errors like timeouts etc
            logger.error("Error fetching user details", serviceException);
            throw new DataSourceException("Failed to fetch comments");
        }
    }
}
