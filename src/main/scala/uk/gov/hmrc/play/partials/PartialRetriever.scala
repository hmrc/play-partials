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

import play.api.Logger
import play.api.mvc.RequestHeader
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.play.http.HttpGet
import scala.concurrent.Future
import scala.concurrent.duration._

trait PartialRetriever extends TemplateProcessor {

  def httpGet: HttpGet

  def partialRetrievalTimeout: Duration = 20.seconds

  protected def loadPartial(url: String)(implicit request: RequestHeader) : Html

  protected def loadPartialFuture(url: String)(implicit request: RequestHeader) : Future[Partial]

  def get(url: String, templateParameters: Map[String, String] = Map.empty, errorMessage: Html = HtmlFormat.empty)(implicit request: RequestHeader): Html = {
    try {
      processTemplate(loadPartial(url), templateParameters)
    } catch {
      case e: Throwable => {
        val exMessage = Option(e.getCause).getOrElse(e).getMessage
        Logger.warn(s"Cannot load partial from $url: $exMessage")
        errorMessage
      }
    }
  }

}
