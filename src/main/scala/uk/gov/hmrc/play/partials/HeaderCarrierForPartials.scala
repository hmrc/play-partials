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

import play.api.http.HeaderNames
import play.api.mvc.{Cookies, RequestHeader, Session}
import uk.gov.hmrc.play.audit.http.HeaderCarrier


case class HeaderCarrierForPartials(hc: HeaderCarrier, encodedCookies: String) {
  def toHeaderCarrier = hc.copy(extraHeaders = Seq(HeaderNames.COOKIE -> encodedCookies))
}

trait HeaderCarrierForPartialsConverter {

  def crypto: (String) => String

  private def encryptSessionCookie(rh: RequestHeader): String = {
    val updatedCookies = rh.headers.getAll(HeaderNames.COOKIE).flatMap(Cookies.decode).flatMap {
      case cookie if cookie.name == Session.COOKIE_NAME =>
        Some(cookie.copy(value = crypto(cookie.value)))
      case other => Some(other)
    }

    Cookies.encode(updatedCookies)
  }

  implicit def headerCarrierEncryptingSessionCookieFromRequest(implicit r: RequestHeader) = {
    HeaderCarrierForPartials(HeaderCarrier.fromHeadersAndSession(r.headers, Some(r.session)), encryptSessionCookie(r))
  }

  implicit def headerCarrierForPartialsToHeaderCarrier(implicit hcwc: HeaderCarrierForPartials): HeaderCarrier = {
    hcwc.toHeaderCarrier
  }

}
