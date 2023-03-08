play-partials
=============



A library used to retrieve HTML partials to use when composing HTML Play frontend applications.

It supports caching the partials and substituting placeholders.

## Adding to your service

Include the following dependency in your SBT build

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "play-partials" % "x.x.x"
```

## PartialRetriever

This is the simplest way to use the library. It contains error handling and placeholder substitutions.

There are two implementations provided: [CachedStaticHtmlPartialRetriever](#using-cached-static-partials) and [FormPartialRetriever](#using-html-form-partials).

First implement `PartialRetriever` and override `loadPartial`:

```scala
object MyRetriever extends PartialRetriever {
  override def loadPartial(url: String): Future[HtmlPartial] =
    httpClient.GET[HtmlPartial](url"/some/url").recover(HtmlPartial.connectionExceptionsAsHtmlPartialFailure)
}
```

Then you can request the `Html` with `getPartialContentAsync`. You can provide parameter subsitutions which replaces any placeholder with the form `{{parameterKey}}`.

```scala
// Returns a Future[Html] - you can pass the Html on to the view
MyRetriever.getPartialContentAsync("http://my.partial")

// Returns a Future[Html] - you can pass the Html on to the view
// You can provide template parameters and HTML to be returned in the case of error
MyRetriever.getPartialContentAsync(
  url                = "http://my.partial",
  templateParameters = Map("NONCE_ATTR" -> CSPNonce.attr),
  errorMessage       = Html("Could not load partial")
  )
```

If you want to do your own error handling, you can call `getPartial` which returns the [HtmlPartial](#the-htmlpartial-type)

## Using cached static partials

If you need to use a static cached partial, use `CachedStaticHtmlPartialRetriever`. It will retrieve the partial from the given URL and cache it (the cache key is the partial URL) for the defined period of time. You can also pass through a map of parameters used to replace placeholders in the retrieved partial.

You can configure the following cache parameters in your `application.conf`:
- `play-partial.cache.refreshAfter`
- `play-partial.cache.expireAfter`
- `play-partial.cache.maxEntries`

### example

An instance is already provided for injection. It is used in the same way as `PartialRetriever` above. e.g.

```scala
class MyController @Inject()(cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever) {
  // Returns a Future[Html] - you can pass the Html on to the view
  cachedStaticHtmlPartialProvider.getPartialContentAsync("http://my.partial")

  // Returns a Future[Html] - you can pass the Html on to the view
  // You can provide template parameters and HTML to be returned in the case of error
  cachedStaticHtmlPartialProvider.getPartialContentAsync(
    url                = "http://my.partial",
    templateParameters = Map("NONCE_ATTR", CSPNonce.attr),
    errorMessage       = Html("Could not load partial")
  )
}
```

## Using HTML Form partials

A special case of the static partials are HTML forms. By using `FormPartialRetriever` a csrfToken will be added in the request and any `{{csrfToken}}` placeholder will be replaced with the Play CSRF token value in the response.

Note, these are not cached.

### example

```scala
class MyView @Inject()(formPartialRetriever: FormPartialRetriever) {
  formPartialRetriever.getPartialContentAsync("http://my.partial")
}
```

## The `HtmlPartial` type

Use this type to read an HTTP response containing a partial, and safely
handle the possible outcomes:

* For success (2xx) status codes, an `HtmlPartial.Success`
is returned, which contains the HTML body and optionally a hint on the title that
should be used on the page.
* For non-success (400 -> 599) status codes, an `HtmlPartial.Failure` is returned
* A handler is also supplied which translates connection-related exceptions into an
`HtmlPartial.Failure`

#### Examples

```scala
object Connector {
  def somePartial(): Future[HtmlPartial] =
    httpClient.GET[HtmlPartial](url"/some/url").recover(HtmlPartial.connectionExceptionsAsHtmlPartialFailure)
}

// Elsewhere in your service:
Connector.somePartial().map(p =>
  Ok(views.html.my_view(partial = p.successfulContentOrElse(Html("Sorry, there's been a problem retrieving ..."))))
)

// or, if you just want to blank out a missing partial:
Connector.somePartial().map(p =>
  Ok(views.html.my_view(partial = p.successfulContentOrEmpty))
)

// or, if you want to have finer-grained control:
Connector.somePartial().map {
  case HtmlPartial.Success(Some(title), content) =>
    Ok(views.html.my_view(message = content, title = title))
  case HtmlPartial.Success(None, content)        =>
    Ok(views.html.my_view(message = content, title = "A fallback title"))
  case HtmlPartial.Failure                       =>
    Ok(views.html.my_view(message = Html("Sorry, there's been a technical problem retrieving your info"), title = "A fallback title"))
}
```


## Forwarding Cookies

In order to include cookies in the partial request, the HeaderCarrier must be created from the request with `HeaderCarrierForPartialsConverter.fromRequestWithEncryptedCookie` rather than with `HeaderCarrierConverter` from http-verbs.

### example

```scala
class MyView @Inject()(headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter) {
  def getPartial(request: RequestHeader) = {
    implicit val hc = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(request)
    httpClient.GET[HtmlPartial](url("http://my.partial"))
  }
}
```


## Migrations

### Version 8.4.0

Fixes template replacements for `templateParameters` parameter passed to `getPartial` and `getPartialContentAsync`.

### Version 8.3.0

Drops support for Play 2.6 and 2.7.

Built for Scala 2.12 and 2.13.

### Version 8.0.0

Built for Play 2.6, 2.7 and 2.8.

- Injectable instances for `CachedStaticHtmlPartialRetriever`, `FormPartialRetriever` and `HeaderCarrierForPartialsConverter` are provided. They should be used in preference to implementing the traits.
- `HeaderCarrierForPartialsConverter` requires a `ApplicationCrypto` (amongst other dependencies) instead of an ambiguous `def crypto: (String) => String` function. Using the injectable instance of `HeaderCarrierForPartialsConverter` should suffice for most use-cases, and ensures that encryption is properly applied.
- `PartialRetriever.loadPartial` and `PartialRetriever.getPartial` now return an asynchronous `Future[HtmlPartial]`
- `PartialRetriever.getPartialContent` is deprecated, in preference to `PartialRetriever.getPartialContentAsync`, which returns `Future[Html]`

Deprecated removals:
- The deprecated type `CachedStaticHtmlPartial` was removed - use `CachedStaticHtmlPartialRetriever` instead.
- The deprecated type `FormPartial` was removed - use `FormPartialRetriever` instead.
- The deprecated method `PartialRetriever.get` was removed, use `PartialRetriever.getPartial` or `PartialRetriever.getPartialContent` instead.


## License ##

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
