# catbird

[![Build status](https://img.shields.io/github/workflow/status/typelevel/catbird/Continuous%20Integration.svg)](http://github.com/typelevel/catbird/actions)
[![Coverage status](https://img.shields.io/codecov/c/github/typelevel/catbird/master.svg)](https://codecov.io/github/typelevel/catbird)
[![Maven Central](https://img.shields.io/maven-central/v/org.typelevel.catbird/catbird-finagle_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/org.typelevel.catbird/catbird-finagle_2.13)

This project provides [Cats](https://github.com/typelevel/cats) type class instances (and other useful
Cats-related stuff) for various [Twitter Open Source](https://twitter.com/twitteross) Scala
projects.

It currently includes the following:

* Type class instances for `Future`, `Var`, and `Try` (including `Monad` or `MonadError`, `Semigroup`, and equality)
* Category and profunctor instances for `Service`
* A `Rerunnable` type that wraps `Future` but provides semantics more like Cats Effect's `IO`

These are reasonably well-tested (thanks to [Discipline](https://github.com/typelevel/discipline)).

## Community

People are expected to follow the [Scala Code of Conduct][code-of-conduct] on GitHub and in any
other project channels.

## License

Licensed under the **[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[code-of-conduct]: https://www.scala-lang.org/conduct.html
