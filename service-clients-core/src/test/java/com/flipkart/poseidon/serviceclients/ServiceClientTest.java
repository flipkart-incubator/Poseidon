package com.flipkart.poseidon.serviceclients;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kiran.japannavar on 16/08/16.
 */
@RunWith(PowerMockRunner.class)
public class ServiceClientTest {
    private AbstractServiceClient abstractServiceClient;
    private String filters = "filters";

    @Before
    public void setUp() {
        abstractServiceClient = new TestAbstractServiceClient();
    }

    @Test
    public void testValidMultipleQueryParams() throws Exception {
        List<String> queryValues = new ArrayList<>();
        queryValues.add("one");
        queryValues.add("two");
        queryValues.add("three");
        String actualOutput = abstractServiceClient.getOptURI(filters, queryValues);
        String expectedOutput = "filters=one&filters=two&filters=three";
        Assert.assertEquals(actualOutput, expectedOutput);
    }

    @Test
    public void testZeroQueryParams() throws Exception {
        List<String> queryValues = new ArrayList<>();
        String actualOutput = abstractServiceClient.getOptURI(filters, queryValues);
        String expectedOutput = "";
        Assert.assertEquals(expectedOutput, actualOutput);
    }

    @Test
    public void testSingleQueryParams() throws Exception {
        List<String> queryValues = new ArrayList<>();
        queryValues.add("one");
        String actualOutput = abstractServiceClient.getOptURI(filters, queryValues);
        String expectedOutput = "filters=one";
        Assert.assertEquals(expectedOutput, actualOutput);
    }

    @Test
    public void testNullQueryParams() throws Exception {
        List<String> queryValues = null;
        String actualOutput = abstractServiceClient.getOptURI(filters, queryValues);
        String expectedOutput = "";
        Assert.assertEquals(expectedOutput, actualOutput);
    }

    @Test
    public void testQueryParams() throws Exception {
        String queryValues = "one";
        String actualOutput = abstractServiceClient.getOptURI(filters, queryValues);
        String expectedOutput = "filters=one";
        Assert.assertEquals(expectedOutput, actualOutput);
    }

    @Test
    public void testCommaSeparatedQueryParams() throws Exception {
        String queryValues = "one,two";
        String actualOutput = abstractServiceClient.getOptURI(filters, queryValues);
        String expectedOutput = "filters=one,two";
        Assert.assertEquals(expectedOutput, actualOutput);
    }
}
