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
