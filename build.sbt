name := "cormas-task"

version := "1.0"

scalaVersion := "2.12.6"

addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M11" cross CrossVersion.full)

enablePlugins(SbtOsgi)

OsgiKeys.importPackage := Seq("*")
OsgiKeys.privatePackage := Seq("!scala.*")
OsgiKeys.requireCapability := """osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"""
OsgiKeys.exportPackage := Seq("org.openmole.plugin.task.cormas.*")
OsgiKeys.bundleActivator := Some("org.openmole.plugin.task.cormas.Activator")

def openmoleVersion = "8.0-SNAPSHOT"

libraryDependencies += "org.openmole" %% "org-openmole-core-dsl" % openmoleVersion
libraryDependencies += "org.openmole" %% "org-openmole-plugin-tool-json" % openmoleVersion
libraryDependencies += "org.openmole" %% "org-openmole-plugin-task-udocker" % openmoleVersion

