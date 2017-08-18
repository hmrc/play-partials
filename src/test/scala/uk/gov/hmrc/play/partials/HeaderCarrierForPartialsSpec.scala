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
import play.api.http.HeaderNames
import play.api.mvc.{Cookie, Cookies, Session}
import play.api.test.{FakeHeaders, FakeRequest}

class HeaderCarrierForPartialsSpec extends WordSpecLike with Matchers {

  object Converter extends HeaderCarrierForPartialsConverter {
    def encrypt(value: String) = value

    override def crypto: (String) => String = encrypt
  }

  "HeaderCarrierForPartials" should {
    "encrypt request cookie" in {

      import Converter._

      def assertHeaderCarrier(implicit hcfp: HeaderCarrierForPartials): Unit = {
        val hc = hcfp.toHeaderCarrier
        val cookiesHeader = hc.headers.filter(_._1 == HeaderNames.COOKIE).head._2
        Cookies.decodeCookieHeader(cookiesHeader) should contain (Cookie("cookieName", "cookieValue"))
      }

     val cookieWithUnencryptedSession = Cookies.encodeCookieHeader(Seq(Cookie("cookieName", "cookieValue"), Cookie(Session.COOKIE_NAME, "unencrypted")))

      val headers = new FakeHeaders(Seq(
        ("headerName", "headerValue"),
        (HeaderNames.COOKIE, cookieWithUnencryptedSession)
      ))
      implicit val request = FakeRequest("GET", "http:/localhost/", headers, Nil)

      assertHeaderCarrier
    }
  }
}
