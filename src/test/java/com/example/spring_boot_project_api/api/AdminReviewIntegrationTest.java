package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.example.spring_boot_project_api.model.Review;
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
class AdminReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private EmailService emailService;

    private Long productId;
    private Long customerId;
    private User admin;
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

        admin = user(Role.ADMIN, "admin@review.test", "Admin");
        customer = user(Role.CUSTOMER, "review@customer.test", "Alice");
        otherCustomer = user(Role.CUSTOMER, "review-other@customer.test", "Bob");
        customerId = customer.getId();
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

    private Long createReview(User reviewer, String comment) throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(reviewer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"rating\":5,\"comment\":\"" + comment + "\"}"))
                .andExpect(status().isCreated());
        return reviewRepository.findByUserIdAndIsDeletedFalse(
                reviewer.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent().get(0).getId();
    }

    @Test
    void adminEndpoints_requireAdminRole() throws Exception {
        Long reviewId = createReview(customer, "Nice");

        mockMvc.perform(get("/api/admin/reviews").header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/reviews/" + reviewId).header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/reviews/" + reviewId).header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllReviews_listsAndFiltersByRatingAndSearch() throws Exception {
        createReview(customer, "Lovely scent");
        createReview(otherCustomer, "Too strong for me");

        mockMvc.perform(get("/api/admin/reviews?rating=5")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/admin/reviews?search=strong")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].comment").value("Too strong for me"));
    }

    @Test
    void getReviewById_returnsDetails() throws Exception {
        Long reviewId = createReview(customer, "Great");

        mockMvc.perform(get("/api/admin/reviews/" + reviewId).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId.intValue()))
                .andExpect(jsonPath("$.userId").value(customerId.intValue()))
                .andExpect(jsonPath("$.approved").value(true));
    }

    @Test
    void moderateReview_rejectsThenApproves() throws Exception {
        Long reviewId = createReview(customer, "Questionable");

        mockMvc.perform(put("/api/admin/reviews/" + reviewId + "/moderation")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":false,\"note\":\"Inappropriate language\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(false))
                .andExpect(jsonPath("$.moderationNote").value("Inappropriate language"));

        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(put("/api/admin/reviews/" + reviewId + "/moderation")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true));
    }

    @Test
    void deleteReview_thenRestore() throws Exception {
        Long reviewId = createReview(customer, "Delete me");

        mockMvc.perform(delete("/api/admin/reviews/" + reviewId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        mockMvc.perform(get("/api/admin/reviews?status=DELETED")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(patch("/api/admin/reviews/" + reviewId + "/restore")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(false));
    }
}