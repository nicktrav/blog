package controllers

import javax.inject.{Inject, Singleton}

import models.Article
import play.api.Environment
import play.api.mvc._
import play.twirl.api.Html

@Singleton
class Application @Inject() (environment: Environment) extends Controller {

  def index = Action {
    Redirect(routes.Application.tilIndex())
  }

  def tilIndex = Action {
    environment.getExistingFile(s"public/pages/til/index.html")
      .map(path => {
        val content = scala.io.Source.fromFile(path).mkString
        Ok(views.html.til(Html(content)))
      })
      .getOrElse(NotFound)
  }

  def tilArticle(path: String) = Action {
    environment.getExistingFile(s"public/pages/til/$path.html")
      .map(file => {
        val article = new Article(file)
        Ok(views.html.article(article))
      })
      .getOrElse(NotFound)
  }
}