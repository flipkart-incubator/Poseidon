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

package com.flipkart.poseidon.legoset;

import com.flipkart.poseidon.legoset.test.NamedDataSource;
import com.flipkart.poseidon.legoset.test.OldSchoolDataSource;
import com.flipkart.poseidon.legoset.test.ProperDataSource;
import com.flipkart.poseidon.legoset.test.VersionedDataSource;
import flipkart.lego.api.entities.DataSource;
import flipkart.lego.api.exceptions.ElementNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by shrey.garg on 19/05/16.
 */
public class PoseidonLegoSetTest {
    public static final String NAMED_DS_NAME = "ThisIsNamed";
    public static final String PROPER_DS_NAME = "ThisIsProper";

    @Test
    public void testVariousDataSourceStyles() throws Exception {
        TestLegoSet legoSet = new TestLegoSet();

        DataSource namedDS = new NamedDataSource(null, null);
        legoSet.getDataSource(namedDS.getId(), null);

        DataSource oldStyleDS = new OldSchoolDataSource(null, null);
        legoSet.getDataSource(oldStyleDS.getId(), null);

        legoSet.getDataSource(PROPER_DS_NAME + "_4.1.6", null);
        legoSet.getDataSource("VersionedDataSource_1.0.0", null);

        try {
            legoSet.getDataSource(NAMED_DS_NAME, null);
            fail();
        } catch (Exception e) {
            assertEquals(ElementNotFoundException.class, e.getClass());
        }

        try {
            DataSource properDS = new ProperDataSource(null, null);
            legoSet.getDataSource(properDS.getId(), null);
            fail();
        } catch (Exception e) {
            assertEquals(ElementNotFoundException.class, e.getClass());
        }

        try {
            DataSource versionedDS = new VersionedDataSource(null, null);
            legoSet.getDataSource(versionedDS.getId(), null);
            fail();
        } catch (Exception e) {
            assertEquals(ElementNotFoundException.class, e.getClass());
        }
    }

    private static class TestLegoSet extends PoseidonLegoSet {
        @Override
        public List<String> getPackagesToScan() {
            return Arrays.asList("com.flipkart.poseidon.legoset.test");
        }
    }
}