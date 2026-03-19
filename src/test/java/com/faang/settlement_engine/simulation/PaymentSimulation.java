package com.faang.settlement_engine.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.UUID;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class PaymentSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    // Feeder for unique idempotency keys and random accounts (1 to 500)
    java.util.Iterator<java.util.Map<String, Object>> feeder = java.util.stream.Stream.generate(() -> {
        java.util.Random rand = new java.util.Random();
        long fromId = rand.nextInt(500) + 1;
        long toId = rand.nextInt(500) + 1;
        while (fromId == toId) {
            toId = rand.nextInt(500) + 1;
        }
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("idempotencyKey", java.util.UUID.randomUUID().toString());
        map.put("fromAccountId", fromId);
        map.put("toAccountId", toId);
        return map;
    }).iterator();

    ScenarioBuilder scn = scenario("High-Throughput Payment Scenario")
            .feed(feeder)
            .exec(http("Payment Request")
                    .post("/api/payments/pay")
                    .body(StringBody("{ \"idempotencyKey\": \"#{idempotencyKey}\", \"amount\": 10.0, \"fromAccountId\": #{fromAccountId}, \"toAccountId\": #{toAccountId} }"))
                    .check(status().is(200)));

    {
        setUp(
                scn.injectOpen(rampUsers(1000).during(5))
        ).protocols(httpProtocol);
    }
}
