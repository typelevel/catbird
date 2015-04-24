# catbird

[![Build status](https://img.shields.io/travis/travisbrown/catbird/master.svg)](http://travis-ci.org/travisbrown/catbird) [![Coverage status](https://img.shields.io/coveralls/travisbrown/catbird/master.svg)](https://coveralls.io/r/travisbrown/catbird?branch=master)

This project provides [cats](https://github.com/non/cats) type class instances (and other useful
cats-related stuff) for various [Twitter Open Source](https://twitter.com/twitteross) Scala
projects.

It currently includes the following:

* An injection from [Finagle](https://github.com/twitter/finagle) services to Kleisli arrows over
  `Future`
* Monad instances for `Future` and `Var`
* Semigroup and equality instances for `Future`, `Var`, and `Try`
* A bijection from `Try[A]` to `Xor[Throwable, A]`

These are reasonably well-tested (thanks to [discipline](https://github.com/typelevel/discipline)).

## License

Licensed under the **[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
