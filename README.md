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
| Version 3.0.0             | Feb 02 2016      |    Batching support, collections with nested generics support in service clients 
| Version 2.0.2             | Jan 14 2016      |    Datasources can call other datasources, headers injection, nested IDL files, bug fixes 
| Version 2.0.1             | Jan 06 2016      |    Request caching in task handler, bug fix in service response decoding
| Version 2.0.0             | Jan 04 2016      |    First public release

## Changelog

Changelog can be viewed in [CHANGELOG.md](https://github.com/flipkart-incubator/Poseidon/blob/master/CHANGELOG.md)

## Getting Started

A sample application will be available soon.

## Users

[Flipkart](http://www.flipkart.com)

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
