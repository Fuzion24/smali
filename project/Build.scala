import sbt._
import Keys._
import com.github.hexx.GithubRepoPlugin._
import com.typesafe.sbt.SbtGit.GitKeys._

object Versions {
  val scala     = "2.10.2"
  val scalatest = "1.9.1"
}

object Publish {
  val organization = "android"
  val version      = "0.17"
  val localRepo    = Path.userHome / "maven" / "android" / "smali"
  val githubRepo   = ""
}

object Smali extends Build {

  lazy val a_smali      =  Project("smali",     file("smali"),  settings = smaliSettings).
    dependsOn(dexlib2).aggregate(dexlib2).
    dependsOn(smaliUtil).aggregate(smaliUtil).
    settings(sbtantlr.SbtAntlrPlugin.antlrSettings : _*).
    settings(sbtjflex.SbtJFlexPlugin.jflexSettings : _*)

  lazy val dexlib2    =  Project("dexlib2",    file("dexlib2"), settings = dexlib2Settings).
    dependsOn(smaliUtil).aggregate(smaliUtil)

  lazy val dexlib     =  Project("dexlib",     file("dexlib"), settings = dexlibSettings).
    dependsOn(smaliUtil).aggregate(smaliUtil)

  lazy val smaliUtil  =  Project("smaliUtil", file("util"),   settings = utilSettings)

  lazy val dexlibSettings = Defaults.defaultSettings ++ githubRepoSettings ++ Seq(
    name         := "Dexlib",
    scalaVersion := Versions.scala,
    version      := Publish.version,
    organization := Publish.organization,    
    localRepo    := Publish.localRepo,
    githubRepo   := Publish.githubRepo,
    scalaVersion := Versions.scala,
    libraryDependencies ++= Seq(
      "com.google.code.findbugs" % "jsr305" % "1.3.9",
      "com.google.collections" % "google-collections" % "1.0"
    )
  )

  lazy val dexlib2Settings = Defaults.defaultSettings ++ githubRepoSettings ++ Seq(
    name         := "Dexlib2",
    scalaVersion := Versions.scala,
    version      := Publish.version,
    organization := Publish.organization,    
    localRepo    := Publish.localRepo,
    githubRepo   := Publish.githubRepo,
    scalaVersion := Versions.scala,
    libraryDependencies ++= Seq(
      "com.google.code.findbugs" % "jsr305" % "1.3.9",
      "com.google.collections" % "google-collections" % "1.0"
    )
  )

  lazy val utilSettings = Defaults.defaultSettings ++ githubRepoSettings ++ Seq(
    name         := "Util",
    version      := Publish.version,
    organization := Publish.organization,    
    localRepo    := Publish.localRepo,
    githubRepo   := Publish.githubRepo,
    scalaVersion := Versions.scala,
    libraryDependencies ++= Seq(
      "commons-cli" % "commons-cli" % "1.2",
      "com.google.code.findbugs" % "jsr305" % "1.3.9",
      "com.google.guava" % "guava" % "14.0"
    )
  )

  lazy val smaliSettings = Defaults.defaultSettings ++ githubRepoSettings ++ Seq(
    name         := "Smali",
    version      := Publish.version,
    organization := Publish.organization,    
    localRepo    := Publish.localRepo,
    githubRepo   := Publish.githubRepo,
    scalaVersion := Versions.scala,
    libraryDependencies ++= Seq(
      "commons-cli" % "commons-cli" % "1.2",
      "com.google.guava" % "guava" % "14.0"
    )
  )

}
