Poseidon
=======

Poseidon is a platform to build API applications that have to aggregate data from distributed services in an efficient way.

## Features

1. Resilient and fault tolerant API layer in SOA achieved using [Phantom](https://github.com/flipkart/phantom) 
2. Scatter-Gather capability built using [Lego](https://github.com/flipkart-incubator/Lego)
3. Dynamic dispatching and composing at API layer built using [Hydra](https://github.com/flipkart-incubator/hydra)
4. Embedded web server (Jetty), out of box metrics exposed through JMX
5. Configuration driven Distributed Tracing support
6. Data governance support to collect and log events
7. In-built, [phantom dashboard](https://github.com/Flipkart/Phantom#phantom-consoles)

## Releases

| Release | Date | Description |
|:------------|:----------------|:------------|
| Version 5.10.0            | Sep 01 2020      |    Upgrade to Spring 5.2.5.RELEASE
| Version 5.9.0             | Aug 05 2020      |    Custom servlets, singleton DS, bug fixes
| Version 5.8.0             | May 31 2019      |    Support for byte[] in ServiceResponseDecoder
| Version 5.6.0             | Jun 28 2018      |    Multiple error types in ServiceResponseDecoder
| Version 5.5.0             | May 23 2018      |    Added follow redirect option in SinglePoolHttpTaskHandler
| Version 5.4.1             | Nov 05 2017      |    Bugfix to stop stacktrace leak

## Changelog

Changelog can be viewed in [CHANGELOG.md](https://github.com/flipkart-incubator/Poseidon/blob/master/CHANGELOG.md)

## Getting Started

A sample application is available [here](https://github.com/flipkart-incubator/Poseidon/tree/master/sample#poseidon-sample).

## Users

[Flipkart](http://www.flipkart.com)

[HealthFace.in](http://healthface.in)

## Continuous Integration

 Uses https://travis-ci.org/ to run tests for pushes and pull requests

## Getting help
For discussion, help regarding usage, or receiving important announcements, subscribe to the [Poseidon users mailing list](https://groups.google.com/a/flipkart.com/forum/#!forum/poseidon-users)

## Contribution, Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/flipkart-incubator/Poseidon/issues).
Please follow the [contribution guidelines](https://github.com/flipkart-incubator/Poseidon/blob/master/CONTRIBUTING.md) when submitting pull requests.

## License

Copyright 2016 Flipkart Internet, pvt ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
