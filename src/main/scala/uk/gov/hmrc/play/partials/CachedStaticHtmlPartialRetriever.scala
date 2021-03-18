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

import java.util.concurrent.TimeUnit

import com.google.common.base.Ticker
import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationLong}


// TODO provide injectable instances

trait CachedStaticHtmlPartialRetriever extends PartialRetriever {

  val cacheTicker =  Ticker.systemTicker()

  def refreshAfter: Duration = 60.seconds

  def expireAfter: Duration = 60.minutes

  def maximumEntries: Int = 1000

  private[partials] lazy val cache: LoadingCache[String, HtmlPartial.Success] =
    CacheBuilder.newBuilder()
      .maximumSize(maximumEntries)
      .ticker(cacheTicker)
      .refreshAfterWrite(refreshAfter.toMillis, TimeUnit.MILLISECONDS)
      .expireAfterWrite(expireAfter.toMillis, TimeUnit.MILLISECONDS)
      .build(new CacheLoader[String, HtmlPartial.Success]() {
        def load(url: String) = fetchPartial(url) match {
          case s: HtmlPartial.Success => s
          case f: HtmlPartial.Failure => throw new RuntimeException("Could not load partial")
        } //TODO we could also override reload() and refresh the cache asynchronously: https://code.google.com/p/guava-libraries/wiki/CachesExplained#Refresh
      })

  override protected def loadPartial(url: String)(implicit ec: ExecutionContext, request: RequestHeader) =
    try {
      Future.successful(cache.get(url))
    } catch {
      case e: Exception => Future.successful(HtmlPartial.Failure())
    }

  private def fetchPartial(url: String): HtmlPartial = {
    import ExecutionContext.Implicits.global
    implicit val hc = HeaderCarrier()
    Await.result(httpGet.GET[HtmlPartial](url).recover(HtmlPartial.connectionExceptionsAsHtmlPartialFailure), partialRetrievalTimeout)
  }
}
