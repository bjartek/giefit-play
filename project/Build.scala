import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "gief-it"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "reactivemongo" %% "reactivemongo" % "0.1-SNAPSHOT" cross CrossVersion.full,
      "play.modules.reactivemongo" %% "play2-reactivemongo" % "0.1-SNAPSHOT" cross CrossVersion.full,
      "com.typesafe" % "play-plugins-mailer_2.10" % "2.1-SNAPSHOT"

    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers ++= Seq( 
        "sgodbillon" at "https://bitbucket.org/sgodbillon/repository/raw/master/snapshots/",
        "Daniel's Repository" at "http://danieldietrich.net/repository/snapshots/"
      )
    )
}
