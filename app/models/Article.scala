package models

import java.io.File

import play.twirl.api.Html

class Article(file: File) {

  val fileName: String = file.getName
  val title: String = "test"

  lazy val content: Html = {
    Html(scala.io.Source.fromFile(file).mkString)
  }
}
