package com.orderflow.controller;

import com.orderflow.entity.OrderStatus;
import com.orderflow.exception.RestExceptionHandler;
import com.orderflow.service.OrderResult;
import com.orderflow.service.OrderService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OrderController.class,
        properties = "spring.datasource.url=jdbc:postgresql://localhost:5432/orderflow")
@Import(RestExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldCreateOrderAndReturnItsLocation() throws Exception {
        var customerId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var createdAt = Instant.parse("2026-10-28T10:00:00Z");
        when(orderService.create(eq(customerId), anyList(), isNull()))
                .thenReturn(new OrderResult(
                        orderId,
                        customerId,
                        List.of(new OrderResult.Item(productId, 2)),
                        OrderStatus.CREATED,
                        createdAt));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(customerId, productId, 2)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Location", "/api/v1/orders/" + orderId))
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-28T10:00:00Z"));
    }

    @Test
    void shouldRetrieveOrderByUuid() throws Exception {
        var customerId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        when(orderService.get(orderId)).thenReturn(new OrderResult(
                orderId,
                customerId,
                List.of(new OrderResult.Item(productId, 1)),
                OrderStatus.CREATED,
                Instant.parse("2026-10-28T10:00:00Z")));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()));
    }

    @Test
    void shouldRejectStructurallyInvalidOrderRequestsWithoutCallingTheService() throws Exception {
        var customerId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var validItem = "{\"productId\":\"" + productId + "\",\"quantity\":1}";
        var tooManyItems = "{\"customerId\":\"" + customerId + "\",\"items\":["
                + String.join(",", Collections.nCopies(101, validItem)) + "]}";
        var invalidRequests = List.of(
                "{\"items\":[" + validItem + "]}",
                "{\"customerId\":\"not-a-uuid\",\"items\":[" + validItem + "]}",
                "{\"customerId\":\"" + customerId + "\",\"items\":[]}",
                "{\"customerId\":\"" + customerId + "\",\"items\":["
                        + "{\"productId\":\"" + productId + "\",\"quantity\":0}]}",
                "{\"customerId\":\"" + customerId + "\",\"items\":["
                        + "{\"productId\":\"" + productId + "\",\"quantity\":2147483648}]}",
                "{\"customerId\":\"" + customerId + "\",\"items\":["
                        + "{\"quantity\":1}]}",
                tooManyItems);

        for (var body : invalidRequests) {
            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldValidateOptionalIdempotencyKeyAndAcceptItsMaximumLength() throws Exception {
        var customerId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var maximumKey = "k".repeat(128);
        when(orderService.create(eq(customerId), anyList(), eq(maximumKey)))
                .thenReturn(new OrderResult(
                        orderId,
                        customerId,
                        List.of(new OrderResult.Item(productId, Integer.MAX_VALUE)),
                        OrderStatus.CREATED,
                        Instant.parse("2026-10-28T10:00:00Z")));

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", maximumKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(customerId, productId, Integer.MAX_VALUE)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", " ")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(customerId, productId, 1)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "k".repeat(129))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(customerId, productId, 1)))
                .andExpect(status().isBadRequest());

        verify(orderService).create(eq(customerId), anyList(), eq(maximumKey));
    }

    @Test
    void shouldReturnBadRequestForInvalidOrderPathUuid() throws Exception {
        mockMvc.perform(get("/api/v1/orders/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(orderService);
    }

    private static String requestJson(UUID customerId, UUID productId, int quantity) {
        return """
                {
                  "customerId": "%s",
                  "items": [{"productId": "%s", "quantity": %d}]
                }
                """.formatted(customerId, productId, quantity);
    }
}
