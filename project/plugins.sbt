libraryDependencies ++= Seq(
  "org.antlr" % "antlr" % "3.5",
  "de.jflex" % "jflex" % "1.4.3"
)

resolvers ++= Seq("fuzion24 Releases" at "http://fuzion24.github.io/maven/releases")

addSbtPlugin("com.github.hexx" % "sbt-github-repo" % "0.1.0")