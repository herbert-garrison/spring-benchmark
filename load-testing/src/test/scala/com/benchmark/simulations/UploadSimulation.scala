package com.benchmark.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.nio.file.Paths

class UploadSimulation extends BaseSimulation {
  val fileFeeder = Seq(
    Map("file" -> "bodies/test_1mb.bin",  "filename" -> "test_1mb.bin"),
    Map("file" -> "bodies/test_5mb.bin",  "filename" -> "test_5mb.bin"),
    Map("file" -> "bodies/test_10mb.bin", "filename" -> "test_10mb.bin"),
    Map("file" -> "bodies/test_25mb.bin", "filename" -> "test_25mb.bin")
  ).toArray.circular

  val scn = scenario("Upload → S3")
    .feed(fileFeeder)
    .exec(
      http("POST /api/upload")
        .post("/api/upload")
        .bodyPart(
          RawFileBodyPart("file", "#{file}")
            .fileName("#{filename}")
            .contentType("application/octet-stream")
        )
        .check(status.is(200))
        .check(jsonPath("$.status").is("UPLOADED"))
    )
    .pause(200.milliseconds, 800.milliseconds)

  setUp(
    scn.inject(
      rampUsersPerSec(1).to(10).during(60.seconds),
      constantUsersPerSec(10).during(120.seconds),
      rampUsersPerSec(10).to(30).during(60.seconds),
      constantUsersPerSec(30).during(180.seconds),
      rampUsersPerSec(30).to(5).during(60.seconds)
    )
  ).protocols(httpProtocol)
    .maxDuration(10.minutes)
    .assertions(
      global.responseTime.percentile(95).lt(20000),
      global.responseTime.percentile(99).lt(45000),
      global.failedRequests.percent.lt(10.0),
      global.successfulRequests.percent.gt(90.0)
    )
}
