## 5.7.0 (WIP)
- Loop over DataSource bug fixes and sample
- LONG support in API parameter
- Upgrade hibernate-validator to 5.4.2.Final

## 5.6.0 (June 28, 2018)
- Support for multiple error types in ServiceResponseDecoder 

## 5.5.1 (May 31, 2018)
- Bugfix in code generation for missing patch version

## 5.5.0 (May 23, 2018)
- Added follow redirect option in SinglePoolHttpTaskHandler

## 5.4.1 (Nov 5, 2017)
- [Bugfix] Stop stacktrace leak when gzip is not supported

## 5.4.0 (Oct 13, 2017)
- Throw original exceptions for service failures (de-serialization exceptions etc) without wrapping in other exceptions
- Exposed an interface in Validator framework for user defined logic
- Optionally view upstream cache candidates in debug APIs
- DataType support for Headers and Path params
- Maven plugin to block unwanted transitive dependencies in service clients
- DataType supports ENUM as an option
- Generic types are supported while defining a request body type in API definitions

## 5.3.0 (June 27, 2017)
- Ability to define and consume additional fields for an endpoint to be used as meta information

## 5.2.0 (June 7, 2017)
- Debug flag in APIs to list all service responses
- Collect response headers from all upstream services for a particular call to the system.

## 5.1.0 (May 25, 2017)
- Upgrade Jetty version to v9.4.1.v20170120
- Support incoming PATCH method calls
- Support PATCH calls for service clients
- Bumping Phantom to 3.4.0
- HttpConnectionPool supports form encoded POST body
- API reponse status metric names have http methods as well
- Support for returning raw bytes in response

## 5.0.0 (Jan 2, 2017)
- Included snapshot version fixes
- Sample application

## 5.0.0-SNAPSHOT (Dec 22, 2016)
- Log4j2 over logback for both service and access logs 
- Avoided serialization and deserialization between service clients and task handlers
- Zipkin headers ingestion to HTTP upstreams
- 5xx from HTTP upstreams will be treated as Hystrix command failures
- Upgrade to servlet-api 3.1.0
- API - endPoint JMX metrics (status code)
- Upgrade to Phantom 3.2.0-SNAPSHOT for customizable core and max pool size in task handlers
- Upgrade to Hystrix 1.5.8
- Upgrade to Lego 3.0.0
- Using annotations like Name, Version over interfaces like Identifiable, Versioned
- Removed AbstractDataType
- Max connections in SinglePoolHttpTaskHandler is inferred from concurrency automatically

## 4.7.0 (Feb 15, 2017)
- Upgrade Jetty version to v9.4.1.v20170120
- Support incoming PATCH method calls
- Support PATCH calls for service clients

## 4.6.1-SNAPSHOT (Dec 7, 2016)
- 5xx from HTTP upstreams will be logged as errors and 4xx, 3xx as debug logs 
- Support for multi-value response headers
- Synchronous execution of DataSource from another DataSource

## 4.6.0 (Sep 30, 2016)
- Included snapshot version fixes

## 4.6.0-SNAPSHOT (Sep 30, 2016)
- Upgrade to Hystrix 1.5.0 from 1.4.0-RC5

## 4.5.1 (Sep 12, 2016)
- Included snapshot version fixes

## 4.5.1-SNAPSHOT (Sep 2, 2016)
- Bug Fixes - In API validator
- Support for multivalue query parameter in Service Clients
- Support to send binary data in Service Clients
- Service response failures will have status code logged

## 4.5.0 (Aug 8, 2016)
- Included snapshot version fixes
- MDC context passing

## 4.5.0-SNAPSHOT (Jul 28, 2016)
- Greedy wildcards in API urls 
- Bug fix in API routing

## 4.4.0 (Jul 18, 2016)
- Included snapshot version fixes

## 4.4.0-SNAPSHOT (Jul 18, 2016)
- DataSource calling DataSource - objects can be used instead of id
- Clean shutdown
- Bug Fixes - In service clients pom version, generation for javaType
- Timestamp injected in Http header to upstream service calls
- Blocks Validator

## 4.3.0 (Jul 11, 2016)
- Included snapshot version fixes

