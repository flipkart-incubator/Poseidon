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
import com.flipkart.poseidon.sample.datatypes.UserDataType;
import com.flipkart.poseidon.serviceclients.sampleSC.v1.SampleServiceClient;
import com.flipkart.poseidon.serviceclients.sampleSC.v1.User;
import com.flipkart.poseidon.serviceclients.sampleSC.v1.UserList;
import flipkart.lego.api.entities.LegoSet;
import flipkart.lego.api.entities.Request;
import flipkart.lego.concurrency.api.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Name("UserNBDS")
@Version(major = 1, minor = 0, patch = 0)
@Description("Fetches an user for a given id in a non blocking way")
public class UserNonBlockingDataSource extends AbstractNonBlockingDataSource<UserList, UserDataType> {
    private static final Logger logger = LoggerFactory.getLogger(UserNonBlockingDataSource.class);

    public UserNonBlockingDataSource(LegoSet legoset, Request request) {
        super(legoset, request);
    }

    @Override
    public Promise<UserList> callAsync() throws Exception {
        logger.info("Thread executing callAsync - {}", Thread.currentThread().getName());

        String userId = request.getAttribute("userId");
        SampleServiceClient serviceClient = (SampleServiceClient) legoset.getServiceClient("sampleSC_1.0.0");
        return serviceClient.getUser(userId);
    }

    @Override
    public UserDataType map(UserList userList) {
        logger.info("Thread executing map - {}", Thread.currentThread().getName());

        User user = userList.get(0);
        UserDataType userDataType = new UserDataType();
        userDataType.setUserId(user.getId());
        userDataType.setUserName(user.getName());
        userDataType.setEmailId(user.getEmail());
        return userDataType;
    }
}
