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

import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import play.api.cache.AsyncCacheApi

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationLong}


// TODO provide injectable instances
// Note, clients should be responsible for wrapping apis in cacheApi as required?
// Note behaviour changes: before file would be refreshed every 60 seconds, but not expired until 1 hr.
// the difference is now, after 60 seconds, there will be a cache miss, and client will have to wait until it is reloaded
// - and if it failes, the old value will not be returned...
// also if there is a failure, we now continue to request again and again, where as before, it would wait to request again?
trait CachedStaticHtmlPartialRetriever extends PartialRetriever {

  private val logger = Logger(classOf[CachedStaticHtmlPartialRetriever])

  def cacheApi: AsyncCacheApi

  def expireAfter: Duration = 60.seconds // TODO inject config and move default to reference.conf

  override protected def loadPartial(url: String)(implicit ec: ExecutionContext, request: RequestHeader): Future[HtmlPartial] =
    cacheApi.getOrElseUpdate(url, expiration = expireAfter){
      implicit val hc = HeaderCarrier()
      httpGet.GET[HtmlPartial](url)
        .recover(HtmlPartial.connectionExceptionsAsHtmlPartialFailure)
        .flatMap {
          case s: HtmlPartial.Success => Future.successful(s)
          case f: HtmlPartial.Failure => val msg = s"Could not load partial. Status: ${f.status}"
                                         logger.error(msg)
                                         Future.failed(sys.error(s"Could not load partial. Status: ${f.status}")) // this ensures the failure is not cached
      }
    }.recover {
      case e => HtmlPartial.Failure()
    }
}
