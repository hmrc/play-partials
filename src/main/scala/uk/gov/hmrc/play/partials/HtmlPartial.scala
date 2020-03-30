/*
 * Copyright 2020 HM Revenue & Customs
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
import play.twirl.api.Html
import play.utils.UriEncoding
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.{HttpException, _}

sealed trait HtmlPartial {
  def successfulContentOrElse(fallbackContent: => Html): Html
  def successfulContentOrEmpty: Html = successfulContentOrElse(Html(""))
}
object HtmlPartial {
  case class Success(title: Option[String], content: Html) extends HtmlPartial {
    def successfulContentOrElse(fallbackContent: => Html) = content
  }
  case class Failure(status: Option[Int] = None, body: String = "") extends HtmlPartial {
    def successfulContentOrElse(fallbackContent: => Html) = fallbackContent
  }

  trait HtmlPartialHttpReads extends HttpReads[HtmlPartial] {
    def read(method: String, url: String, response: HttpResponse) = response.status match {
      case s if s >= 200 && s <= 299 => Success(
        title = response.header("X-Title").map(UriEncoding.decodePathSegment(_, "UTF-8")),
        content = Html(response.body)
      )
      case other =>
        Logger.warn(s"Failed to load partial from $url, received $other")
        Failure(Some(other), response.body)
    }
  }

  implicit val readsPartial = new HtmlPartialHttpReads {}

  val connectionExceptionsAsHtmlPartialFailure: PartialFunction[Throwable, HtmlPartial] = {
    case e@(_: BadGatewayException | _: GatewayTimeoutException) =>
      Logger.warn(s"Failed to load partial", e)
      HtmlPartial.Failure(Some(e.asInstanceOf[HttpException].responseCode))
  }
}
