package com.thoughtworks.modularizer.server

import java.nio.file.{Files, InvalidPathException, Path, Paths}

import caseapp.{AppName, AppVersion, ProgName}
import caseapp.core.argparser.ArgParser
import com.thoughtworks.modularizer.BuildInfo

/**
  * @author 杨博 (Yang Bo)
  */
@AppName(BuildInfo.name)
@AppVersion(BuildInfo.version)
final case class Configuration(
    gitUri: String,
    gitUsername: Option[String],
    gitPassword: Option[String],
    listeningHost: String = "localhost",
    listeningPort: Int = 42019,
    numberOfTemporaryGitClones: Int = 3,
    maxRetriesForUploading: Int = 3,
    temporaryDirectory: Path = Files.createTempDirectory("modularizer"),
)

object Configuration {
  // TODO: Delete this method once https://github.com/alexarchambault/case-app/pull/114 is merged
  implicit val pathArgParser: ArgParser[Path] = {
    def parsePath(pathString: String) = {
      try {
        Right(Paths.get(pathString))
      } catch {
        case e: InvalidPathException =>
          Left(caseapp.core.Error.MalformedValue("path", e.getMessage))
      }
    }
    ArgParser[String].xmapError(_.toString, parsePath)
  }
}
