Poseidon Sample Service Client
==============================

Poseidon Sample Service Client helps as a reference to build new HTTP service clients using Poseidon. It defines the contracts of a dummy online [REST API Service](http://jsonplaceholder.typicode.com) in JSON based IDL files. Poseidon behind the screen generates the implementations and abstracts out

+ Phantom/hystrix for circuit breaking, isolation
+ RPC mechanism (JSON over HTTP), serialization and de-serialization
+ Connection pooling
+ Validations, passing generic headers like zipkin etc

### Models

Models are defined using [JSON Schema](http://json-schema.org/) and POJOs are generated using [JSON SChema to POJO](http://www.jsonschema2pojo.org/). Example is [here](src/main/resources/idl/pojos/User.json) which corresponds to the User schema of [REST API Service](http://jsonplaceholder.typicode.com/users?id=1).

### Contracts

Service contracts are defined using a custom [JSON schema](https://github.com/flipkart-incubator/Poseidon/blob/master/service-clients-gen/src/main/java/com/flipkart/poseidon/serviceclients/idl/pojo/ServiceIDL.java). Example is [here](src/main/resources/idl/service/SampleService.json) which corresponds to the API to get [user details](http://jsonplaceholder.typicode.com/users/1) and to get [all posts](http://jsonplaceholder.typicode.com/posts?userId=1) of the user. Service client framework [POJO](https://github.com/flipkart-incubator/Poseidon/blob/master/service-clients-gen/src/main/java/com/flipkart/poseidon/serviceclients/idl/pojo/ServiceIDL.java).

We use declarative way of specifying service contracts for reasons mentioned [here](https://github.com/flipkart-incubator/Poseidon/wiki/Service-Clients), but still we don't block hand-written service client implementations.

## More Details

A maven archetype will be released to help create a new service client using Poseidon in a few steps.

For bugs, questions and discussions please use the [Github Issues](https://github.com/flipkart-incubator/Poseidon/issues). 
