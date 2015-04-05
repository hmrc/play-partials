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

import java.util.concurrent.{ExecutionException, TimeUnit}

import com.google.common.base.Ticker
import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import play.api.Logger
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.HttpGet

import scala.concurrent._
import scala.concurrent.duration._

trait CachedStaticHtmlPartial {

  val cacheTicker = new Ticker {
    override def read() = System.currentTimeMillis()
  }

  def httpGet: HttpGet

  def partialRetrievalTimeout: Duration = 10.seconds

  def refreshAfter: Duration = 60.seconds

  def expireAfter: Duration = 60.minutes

  def maximumEntries: Int = 1000

  private[partials] lazy val cache: LoadingCache[String, Html] = CacheBuilder.newBuilder()
    .maximumSize(maximumEntries)
    .ticker(cacheTicker)
    .refreshAfterWrite(refreshAfter.toMillis, TimeUnit.MILLISECONDS)
    .expireAfterWrite(expireAfter.toMillis, TimeUnit.MILLISECONDS)
    .build(new CacheLoader[String, Html]() {
      override def load(url: String) : Html = {
        loadPartial(url)
      } //TODO we could also override reload() and refresh the cache asynchronously: https://code.google.com/p/guava-libraries/wiki/CachesExplained#Refresh
  })

  def get(url: String, errorMessage: Html = HtmlFormat.empty): Html = {
    try {
      cache.get(url)
    } catch {
      case e: ExecutionException => Logger.warn(s"Cannot refresh cached partial from $url: ${e.getCause.getMessage}"); errorMessage
    }
  }

  private def loadPartial(url: String) : Html = {
    implicit val hc = HeaderCarrier()
    Await.result(httpGet.GET[Html](url), partialRetrievalTimeout)
  }

}
