/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeApplication, FakeHeaders, FakeRequest, WithApplication}
import play.filters.csrf.CSRF.Token
import play.twirl.api.Html
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpException, HttpGet, HttpReads}

import scala.concurrent.Future

class FormPartialSpec extends WordSpecLike with Matchers with MockitoSugar with BeforeAndAfterEach {

  val fakeApplication = new GuiceApplicationBuilder().configure("csrf.sign.tokens" -> false).build()

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

  private def csrfTags(token: String) = Map(Token.NameRequestTag -> "csrfToken", Token.RequestTag -> token)

  "get" should {

    "retrieve HTML from the given URL" in new WithApplication(fakeApplication) {

      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = csrfTags("token"))

      when(mockHttpGet.GET[HtmlPartial](MockitoMatchers.eq("foo?csrfToken=token"))(any[HttpReads[HtmlPartial]], any[HeaderCarrier]))
        .thenReturn(Future.successful(HtmlPartial.Success(title = None, content = Html("some content A"))))
        .thenReturn(Future.successful(HtmlPartial.Success(title = None, content = Html("some content B"))))

      val p1 = partialProvider.getPartial("foo").asInstanceOf[HtmlPartial.Success]
      p1.title should be (None)
      p1.content.body should be ("some content A")

      val p2 = partialProvider.getPartial("foo").asInstanceOf[HtmlPartial.Success]
      p2.title should be (None)
      p2.content.body should be ("some content B")
    }

    "retrieve HTML from the given URL, which includes query string" in new WithApplication(fakeApplication) {

      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = csrfTags("token"))

      when(mockHttpGet.GET[HtmlPartial](MockitoMatchers.eq("foo?attrA=valA&attrB=valB&csrfToken=token"))(any[HttpReads[HtmlPartial]], any[HeaderCarrier]))
        .thenReturn(Future.successful(HtmlPartial.Success(title = None, content = Html("some content C"))))

      val p = partialProvider.getPartial("foo?attrA=valA&attrB=valB").asInstanceOf[HtmlPartial.Success]
      p.title should be (None)
      p.content.body should be ("some content C")
    }

    "return HtmlPartial.Failure when there is an exception retrieving the partial from the URL" in new WithApplication(fakeApplication) {

      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = csrfTags("token"))

      when(mockHttpGet.GET[HtmlPartial](MockitoMatchers.eq("foo?csrfToken=token"))(any[HttpReads[HtmlPartial]], any[HeaderCarrier]))
        .thenReturn(Future.successful(HtmlPartial.Failure()))

      partialProvider.getPartial("foo") should be (HtmlPartial.Failure())
    }

    "return provided Html when there is an exception retrieving the partial from the URL" in new WithApplication(fakeApplication) {

      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = csrfTags("token"))

      when(mockHttpGet.GET[HtmlPartial](MockitoMatchers.eq("foo?csrfToken=token"))(any[HttpReads[HtmlPartial]], any[HeaderCarrier]))
        .thenReturn(Future.successful(HtmlPartial.Failure()))

      partialProvider.getPartialContent(url = "foo", errorMessage = Html("something went wrong")).body should be("something went wrong")
    }

  }

  "processTemplate" should {

    "return the original template if there is no csrf token in it" in new WithApplication(fakeApplication) {

      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = csrfTags("token"))

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
      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = csrfTags("token"))

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
    implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "", tags = csrfTags("token"))

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
