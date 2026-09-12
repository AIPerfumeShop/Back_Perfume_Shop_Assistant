package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class ProductCrudIntegrationTest {

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

    private Category category;
    private Brand brand;
    private User admin;
    private User customer;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setName("Perfume");
        categoryRepository.save(category);

        brand = new Brand();
        brand.setName("Chanel");
        brand.setIsActive(true);
        brandRepository.save(brand);

        admin = user(Role.ADMIN, "admin@product.test");
        customer = user(Role.CUSTOMER, "customer@product.test");
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

    private Product newProduct(String name, String sku) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(name + " description");
        product.setCategory(category);
        product.setBrand(brand);
        product.setIsActive(true);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setSizeMl(50);
        variant.setPrice(new BigDecimal("100.00"));
        variant.setStock(5);
        variant.setIsActive(true);
        product.getVariants().add(variant);
        return productRepository.save(product);
    }

    private String singleVariant(String sku, String sizeMl, String price, String stock) {
        return "{\"name\":\"perfume\",\"categoryId\":" + category.getId()
                + ",\"brand\":\"Brand\",\"variants\":[{\"sku\":\"" + sku
                + "\",\"sizeMl\":" + sizeMl + ",\"price\":" + price
                + ",\"stock\":" + stock + ",\"isActive\":true}]}";
    }

    @Test
    void createProduct_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleVariant("SKU-NA-1", "50", "99.99", "3")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_requiresAdminRole() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleVariant("SKU-NP-1", "50", "99.99", "3")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_createsProductAndBrandOnTheFly() throws Exception {
        String payload = "{\"name\":\"Rose Elixir\",\"categoryId\":" + category.getId()
                + ",\"brand\":\"Maison Margiela\",\"gender\":\"FEMALE\",\"intensity\":\"Eau de Parfum\""
                + ",\"fragNotes\":[\"Rose\",\"Musk\"]"
                + ",\"variants\":[{\"sku\":\"SKU-ME-1\",\"sizeMl\":50,\"price\":99.99,\"stock\":10,\"isActive\":true}]}";

        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rose Elixir"))
                .andExpect(jsonPath("$.brand").value("Maison Margiela"))
                .andExpect(jsonPath("$.gender").value("WOMEN"))
                .andExpect(jsonPath("$.intensity").value("MEDIUM"))
                .andExpect(jsonPath("$.inStock").value(true))
                .andExpect(jsonPath("$.variant[0].stock").value(10));

        Long createdId = productRepository.findAll().stream()
                .filter(product -> "Rose Elixir".equals(product.getName()))
                .map(Product::getId)
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/products/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rose Elixir"))
                .andExpect(jsonPath("$.brand").value("Maison Margiela"));
    }

    @Test
    void createProduct_missingName_returnsBadRequest() throws Exception {
        String payload = "{\"categoryId\":" + category.getId() + ",\"brand\":\"Brand\","
                + "\"variants\":[{\"sku\":\"SKU-MN-1\",\"sizeMl\":50,\"price\":10}]}";
        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_unknownCategory_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Missing Cat\",\"categoryId\":999999,"
                                + "\"brand\":\"Brand\",\"variants\":[{\"sku\":\"SKU-MC-1\",\"sizeMl\":50,\"price\":10}]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProduct_updatesFields() throws Exception {
        Product product = newProduct("Original Name", "SKU-UP-1");

        String payload = "{\"name\":\"Updated Name\",\"categoryId\":" + category.getId()
                + ",\"brand\":\"Dior\",\"variants\":[{\"sku\":\"SKU-UP-1\",\"sizeMl\":50,\"price\":99.99,\"stock\":20,\"isActive\":true}]}";
        mockMvc.perform(put("/api/products/" + product.getId())
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.brand").value("Dior"))
                .andExpect(jsonPath("$.variant[0].stock").value(20));
    }

    @Test
    void updateProduct_reusesKeptVariantAndRejectsForeignSku() throws Exception {
        Product first = newProduct("First", "SKU-DUP-1");
        Product second = newProduct("Second", "SKU-DUP-2");

        String payload = "{\"name\":\"Second\",\"categoryId\":" + category.getId()
                + ",\"brand\":\"Chanel\",\"variants\":[{\"sku\":\"SKU-DUP-1\",\"sizeMl\":50,\"price\":99.99,\"stock\":5,\"isActive\":true}]}";
        mockMvc.perform(put("/api/products/" + second.getId())
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/products/" + first.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("First"))
                .andExpect(jsonPath("$.variant[0].sku").value("SKU-DUP-1"));
    }

    @Test
    void deleteProduct_softDeletesAndHidesIt() throws Exception {
        Product product = newProduct("Disappear Me", "SKU-DEL-1");

        mockMvc.perform(delete("/api/products/" + product.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isNotFound());

        Product stored = productRepository.findById(product.getId()).orElseThrow();
        assert !stored.getIsActive();
    }

    @Test
    void updateStock_updatesVariant() throws Exception {
        Product product = newProduct("Stocked", "SKU-ST-1");
        Long variantId = product.getVariants().get(0).getId();

        mockMvc.perform(patch("/api/products/" + product.getId() + "/variants/" + variantId + "/stock")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inStock").value(false))
                .andExpect(jsonPath("$.variant[0].stock").value(0));

        Product stored = productRepository.findById(product.getId()).orElseThrow();
        assert stored.getVariants().get(0).getStock() == 0;
    }

    @Test
    void getAllProducts_hidesSoftDeletedByDefaultAndCanFilterInactive() throws Exception {
        newProduct("Active Only", "SKU-ACT-1");
        Product inactive = newProduct("Hidden From Shop", "SKU-HID-1");
        inactive.setIsActive(false);
        productRepository.save(inactive);

        mockMvc.perform(get("/api/products?search=Hidden"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/products?search=Hidden&isActive=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}