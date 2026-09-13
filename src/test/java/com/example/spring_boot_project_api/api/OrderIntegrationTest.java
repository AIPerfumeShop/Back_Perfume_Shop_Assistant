package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.model.Brand;
import com.example.spring_boot_project_api.model.Category;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.BrandRepository;
import com.example.spring_boot_project_api.repository.CategoryRepository;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.ProductVariantRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long variantId;
    private User customer;
    private User otherCustomer;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("Perfume");
        categoryRepository.save(category);

        Brand brand = new Brand();
        brand.setName("Chanel");
        brand.setIsActive(true);
        brandRepository.save(brand);

        Product product = new Product();
        product.setName("Coco Mademoiselle");
        product.setCategory(category);
        product.setBrand(brand);
        product.setIsActive(true);
        productRepository.save(product);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("SKU-ORD-1");
        variant.setSizeMl(100);
        variant.setPrice(new BigDecimal("50.00"));
        variant.setStock(5);
        variant.setIsActive(true);
        productVariantRepository.save(variant);
        variantId = variant.getId();

        customer = user(Role.CUSTOMER, "order@customer.test");
        otherCustomer = user(Role.CUSTOMER, "order-other@customer.test");
    }

    private User user(Role role, String email) {
        User user = new User();
        user.setName(role == Role.ADMIN ? "Admin" : "Customer");
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(role);
        return userRepository.save(user);
    }

    private String bearer(User user) {
        return "Bearer " + tokenProvider.generateToken(user);
    }

    private String orderPayload(int quantity) {
        return "{\"shippingAddress\":\"123 Main St\",\"phone\":\"0123456789\","
                + "\"items\":[{\"variantId\":" + variantId + ",\"quantity\":" + quantity + "}]}";
    }

    private Long orderIdOf(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).get(0).getId();
    }

    @Test
    void createOrder_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload(1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_calculatesTotalAndDeductsStock() throws Exception {
        ProductVariant stored = productVariantRepository.findById(variantId).orElseThrow();
        assert stored.getStock() == 5;

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload(3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(150.0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Coco Mademoiselle"))
                .andExpect(jsonPath("$.items[0].subtotal").value(150.0));

        ProductVariant after = productVariantRepository.findById(variantId).orElseThrow();
        assert after.getStock() == 2;
    }

    @Test
    void createOrder_insufficientStock_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload(99)))
                .andExpect(status().isBadRequest());

        ProductVariant stored = productVariantRepository.findById(variantId).orElseThrow();
        assert stored.getStock() == 5;
    }

    @Test
    void checkout_withCash_createsSuccessfulPayment() throws Exception {
        String payload = orderPayload(2).replace("}", ",\"paymentMethod\":\"CASH\"}");

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESSFUL"))
                .andExpect(jsonPath("$.totalAmount").value(100.0));
    }

    @Test
    void checkout_duplicateWithinWindow_returnsSameOrderAndPayment() throws Exception {
        String payload = orderPayload(2).replace("}", ",\"paymentMethod\":\"CASH\"}");

        String first = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESSFUL"))
                .andReturn().getResponse().getContentAsString();

        long firstOrderId = objectMapper.readTree(first).get("orderId").asLong();
        long firstPaymentId = objectMapper.readTree(first).get("paymentId").asLong();

        //Identical checkout double-tapped: must reuse the existing order+payment
        //instead of creating a duplicate order and charging twice.
        String second = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESSFUL"))
                .andReturn().getResponse().getContentAsString();

        assertEquals(firstOrderId, objectMapper.readTree(second).get("orderId").asLong());
        assertEquals(firstPaymentId, objectMapper.readTree(second).get("paymentId").asLong());
        assertEquals(1, orderRepository.count());
    }

    @Test
    void checkout_paymentProcess_isIdempotent() throws Exception {
        String payload = orderPayload(2).replace("}", ",\"paymentMethod\":\"CASH\"}");

        String created = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long paymentId = objectMapper.readTree(created).get("paymentId").asLong();
        User admin = user(Role.ADMIN, "order-admin@customer.test");

        //First process call succeeds.
        mockMvc.perform(post("/api/payments/" + paymentId + "/process")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        //Second (double-tap) call returns the same outcome without re-processing.
        mockMvc.perform(post("/api/payments/" + paymentId + "/process")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assert productVariantRepository.findById(variantId).orElseThrow().getStock() == 3;
    }

    @Test
    void getUserOrders_returnsOnlyOwnOrders() throws Exception {
        mockMvc.perform(post("/api/orders").header("Authorization", bearer(customer))
                .contentType(MediaType.APPLICATION_JSON).content(orderPayload(1)));
        mockMvc.perform(post("/api/orders").header("Authorization", bearer(otherCustomer))
                .contentType(MediaType.APPLICATION_JSON).content(orderPayload(1)));

        mockMvc.perform(get("/api/orders/user/me").header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(customer.getId().intValue()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getOrderById_ofAnotherUser_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/orders").header("Authorization", bearer(otherCustomer))
                .contentType(MediaType.APPLICATION_JSON).content(orderPayload(1)));

        Long orderId = orderIdOf(otherCustomer);

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelOrder_marksCancelledAndRestoresStock() throws Exception {
        mockMvc.perform(post("/api/orders").header("Authorization", bearer(customer))
                .contentType(MediaType.APPLICATION_JSON).content(orderPayload(2)));

        Long orderId = orderIdOf(customer);

        mockMvc.perform(delete("/api/orders/" + orderId)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"changed my mind\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("changed my mind"));

        ProductVariant stored = productVariantRepository.findById(variantId).orElseThrow();
        assert stored.getStock() == 5;
    }

    @Test
    void cancelOrder_ofAnotherUser_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/orders").header("Authorization", bearer(otherCustomer))
                .contentType(MediaType.APPLICATION_JSON).content(orderPayload(1)));

        Long orderId = orderIdOf(otherCustomer);

        mockMvc.perform(delete("/api/orders/" + orderId)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"hijack\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoints_areAdminOnly() throws Exception {
        mockMvc.perform(post("/api/orders").header("Authorization", bearer(customer))
                .contentType(MediaType.APPLICATION_JSON).content(orderPayload(1)));

        Long orderId = orderIdOf(customer);

        mockMvc.perform(get("/api/orders").header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isForbidden());
    }
}