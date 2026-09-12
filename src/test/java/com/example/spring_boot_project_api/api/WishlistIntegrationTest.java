package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class WishlistIntegrationTest {

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

    private Long productId;
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

        Product product = new Product();
        product.setName("Bleu de Chanel");
        product.setCategory(category);
        product.setBrand(brand);
        product.setIsActive(true);
        productRepository.save(product);
        productId = product.getId();

        customer = user(Role.CUSTOMER, "wishlist@customer.test");
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
    void getWishlist_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addItem_addsProduct() throws Exception {
        mockMvc.perform(post("/api/wishlist/items")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].name").value("Bleu de Chanel"))
                .andExpect(jsonPath("$.items[0].brand").value("Chanel"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void addItem_sameProduct_isIdempotent() throws Exception {
        String payload = "{\"productId\":" + productId + "}";
        mockMvc.perform(post("/api/wishlist/items").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(payload));
        mockMvc.perform(post("/api/wishlist/items")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void addItem_unknownProduct_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/wishlist/items")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeItem_removesItem() throws Exception {
        long wishlistItemId = addItemReturningId();

        mockMvc.perform(delete("/api/wishlist/items/" + wishlistItemId)
                        .header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/wishlist").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void clearWishlist_emptiesWishlist() throws Exception {
        addItemReturningId();

        mockMvc.perform(delete("/api/wishlist").header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/wishlist").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    private long addItemReturningId() throws Exception {
        String body = mockMvc.perform(post("/api/wishlist/items")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.items[0].id")).longValue();
    }
}