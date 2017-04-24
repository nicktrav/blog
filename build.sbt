name := "blog"
version := "1.0"

scalaVersion := "2.11.8"
libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test
)

lazy val `blog` = (project in file(".")).enablePlugins(PlayScala)

assemblyJarName in assembly := "server.jar"

mainClass in assembly := Some("play.core.server.ProdServerStart")

fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

assemblyMergeStrategy in assembly := {
  case r if r.startsWith("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", m) if m.equalsIgnoreCase("MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.first
}
