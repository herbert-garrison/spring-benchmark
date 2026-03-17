package com.benchmark.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

class OrdersSimulation extends BaseSimulation {
  val createFeeder = Iterator.continually(Map(
    "userId"    -> s"user-${Random.nextInt(1000)}",
    "productId" -> s"product-${Random.nextInt(100)}",
    "amount"    -> s"${Random.nextInt(9900) / 100.0 + 1.0}"
  ))

  val knownIds = (1 to 100).map(i => Map("orderId" -> i.toString)).toArray.circular
  val readScn = scenario("Read Orders")
    .feed(knownIds)
    .exec(http("GET /api/orders/{id}")
      .get("/api/orders/${orderId}")
      .check(status.is(200))
    )
    .pause(10.milliseconds, 50.milliseconds)

  val createReadScn = scenario("Create + Read Order")
    .feed(createFeeder)
    .exec(
      http("POST /api/orders")
        .post("/api/orders")
        .header("Content-Type", "application/json")
        .body(StringBody("""{"userId":"${userId}","productId":"${productId}","amount":${amount}}"""))
        .check(status.is(200))
        .check(jsonPath("$.id").saveAs("orderId"))
    )
    .pause(50.milliseconds)
    .exec(http("GET /api/orders/{id}").get("/api/orders/${orderId}").check(status.is(200)))

  val multiIoScn = scenario("Multi-IO Process")
    .feed(createFeeder)
    .exec(
      http("POST /api/orders/process")
        .post("/api/orders/process")
        .header("Content-Type", "application/json")
        .body(StringBody("""{"userId":"${userId}","productId":"${productId}","amount":${amount}}"""))
        .check(status.is(200))
    )

  setUp(
    readScn.inject(
      nothingFor(5.seconds),
      rampUsersPerSec(1).to(70).during(20.seconds),
      constantUsersPerSec(70).during(60.seconds),
      rampUsersPerSec(70).to(350).during(30.seconds),
      constantUsersPerSec(350).during(120.seconds)
    ),
    createReadScn.inject(
      nothingFor(5.seconds),
      rampUsersPerSec(1).to(20).during(20.seconds),
      constantUsersPerSec(20).during(60.seconds),
      rampUsersPerSec(20).to(100).during(30.seconds),
      constantUsersPerSec(100).during(120.seconds)
    ),
    multiIoScn.inject(
      nothingFor(5.seconds),
      rampUsersPerSec(1).to(10).during(20.seconds),
      constantUsersPerSec(10).during(60.seconds),
      rampUsersPerSec(10).to(50).during(30.seconds),
      constantUsersPerSec(50).during(120.seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
      global.responseTime.percentile(99).lt(1000),
      global.responseTime.percentile(95).lt(500),
      global.responseTime.percentile(50).lt(100),
      global.successfulRequests.percent.gt(99.0)
   )
}
