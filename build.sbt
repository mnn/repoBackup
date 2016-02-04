name := "repoBackup"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies += "com.lihaoyi" %% "ammonite-ops" % "0.5.3"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0"

resolvers += Resolver.sonatypeRepo("public")

assemblyJarName in assembly := s"${name.value}_${version.value}.jar"

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "tk.monnef.repobackup",
    buildInfoOptions += BuildInfoOption.Traits("BuildInfoTrait")
  )
