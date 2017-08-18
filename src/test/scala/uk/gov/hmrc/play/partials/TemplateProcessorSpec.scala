/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatest.{Matchers, WordSpecLike}
import play.api.test.FakeRequest
import play.twirl.api.Html

class TemplateProcessorSpec extends WordSpecLike with Matchers {

  implicit val request = FakeRequest()

  val processor = new TemplateProcessor {}

  "processTemplate" should {

    "return the original template if there is no parameter match" in {
      val template =
        """
          |<div>hello {{csrfToken}}</div>
        """.stripMargin

      processor.processTemplate(Html(template), Map.empty).body shouldBe template
    }

    "return the template with the provided placeholders replaced with the actual values" in {
      val template =
        """
          |<div>hello {{csrfToken}} world {{name}} {}</div>
        """.stripMargin

      val expectedResult =
        """
          |<div>hello token world John {}</div>
        """.stripMargin

      processor.processTemplate(Html(template), Map("csrfToken" -> "token", "name" -> "John")).body shouldBe expectedResult
    }

  }

}
