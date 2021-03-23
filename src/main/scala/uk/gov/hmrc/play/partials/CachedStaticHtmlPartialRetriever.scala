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

import com.google.inject.ImplementedBy
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpClient}
import com.github.benmanes.caffeine.cache.{AsyncLoadingCache, Caffeine, Ticker}

import scala.compat.java8.FutureConverters.{fromExecutor, toJava, toScala}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationLong}


// Note, we're not using plays asyncCacheApi (backed by caffeine/eh-cache) since this api does not offer
// refreshing - i.e. if a result expires, and cannot be loaded (due to error), the result will be unserveable.
// Also pre-caffeine Play 2.8 (i.e. eh-cache & Play 2.7 caffeine) multiple requests did not wait for the same future, but launched multiple futures.
@ImplementedBy(classOf[CachedStaticHtmlPartialRetrieverImpl])
trait CachedStaticHtmlPartialRetriever extends PartialRetriever {

  private val logger = Logger(classOf[CachedStaticHtmlPartialRetriever])

  protected lazy val cacheTicker = Ticker.systemTicker()

  def refreshAfter: Duration

  def expireAfter: Duration

  def maximumEntries: Int

  private[partials] lazy val cache: AsyncLoadingCache[String, HtmlPartial.Success] =
    Caffeine.newBuilder()
      .maximumSize(maximumEntries)
      .ticker(cacheTicker)
      .refreshAfterWrite(refreshAfter.toMillis, TimeUnit.MILLISECONDS)
      .expireAfterWrite(expireAfter.toMillis, TimeUnit.MILLISECONDS)
      .buildAsync { (url, executor) =>
        implicit val ec = fromExecutor(executor)
        implicit val hc = HeaderCarrier()
        toJava(fetchPartial(url)).toCompletableFuture
      }

  override protected def loadPartial(url: String)(implicit ec: ExecutionContext, request: RequestHeader): Future[HtmlPartial] =
    toScala(cache.get(url))
      .recoverWith {
        case e: Exception =>
          logger.error(s"Could not load partial", e)
          Future.successful(HtmlPartial.Failure())
      }

  private def fetchPartial(url: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[HtmlPartial.Success] =
    httpGet.GET[HtmlPartial](url)
      .recover(HtmlPartial.connectionExceptionsAsHtmlPartialFailure)
      .flatMap {
        case s: HtmlPartial.Success => Future.successful(s)
        case f: HtmlPartial.Failure => Future.failed(sys.error(s"Failed to fetch partial. Status: ${f.status}")) // this ensures the failure is not cached
    }
}


@Singleton
class CachedStaticHtmlPartialRetrieverImpl @Inject()(
  http  : HttpClient,
  config: Config
) extends CachedStaticHtmlPartialRetriever {
  override val httpGet: CoreGet = http

  override val refreshAfter: Duration =
    config.getDuration("play-partial.cache.refreshAfter").toMillis.millis

  override val expireAfter: Duration =
    config.getDuration("play-partial.cache.expireAfter").toMillis.millis

  override val maximumEntries: Int =
    config.getInt("play-partial.cache.maxEntries")
}
