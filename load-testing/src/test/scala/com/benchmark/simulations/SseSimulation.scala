package com.benchmark.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class SseSimulation extends BaseSimulation {
  val maxConn = System.getProperty("MAX_CONNECTIONS", "500").toInt
  val userFeeder = (1 to maxConn * 2).map(i => Map("userId" -> s"sse-user-$i")).toArray.circular

  val eventFeeder = Iterator.continually(Map(
    "userId"    -> s"sse-user-${scala.util.Random.nextInt(maxConn) + 1}",
    "productId" -> s"product-${scala.util.Random.nextInt(100)}",
    "amount"    -> (scala.util.Random.nextInt(999) + 1).toString
  ))

  val connectScn = scenario("SSE: Open and Hold")
    .feed(userFeeder)
    .exec(
      sse("Open SSE").sseName("sse-${userId}")
        .connect("/api/events/${userId}")
        .await(5.seconds)(
          sse.checkMessage("connected-ack").check(substring("ok"))
        )
    )
    .pause(120.seconds)
    .exec(sse("Close SSE").sseName("sse-${userId}").close)

  val eventScn = scenario("Generate Events")
    .feed(eventFeeder)
    .exec(
      http("POST /api/orders/process")
        .post("/api/orders/process")
        .header("Content-Type", "application/json")
        .body(StringBody("""{"userId":"${userId}","productId":"${productId}","amount":${amount}}"""))
        .check(status.is(200))
    )
    .pause(1.second, 3.seconds)

  setUp(
    connectScn.inject(rampUsers(maxConn).during(60.seconds)),
    eventScn.inject(
      nothingFor(30.seconds),
      constantUsersPerSec(50).during(90.seconds)
    )
  ).protocols(httpProtocol)
  .assertions(
      forAll.failedRequests.percent.lt(5.0),
      details("Open SSE").responseTime.percentile(99).lt(3000)
  )
}
