organization  := "net.amirrazmjou"

version       := "0.1"

scalaVersion := "2.11.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-client"  % sprayV,
    "io.spray"            %%  "spray-json"    % sprayV,
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-slf4j"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test"
  )
}

Revolver.settings
