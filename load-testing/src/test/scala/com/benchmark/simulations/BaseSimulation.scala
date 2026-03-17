package com.benchmark.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._

trait BaseSimulation extends Simulation {
  val baseUrl: String = System.getProperty("BASE_URL", "http://localhost:8080")

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .shareConnections
}
