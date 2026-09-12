package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AdminOrderIntegrationTest {

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

    private Long variantId;
    private User admin;
    private User customerOne;
    private User customerTwo;

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
        variant.setSku("SKU-ADM-1");
        variant.setSizeMl(100);
        variant.setPrice(new BigDecimal("50.00"));
        variant.setStock(10);
        variant.setIsActive(true);
        productVariantRepository.save(variant);
        variantId = variant.getId();

        admin = user(Role.ADMIN, "admin@order.test", "Admin");
        customerOne = user(Role.CUSTOMER, "one@order.test", "Alice");
        customerTwo = user(Role.CUSTOMER, "two@order.test", "Bob");
    }

    private User user(Role role, String email, String name) {
        User user = new User();
        user.setName(name);
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

    private Long createOrder(User customer, int quantity) throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload(quantity)))
                .andExpect(status().isCreated());
        return orderRepository.findByUserIdOrderByCreatedAtDesc(customer.getId()).get(0).getId();
    }

    @Test
    void adminEndpoints_requireAdminRole() throws Exception {
        Long orderId = createOrder(customerOne, 1);

        mockMvc.perform(get("/api/admin/orders").header("Authorization", bearer(customerOne)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/orders/" + orderId).header("Authorization", bearer(customerOne)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", bearer(customerOne))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllOrders_filtersBySearchAndStatus() throws Exception {
        Long orderOne = createOrder(customerOne, 1);
        createOrder(customerTwo, 2);

        mockMvc.perform(put("/api/admin/orders/" + orderOne + "/status")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        mockMvc.perform(get("/api/admin/orders?search=Alice")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userName").value("Alice"));

        mockMvc.perform(get("/api/admin/orders?status=SHIPPED")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(orderOne.intValue()));
    }

    @Test
    void getOrderDetails_returnsFullOrder() throws Exception {
        Long orderId = createOrder(customerOne, 2);

        mockMvc.perform(get("/api/admin/orders/" + orderId).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.intValue()))
                .andExpect(jsonPath("$.totalAmount").value(100.0))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void cancelOrder_restoresStock() throws Exception {
        ProductVariant before = productVariantRepository.findById(variantId).orElseThrow();
        assert before.getStock() == 10;

        Long orderId = createOrder(customerOne, 3);

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/cancel")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"admin override\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("admin override"));

        ProductVariant after = productVariantRepository.findById(variantId).orElseThrow();
        assert after.getStock() == 10;
    }
}