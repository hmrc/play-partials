play-partials
=============



A library used to retrieve HTML partials to use when composing HTML Play frontend applications.

In supports caching the partials.

## Adding to your service

Include the following dependency in your SBT build

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "play-partials" % "x.x.x"
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
import HtmlPartial._

object Connector {
  def somePartial(): Future[HtmlPartial] =
    http.GET[HtmlPartial](url("/some/url")) recover connectionExceptionsAsHtmlPartialFailure
}

// Elsewhere in your service:
Connector.partial.map(p =>
  Ok(views.html.my_view(partial = p successfulContentOrElse Html("Sorry, there's been a problem retrieving ...")))
)

// or, if you just want to blank out a missing partial:
Connector.partial.map(p =>
  Ok(views.html.my_view(partial = p successfulContentOrEmpty))
)

// or, if you want to have finer-grained control:
Connector.partial.map {
  case HtmlPartial.Success(Some(title), content) =>
    Ok(views.html.my_view(message = content, title = title))
  case HtmlPartial.Success(None, content)        =>
    Ok(views.html.my_view(message = content, title = "A fallback title"))
  case HtmlPartial.Failure                       =>
    Ok(views.html.my_view(message = Html("Sorry, there's been a technical problem retrieving your info"), title = "A fallback title"))
}
```

## Using cached static partials

If you need to use a static cached partial, use `CachedStaticHtmlPartialRetriever`. It will retrieve the partial from the given URL and cache it (the cache key is the partial URL) for the defined period of time. You can also pass through a map of parameters used to replace placeholders in the retrieved partial. Placeholders have the form of `{{parameterKey}}`.

You can configure the following cache parameters in your `application.conf`:
- `play-partial.cache.refreshAfter`
- `play-partial.cache.expireAfter`
- `play-partial.cache.maxEntries`

### example

```scala
class MyView @Inject()(cachedStaticHtmlPartialRetriever: CachedStaticHtmlPartialRetriever) {
  cachedStaticHtmlPartialProvider.loadPartial("http://my.partial")
}
```

## Using HTML Form partials

A special case of the static partials are HTML forms. By using `FormPartialRetriever` a `{{csrfToken}}` placeholder will be replaced with the Play CSRF token value.

### example

```scala
class MyView @Inject()(formPartialRetriever: FormPartialRetriever) {
  formPartialRetriever.loadPartial("http://my.partial")
}
```

## Migrations

### Version 8.0.0

Built for Play 2.6, 2.7 and 2.8.

- The deprecated type `CachedStaticHtmlPartial` was removed - use `CachedStaticHtmlPartialRetriever` instead.
- The deprecated type `FormPartial` was removed - use `FormPartialRetriever` instead.
- Trait `HeaderCarrierForPartialsConverter` and class `HeaderCarrierForPartials` were removed - use `CookieForwarder.cookieForwardingHeaderCarrier` instead.
- `PartialRetriever.loadPartial` now returns an asynchronous `Future[HtmlPartial]`
- The deprecated method `PartialRetriever.get` was removed, use `PartialRetriever.getPartial` or `PartialRetriever.getPartialContent` instead.
- Injectable instances for `CachedStaticHtmlPartialRetriever` and `FormPartialRetriever` are provided.


## License ##

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
