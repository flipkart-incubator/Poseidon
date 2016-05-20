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

package com.flipkart.poseidon.legoset;

import com.flipkart.poseidon.annotations.Name;
import com.flipkart.poseidon.annotations.Version;
import com.flipkart.poseidon.mappers.Mapper;
import com.flipkart.poseidon.core.PoseidonRequest;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import flipkart.lego.api.entities.*;
import flipkart.lego.api.exceptions.ElementNotFoundException;
import flipkart.lego.api.exceptions.LegoSetException;
import flipkart.lego.api.helpers.Identifiable;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class PoseidonLegoSet implements LegoSet {

    private static final Logger logger = getLogger(PoseidonLegoSet.class);
    private static Map<String, Constructor<DataSource>> dataSources = new HashMap<>();
    private static Map<String, ServiceClient> serviceClients = new HashMap<>();
    private static Map<String, Filter> filters = new HashMap<>();
    private static Map<String, Mapper> mappers = new HashMap<>();
    private Map<String, Buildable> buildableMap = new HashMap<>();
    private ExecutorService dataSourceExecutor;

    {
        try {
            ClassPath classpath = ClassPath.from(ClassLoader.getSystemClassLoader());
            List<String> basePackagesToScan = getPackagesToScan();
            for (String basePackage : basePackagesToScan) {
                ImmutableSet<ClassPath.ClassInfo> classInfos = classpath.getTopLevelClassesRecursive(basePackage);
                for (ClassPath.ClassInfo classInfo : classInfos) {
                    bucketize(classInfo);
                }
            }
        } catch (Exception e) {
            logger.error("Unable to load lego-blocks into their containers", e);
        }
    }

    private void bucketize(ClassPath.ClassInfo aClass) {
        try {
            Class klass = Class.forName(aClass.getName());
            if (!isAbstract(klass)) {
                if (DataSource.class.isAssignableFrom(klass)) {
                    Constructor<DataSource> constructor = klass.getDeclaredConstructor(LegoSet.class, Request.class);
                    Optional<String> id = getBlockId(klass);
                    if (!id.isPresent()) {
                        id = Optional.ofNullable(constructor.newInstance(this, null).getId());
                    }
                    dataSources.put(id.get(), constructor);
                } else if (ServiceClient.class.isAssignableFrom(klass)) {
                    Constructor<ServiceClient> constructor = klass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    ServiceClient serviceClient = constructor.newInstance();
                    serviceClients.put(getBlockId(klass).orElseGet(serviceClient::getId), serviceClient);
                } else if (Filter.class.isAssignableFrom(klass)) {
                    Filter filter;
                    try {
                        Constructor<Filter> constructor = klass.getDeclaredConstructor(LegoSet.class);
                        filter = constructor.newInstance(this);
                    } catch (NoSuchMethodException e) {
                        filter = (Filter) klass.newInstance();
                    }
                    filters.put(getBlockId(klass).orElseGet(filter::getId), filter);
                } else if (Mapper.class.isAssignableFrom(klass)) {
                    Mapper mapper = (Mapper) klass.newInstance();
                    mappers.put(getBlockId(klass).orElseGet(mapper::getId), mapper);
                }
            }
        } catch (Throwable t) {
            logger.error("Unable to instantiate " + aClass.getName(), t);
        }
    }

    private Optional<String> getBlockId(Class klass) {
        Optional<String> optionalName = Optional.ofNullable((Name) klass.getDeclaredAnnotation(Name.class)).map(Name::value);
        Optional<String> optionalVersion = Optional.ofNullable((Version) klass.getDeclaredAnnotation(Version.class)).map(value -> String.format("%s.%s.%s", value.major(), value.minor(), value.patch()));
        String id = String.format("%s_%s", optionalName.orElse(klass.getSimpleName()), optionalVersion.orElse(""));
        return optionalVersion.isPresent() ? Optional.of(id) : Optional.empty();
    }

    private boolean isAbstract(Class klass) {
        return Modifier.isAbstract(klass.getModifiers());
    }

    public DataSource getDataSource(String id, Request request) throws LegoSetException, ElementNotFoundException {
        if (!dataSources.containsKey(id)) {
            throw new ElementNotFoundException("Unable to find DataSource for provided id = " + id);
        }

        try {
            DataSource dataSource = dataSources.get(id).newInstance(this, request);
            return wrapDataSource(dataSource);
        } catch (Exception e) {
            throw new LegoSetException("Unable to instantiate DataSource for provided id = " + id, e);
        }
    }

    public DataSource wrapDataSource(DataSource dataSource) {
        return new ContextInducedDataSource(dataSource);
    }

    public ServiceClient getServiceClient(String id) throws ElementNotFoundException {
        if (!serviceClients.containsKey(id)) {
            throw new ElementNotFoundException("Unable to find ServiceClient for provided id = " + id);
        }

        return serviceClients.get(id);
    }

    public Filter getFilter(String id) throws ElementNotFoundException {
        if (!filters.containsKey(id)) {
            throw new ElementNotFoundException("Unable to find Filter for provided id = " + id);
        }

        return wrapFilter(filters.get(id));
    }

    public Filter wrapFilter(Filter filter) {
        return new ContextInducedFilter(filter);
    }

    public Mapper getMapper(String id) throws ElementNotFoundException {
        if (!mappers.containsKey(id)) {
            throw new ElementNotFoundException("Unable to find Mapper for provided id = " + id);
        }

        return mappers.get(id);
    }

    public abstract List<String> getPackagesToScan();

    @Override
    public Buildable getBuildable(Request request) throws LegoSetException, ElementNotFoundException {
        PoseidonRequest poseidonRequest = (PoseidonRequest) request;
        Buildable buildable = buildableMap.get(poseidonRequest.getUrl());
        if (buildable == null) {
            throw new ElementNotFoundException("Buildable not found for given url: " + poseidonRequest.getUrl());
        }

        return buildable;
    }

    public void updateBuildables(Map<String, Buildable> buildableMap) {
        this.buildableMap = buildableMap;
    }

    public void setDataSourceExecutor(ExecutorService dataSourceExecutor) {
        this.dataSourceExecutor = dataSourceExecutor;
    }

    public ExecutorService getDataSourceExecutor() {
        return dataSourceExecutor;
    }
}
