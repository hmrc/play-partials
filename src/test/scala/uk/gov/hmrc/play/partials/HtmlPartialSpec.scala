/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException, HttpResponse}

class HtmlPartialSpec extends WordSpecLike with Matchers {

  "Reading an HtmlPartial from an HTTP Response" should {

    "return Success when the response status is 200" in new TestCase {
      val response = HttpResponse(responseStatus = 200, responseString = Some(content))

      val partial = HtmlPartial.readsPartial.read("someMethod", "someUrl", response)
      partial should be (an [HtmlPartial.Success])
      partial.asInstanceOf[HtmlPartial.Success].title should be (None)
      partial.asInstanceOf[HtmlPartial.Success].content.body should be (content)
    }

    "return Success with a title if present in the X-title header" in new TestCase {
      val response = HttpResponse(responseStatus = 200, responseHeaders = Map("X-Title" -> Seq(encoded_title)), responseString = Some(content))

      val partial = HtmlPartial.readsPartial.read("someMethod", "someUrl", response)
      partial should be (an [HtmlPartial.Success])
      partial.asInstanceOf[HtmlPartial.Success].title should be (Some(title))
      partial.asInstanceOf[HtmlPartial.Success].content.body should be (content)
    }

    "return Success with empty content if the response body is empty" in new TestCase {
      val response = HttpResponse(responseStatus = 200)

      val partial = HtmlPartial.readsPartial.read("someMethod", "someUrl", response)
      partial should be (an [HtmlPartial.Success])
      partial.asInstanceOf[HtmlPartial.Success].content.body should be ("")
    }

    "return Success with no content if the response status is 204" in new TestCase {
      val response = HttpResponse(responseStatus = 204)

      val partial = HtmlPartial.readsPartial.read("someMethod", "someUrl", response)
      partial should be (an [HtmlPartial.Success])
      partial.asInstanceOf[HtmlPartial.Success].content.body should be ("")
    }

    "return Failure with the response body if the response status is between 400 and 599" in new TestCase {
      val responseBody = """{"error": "it's all gone horribley wrong"}"""
      for (status <- 400 to 599) {
        val response = HttpResponse(responseStatus = status, responseString = Some(responseBody))

        val partial = HtmlPartial.readsPartial.read("someMethod", "someUrl", response)
        partial should be (an [HtmlPartial.Failure])
        partial.asInstanceOf[HtmlPartial.Failure].body should be (responseBody)
      }
    }

    "return Failure with no body if the response status is between 400 and 599 but no response body is provided" in new TestCase {
      for (status <- 400 to 599) {
        val response = HttpResponse(responseStatus = status, responseString = Some(""))

        val partial = HtmlPartial.readsPartial.read("someMethod", "someUrl", response)
        partial should be (an [HtmlPartial.Failure])
        partial.asInstanceOf[HtmlPartial.Failure].body should be (empty)
      }
    }
  }

  "HttpReads.connectionExceptionsAsHtmlPartialFailure" should {
    "Turn a BadGatewayException into a Failure" in {
      HtmlPartial.connectionExceptionsAsHtmlPartialFailure(new BadGatewayException("sdf")) should be (HtmlPartial.Failure(Some(502)))
    }
    "Turn a GatewayTimeoutException into a Failure" in {
      HtmlPartial.connectionExceptionsAsHtmlPartialFailure(new GatewayTimeoutException("sdf")) should be (HtmlPartial.Failure(Some(504)))
    }
    "Ignore other types of exception" in {
      a [MatchError] should be thrownBy HtmlPartial.connectionExceptionsAsHtmlPartialFailure(new RuntimeException("sdf"))
    }
  }

  trait TestCase {
    val title = "test title"
    val encoded_title = UriEncoding.encodePathSegment(title, "UTF-8")
    val content = "some content"
  }
}
