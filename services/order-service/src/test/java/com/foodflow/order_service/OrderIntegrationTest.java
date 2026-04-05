package com.foodflow.order_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodflow.order_service.auth.dto.LoginRequest;
import com.foodflow.order_service.auth.dto.RegisterRequest;
import com.foodflow.order_service.auth.entity.User;
import com.foodflow.order_service.auth.repository.UserRepository;
import com.foodflow.order_service.order.dto.OrderItemRequest;
import com.foodflow.order_service.order.dto.PlaceOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String customerToken;
    private static final String CUSTOMER_EMAIL = "order-test-customer@foodflow.com";

    @BeforeEach
    void setup() throws Exception {
        // Clean up
        userRepository.findByEmail(CUSTOMER_EMAIL).ifPresent(userRepository::delete);

        // Register a customer and get token
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail(CUSTOMER_EMAIL);
        reg.setPassword("password123");
        reg.setFullName("Order Test Customer");
        reg.setRole(User.Role.CUSTOMER);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        customerToken = (String) response.get("accessToken");
    }

    @Test
    void placeOrder_shouldReturn202_withOrderId() throws Exception {
        PlaceOrderRequest request = buildSampleOrder();

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                // ↑ 202 Accepted — not 201. The order is saved but processing is async.
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.restaurantName").value("Test Restaurant"))
                .andExpect(jsonPath("$.totalAmount").value(299.0))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].name").value("Test Item"));
    }

    @Test
    void placeOrder_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleOrder())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyOrders_shouldReturnOrdersList() throws Exception {
        // Place an order first
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleOrder())))
                .andExpect(status().isAccepted());

        // Get my orders
        mockMvc.perform(get("/api/v1/orders/my")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void cancelOrder_shouldReturn200_withCancelledStatus() throws Exception {
        // Place order
        MvcResult placeResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleOrder())))
                .andExpect(status().isAccepted())
                .andReturn();

        Map<?, ?> order = objectMapper.readValue(
                placeResult.getResponse().getContentAsString(), Map.class);
        String orderId = (String) order.get("id");

        // Cancel it
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_alreadyCancelled_shouldReturn400() throws Exception {
        // Place and cancel
        MvcResult placeResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleOrder())))
                .andReturn();

        Map<?, ?> order = objectMapper.readValue(
                placeResult.getResponse().getContentAsString(), Map.class);
        String orderId = (String) order.get("id");

        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Try to cancel again
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Only PENDING orders can be cancelled. Current status: CANCELLED"));
    }

    private PlaceOrderRequest buildSampleOrder() {
        OrderItemRequest item = new OrderItemRequest();
        item.setMenuItemId(UUID.randomUUID());
        item.setName("Test Item");
        item.setUnitPrice(new BigDecimal("299.00"));
        item.setQuantity(1);

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setRestaurantId(UUID.randomUUID());
        request.setRestaurantName("Test Restaurant");
        request.setDeliveryAddress("123 Test Street");
        request.setItems(List.of(item));
        return request;
    }
}