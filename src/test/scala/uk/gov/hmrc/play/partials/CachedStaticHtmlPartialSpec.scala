/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.partials

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.cache.AsyncCacheApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpReads}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationLong}

class CachedStaticHtmlPartialSpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar
     with ArgumentMatchersSugar
     with BeforeAndAfterEach
     with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits.global

  val app =
    new GuiceApplicationBuilder().configure("csrf.sign.tokens" -> false).build()

  val mockHttpGet = mock[CoreGet]

  val htmlPartial = new CachedStaticHtmlPartialRetriever {
    override val httpGet: CoreGet = mockHttpGet

    override val cacheApi: AsyncCacheApi =
      app.injector.instanceOf[AsyncCacheApi]

    override def expireAfter: Duration = 2.seconds
  }

  implicit val request = FakeRequest()

  override protected def beforeEach() = {
    super.beforeEach()
    reset(mockHttpGet)
    htmlPartial.cacheApi.removeAll().futureValue
  }

  "get" should {
    "retrieve HTML from the given URL" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content A"))),
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content B")))
        )

      htmlPartial.getPartial("foo").futureValue should be(HtmlPartial.Success(title = None, content = Html("some content A")))

      // whilst still within the refresh time the same content should be returned from cache
      htmlPartial.getPartial("foo").futureValue should be(HtmlPartial.Success(title = None, content = Html("some content A")))

      // now move beyond the refresh time
      Thread.sleep(htmlPartial.expireAfter.toMillis + 1)
      htmlPartial.getPartial("foo").futureValue should be(HtmlPartial.Success(title = None, content = Html("some content B")))
      verify(mockHttpGet, times(2))
        .GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext])
    }

    // with change, if there is an exception, then the page is lost...
    /*"use stale value when there is an exception retrieving the partial from the URL" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content C"))),
          Future.successful(HtmlPartial.Failure())
        )

      htmlPartial.getPartial("foo").futureValue should be(HtmlPartial.Success(title = None, content = Html("some content C")))

      Thread.sleep(htmlPartial.expireAfter.toMillis + 1)
      htmlPartial.getPartial("foo").futureValue should be(HtmlPartial.Success(title = None, content = Html("some content C")))
    }*/

    "return HtmlPartial.Failure when there is an exception retrieving the partial from the URL and we have no cached value yet" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(HtmlPartial.Failure()))

      htmlPartial.getPartial("foo").futureValue should be(HtmlPartial.Failure())
    }

    "return provided Html when there is an exception retrieving the partial from the URL and we have no cached value yet" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(HtmlPartial.Failure()))

      htmlPartial.getPartialContent(url = "foo", errorMessage = Html("something went wrong")).futureValue.body should be("something went wrong")
    }

    "return error message when stale value has expired and th:ere is an exception reloading the cache" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content D"))),
          Future.successful(HtmlPartial.Failure())
        )

      htmlPartial.getPartial("foo").futureValue should be(HtmlPartial.Success(title = None, content = Html("some content D")))

      Thread.sleep(htmlPartial.expireAfter.toMillis + 1)

      htmlPartial.getPartialContent(url = "foo", errorMessage = Html("something went wrong")).futureValue.body should be("something went wrong")
    }
  }
}
