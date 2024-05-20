/*
 * Copyright 2023 HM Revenue & Customs
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

import com.github.benmanes.caffeine.cache.Ticker
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpReads}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationLong}
import java.util.concurrent.atomic.AtomicLong

class CachedStaticHtmlPartialSpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar
     with BeforeAndAfterEach
     with ScalaFutures
     with IntegrationPatience {

  import scala.concurrent.ExecutionContext.Implicits.global

  val mockHttpGet = mock[CoreGet]

  val testTicker = new TestTicker

  val htmlPartial = new CachedStaticHtmlPartialRetriever {
    override val httpGet: CoreGet = mockHttpGet

    override lazy val cacheTicker: Ticker = testTicker

    override val refreshAfter: Duration = 20.seconds

    override val expireAfter: Duration = 2.hours

    override val maximumEntries: Int = 100
  }

  implicit val request: Request[AnyContent] = FakeRequest()

  override protected def beforeEach() = {
    super.beforeEach()
    reset(mockHttpGet)
    testTicker.resetTime()
    htmlPartial.cache.synchronous.invalidateAll()
  }

  "CachedStaticHtmlPartial.getPartial" should {
    "retrieve HTML from the given URL" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content A"))),
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content B")))
        )

      htmlPartial.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content A"))

      // whilst still within the refresh time the same content should be returned from cache
      htmlPartial.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content A"))

      // now move beyond the refresh time
      testTicker.shiftTime(htmlPartial.refreshAfter + 1.second)
      // first call will trigger the refresh (and return cached value)
      htmlPartial.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content A"))
      Thread.sleep(2000) // give the cache time to update
      // after that, the cache will have been updated
      htmlPartial.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content B"))

      verify(mockHttpGet, times(2))
        .GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext])
    }

    "use stale value when there is an exception retrieving the partial from the URL" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content C"))),
          Future.successful(HtmlPartial.Failure())
        )

      htmlPartial.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content C"))

      testTicker.shiftTime(htmlPartial.refreshAfter + 1.second)
      htmlPartial.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content C"))
      Thread.sleep(200) // give the cache time to update
      htmlPartial.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content C"))
    }

    "return HtmlPartial.Failure when there is an exception retrieving the partial from the URL and we have no cached value yet" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(HtmlPartial.Failure()))

      htmlPartial.getPartial("foo").futureValue shouldBe HtmlPartial.Failure()
    }

    "return HtmlPartial.Failure when stale value has expired and there is an exception reloading the cache" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content D"))),
          Future.successful(HtmlPartial.Failure())
        )

      htmlPartial.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content D"))

      testTicker.shiftTime(htmlPartial.expireAfter + 1.hour)

      htmlPartial.getPartial("foo").futureValue shouldBe HtmlPartial.Failure()
    }

    "invalidate cache entries when using real ticker" in {
      //   NOTE - yes this is a slow and ugly test - but it is catching a real bug that was not otherwise caught with the testTicker

      val htmlPartialWithRealTicker = new CachedStaticHtmlPartialRetriever {
        override val httpGet = mockHttpGet

        override val refreshAfter: Duration = 2.seconds

        override val expireAfter: Duration = 1.hour

        override val maximumEntries: Int = 100
      }

      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content A"))),
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content B")))
        )

      htmlPartialWithRealTicker.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content A"))

      // whilst still within the refresh time the same content should be returned from cache
      htmlPartialWithRealTicker.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content A"))

      // now move beyond the refresh time
      Thread.sleep(htmlPartialWithRealTicker.refreshAfter.toMillis + 1)
      // first call will trigger the refresh (and return cached value)
      htmlPartialWithRealTicker.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content A"))
      Thread.sleep(200) // give the cache time to update
      // after that, the cache will have been updated
      htmlPartialWithRealTicker.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content B"))
    }

    "apply templates" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content with {{PLACEHOLDER}}")))
        )

      htmlPartial.getPartial("foo"                                                    ).futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content with {{PLACEHOLDER}}"))
      htmlPartial.getPartial("foo", templateParameters = Map("PLACEHOLDER" -> "text1")).futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content with text1"))
      htmlPartial.getPartial("foo", templateParameters = Map("PLACEHOLDER" -> "text2")).futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content with text2"))
    }
  }

  "CachedStaticHtmlPartial.getPartialContentAsync" should {
    "return provided Html when there is an exception retrieving the partial from the URL and we have no cached value yet" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(HtmlPartial.Failure()))

      htmlPartial.getPartialContentAsync(url = "foo", errorMessage = Html("something went wrong")).futureValue.body shouldBe "something went wrong"
    }

    "apply templates" in {
      when(mockHttpGet.GET[HtmlPartial](eqTo("foo"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content with {{PLACEHOLDER}}")))
        )

      htmlPartial.getPartialContentAsync(url = "foo"                                                    ).futureValue.body shouldBe "some content with {{PLACEHOLDER}}"
      htmlPartial.getPartialContentAsync(url = "foo", templateParameters = Map("PLACEHOLDER" -> "text1")).futureValue.body shouldBe "some content with text1"
      htmlPartial.getPartialContentAsync(url = "foo", templateParameters = Map("PLACEHOLDER" -> "text2")).futureValue.body shouldBe "some content with text2"
    }
  }
}

class TestTicker extends Ticker {
  private val timestamp: AtomicLong = new AtomicLong(0)

  override def read(): Long =
    timestamp.get

  def shiftTime(time: Duration): Unit =
    timestamp.updateAndGet(_ + time.toNanos)

  def resetTime(): Unit =
    timestamp.set(0)
}
