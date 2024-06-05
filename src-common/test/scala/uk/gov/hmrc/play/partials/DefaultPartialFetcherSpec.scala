/*
 * Copyright 2024 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{verify => _, _}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}

import scala.concurrent.ExecutionContext.Implicits.global

class DefaultPartialFetcherSpec
  extends AnyWordSpec
     with Matchers
     with WireMockSupport
     with HttpClientV2Support
     with ScalaFutures
     with IntegrationPatience {

  val defaultPartialFetcher = new DefaultPartialFetcher(httpClientV2)

  "DefaultPartialFetcher" should {
    "fetch partial" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      wireMockServer.stubFor(
        WireMock.get(urlEqualTo("/"))
          .willReturn(aResponse().withBody("""PARTIAL_CONTENT""").withStatus(200))
      )

      defaultPartialFetcher
        .fetchPartial(s"$wireMockUrl/")
        .futureValue shouldBe HtmlPartial.Success(title = None, content = Html("PARTIAL_CONTENT"))
    }

    "fetch partial with title" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      wireMockServer.stubFor(
        WireMock.get(urlEqualTo("/"))
          .willReturn(aResponse().withBody("""PARTIAL_CONTENT""").withHeader("X-Title", "asd%20dsa").withStatus(200))
      )

      defaultPartialFetcher
        .fetchPartial(s"$wireMockUrl/")
        .futureValue shouldBe HtmlPartial.Success(title = Some("asd dsa"), content = Html("PARTIAL_CONTENT"))

    }
  }
}
