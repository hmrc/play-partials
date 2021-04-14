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

import com.google.inject.ImplementedBy
import javax.inject.Singleton
import play.api.http.HeaderNames
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter


/** Just a wrapper around HeaderCarrier to know that it is populated such that it will forward on the encrypted cookies */
// Note, ideally, we would remove the HeaderCarrierForPartials class, and just pass the standard HeaderCarrier around. The cookie header should be
// extracted and passed to http-verb's VERB functions directly.
case class HeaderCarrierForPartials(
  hc : HeaderCarrier
) {
  def toHeaderCarrier = hc
}

// To just forward the encrypted (and signed) cookies - just copy the Headers over in HeaderCarrier
// This get's round the `bootstrap.http.headersAllowlist` config which stops the otherHeaders.Cookies being forwarded
@ImplementedBy(classOf[HeaderCarrierForPartialsConverterImpl])
trait HeaderCarrierForPartialsConverter {

  def fromRequestWithEncryptedCookie(request: RequestHeader): HeaderCarrier =
    headerCarrierEncryptingSessionCookieFromRequest(request).toHeaderCarrier

  implicit def headerCarrierEncryptingSessionCookieFromRequest(implicit request: RequestHeader): HeaderCarrierForPartials = {
    val hc = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    HeaderCarrierForPartials(
      hc = hc.copy(extraHeaders = hc.headers(Seq(HeaderNames.COOKIE)))
    )
  }

  implicit def headerCarrierForPartialsToHeaderCarrier(implicit hcfp: HeaderCarrierForPartials): HeaderCarrier =
    hcfp.toHeaderCarrier
}


@Singleton
class HeaderCarrierForPartialsConverterImpl extends HeaderCarrierForPartialsConverter
