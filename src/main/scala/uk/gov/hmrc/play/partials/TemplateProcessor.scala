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
import play.twirl.api.Html

trait TemplateProcessor {

  def processTemplate(template: Html, parameters: Map[String, String])(implicit request: RequestHeader): Html = {
    val templateString = template.toString()
    Html(replaceTemplates(templateString, parameters))
  }

  private def replaceTemplates(s: String, parameters: Map[String, String]): String = {
    type DataList = List[(Int, String, Int)]
    def matchedData(from: Int, l: DataList): DataList = {
      val end = s.lastIndexOf("}}", from)
      if (end == -1) l
      else {
        val begin = s.lastIndexOf("{{", end)
        if (begin == -1) l
        else {
          val template = s.substring(begin, end + 2)
          matchedData(begin - 1, (begin, template, end + 2) :: l)
        }
      }
    }

    val sb = new StringBuilder(s.length)
    var prev = 0
    for ((begin, template, end) <- matchedData(s.length, Nil)) {
      sb.append(s.substring(prev, begin))
      val ident = template.substring(2, template.length - 2)
      sb.append(parameters.getOrElse(ident, template))
      prev = end
    }
    sb.append(s.substring(prev, s.length))
    sb.toString
  }
}
