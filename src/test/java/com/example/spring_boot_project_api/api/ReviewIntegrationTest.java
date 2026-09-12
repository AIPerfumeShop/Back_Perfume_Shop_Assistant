package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.example.spring_boot_project_api.repository.ReviewRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.EmailService;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ReviewIntegrationTest {

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
    private ReviewRepository reviewRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private EmailService emailService;

    private Long productId;
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
        productId = product.getId();

        customer = user(Role.CUSTOMER, "review@customer.test");
        otherCustomer = user(Role.CUSTOMER, "review-other@customer.test");
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

    private String bearer(User user) {
        return "Bearer " + tokenProvider.generateToken(user);
    }

    @Test
    void getProductReviews_isPublic() throws Exception {
        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void createReview_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"rating\":5,\"comment\":\"Nice\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReview_createsAndIsVisiblePublicly() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"rating\":5,\"comment\":\"Beautiful scent\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.approved").value(true))
                .andExpect(jsonPath("$.userName").value("Customer"));

        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].comment").value("Beautiful scent"));
    }

    @Test
    void createReview_duplicate_returnsConflict() throws Exception {
        String payload = "{\"productId\":" + productId + ",\"rating\":4,\"comment\":\"Good\"}";
        mockMvc.perform(post("/api/reviews").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(payload));
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void createReview_invalidRating_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"rating\":6}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyReviews_showsUserReviews() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"rating\":3,\"comment\":\"Okay\"}"));

        mockMvc.perform(get("/api/reviews/my").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].productName").value("Coco Mademoiselle"))
                .andExpect(jsonPath("$.data[0].rating").value(3));
    }

    @Test
    void updateMyReview_updatesOwnReview() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"rating\":2,\"comment\":\"Weak\"}"))
                .andExpect(status().isCreated());

        Long reviewId = reviewIdOf(customer);

        mockMvc.perform(patch("/api/reviews/" + reviewId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"Changed my mind\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Changed my mind"));

        mockMvc.perform(get("/api/reviews/my").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].rating").value(4));
    }

    @Test
    void updateMyReview_anotherUsersReview_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(otherCustomer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"rating\":5,\"comment\":\"Mine\"}"));

        Long reviewId = reviewIdOf(otherCustomer);

        mockMvc.perform(patch("/api/reviews/" + reviewId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":1}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMyReview_invalidRating_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"rating\":4}"))
                .andExpect(status().isCreated());

        Long reviewId = reviewIdOf(customer);

        mockMvc.perform(patch("/api/reviews/" + reviewId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":9}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteMyReview_softDeletesAndHidesIt() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"rating\":3,\"comment\":\"Delete me\"}"))
                .andExpect(status().isCreated());

        Long reviewId = reviewIdOf(customer);

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/reviews/my").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(patch("/api/reviews/" + reviewId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isNotFound());
    }

    private Long reviewIdOf(User user) {
        return reviewRepository
                .findByUserIdAndIsDeletedFalse(user.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent().get(0).getId();
    }
}