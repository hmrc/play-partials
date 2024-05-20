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

import java.util.concurrent.TimeUnit

import com.google.inject.ImplementedBy
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import com.github.benmanes.caffeine.cache.{AsyncLoadingCache, Caffeine, Ticker}

import scala.jdk.FutureConverters._
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
        implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(executor)
        implicit val hc: HeaderCarrier = HeaderCarrier()
        partialFetcher
          .fetchPartial(url)
          .flatMap {
            case s: HtmlPartial.Success => Future.successful(s)
            case f: HtmlPartial.Failure => Future.failed(new RuntimeException(s"Failed to fetch partial. Status: ${f.status}")) // this ensures the failure is not cached
          }
          .asJava.toCompletableFuture
      }

  override protected def loadPartial(url: String)(implicit ec: ExecutionContext, request: RequestHeader): Future[HtmlPartial] =
    cache.get(url).asScala
      .recoverWith {
        case e: Exception =>
          logger.error(s"Could not load partial", e)
          Future.successful(HtmlPartial.Failure())
      }
}

@Singleton
class CachedStaticHtmlPartialRetrieverImpl @Inject()(
  override val httpClientV2: HttpClientV2,
  config      : Config
) extends CachedStaticHtmlPartialRetriever {
  override val refreshAfter: Duration =
    config.getDuration("play-partial.cache.refreshAfter").toMillis.millis

  override val expireAfter: Duration =
    config.getDuration("play-partial.cache.expireAfter").toMillis.millis

  override val maximumEntries: Int =
    config.getInt("play-partial.cache.maxEntries")
}
