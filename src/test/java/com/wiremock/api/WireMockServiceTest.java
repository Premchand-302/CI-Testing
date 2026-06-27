package com.wiremock.api;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static io.restassured.RestAssured.given;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WireMockServiceTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    private HttpClient client;

    @BeforeEach
    void pointCustomerAtWireMock(){
        RestAssured.baseURI = wm.baseUrl();
        client = HttpClient.newHttpClient();
    }

    @Test
    @DisplayName("Stub inventory")
    void returnsConfirmedOrder(){

        wm.stubFor(
                get(urlPathEqualTo("/inventory/SKU-9"))
                        .willReturn(
                                okJson("""
                                        {"sku":"SKU-9","qty":5} """)
                        )
        );
        wm.stubFor(
                get(urlPathEqualTo("/inventory/SKU-0"))
                        .willReturn(
                                aResponse()
                                        .withStatus(409)
                                        .withHeader("content-type", "application/json")
                                        .withBody("{\"error\" : \"OUT_OF_STOCK\"}")
                        )
        );

        wm.stubFor(
                get(urlPathEqualTo("/inventory/SKU-999"))
                        .willReturn(
                                aResponse()
                                        .withStatus(404)
                                        .withHeader("content-type", "application/json")
                                        .withBody("{\"error\" : \"PRODUCT_NOT_FOUND\"}")
                        )
        );


        given()
                .baseUri(wm.baseUrl())
                .when()
                .get("/inventory/{sku}","SKU-9")
                .then()
                .statusCode(200)
                .body("sku",equalTo("SKU-9"))
                .body("qty",equalTo(5));

        given()
                .baseUri(wm.baseUrl())
                .when()
                .get("/inventory/{sku}","SKU-0")
                .then()
                .statusCode(409)
                .body("error",equalTo("OUT_OF_STOCK"));

        given()
                .baseUri(wm.baseUrl())
                .when()
                .get("/inventory/{sku}","SKU-999")
                .then()
                .statusCode(404)
                .body("error",equalTo("PRODUCT_NOT_FOUND"));

        given()
                .when()
                .get("/inventory/{sku}", "SKU-9")
                .then()
                .statusCode(200);

        wm.verify(exactly(2), getRequestedFor(urlPathEqualTo("/inventory/SKU-9")));
        wm.verify(exactly(1), getRequestedFor(urlPathEqualTo("/inventory/SKU-0")));
        wm.verify(exactly(1), getRequestedFor(urlPathEqualTo("/inventory/SKU-999")));

    }

    @Test
    @DisplayName("Force a timeout")
    void exercise2() throws IOException, InterruptedException {
        wm.stubFor(get("/orders/slow").willReturn(ok().withFixedDelay(3000)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wm.baseUrl()+"/orders/slow"))
                .timeout(Duration.ofSeconds(1))
                .GET()
                .build();

        assertThrows(HttpTimeoutException.class, () -> client.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    @DisplayName("Exercise 3")
    void exercise3() {
        wm.stubFor(get("/orders/42").inScenario("fulfillment").whenScenarioStateIs(STARTED)
                .willSetStateTo("CONFIRMED")
                .willReturn(aResponse().withHeader("content-type", "application/json")
                        .withBody("""
                            {"status": "PENDING"}
                            """)));

        wm.stubFor(get("/orders/42").inScenario("fulfillment").whenScenarioStateIs("CONFIRMED")
                .willSetStateTo("SHIPPED").willReturn(okJson("""
            {"status":"CONFIRMED"}""")));

        wm.stubFor(get("/orders/42")
                .inScenario("fulfillment")
                .whenScenarioStateIs("SHIPPED")
                .willReturn(okJson("""
                    {"status":"SHIPPED"}
                    """))
        );

        given().when().get("/orders/42").then().body("status", equalTo( "PENDING"));
        given().when().get("/orders/42").then().body("status", equalTo( "CONFIRMED"));
        given().when().get("/orders/42").then().statusCode(200).body("status", equalTo("SHIPPED"));

        wm.verify(3, getRequestedFor(urlEqualTo("/orders/42")));
    }
}
