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

import play.api.http.HeaderNames
import play.api.mvc.{Cookie, CookieHeaderEncoding, RequestHeader, SessionCookieBaker}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter


trait CookieForwarder {

  def applicationCrypto: ApplicationCrypto
  def sessionCookieBaker: SessionCookieBaker
  def cookieHeaderEncoding: CookieHeaderEncoding

  private[partials] def encryptCookie(cookie: String): String =
    applicationCrypto.SessionCookieCrypto.encrypt(PlainText(cookie)).toString

  private def encryptSessionCookie(request: RequestHeader): String = {
    val cookies: Seq[Cookie] =
      request
        .headers.getAll(HeaderNames.COOKIE)
        .flatMap(cookieHeaderEncoding.decodeCookieHeader)

    val updatedCookies =
      cookies
        .map {
          case cookie if cookie.name == sessionCookieBaker.COOKIE_NAME =>
            cookie.copy(value = encryptCookie(cookie.value))
          case other => other
        }

    cookieHeaderEncoding.encodeCookieHeader(updatedCookies)
  }

  def cookieForwardingHeaderCarrier(request: RequestHeader): HeaderCarrier = {
    val hc = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val encrypedCookies = encryptSessionCookie(request)
    hc.copy(otherHeaders = hc.otherHeaders.filterNot(_._1 == HeaderNames.COOKIE) ++ Seq(HeaderNames.COOKIE -> encrypedCookies))
  }
}
