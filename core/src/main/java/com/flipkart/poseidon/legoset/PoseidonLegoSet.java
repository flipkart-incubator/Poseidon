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

import com.flipkart.poseidon.core.PoseidonRequest;
import com.flipkart.poseidon.datasources.RequestAttribute;
import com.flipkart.poseidon.handlers.http.utils.StringUtils;
import com.flipkart.poseidon.helper.AnnotationHelper;
import com.flipkart.poseidon.helper.CallableNameHelper;
import com.flipkart.poseidon.helper.ClassPathHelper;
import com.flipkart.poseidon.mappers.Mapper;
import com.flipkart.poseidon.model.annotations.Name;
import com.flipkart.poseidon.model.annotations.Version;
import com.flipkart.poseidon.model.exception.MissingInformationException;
import com.flipkart.poseidon.model.exception.PoseidonFailureException;
import com.google.common.reflect.ClassPath;
import flipkart.lego.api.entities.*;
import flipkart.lego.api.exceptions.ElementNotFoundException;
import flipkart.lego.api.exceptions.LegoSetException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class PoseidonLegoSet implements LegoSet {

    private static final Logger logger = getLogger(PoseidonLegoSet.class);
    private static Map<String, Constructor<DataSource<? extends DataType>>> dataSources = new HashMap<>();
    private static Map<String, ServiceClient> serviceClients = new HashMap<>();
    private static Map<String, ServiceClient> serviceClientsByName = new HashMap<>();
    private static Map<String, Filter> filters = new HashMap<>();
    private static Map<String, Mapper> mappers = new HashMap<>();
    private Map<String, Buildable> buildableMap = new HashMap<>();
    private ExecutorService dataSourceExecutor;
    private ApplicationContext context;

    public void init() {
        try {
            Set<ClassPath.ClassInfo> classInfos = ClassPathHelper.getPackageClasses(ClassLoader.getSystemClassLoader(), getPackagesToScan());

            List<Class<ServiceClient>> serviceClientClasses = new ArrayList<>();
            List<Class<Filter>> filterClasses = new ArrayList<>();
            for (ClassPath.ClassInfo classInfo : classInfos) {
                bucketize(classInfo, serviceClientClasses, filterClasses);
            }

            for (Class<ServiceClient> serviceClientClass : serviceClientClasses) {
                processServiceClient(serviceClientClass);
            }

            for (Class<Filter> filterClass : filterClasses) {
                processFilter(filterClass);
            }
        } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | MissingInformationException | ElementNotFoundException e) {
            logger.error("Unable to load lego-blocks into their containers", e);
            throw new PoseidonFailureException("Unable to load lego-blocks into their containers", e);
        }
    }

    private void bucketize(ClassPath.ClassInfo aClass, List<Class<ServiceClient>> serviceClientClasses, List<Class<Filter>> filterClasses) {
        try {
            Class klass = Class.forName(aClass.getName());
            if (!isAbstract(klass)) {
                if (DataSource.class.isAssignableFrom(klass)) {
                    processDataSource(klass);
                } else if (ServiceClient.class.isAssignableFrom(klass)) {
                    serviceClientClasses.add(klass);
                } else if (Filter.class.isAssignableFrom(klass)) {
                    filterClasses.add(klass);
                } else if (Mapper.class.isAssignableFrom(klass)) {
                    Mapper mapper = (Mapper) klass.newInstance();
                    mappers.put(getBlockId(klass).orElseThrow(MissingInformationException::new), mapper);
                }
            }
        } catch (Throwable t) {
            logger.error("Unable to instantiate " + aClass.getName(), t);
            throw new PoseidonFailureException("Unable to instantiate " + aClass.getName(), t);
        }
    }

    private void processDataSource(Class<DataSource<? extends DataType>> klass) throws NoSuchMethodException, MissingInformationException {
        final List<Constructor<DataSource<? extends DataType>>> injectableConstructors = findInjectableConstructors(klass);
        Constructor<DataSource<? extends DataType>> constructor;
        if (injectableConstructors.isEmpty()) {
            constructor = klass.getDeclaredConstructor(LegoSet.class, Request.class);
        } else {
            constructor = injectableConstructors.get(0);
            if (constructor.getParameterCount() < 2 || constructor.getParameterTypes()[0] != LegoSet.class || constructor.getParameterTypes()[1] != Request.class) {
                throw new UnsupportedOperationException("The injectable constructor of " + klass.getName() + " must have LegoSet and Request as the first two parameter");
            }
        }

        Optional<String> id = getBlockId(klass);
        if (!id.isPresent()) {
            throw new MissingInformationException();
        }
        dataSources.put(id.get(), constructor);
    }

    private void processServiceClient(Class<ServiceClient> klass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, MissingInformationException {
        Constructor<ServiceClient> constructor = klass.getDeclaredConstructor();
        constructor.setAccessible(true);
        ServiceClient serviceClient = constructor.newInstance();
        serviceClients.put(getBlockId(klass).orElseThrow(MissingInformationException::new), serviceClient);
        for (Class<?> interfaceClass : klass.getInterfaces()) {
            if (ServiceClient.class.isAssignableFrom(interfaceClass)) {
                serviceClientsByName.put(interfaceClass.getName(), serviceClient);
            }
        }
        serviceClientsByName.put(klass.getName(), serviceClient);
    }

    private void processFilter(Class<Filter> klass) throws IllegalAccessException, InvocationTargetException, InstantiationException, MissingInformationException, ElementNotFoundException {
        Filter filter;
        try {
            final List<Constructor<Filter>> injectableConstructors = findInjectableConstructors(klass);
            if (injectableConstructors.isEmpty()) {
                Constructor<Filter> constructor = klass.getDeclaredConstructor(LegoSet.class);
                filter = constructor.newInstance(this);
            } else {
                Constructor<Filter> constructor = injectableConstructors.get(0);
                if (constructor.getParameterCount() < 1 || constructor.getParameterTypes()[0] != LegoSet.class) {
                    throw new UnsupportedOperationException("The injectable constructor of " + klass.getName() + " must have LegoSet as the first parameter");
                }

                Object[] initParams = new Object[constructor.getParameterCount()];
                initParams[0] = this;
                resolveInjectableConstructorDependencies(constructor, initParams, 1, Optional.empty());
                filter = constructor.newInstance(initParams);
            }
        } catch (NoSuchMethodException e) {
            filter = klass.newInstance();
        }
        filters.put(getBlockId(klass).orElseThrow(MissingInformationException::new), filter);
    }

    private <T> List<Constructor<T>> findInjectableConstructors(Class<T> klass) {
        final Constructor<T>[] declaredConstructors = (Constructor<T>[]) klass.getDeclaredConstructors();
        final List<Constructor<T>> injectableConstructors = Arrays.stream(declaredConstructors).filter(c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
        if (injectableConstructors.size() > 1) {
            throw new UnsupportedOperationException(klass.getName() + " has more than one injectable constructor");
        }
        return injectableConstructors;
    }

    public static Optional<String> getBlockId(Class<?> klass) {
        Optional<String> optionalName = Optional.ofNullable(klass.getDeclaredAnnotation(Name.class)).map(Name::value);
        Optional<String> optionalVersion = Optional.ofNullable(klass.getDeclaredAnnotation(Version.class)).map(AnnotationHelper::constructVersion);
        String id = CallableNameHelper.versionedName(optionalName.orElse(klass.getSimpleName()), optionalVersion.orElse(""));
        return optionalVersion.isPresent() ? Optional.of(id) : Optional.empty();
    }

    private boolean isAbstract(Class klass) {
        return Modifier.isAbstract(klass.getModifiers());
    }

    public <T extends DataType> DataSource<T> getDataSource(String id, Request request) throws LegoSetException, ElementNotFoundException {
        DataSource<? extends DataType> dataSource = getBasicDataSource(id, request);
        return wrapDataSource((DataSource<T>) dataSource, request);
    }

    public <T extends DataType> DataSource<T> getBasicDataSource(String id, Request request) throws LegoSetException, ElementNotFoundException {
        if (!dataSources.containsKey(id)) {
            throw new ElementNotFoundException("Unable to find DataSource for provided id = " + id);
        }

        try {
            final Constructor<DataSource<? extends DataType>> dataSourceConstructor = dataSources.get(id);
            DataSource<T> dataSource;
            if (dataSourceConstructor.getParameterCount() == 2) {
                dataSource = (DataSource<T>) dataSourceConstructor.newInstance(this, request);
            } else {
                Object[] initParams = new Object[dataSourceConstructor.getParameterCount()];
                initParams[0] = this;
                initParams[1] = request;
                resolveInjectableConstructorDependencies(dataSourceConstructor, initParams, 2, Optional.ofNullable(request));

                dataSource = (DataSource<T>) dataSourceConstructor.newInstance(initParams);
            }
            return dataSource;
        } catch (Exception e) {
            throw new LegoSetException("Unable to instantiate DataSource for provided id = " + id, e);
        }
    }

    public <T extends DataType> DataSource<T> wrapDataSource(DataSource<T> dataSource, Request request) {
        return new ContextInducedDataSource<>(dataSource, request);
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

    public void setContext(ApplicationContext context) {
        this.context = context;
    }

    private void resolveInjectableConstructorDependencies(Constructor<?> constructor, Object[] initParams, int offset, Optional<Request> request) throws MissingInformationException, ElementNotFoundException {
        for (int i = offset; i < constructor.getParameterCount(); i++) {
            final RequestAttribute requestAttribute = constructor.getParameters()[i].getAnnotation(RequestAttribute.class);
            final com.flipkart.poseidon.datasources.ServiceClient serviceClientAttribute = constructor.getParameters()[i].getAnnotation(com.flipkart.poseidon.datasources.ServiceClient.class);
            if (requestAttribute != null) {
                final String attributeName = StringUtils.isNullOrEmpty(requestAttribute.value()) ? constructor.getParameters()[i].getName() : requestAttribute.value();
                initParams[i] = request.map(r -> r.getAttribute(attributeName)).orElse(null);
            } else if (serviceClientAttribute != null) {
                ServiceClient serviceClient = serviceClientsByName.get(constructor.getParameterTypes()[i].getName());
                if (serviceClient == null) {
                    throw new ElementNotFoundException("Unable to find ServiceClient for class = " + constructor.getParameterTypes()[i]);
                }
                initParams[i] = serviceClient;
            } else {
                final Qualifier qualifier = constructor.getParameters()[i].getAnnotation(Qualifier.class);
                String beanName = null;
                if (qualifier != null && !StringUtils.isNullOrEmpty(qualifier.value())) {
                    beanName = qualifier.value();
                }

                initParams[i] = beanName == null ? context.getBean(constructor.getParameterTypes()[i]) : context.getBean(beanName, constructor.getParameterTypes()[i]);
                if (initParams[i] == null) {
                    throw new MissingInformationException("Unmet dependency for constructor " + constructor.getName() + ": " + constructor.getParameterTypes()[i].getCanonicalName());
                }
            }
        }
    }
}
