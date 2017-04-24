package models

import java.io.File

import scala.util.matching.Regex

object DatedArticle {

  val pattern: Regex = ".*(\\d{4})(\\d{2})(\\d{2})-(.*).md".r

  def fromFile(file: File): Option[DatedArticle] = {
    if (!isDatedArticle(file)) return None
    Some(new DatedArticle(file))
  }

  private def isDatedArticle(file: File): Boolean = {
    file.getAbsolutePath.matches(pattern.toString())
  }
}

class DatedArticle(file: File) extends Article(file) {

  override def toString: String = s"$dateString - $title"

  private def dateString: String = {
    DatedArticle.pattern.findFirstMatchIn(file.getName).map { m =>
      s"${m.group(1).toInt}-${m.group(2).toInt}-${m.group(3).toInt}"
    }.get
  }
}
