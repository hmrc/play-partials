play-partials
=============

[![Build Status](https://travis-ci.org/hmrc/play-partials.svg?branch=add-travis)](https://travis-ci.org/hmrc/play-partials) [ ![Download](https://api.bintray.com/packages/hmrc/releases/play-partials/images/download.svg) ](https://bintray.com/hmrc/releases/play-partials/_latestVersion)

A library used to retrieve HTML partials to use when composing HTML Play frontend applications.

In supports caching the partials.

## Adding to your service

Include the following dependency in your SBT build

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "play-partials" % "1.2.0"
```
