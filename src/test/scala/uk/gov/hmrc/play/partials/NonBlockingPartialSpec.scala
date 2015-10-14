/*
 * Copyright 2015 HM Revenue & Customs
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
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.{Matchers => MockitoMatchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import play.api.mvc.RequestHeader
import play.api.test.{FakeApplication, FakeHeaders, FakeRequest, WithApplication}
import play.filters.csrf.CSRF.Token
import play.twirl.api.Html
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.{HttpException, HttpGet, HttpReads}

import scala.concurrent.Future



class NonBlockingPartialSpec extends WordSpecLike with Matchers with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  val fakeApplication = FakeApplication(additionalConfiguration = Map("csrf.sign.tokens" -> false))

  val mockHttpGet = mock[HttpGet]

  val partialProvider = new NonBlockingPartial {
    override val httpGet: HttpGet = mockHttpGet

    override val crypto = c _

    private def c(value: String) = value

  }

  override protected def beforeEach() = {
    super.beforeEach()

    reset(mockHttpGet)
  }

  "load partial future" should {

    "retrieve HTML from the given URL" in new WithApplication(fakeApplication) {
      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = Map(Token.RequestTag -> "token"))

      private val partial1 = Partial(Some("title 1"), Html("some content A"))
      private val partial2 = Partial(Some("title 2"), Html("some content B"))

      when(mockHttpGet.GET[Partial](MockitoMatchers.eq("foo?csrfToken=token"))(any[HttpReads[Partial]], any[HeaderCarrier]))
        .thenReturn(Future.successful(partial1))
        .thenReturn(Future.successful(partial2))

      partialProvider.loadPartialFuture("foo").futureValue should be(partial1)
      partialProvider.loadPartialFuture("foo").futureValue should be(partial2)
    }
  }
}