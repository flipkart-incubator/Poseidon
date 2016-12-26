Poseidon Sample
===============

Poseidon Sample helps as a reference to build new applications using Poseidon.

## To run

1. Build and install sample using maven
```mvn clean install```
2. Start sample application using ```sudo ./run.sh```
3. Access the [API](http://localhost:21000/v1/userPosts/1)![API Response](docs/APIResponse.png)

## What it does

It has a sample service client to a dummy online [REST API Service](http://jsonplaceholder.typicode.com).

It exposes an [API](http://localhost:21000/v1/userPosts/1) to fetch user details and all posts of a given user.

Internally it makes parallel calls to online REST API to get [user details](http://jsonplaceholder.typicode.com/users/1) and to get [all posts](http://jsonplaceholder.typicode.com/posts?userId=1) of the user. It composes a final response out of these two service responses as seen in [API](http://localhost:21000/v1/userPosts/1)

It demonstrates the following capabilities of Poseidon
+ Power of [Phantom](https://github.com/flipkart/phantom). It comes with a phantom [dashboard](http://localhost:8989/admin/dashboard)
![Phantom DB](docs/PhantomDB.png?raw=true)
+ Distributed Tracing
![DT](docs/DT.png?raw=true)
![DT Details](docs/DTDetails.png?raw=true)
+ Scatter-Gather capability built using [Lego](https://github.com/flipkart-incubator/Lego)
+ Dispatcher Composer Engine at Poseidon API implemented using [Hydra](https://github.com/flipkart-incubator/hydra)
+ Data Governance
+ Metrics out of the box. Connect to jmx port 3335 on localhost using jconsole and look for "metrics" under MBeans
![Metrics](docs/Metrics.png?raw=true)

## How it does

Start reading the code from the [API file]() definition and keep drilling down till [service client]() definition.

## More Details

A maven archetype will be released to help create a new application using Poseidon in a few steps.

For bugs, questions and discussions please use the [Github Issues](https://github.com/flipkart-incubator/Poseidon/issues). 
