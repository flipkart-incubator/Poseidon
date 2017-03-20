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

import co.paralleluniverse.fibers.Suspendable;
import com.flipkart.poseidon.datasources.AbstractDataSource;
import com.flipkart.poseidon.exception.DataSourceException;
import com.flipkart.poseidon.model.annotations.Description;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;
import com.flipkart.poseidon.sample.datatypes.UserDataType;
import com.flipkart.poseidon.serviceclients.sampleSC.v1.*;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Name("UserDS")
@Version(major = 1, minor = 0, patch = 0)
@Description("Fetches an user for a given id")
public class UserDataSource extends AbstractDataSource<UserDataType> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public UserDataSource(LegoSet legoset, Request request) {
        super(legoset, request);
    }

    @Override
    @Suspendable
    public UserDataType call() throws Exception {
        try {
            String userId = request.getAttribute("userId");
            SampleServiceClient serviceClient = (SampleServiceClient) legoset.getServiceClient("sampleSC_1.0.0");
            UserList userList = serviceClient.getUser(userId).get();
            if (userList == null || userList.isEmpty()) {
                throw new DataSourceException("User not found", 404);
            }

            User user = userList.get(0);
            UserDataType userDataType = new UserDataType();
            userDataType.setUserId(user.getId());
            userDataType.setUserName(user.getName());
            userDataType.setEmailId(user.getEmail());
            return userDataType;
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
