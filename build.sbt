import org.scalajs.linker.interface.ModuleSplitStyle

val fastLinkOutputDir = taskKey[String]("output directory for `npm run dev`")
val fullLinkOutputDir = taskKey[String]("output directory for `npm run build`")

lazy val `typicode-laminar-vite` = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name         := "typicode-laminar-vite",
    organization := "pro.reiss",
    version      := "0.1.0",
    scalaVersion := "3.2.0",
    scalacOptions ++= Seq("-encoding", "utf-8", "-deprecation", "-feature"),

    // We have a `main` method
    scalaJSUseMainModuleInitializer := true,

    // Emit modules in the most Vite-friendly way
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("typicode-laminar-vite")))
    },
    libraryDependencies ++= Seq(
      "com.raquo"                   %%% "laminar"           % "0.14.2",
      "com.softwaremill.sttp.tapir" %%% "tapir-sttp-client" % "1.0.6",
      "com.softwaremill.sttp.tapir" %%% "tapir-json-circe"  % "1.0.6",
      "io.github.cquiroz"           %%% "scala-java-time"   % "2.4.0"
    ),
    fastLinkOutputDir := {
      // Ensure that fastLinkJS has run, then return its output directory
      (Compile / fastLinkJS).value
      (Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value.getAbsolutePath()
    },
    fullLinkOutputDir := {
      // Ensure that fullLinkJS has run, then return its output directory
      (Compile / fullLinkJS).value
      (Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value.getAbsolutePath()
    }
  )
