package com.orderflow.acceptance;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class OrderAcceptanceSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID customerId;
    private UUID productId;
    private UUID orderId;
    private UUID missingOrderId;
    private String idempotencyKey;
    private int submittedQuantity;
    private MvcResult response;
    private MvcResult firstResponse;

    @Given("the customer and product references are not registered")
    public void customerAndProductReferencesAreNotRegistered() {
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Given("the order contains no items")
    public void orderContainsNoItems() {
        customerId = UUID.randomUUID();
    }

    @Given("a customer has a new idempotency key")
    public void customerHasANewIdempotencyKey() {
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID().toString();
    }

    @Given("an order identifier that does not exist")
    public void orderIdentifierDoesNotExist() {
        missingOrderId = UUID.randomUUID();
    }

    @When("the customer places an order for {int} units")
    public void customerPlacesAnOrder(int quantity) throws Exception {
        submittedQuantity = quantity;
        response = submitOrder(quantity, null);
    }

    @When("the customer places an order for {int} units with that key")
    public void customerPlacesAnOrderWithKey(int quantity) throws Exception {
        submittedQuantity = quantity;
        response = submitOrder(quantity, idempotencyKey);
    }

    @When("the customer retries the same order with that key")
    public void customerRetriesTheSameOrderWithKey() throws Exception {
        firstResponse = response;
        response = submitOrder(submittedQuantity, idempotencyKey);
    }

    @When("the customer reuses that key for {int} units")
    public void customerReusesKeyForDifferentOrder(int quantity) throws Exception {
        response = submitOrder(quantity, idempotencyKey);
    }

    @When("the customer retrieves the order")
    public void customerRetrievesTheOrder() throws Exception {
        response = mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andReturn();
    }

    @When("the customer requests that missing order")
    public void customerRequestsMissingOrder() throws Exception {
        response = mockMvc.perform(get("/api/v1/orders/{id}", missingOrderId))
                .andReturn();
    }

    @When("the customer submits the order")
    public void customerSubmitsTheOrderWithoutItems() throws Exception {
        var body = """
                {
                  "customerId": "%s",
                  "items": []
                }
                """.formatted(customerId);
        response = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    @Then("the order is created with the submitted references")
    public void orderIsCreatedWithSubmittedReferences() throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(201);
        assertThat(response.getResponse().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        var order = responseBody();
        orderId = UUID.fromString(order.get("id").asText());
        assertThat(order.get("customerId").asText()).isEqualTo(customerId.toString());
        assertThat(order.get("items").get(0).get("productId").asText())
                .isEqualTo(productId.toString());
        assertThat(order.get("items").get(0).get("quantity").asInt())
                .isEqualTo(submittedQuantity);
        assertThat(order.get("status").asText()).isEqualTo("CREATED");
        assertThat(response.getResponse().getHeader("Location"))
                .isEqualTo("/api/v1/orders/" + orderId);
    }

    @Then("the order can be retrieved with its item")
    public void orderCanBeRetrievedWithItsItem() throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        var order = responseBody();
        assertThat(order.get("id").asText()).isEqualTo(orderId.toString());
        assertThat(order.get("items").get(0).get("productId").asText())
                .isEqualTo(productId.toString());
        assertThat(order.get("items").get(0).get("quantity").asInt())
                .isEqualTo(submittedQuantity);
    }

    @Then("the request is rejected with a validation problem")
    public void requestIsRejectedWithValidationProblem() {
        assertProblemResponse(400);
    }

    @Then("the order is reported as missing")
    public void orderIsReportedAsMissing() {
        assertProblemResponse(404);
    }

    @Then("the original order is returned")
    public void originalOrderIsReturned() throws Exception {
        assertThat(response.getResponse().getStatus()).isEqualTo(201);
        assertThat(responseBody().get("id").asText())
                .isEqualTo(responseBody(firstResponse).get("id").asText());
        assertThat(response.getResponse().getHeader("Location"))
                .isEqualTo(firstResponse.getResponse().getHeader("Location"));
    }

    @Then("the key reuse is rejected with a conflict problem")
    public void keyReuseIsRejectedWithConflictProblem() {
        assertProblemResponse(409);
    }

    private MvcResult submitOrder(int quantity, String key) throws Exception {
        var body = """
                {
                  "customerId": "%s",
                  "items": [{"productId": "%s", "quantity": %d}]
                }
                """.formatted(customerId, productId, quantity);
        var request = post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return mockMvc.perform(request).andReturn();
    }

    private JsonNode responseBody() throws Exception {
        return responseBody(response);
    }

    private JsonNode responseBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void assertProblemResponse(int status) {
        assertThat(response.getResponse().getStatus()).isEqualTo(status);
        assertThat(response.getResponse().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }
}
