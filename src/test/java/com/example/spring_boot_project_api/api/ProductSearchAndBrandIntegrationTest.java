package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.enums.Gender;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.model.Brand;
import com.example.spring_boot_project_api.model.Category;
import com.example.spring_boot_project_api.model.FragranceProfile;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
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
class ProductSearchAndBrandIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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

    private Category category;
    private Brand chanel;
    private Product roseElixir;
    private Product roseNoir;
    private Product oceanMist;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setName("Perfume");
        categoryRepository.save(category);

        chanel = new Brand();
        chanel.setName("Chanel");
        chanel.setDescription("Luxury");
        chanel.setIsActive(true);
        brandRepository.save(chanel);

        Brand dior = new Brand();
        dior.setName("Dior");
        dior.setDescription("Luxury");
        dior.setIsActive(true);
        brandRepository.save(dior);

        User reviewer = new User();
        reviewer.setName("Reviewer");
        reviewer.setEmail("reviewer@it.test");
        reviewer.setPassword("password123");
        reviewer.setRole(Role.CUSTOMER);
        userRepository.save(reviewer);

        roseElixir = product("Rose Elixir", chanel, Gender.WOMEN, "Floral",
                variant("V1", new BigDecimal("120.00"), 50, 10),
                variant("V2", new BigDecimal("200.00"), 100, 5));
        roseNoir = product("Rose Noir", dior, Gender.MEN, "Woody",
                variant("V3", new BigDecimal("90.00"), 60, 0));
        oceanMist = product("Ocean Mist", chanel, Gender.UNISEX, "Aquatic",
                variant("V4", new BigDecimal("30.00"), 30, 100));

        review(reviewer, roseElixir, 5);
        review(reviewer, roseNoir, 2);
        review(reviewer, oceanMist, 4);

        categoryRepository.save(category);
    }

    private ProductVariant variant(String sku, BigDecimal price, int sizeMl, int stock) {
        ProductVariant v = new ProductVariant();
        v.setSku(sku);
        v.setSizeMl(sizeMl);
        v.setPrice(price);
        v.setStock(stock);
        v.setIsActive(true);
        return v;
    }

    private Product product(String name, Brand brand, Gender gender, String family, ProductVariant... variants) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(name + " description");
        p.setCategory(category);
        p.setBrand(brand);
        p.setIsActive(true);
        for (ProductVariant v : variants) {
            v.setProduct(p);
            p.getVariants().add(v);
        }
        FragranceProfile profile = new FragranceProfile();
        profile.setProduct(p);
        profile.setGender(gender);
        profile.setFragranceFamily(family);
        p.setFragranceProfile(profile);
        productRepository.save(p);
        return p;
    }

    private void review(User user, Product product, int rating) {
        Review r = new Review();
        r.setUser(user);
        r.setProduct(product);
        r.setRating(rating);
        r.setComment("ok");
        reviewRepository.save(r);
    }

    private String adminJwt() {
        User admin = new User();
        admin.setName("Admin");
        admin.setEmail("admin@it.test");
        admin.setPassword("password123");
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        return tokenProvider.generateToken(admin);
    }

    @Test
    void listProducts_returnsAll() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void search_returnsMatchingProducts() throws Exception {
        mockMvc.perform(get("/api/products").param("search", "rose"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void filterByBrandAndCategory() throws Exception {
        mockMvc.perform(get("/api/products").param("brand", "chanel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/products")
                        .param("brand", "chanel")
                        .param("categoryId", category.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void filterByGenderAndFragranceFamily() throws Exception {
        mockMvc.perform(get("/api/products").param("gender", "WOMEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Rose Elixir"));

        mockMvc.perform(get("/api/products").param("fragranceFamily", "floral"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void filterByPriceRange() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "90")
                        .param("maxPrice", "150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void filterByStock() throws Exception {
        mockMvc.perform(get("/api/products").param("inStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void filterByRating() throws Exception {
        mockMvc.perform(get("/api/products").param("minRate", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.data[*].name", org.hamcrest.Matchers.hasItems("Rose Elixir", "Ocean Mist")));
    }

    @Test
    void sortByPriceAscending() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "price").param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Ocean Mist"))
                .andExpect(jsonPath("$.data[1].name").value("Rose Noir"))
                .andExpect(jsonPath("$.data[2].name").value("Rose Elixir"));
    }

    @Test
    void pagination_works() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/products").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void combinedFilters_workTogether() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("gender", "WOMEN")
                        .param("fragranceFamily", "floral")
                        .param("minRate", "4")
                        .param("minPrice", "100")
                        .param("inStock", "true")
                        .param("sort", "price")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Rose Elixir"));
    }

    @Test
    void brandStatistics_returnProductCounts() throws Exception {
        mockMvc.perform(get("/api/brands/" + chanel.getId() + "/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brandId").value(chanel.getId()))
                .andExpect(jsonPath("$.totalProducts").value(2));
    }

    @Test
    void brandProducts_endpoint_returnsPaginatedBrandProducts() throws Exception {
        mockMvc.perform(get("/api/brands/" + chanel.getId() + "/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.data[*].name", org.hamcrest.Matchers.hasItems("Rose Elixir", "Ocean Mist")));

        mockMvc.perform(get("/api/brands/" + chanel.getId() + "/products")
                        .param("search", "rose"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Rose Elixir"));

        mockMvc.perform(get("/api/brands/9999999/products"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminEndpoints_requireAdminRole() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());

        String customerJwt = userRepository.findByEmail("reviewer@it.test")
                .map(u -> tokenProvider.generateToken(u))
                .orElseThrow();
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + adminJwt()))
                .andExpect(status().isOk());
    }
}