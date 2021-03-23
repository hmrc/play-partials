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
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeHeaders, FakeRequest, WithApplication}
import play.api.test.CSRFTokenHelper._
import play.filters.csrf.CSRF
import play.twirl.api.Html
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpReads}

import scala.concurrent.{ExecutionContext, Future}

class FormPartialSpec
  extends AnyWordSpecLike
     with Matchers
     with MockitoSugar
     with ArgumentMatchersSugar
     with BeforeAndAfterEach
     with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits.global

  val fakeApplication =
    new GuiceApplicationBuilder()
      .configure("csrf.sign.tokens" -> false)
      .build()

  val mockHttpGet = mock[CoreGet]

  val partialProvider = new FormPartialRetriever {
    override val httpGet: CoreGet =
      mockHttpGet

    override val headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter =
      fakeApplication.injector.instanceOf[HeaderCarrierForPartialsConverter]
  }

  override protected def beforeEach() = {
    super.beforeEach()

    reset(mockHttpGet)
  }

  "get" should {
    "retrieve HTML from the given URL" in new WithApplication(fakeApplication) {
      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "").withCSRFToken

      val csrfValue = CSRF.getToken(request).get.value

      when(mockHttpGet.GET[HtmlPartial](any[String], any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content A"))),
          Future.successful(HtmlPartial.Success(title = None, content = Html("some content B")))
        )

      partialProvider.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content A"))

      partialProvider.getPartial("foo").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content B"))

      verify(mockHttpGet, times(2))
        .GET(eqTo(s"foo?csrfToken=${csrfValue}"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext])
    }

    "retrieve HTML from the given URL, which includes query string" in new WithApplication(fakeApplication) {
      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "").withCSRFToken

      val csrfValue = CSRF.getToken(request).get.value

      when(mockHttpGet.GET[HtmlPartial](any[String], any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(HtmlPartial.Success(title = None, content = Html("some content C"))))

      partialProvider.getPartial("foo?attrA=valA&attrB=valB").futureValue shouldBe HtmlPartial.Success(title = None, content = Html("some content C"))

      verify(mockHttpGet)
        .GET(eqTo(s"foo?attrA=valA&attrB=valB&csrfToken=${csrfValue}"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext])
    }

    "return HtmlPartial.Failure when there is an exception retrieving the partial from the URL" in new WithApplication(fakeApplication) {
      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "").withCSRFToken

      val csrfValue = CSRF.getToken(request).get.value

      when(mockHttpGet.GET[HtmlPartial](any[String], any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(HtmlPartial.Failure()))

      partialProvider.getPartial("foo").futureValue should be (HtmlPartial.Failure())

      verify(mockHttpGet)
        .GET(eqTo(s"foo?csrfToken=${csrfValue}"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext])
    }

    "return provided Html when there is an exception retrieving the partial from the URL" in new WithApplication(fakeApplication) {
      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "").withCSRFToken

      val csrfValue = CSRF.getToken(request).get.value

      when(mockHttpGet.GET[HtmlPartial](any[String], any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(HtmlPartial.Failure()))

      partialProvider.getPartialContentAsync(url = "foo", errorMessage = Html("something went wrong")).futureValue.body should be("something went wrong")

      verify(mockHttpGet)
        .GET(eqTo(s"foo?csrfToken=${csrfValue}"), any, any)(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext])
    }
  }

  "processTemplate" should {

    "return the original template if there is no csrf token in it" in new WithApplication(fakeApplication) {

      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "").withCSRFToken

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
      implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "").withCSRFToken

      val csrfValue = CSRF.getToken(request).get.value

      val template =
        """
          |<div>hello {{param}} {{csrfToken}}</div>
        """.stripMargin

      val expectedTemplate =
        s"""
          |<div>hello world ${csrfValue}</div>
        """.stripMargin

      partialProvider.processTemplate(Html(template), Map("param" -> "world")).body shouldBe expectedTemplate
    }
  }

  "urlWithCsrfToken" should {
    implicit val request = FakeRequest("GET", "/getform", FakeHeaders(), "").withCSRFToken

    val csrfValue = CSRF.getToken(request).get.value

    "add a query string" in new WithApplication(fakeApplication) {
      val url = partialProvider.urlWithCsrfToken("/contact/problem_reports")
      url shouldBe s"/contact/problem_reports?csrfToken=${csrfValue}"
    }

    "append to the existing query string with 1 value" in new WithApplication(fakeApplication) {
      val url = partialProvider.urlWithCsrfToken("/contact/problem_reports?service=yta")
      url shouldBe s"/contact/problem_reports?service=yta&csrfToken=${csrfValue}"
    }

    "append to the existing query string with 2 values" in new WithApplication(fakeApplication) {
      val url = partialProvider.urlWithCsrfToken("/contact/problem_reports?service=yta&secure=true")
      url shouldBe s"/contact/problem_reports?service=yta&secure=true&csrfToken=${csrfValue}"
    }
  }
}
