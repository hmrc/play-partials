play-partials
=============

[ ![Download](https://api.bintray.com/packages/hmrc/releases/play-partials/images/download.svg) ](https://bintray.com/hmrc/releases/play-partials/_latestVersion) [ ![Download](https://api.bintray.com/packages/hmrc/releases/play-partials/images/download.svg) ](https://bintray.com/hmrc/releases/play-partials/_latestVersion)

A library used to retrieve HTML partials to use when composing HTML Play frontend applications.

In supports caching the partials.

## Adding to your service

Include the following dependency in your SBT build

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "play-partials" % "x.x.x"
```

## Using cached static partials

If you need to use a static cached partial, use the `CachedStaticHtmlPartial` trait. It will retrieve the partial from the given URL and cache it (the cache key is the partial URL) for the defined period of time. You can also pass through a map of parameters used to replace placeholders in the retrieved partial. Placeholders have the form of {{parameteKey}}.

### Using HTML Form partials

A special case of the static partials are HTML forms. By using `CachedStaticFormPartial` a {{csrfToken}} placeholder will be replaced with the Play CSRF token value.

## License ##

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
