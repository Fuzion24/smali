/*
 * Copyright 2011 Steffen Fritzsche.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbtjflex

import sbt._
import Process._
import Keys._
import JFlex.Options
import JFlex.Main
import scala.collection.JavaConversions._
import Project.Initialize

object SbtJFlexPlugin extends Plugin {

  final case class JFlexToolConfiguration(
    dot: Boolean = false,
    dump: Boolean = false,
    verbose: Boolean = false)

  final case class PluginConfiguration(
    grammarSuffix: String = ".flex"
    )

  val jflex = config("jflex")
  val generate = TaskKey[Seq[File]]("generate")
  val jflexDependency = SettingKey[ModuleID]("jflex-dependency")
  val jflexToolConfiguration = SettingKey[JFlexToolConfiguration]("jflex-tool-configuration")
  val jflexPluginConfiguration = SettingKey[PluginConfiguration]("jflex-plugin-configuration")
  val jflexTokensResource = SettingKey[File]("jflex-tokens-resource-directory")

  lazy val jflexSettings: Seq[Project.Setting[_]] = inConfig(jflex)(Seq(
    jflexToolConfiguration := JFlexToolConfiguration(),
    jflexPluginConfiguration := PluginConfiguration(),
    jflexDependency := "de.jflex" % "jflex" % "1.4.3",

    sourceDirectory <<= (sourceDirectory in Compile) { _ / "jflex" },
    javaSource <<= sourceManaged in Compile,
    jflexTokensResource <<= sourceManaged in Compile,

    managedClasspath <<= (classpathTypes in jflex, update) map { (ct, report) =>
      Classpaths.managedJars(jflex, ct, report)
    },

    generate <<= sourceGeneratorTask

  )) ++ Seq(
    unmanagedSourceDirectories in Compile <+= (sourceDirectory in jflex),
    sourceGenerators in Compile <+= (generate in jflex),
    cleanFiles <+= (javaSource in jflex),
    libraryDependencies <+= (jflexDependency in jflex),
    ivyConfigurations += jflex
  )


  private def sourceGeneratorTask = (streams, sourceDirectory in jflex, javaSource in jflex,
    jflexToolConfiguration in jflex, jflexPluginConfiguration in jflex, cacheDirectory) map {
      (out, srcDir, targetDir, tool, options, cache) =>
        val cachedCompile = FileFunction.cached(cache / "flex", inStyle = FilesInfo.lastModified, outStyle = FilesInfo.exists) { (in: Set[File]) =>
          generateWithJFlex(srcDir, targetDir, tool, options, out.log)
        }
        cachedCompile((srcDir ** ("*" + options.grammarSuffix)).get.toSet).toSeq
    }

  private def generateWithJFlex(srcDir: File, target: File, tool: JFlexToolConfiguration,
                                options: PluginConfiguration, log: Logger) = {
    printJFlexOptions(log, tool)

    // prepare target
    target.mkdirs()

    // configure jflex tool
    log.info("JFlex: Using JFlex version %s to generate source files.".format(Main.version))
    Options.dot = tool.dot
    Options.verbose = tool.verbose
    Options.dump = tool.dump
    Options.setDir(target.getPath)

    // process grammars
    val grammars = (srcDir ** ("*" + options.grammarSuffix)).get
    log.info("JFlex: Generating source files for %d grammars.".format(grammars.size))

    // add each grammar file into the jflex tool's list of grammars to process
    grammars foreach { g =>
      Main.generate(g)
      log.info("JFlex: Grammar file '%s' detected.".format(g.getPath))
    }

    (target ** ("*.java")).get.toSet
  }

  private def printJFlexOptions(log: Logger, options: JFlexToolConfiguration) {
    log.debug("JFlex: dump                : " + options.dump)
    log.debug("JFlex: dot                 : " + options.dot)
    log.debug("JFlex: verbose             : " + options.verbose)
  }

}