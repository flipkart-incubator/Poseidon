## 4.2.0-SNAPSHOT (May 31, 2016)
- Datasources and filters can participate in Distributed Tracing

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
