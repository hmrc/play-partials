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

import org.scalatest.{Matchers, WordSpecLike}
import play.utils.UriEncoding
import uk.gov.hmrc.play.http.HttpResponse

class PartialSpec extends WordSpecLike with Matchers {

  "partial" should {

    "read title and content from HttpRequest" in {
      val title = "test title"
      val encoded_title = UriEncoding.encodePathSegment(title, "UTF-8")
      val content = "some lurverly content"
      val response = HttpResponse(200, None, Map("X-Title" -> Seq(encoded_title)), Some(content))

      val partial = Partial.readsPartial.read("someMethod", "someUrl", response)
      partial.title should be (Some(title))
      partial.content.body should be (content)

    }

    "read title and content from HttpRequest with not X-title header" in {
      val title = "test title"
      val encoded_title = UriEncoding.encodePathSegment(title, "UTF-8")
      val content = "some lurverly content"
      val response = HttpResponse(200, None, Map("anything" -> Seq(encoded_title)), Some(content))

      val partial = Partial.readsPartial.read("someMethod", "someUrl", response)

      partial.title should be (None)
      partial.content.body should be (content)

    }

    "read title and content from HttpRequest with not X-title header and no content" in {
      val title = "test title"
      val encoded_title = UriEncoding.encodePathSegment(title, "UTF-8")
      val response = HttpResponse(200, None, Map("anything" -> Seq(encoded_title)), None)

      val partial = Partial.readsPartial.read("someMethod", "someUrl", response)

      partial.title should be (None)
      partial.content.body should be ("")

    }
  }
}
