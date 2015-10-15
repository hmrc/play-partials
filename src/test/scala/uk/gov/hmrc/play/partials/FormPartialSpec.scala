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
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import play.api.test.{FakeApplication, FakeHeaders, FakeRequest, WithApplication}
import play.filters.csrf.CSRF.Token
import play.twirl.api.Html
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.{HttpException, HttpGet, HttpReads}

import scala.concurrent.Future

class FormPartialSpec extends WordSpecLike with Matchers with MockitoSugar with BeforeAndAfterEach {

  val fakeApplication = FakeApplication(additionalConfiguration = Map("csrf.sign.tokens" -> false))

  val mockHttpGet = mock[HttpGet]

  val partialProvider = new FormPartialRetriever {
    override val httpGet: HttpGet = mockHttpGet

    override val crypto = c _

    private def c(value: String) = value
  }

  override protected def beforeEach() = {
    super.beforeEach()

    reset(mockHttpGet)
  }

  "get" should {

    "retrieve HTML from the given URL" in new WithApplication(fakeApplication) {

      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = Map(Token.RequestTag -> "token"))

      when(mockHttpGet.GET[Html](MockitoMatchers.eq("foo?csrfToken=token"))(any[HttpReads[Html]], any[HeaderCarrier]))
        .thenReturn(Future.successful(Html("some content A")))
        .thenReturn(Future.successful(Html("some content B")))

      partialProvider.get("foo").body should be("some content A")
      partialProvider.get("foo").body should be("some content B")
    }

    "retrieve HTML from the given URL, which includes query string" in new WithApplication(fakeApplication) {

      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = Map(Token.RequestTag -> "token"))

      when(mockHttpGet.GET[Html](MockitoMatchers.eq("foo?attrA=valA&attrB=valB&csrfToken=token"))(any[HttpReads[Html]], any[HeaderCarrier]))
        .thenReturn(Future.successful(Html("some content C")))

      partialProvider.get("foo?attrA=valA&attrB=valB").body should be("some content C")
    }

    "return HtmlFormat.empty when there is an exception retrieving the partial from the URL" in new WithApplication(fakeApplication) {

      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = Map(Token.RequestTag -> "token"))

      when(mockHttpGet.GET[Html](MockitoMatchers.eq("foo?csrfToken=token"))(any[HttpReads[Html]], any[HeaderCarrier]))
        .thenReturn(Future.failed(new HttpException("error", 404)))

      partialProvider.get("foo").body should be("")
    }

    "return provided Html when there is an exception retrieving the partial from the URL" in new WithApplication(fakeApplication) {

      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = Map(Token.RequestTag -> "token"))

      when(mockHttpGet.GET[Html](MockitoMatchers.eq("foo?csrfToken=token"))(any[HttpReads[Html]], any[HeaderCarrier]))
        .thenReturn(Future.failed(new HttpException("error", 404)))

      partialProvider.get(url = "foo", errorMessage = Html("something went wrong")).body should be("something went wrong")
    }

  }

  "processTemplate" should {

    "return the original template if there is no csrf token in it" in new WithApplication(fakeApplication) {

      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = Map(Token.RequestTag -> "token"))

      val template =
        """
          |<div>hello {{param}}</div>
        """.stripMargin

      val expectedTemplate =
        """
          |<div>hello world</div>
        """.stripMargin

      partialProvider.processTemplate(Html(template), Map("param" -> "world")).body shouldBe expectedTemplate
    }

    "use empty string for csrf token if there is no csrf token in the request" in new WithApplication(fakeApplication) {
      implicit val request = FakeRequest()

      val template =
        """
          |<div>hello {{param}} {{csrfToken}}</div>
        """.stripMargin

      val expectedTemplate =
        """
          |<div>hello world </div>
        """.stripMargin


      partialProvider.processTemplate(Html(template), Map("param" -> "world")).body shouldBe expectedTemplate
    }

    "return the template with the CSRF token placeholder replaced with the actual value" in new WithApplication(fakeApplication) {
      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = Map(Token.RequestTag -> "token"))

      val template =
        """
          |<div>hello {{param}} {{csrfToken}}</div>
        """.stripMargin

      val expectedTemplate =
        """
          |<div>hello world token</div>
        """.stripMargin


      partialProvider.processTemplate(Html(template), Map("param" -> "world")).body shouldBe expectedTemplate
    }

  }

  "urlWithCsrfToken" should {
    implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = Map(Token.RequestTag -> "token"))

    "add a query string" in new WithApplication(fakeApplication) {
      val url = partialProvider.urlWithCsrfToken("/contact/problem_reports")
      url shouldBe "/contact/problem_reports?csrfToken=token"
    }

    "append to the existing query string with 1 value" in new WithApplication(fakeApplication) {
      val url = partialProvider.urlWithCsrfToken("/contact/problem_reports?service=yta")
      url shouldBe "/contact/problem_reports?service=yta&csrfToken=token"
    }

    "append to the existing query string with 2 values" in new WithApplication(fakeApplication) {
      val url = partialProvider.urlWithCsrfToken("/contact/problem_reports?service=yta&secure=true")
      url shouldBe "/contact/problem_reports?service=yta&secure=true&csrfToken=token"
    }

  }
}
