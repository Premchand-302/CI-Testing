package com.wiremock.pact.pos;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(pactVersion = PactSpecVersion.V4)
public class PosOmsConsumerPactTest {

    @Pact(provider = "OMS", consumer = "POS")
    public V4Pact createPact(PactDslWithProvider builder) {

        return builder
                .given("Order 1 exists")
                .uponReceiving("Get Order")
                .path("/orders/1")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .integerType("id", 1)
                        .stringType("status", "CONFIRMED"))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createPact")
    void verifyOrder(MockServer mockServer) throws IOException {

        given()
                .baseUri(mockServer.getUrl())
                .when()
                .get("/orders/1")
                .then()
                .statusCode(200)
                .log().all()
                .body("id", equalTo(1))
                .body("status", equalTo("CONFIRMED"));
    }

    @Pact(provider = "OMS", consumer = "POS")
    public V4Pact createOrderPact(PactDslWithProvider builder) {

        return builder
                .given("Provider can create order")
                .uponReceiving("Create Order")
                .path("/orders")
                .method("POST")
                .willRespondWith()
                .status(201)
                .body(new PactDslJsonBody()
                        .integerType("id", 100)
                        .stringType("status", "CREATED"))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createOrderPact")
    void createOrder(MockServer mockServer) {

        given()
                .baseUri(mockServer.getUrl())
                .when()
                .post("/orders")
                .then()
                .statusCode(201)
                .body("id", equalTo(100))
                .body("status", equalTo("CREATED"));
    }

    @Pact(provider = "OMS", consumer = "POS")
    public V4Pact getInventoryPact(PactDslWithProvider builder) {

        return builder
                .given("SKU-9 has stock")
                .uponReceiving("Get Inventory")
                .path("/inventory/SKU-9")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .stringType("sku", "SKU-9")
                        .integerType("stock", 50))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getInventoryPact")
    void getInventory(MockServer mockServer) {

        given()
                .baseUri(mockServer.getUrl())
                .when()
                .get("/inventory/SKU-9")
                .then()
                .statusCode(200)
                .body("sku", equalTo("SKU-9"))
                .body("stock", equalTo(50));
    }
}