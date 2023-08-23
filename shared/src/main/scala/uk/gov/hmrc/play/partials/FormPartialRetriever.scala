/*
 * Copyright 2023 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import play.api.mvc.RequestHeader
import play.twirl.api.Html
import uk.gov.hmrc.http.{CoreGet, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[FormPartialRetrieverImpl])
trait FormPartialRetriever extends PartialRetriever {

  def headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter

  override def processTemplate(template: Html, parameters: Map[String, String])(implicit request: RequestHeader): Html = {
    val formParameters = parameters + ("csrfToken" -> getCsrfToken)
    super.processTemplate(template, formParameters)
  }

  override protected def loadPartial(url: String)(implicit ec: ExecutionContext, request: RequestHeader): Future[HtmlPartial] = {
    implicit val hc = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(request)
    httpGet.GET[HtmlPartial](urlWithCsrfToken(url))
      .recover(HtmlPartial.connectionExceptionsAsHtmlPartialFailure)
  }

  protected def getCsrfToken(implicit request: RequestHeader): String = {
    import play.filters.csrf.CSRF
    CSRF.getToken(request).fold("")(_.value)
  }

  protected[partials] def urlWithCsrfToken(url: String)(implicit request: RequestHeader): String = {
    val sep = if (url.contains("?")) "&" else "?"
    s"$url${sep}csrfToken=$getCsrfToken"
  }
}


@Singleton
class FormPartialRetrieverImpl @Inject()(
  httpClient: HttpClient,
  override val headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter
) extends FormPartialRetriever {
  override val httpGet: CoreGet = httpClient
}
