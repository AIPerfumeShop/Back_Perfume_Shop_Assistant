package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.BrandRepository;
import com.example.spring_boot_project_api.repository.CategoryRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.EmailService;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private EmailService emailService;

    private Product product;
    private Long variantId;
    private User customer;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("Perfume");
        categoryRepository.save(category);

        Brand brand = new Brand();
        brand.setName("Chanel");
        brand.setIsActive(true);
        brandRepository.save(brand);

        product = new Product();
        product.setName("No. 5");
        product.setCategory(category);
        product.setBrand(brand);
        product.setIsActive(true);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("SKU-CART-1");
        variant.setSizeMl(50);
        variant.setPrice(new BigDecimal("120.00"));
        variant.setStock(10);
        variant.setIsActive(true);
        product.getVariants().add(variant);
        productRepository.save(product);
        variantId = product.getVariants().get(0).getId();

        customer = user(Role.CUSTOMER, "cart@customer.test");
    }

    private User user(Role role, String email) {
        User user = new User();
        user.setName(role == Role.ADMIN ? "Admin" : "Customer");
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(role);
        return userRepository.save(user);
    }

    private String bearer() {
        return "Bearer " + tokenProvider.generateToken(customer);
    }

    @Test
    void getCart_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addItem_addsToCart() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":" + variantId + ",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sku").value("SKU-CART-1"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].price").value(120.0))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalAmount").value(240.0));
    }

    @Test
    void addItem_sameVariant_accumulatesOnOneLine() throws Exception {
        String payload = "{\"variantId\":" + variantId + ",\"quantity\":1}";
        mockMvc.perform(post("/api/cart/items").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(payload));
        mockMvc.perform(post("/api/cart/items").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void addItem_overStock_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":" + variantId + ",\"quantity\":999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addItem_unknownVariant_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":999999,\"quantity\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateItemQuantity_changesQuantity() throws Exception {
        long cartItemId = addItemReturningId();

        mockMvc.perform(patch("/api/cart/items/" + cartItemId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.totalItems").value(5));
    }

    @Test
    void removeItem_removesItem() throws Exception {
        long cartItemId = addItemReturningId();

        mockMvc.perform(delete("/api/cart/items/" + cartItemId)
                        .header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cart").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void clearCart_emptiesCart() throws Exception {
        addItemReturningId();

        mockMvc.perform(delete("/api/cart").header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cart").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    private long addItemReturningId() throws Exception {
        String body = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":" + variantId + ",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.items[0].id")).longValue();
    }
}