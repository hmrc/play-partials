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

import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import play.api.test.{FakeApplication, FakeHeaders, FakeRequest, WithApplication}
import play.filters.csrf.CSRF.Token
import play.twirl.api.Html
import uk.gov.hmrc.play.http.HttpGet

class CachedStaticFormPartialSpec extends WordSpecLike with Matchers with MockitoSugar with BeforeAndAfterEach {

  val fakeApplication = FakeApplication(additionalConfiguration = Map("csrf.sign.tokens" -> false))

  val partialProvider = new CachedStaticFormPartial {
    override def httpGet: HttpGet = ???
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
}