## 4.3.0-SNAPSHOT (Ju1 7, 2016)
- Hystrix metrics exposed through JMX using plug-in
- Bug fix in shutdown of OAuthTokenGenerator
- Bug fix for NPE in Trie
- API Validator
- Upgrade to phantom 2.0.1
- Failed service calls (hystrix commands) are now logged per request

## 4.2.1-SNAPSHOT (Jun 15, 2016)
- Using phantom 2.0.1, bug fix in zipkin span collector
- Service clients - object mapper can be configured per service
- Endpoint name is available in RequestContext
- API query param values can be tokenized by specifying separator

## 4.2.0 (Jun 7, 2016)
- Included snapshot version fixes
- [Bug fix](https://github.com/flipkart-incubator/Poseidon/issues/31) - NPE with inactive rewrite rules

## 4.2.0-SNAPSHOT (May 31, 2016)
- Datasources and filters can participate in Distributed Tracing
- Parameterized URI is available in RequestContext

## 4.1.0 (May 27, 2016)
- Included snapshot version fixes
- Filter to unzip POST/PUT request body

## 4.1.0-SNAPSHOT (May 25, 2016)
- Annotation driven Identifiable, Versioned and Describable
- Retaining response headers and cookies in non 2xx responses too
- API - params is optional
- Jetty filters, object mappers are configurable
- OAuth Task Handler
- Upgrade to phantom 2.0.0

## 4.0.0 (May 18, 2016)
- Included snapshot version fixes

## 4.0.0-SNAPSHOT (May 13, 2016)
- Pass through headers can be configured by Applications
- Service Clients - runtime and generation dependencies are separated
- Moved to Lego 2.0.0
- [Bug fix](https://github.com/flipkart-incubator/Poseidon/issues/17) - Path parameter at the end of URL
- Pretty printing of all API URLs
- API - support for Boolean parameter and default values
- Response gzipping for POST, PUT & DELETE

## 3.1.1 (Apr 6, 2016)
- Included snapshot version fixes
- API - endPoint JMX metrics (latency and request rate)

## 3.1.1-SNAPSHOT (Mar 20, 2016)
- Made response object available at response filters
- Service IDL - when requestObject is a java generic type, generated code wasn't compiling
- PoseidonRequest - headers is a map of String to String instead of String to Object
- Exception mapper to map exceptions to http response
- Made remote address accessible through RequestContext
- Upgraded phantom to 1.4.4
- Bug fix - Service clients: null for optional header was sent as "null"
- Bug fix - API nested files are not discovered properly
- API - Post body can be read as String by omitting javatype and datatype
- Service Clients - Support for errorResponseObject
- API - Support for OPTIONS request

## 3.1.0 (Feb 10, 2016)
- Included snapshot version fixes
- Jetty configuration can be tuned by applications

## 3.1.0-SNAPSHOT (Feb 5, 2016)
- Service IDL - Query parameter name can be different from generated java variable name
- API IDL - httpMethod can be defined optionally

## 3.0.0 (Feb 2, 2016)
- Included snapshot version fixes
- Batching support in service clients

## 3.0.0-SNAPSHOT (Jan 28, 2016)
- Service IDL now accepts all java types (including collections with nested generic types)
- dependencies module is removed

## 2.0.2 (Jan 14, 2016)
- Included snapshot version fixes
- Bug fix - NPE in AbstractServiceClient when RequestContext is not available

## 2.0.2-SNAPSHOT (Jan 11, 2016)
- Datasources can now call other datasources in an asynchronous way
- Headers like X-PERF-TEST, X-REQUEST-ID etc are injected to all HTTP Services
- API IDL and Service Client IDL files can be in nested folders
- Bug fix - RequestContext isn't accessible in Filters
 
## 2.0.1 (Jan 6, 2016)
- Upgrading snapshot to release version

## 2.0.1-SNAPSHOT (Jan 5, 2016)
- Support for HystrixRequestCaching in SinglePoolHttpTaskHandler
- Bug fix: If service response object is plain String, an exception is thrown while decoding 

## 2.0.0  (Jan 4, 2016)
- First public release

## 1.0.0
- Internal release
