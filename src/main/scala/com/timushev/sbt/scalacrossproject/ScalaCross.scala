package com.timushev.sbt.scalacrossproject

import java.lang.reflect.{Field, Modifier}

import sbt.Keys._
import sbt._
import sbtcrossproject.{CrossProject, JVMPlatform, Platform}

case class ScalaPlatform(platform: Platform, version: String) extends Platform {
  def identifier: String = s"${platform.identifier}-$version"
  def sbtSuffix: String = s"${platform.sbtSuffix}_${version.replace('.', '_')}"
  def enable(project: Project): Project =
    platform.enable(project
      .settings(
        Seq(Compile, Test).flatMap(inConfig(_) {
          unmanagedResourceDirectories ++= {
            unmanagedSourceDirectories.value
              .map(src => (src / ".." / "resources").getCanonicalFile)
              .filterNot(unmanagedResourceDirectories.value.contains)
              .distinct
          }
        })
      ))

  @deprecated("Will be removed", "0.3.0")
  val crossBinary: CrossVersion = CrossVersion.binary

  @deprecated("Will be removed", "0.3.0")
  val crossFull: CrossVersion = CrossVersion.full
}

trait ScalaCross {

  def ScalaPlatform(platform: Platform, version: String): ScalaPlatform =
    com.timushev.sbt.scalacrossproject.ScalaPlatform(platform, version)

  def ScalaPlatform(version: String): ScalaPlatform =
    ScalaPlatform(JVMPlatform, version)

  implicit class ScalaCrossProjectOps(project: CrossProject) {

    def scala(version: String): CrossProject = {
      def withProjects(project: CrossProject, projects: Map[Platform, Project]): CrossProject = {
        val newProject = project.configurePlatforms()(identity)

        val field = classOf[CrossProject].getDeclaredField("projects")
        field.setAccessible(true)

        val modifiersField = classOf[Field].getDeclaredField("modifiers")
        modifiersField.setAccessible(true)
        modifiersField.setInt(field, field.getModifiers & ~Modifier.FINAL)

        field.set(newProject, projects)

        newProject
      }

      withProjects(project, project.projects.map {
        case (ScalaPlatform(platform, `version`), project) => platform -> project
        case e => e
      })
    }

    def scalaSettings(ss: Def.SettingsDefinition*): CrossProject =
      scalaConfigure(_.settings(ss: _*))

    def scalaSettings(version: String)(
      ss: Def.SettingsDefinition*): CrossProject =
      scalaConfigure(version)(_.settings(ss: _*))

    def scalaConfigure(transformer: Project => Project): CrossProject =
      project.configurePlatforms(
        project.projects.keys.filter(_.isInstanceOf[ScalaPlatform]).toSeq: _*
      )(transformer)

    def scalaConfigure(
                        version: String
                      )(transformer: Project => Project): CrossProject =
      project.configurePlatforms(
        project.projects.keys.collect {
          case platform@ScalaPlatform(_, `version`) => platform
        }.toSeq: _*
      )(transformer)

  }

}
