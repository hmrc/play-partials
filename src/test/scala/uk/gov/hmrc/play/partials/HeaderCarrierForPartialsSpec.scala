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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Cookie, Cookies, CookieHeaderEncoding, SessionCookieBaker}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.http.HeaderCarrier

class HeaderCarrierForPartialsSpec extends AnyWordSpecLike with Matchers {

  val fakeApplication = new GuiceApplicationBuilder().configure("csrf.sign.tokens" -> false).build()

  object Converter extends HeaderCarrierForPartialsConverter {
    override def crypto: String => String =
      s => s.reverse

    override val sessionCookieBaker: SessionCookieBaker =
      fakeApplication.injector.instanceOf[SessionCookieBaker]

    override val cookieHeaderEncoding: CookieHeaderEncoding =
      fakeApplication.injector.instanceOf[CookieHeaderEncoding]
  }

  "HeaderCarrierForPartials" should {
    "encrypt request cookie" in {
     val encryptableCookieName =
       Converter.sessionCookieBaker.COOKIE_NAME

     val cookieWithUnencryptedSession =
       Cookies.encodeCookieHeader(Seq(
         Cookie("cookieName", "cookieValue"),
         Cookie(encryptableCookieName, "unencrypted")
       ))

      val headers =
        new FakeHeaders(Seq(
          ("headerName", "headerValue"),
          (HeaderNames.COOKIE, cookieWithUnencryptedSession)
        ))

      val request =
        FakeRequest("GET", "http:/localhost/", headers, Nil)

      val hc = Converter.headerCarrierForPartials(request)

      val cookiesHeader = hc.headers(Seq(HeaderNames.COOKIE)).head._2
      val cookies = Cookies.decodeCookieHeader(cookiesHeader)
      cookies should contain (Cookie("cookieName", "cookieValue"))
      cookies should contain (Cookie(encryptableCookieName, Converter.crypto("unencrypted")))
    }
  }
}
