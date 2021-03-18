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

import com.google.common.base.Ticker
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpReads}

import scala.concurrent.{ExecutionContext, Future}


class CachedStaticHtmlPartialSpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar
     with BeforeAndAfterEach
     with ArgumentMatchersSugar {

  import scala.concurrent.ExecutionContext.Implicits.global

  val cacheExpiryIntervalInHours = 2
  val cacheRefreshIntervalInSeconds = 20

  val mockHttpGet = mock[CoreGet]

  val testTicker = new Ticker {

    var timestamp: Long = 0

    override def read(): Long = timestamp

    def shiftTimeInSeconds(time: Long): Unit = {
      timestamp = timestamp + (time * 1000000000)
    }

    def shiftTimeInHours(time: Long): Unit = {
      shiftTimeInSeconds(time * 60 * 60)
    }

    def resetTime() {
      timestamp = 0
    }
  }

  val htmlPartial = new CachedStaticHtmlPartialRetriever {
    import scala.concurrent.duration._

    override val httpGet: CoreGet = mockHttpGet

    override val cacheTicker: Ticker = testTicker

    override def refreshAfter: Duration = cacheRefreshIntervalInSeconds.seconds

    override def expireAfter: Duration = cacheExpiryIntervalInHours.hours
  }

  implicit val request = FakeRequest()

  override protected def beforeEach() = {
    super.beforeEach()

    reset(mockHttpGet)
    testTicker.resetTime()
    htmlPartial.cache.invalidateAll()
  }

  "get" should {
    "retrieve HTML from the given URL" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content A"))),
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content B")))
        )

      htmlPartial.getPartial("foo").asInstanceOf[HtmlPartial.Success].content.body should be("some content A")

      // whilst still within the refresh time the same content should be returned from cache
      htmlPartial.getPartial("foo").asInstanceOf[HtmlPartial.Success].content.body should be("some content A")

      // now move beyond the refresh time
      testTicker.shiftTimeInSeconds(cacheRefreshIntervalInSeconds + 1)
      htmlPartial.getPartial("foo").asInstanceOf[HtmlPartial.Success].content.body should be("some content B")
      verify(mockHttpGet, times(2))
        .GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext])
    }

    "use stale value when there is an exception retrieving the partial from the URL" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content C"))),
          Future.successful(HtmlPartial.Failure())
        )

      htmlPartial.getPartial("foo").asInstanceOf[HtmlPartial.Success].content.body should be("some content C")
      testTicker.shiftTimeInSeconds(cacheRefreshIntervalInSeconds + 1)
      htmlPartial.getPartial("foo").asInstanceOf[HtmlPartial.Success].content.body should be("some content C")
    }

    "return HtmlPartial.Failure when there is an exception retrieving the partial from the URL and we have no cached value yet" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(HtmlPartial.Failure()))

      htmlPartial.getPartial("foo") should be(HtmlPartial.Failure())
    }

    "return provided Html when there is an exception retrieving the partial from the URL and we have no cached value yet" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(HtmlPartial.Failure()))

      htmlPartial.get(url = "foo", errorMessage = Html("something went wrong")).body should be("something went wrong")
    }

    "return error message when stale value has expired and th:ere is an exception reloading the cache" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content D"))),
          Future.successful(HtmlPartial.Failure())
        )

      htmlPartial.getPartial("foo").asInstanceOf[HtmlPartial.Success].content.body should be("some content D")

      testTicker.shiftTimeInHours(cacheExpiryIntervalInHours + 1)

      htmlPartial.get(url = "foo", errorMessage = Html("something went wrong")).body should be("something went wrong")
    }

    "invalidate cache entries when using real ticker" in {
      //   NOTE - yes this is a slow and ugly test - but it is catching a real bug that was not otherwise caught with the testTicker

      val htmlPartialWithRealTicker = new CachedStaticHtmlPartialRetriever {

        import scala.concurrent.duration._

        override val httpGet = mockHttpGet

        override def refreshAfter: Duration = 2.seconds
      }

      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content A"))),
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content B")))
        )

      htmlPartialWithRealTicker.getPartial("foo").asInstanceOf[HtmlPartial.Success].content.body should be("some content A")

      // whilst still within the refresh time the same content should be returned from cache
      htmlPartialWithRealTicker.getPartial("foo").asInstanceOf[HtmlPartial.Success].content.body should be("some content A")

      // now move beyond the refresh time
      Thread.sleep(2000)
      htmlPartialWithRealTicker.getPartial("foo").asInstanceOf[HtmlPartial.Success].content.body should be("some content B")
    }
  }
}
