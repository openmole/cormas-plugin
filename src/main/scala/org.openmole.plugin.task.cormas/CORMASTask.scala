package org.openmole.plugin.task.cormas

import monocle.macros.Lenses
import org.openmole.core.context.Context
import org.openmole.core.expansion.FromContext
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.outputredirection.OutputRedirection
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task.{ Task, TaskExecutionContext }
import org.openmole.core.workflow.validation.ValidateTask
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.plugin.task.container
import org.openmole.plugin.task.container.HostFiles
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.systemexec.{ ErrorOnReturnValue, ReturnValue, StdOutErr, WorkDirectory }
import org.openmole.plugin.task.udocker.{ UDockerArguments, UDockerTask }
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.dsl._

object CORMASTask {

  implicit def isTask: InputOutputBuilder[CORMASTask] = InputOutputBuilder(CORMASTask._config)

  implicit def isExternal: ExternalBuilder[CORMASTask] = ExternalBuilder(CORMASTask.external)

  implicit def isInfo = InfoBuilder(info)

  implicit def isBuilder = new ReturnValue[CORMASTask] with ErrorOnReturnValue[CORMASTask] with StdOutErr[CORMASTask] with EnvironmentVariables[CORMASTask] with HostFiles[CORMASTask] with WorkDirectory[CORMASTask] {
    builder ⇒
    override def returnValue = CORMASTask.returnValue

    override def errorOnReturnValue = CORMASTask.errorOnReturnValue

    override def stdOut = CORMASTask.stdOut

    override def stdErr = CORMASTask.stdErr

    override def environmentVariables = CORMASTask.uDocker composeLens UDockerArguments.environmentVariables

    override def hostFiles = CORMASTask.uDocker composeLens UDockerArguments.hostFiles

    override def workDirectory = CORMASTask.uDocker composeLens UDockerArguments.workDirectory
  }

  def apply(
    script: FromContext[String],
    forceUpdate: Boolean = false)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService) = {
    //    val installCommands =
    //      install ++ InstallCommand.installCommands(libraries.toVector ++ Seq(InstallCommand.RLibrary("jsonlite")))

    val uDockerArguments =
      UDockerTask.createUDocker(
        "elcep/cormas",
        install = Vector(),
        cacheInstall = true,
        forceUpdate = forceUpdate,
        mode = "P1",
        reuseContainer = true)

    new CORMASTask(
      script,
      uDockerArguments,
      errorOnReturnValue = true,
      returnValue = None,
      stdOut = None,
      stdErr = None,
      _config = InputOutputConfig(),
      external = External(),
      info = InfoConfig(),
      cormasInputs = Vector.empty,
      cormasOutputs = Vector.empty)
  }

}

@Lenses case class CORMASTask(
  script: FromContext[String],
  uDocker: UDockerArguments,
  errorOnReturnValue: Boolean,
  returnValue: Option[Val[Int]],
  stdOut: Option[Val[String]],
  stdErr: Option[Val[String]],
  _config: InputOutputConfig,
  external: External,
  info: InfoConfig,
  cormasInputs: Vector[(Val[_], String)],
  cormasOutputs: Vector[(String, Val[_])]) extends Task with ValidateTask {

  override def config = UDockerTask.config(_config, returnValue, stdOut, stdErr)
  override def validate = container.validateContainer(Vector(), uDocker.environmentVariables, external, inputs)

  override protected def process(executionContext: TaskExecutionContext): FromContext[Context] = FromContext { p =>
    import p._

    def inputJSONName = "input.json"
    def outputJSONName = "output.json"

    import org.openmole.plugin.tool.json._

    def inputsFields: Seq[JField] = cormasInputs.map { case (v, name) => name -> (toJSONValue(context(v)): JValue) }
    def inputDictionary = JObject(inputsFields: _*)

    newFile.withTmpFile("inputs", ".json") { jsonInputs ⇒
      jsonInputs.content = compact(render(inputDictionary))
      OutputManager.systemOutput.println(jsonInputs.content)

      def uDockerTask =
        UDockerTask(
          uDocker,
          s"""./pharo --headless Pharo.image eval '${script.from(context)}'""",
          errorOnReturnValue,
          returnValue,
          stdOut,
          stdErr,
          _config,
          external,
          info) set (
            resources += (jsonInputs, inputJSONName, true))

      val resultContext = uDockerTask.process(executionContext).from(context)
      resultContext
    }

  }

}
