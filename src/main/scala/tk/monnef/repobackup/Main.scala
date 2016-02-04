package tk.monnef.repobackup

import java.text.SimpleDateFormat
import java.util.Calendar

import ammonite.ops._

trait BuildInfoTrait {
  def name: String

  def version: String

  def scalaVersion: String

  def sbtVersion: String
}

object Main extends App {
  private val buildInfoClass = Class.forName("tk.monnef.repobackup.BuildInfo$")
  val buildInfo: BuildInfoTrait = buildInfoClass.getField("MODULE$").get(buildInfoClass).asInstanceOf[BuildInfoTrait]

  println(s"${buildInfo.name} ${buildInfo.version} by monnef")

  ArgsParser.parse(args) match {
    case Some(config) => Worker.work(config)
    case None =>
  }
}

case class Config(repository: String = "", verbose: Boolean = false)

object ArgsParser {

  val parser = new scopt.OptionParser[Config]("repobackup") {
    head("repoBackup", "0.1")

    opt[String]('r', "repository")
      .required()
      .action { (x, c) => c.copy(repository = x) }

    opt[Unit]('v', "verbose")
      .action { (x, c) => c.copy(verbose = true) }
  }

  def parse(args: Seq[String]) = parser.parse(args, Config())
}

object Worker {
  val wd = cwd
  val tmpDir = wd / 'tmp
  val bckDir = wd / 'backup

  def printStateStart(state: String, config: Config) {
    val end = if (config.verbose) "\n" else "..."
    print(s">> $state$end")
  }

  def printStateDone(config: Config) {
    if (config.verbose) print("<< ")
    println("Done")
  }

  private[this] def cleanTmpDir() {
    ls ! tmpDir | rm
  }

  private[this] def prepare(config: Config) {
    printStateStart("Preparing", config)
    Seq(tmpDir, bckDir).foreach(mkdir)
    cleanTmpDir()
    printStateDone(config)
  }

  def clone(config: Config)(implicit verbosePrint: VerbosePrint) {
    printStateStart("Cloning repository", config)
    val gitRet = %%("git", "clone", "-v", config.repository, ".")(tmpDir)
    verbosePrint(gitRet.out.string)
    verbosePrint(gitRet.err.string)
    printStateDone(config)
  }

  val dateFormat = new SimpleDateFormat("yyyyMMddHHmmss")

  def generateTimestamp() = dateFormat.format(Calendar.getInstance.getTime)

  def generateBackupFileName() = generateTimestamp() + ".tar.bz2"

  def pack(config: Config)(implicit verbosePrint: VerbosePrint) {
    printStateStart("Packing", config)
    val packRet = %%("tar", "cfvj", (bckDir relativeTo tmpDir) / generateBackupFileName, ".")(tmpDir)
    verbosePrint(packRet.out.string)
    printStateDone(config)
  }

  def finish(config: Config) {
    printStateStart("Finishing", config)
    cleanTmpDir()
    printStateDone(config)
  }

  private[this] def rawVerbosePrint(config: Config, msg: String) {
    if (config.verbose) println(msg)
  }

  type VerbosePrint = String => Unit

  def work(config: Config) {
    implicit val verbosePrint: VerbosePrint = rawVerbosePrint(config, _)
    prepare(config)
    clone(config)
    pack(config)
    finish(config)
  }
}
