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

import play.api.mvc.RequestHeader
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.http.CoreGet

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationLong}

trait PartialRetriever extends TemplateProcessor {

  def httpGet: CoreGet

  def partialRetrievalTimeout: Duration = 20.seconds

  protected def loadPartial(
    url: String
  )(implicit
    ec     : ExecutionContext,
    request: RequestHeader
  ): Future[HtmlPartial]

  def getPartial(
    url: String,
    templateParameters: Map[String, String] = Map.empty
  )(implicit
    ec     : ExecutionContext,
    request: RequestHeader
  ): Future[HtmlPartial] =
    loadPartial(url)

  def getPartialContent(
    url               : String,
    templateParameters: Map[String, String] = Map.empty,
    errorMessage      : Html = HtmlFormat.empty
  )(implicit
    ec     : ExecutionContext,
    request: RequestHeader
  ): Future[Html] =
    getPartial(url, templateParameters)
      .map(_.successfulContentOrElse(errorMessage))
}
