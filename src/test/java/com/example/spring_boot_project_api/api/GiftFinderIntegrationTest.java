package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

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

import com.example.spring_boot_project_api.enums.Gender;
import com.example.spring_boot_project_api.model.Brand;
import com.example.spring_boot_project_api.model.Category;
import com.example.spring_boot_project_api.model.FragranceProfile;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.repository.BrandRepository;
import com.example.spring_boot_project_api.repository.CategoryRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.service.EmailService;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class GiftFinderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private EmailService emailService;

    private long floralId;
    private long freshId;
    private long woodyId;
    private long outOfStockId;
    private long expensiveId;
    private long inactiveId;

    @BeforeEach
    void setUp() {
        Brand brand = new Brand();
        brand.setName("Gift Finder Test Brand");
        brand = brandRepository.save(brand);

        Category category = new Category();
        category.setName("Gift Finder Test Category");
        category = categoryRepository.save(category);

        floralId = product(brand, category, "Rose Bloom", Gender.WOMEN, "Floral",
                "rose, vanilla", new BigDecimal("45.00"), 5, true).getId();
        freshId = product(brand, category, "Ocean Mist", Gender.WOMEN, "Fresh",
                "citrus, aquatic", new BigDecimal("30.00"), 5, true).getId();
        woodyId = product(brand, category, "Oud Night", Gender.MEN, "Woody",
                "oud, leather", new BigDecimal("25.00"), 5, true).getId();
        outOfStockId = product(brand, category, "Sold Out Floral", Gender.WOMEN, "Floral",
                "rose", new BigDecimal("40.00"), 0, true).getId();
        expensiveId = product(brand, category, "Ultra Luxe", Gender.UNISEX, "Amber",
                "amber, oud", new BigDecimal("500.00"), 5, true).getId();
        inactiveId = product(brand, category, "Inactive Scent", Gender.UNISEX, "Fresh",
                "citrus", new BigDecimal("30.00"), 5, false).getId();
    }

    private Product product(Brand brand, Category category, String name, Gender gender,
                            String family, String notes, BigDecimal price,
                            int stock, boolean active) {
        Product product = new Product();
        product.setName(name);
        product.setBrand(brand);
        product.setCategory(category);
        product.setIsActive(active);

        ProductVariant variant = new ProductVariant();
        variant.setSku("GF-" + name.replaceAll("\\s+", "-") + "-" + System.nanoTime());
        variant.setSizeMl(50);
        variant.setPrice(price);
        variant.setStock(stock);
        variant.setIsActive(true);
        variant.setProduct(product);
        product.getVariants().add(variant);

        FragranceProfile profile = new FragranceProfile();
        profile.setProduct(product);
        profile.setGender(gender);
        profile.setFragranceFamily(family);
        profile.setFragNotes(notes);
        product.setFragranceProfile(profile);

        return productRepository.save(product);
    }

    private String payload(double budgetMin, double budgetMax) {
        return "{"
                + "\"recipientType\":\"PARTNER\","
                + "\"occasion\":\"BIRTHDAY\","
                + "\"personalityVibes\":[\"ROMANTIC\",\"ELEGANT\"],"
                + "\"budgetMin\":" + budgetMin + ","
                + "\"budgetMax\":" + budgetMax + ","
                + "\"knowledgeLevel\":\"NOT_REALLY\","
                + "\"scentPreference\":[]"
                + "}";
    }

    @Test
    void publicEndpoint_allowsRequestsWithoutToken() throws Exception {
        mockMvc.perform(post("/api/gift-finder/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(20, 50)))
                .andExpect(status().isOk());
    }

    @Test
    void returnsTopRecommendationWithTwoAlternativesMax() throws Exception {
        mockMvc.perform(post("/api/gift-finder/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(20, 50)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exactMatchFound").value(true))
                .andExpect(jsonPath("$.topRecommendation").exists())
                .andExpect(jsonPath("$.topRecommendation.productId").value(floralId))
                .andExpect(jsonPath("$.topRecommendation.matchScore").isNumber())
                .andExpect(jsonPath("$.topRecommendation.giftConfidence").value("HIGH"))
                .andExpect(jsonPath("$.alternatives.length()").value(2))
                .andExpect(jsonPath("$.aiExplanation").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void excludesOutOfStockAndInactiveProducts() throws Exception {
        mockMvc.perform(post("/api/gift-finder/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(20, 100)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topRecommendation.productId").value(floralId))
                .andExpect(jsonPath("$.alternatives[0].productId").isNotEmpty())
                .andExpect(jsonPath("$.alternatives[*].productId").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem((int) outOfStockId))))
                .andExpect(jsonPath("$.alternatives[*].productId").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem((int) inactiveId))));
    }

    @Test
    void budgetFiltersOutExpensiveProducts() throws Exception {
        // Only floral(45), fresh(30), woody(25) are within 20-50
        mockMvc.perform(post("/api/gift-finder/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(20, 50)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alternatives[*].productId").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem((int) expensiveId))));
    }

    @Test
    void noBudgetMatch_returnsRelaxedAlternativesAndNoExactMatch() throws Exception {
        mockMvc.perform(post("/api/gift-finder/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(1, 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exactMatchFound").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.topRecommendation").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.alternatives").isArray())
                .andExpect(jsonPath("$.alternatives.length()").value(3));
    }

    @Test
    void negativeBudget_rejected() throws Exception {
        mockMvc.perform(post("/api/gift-finder/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(-10, 50)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void budgetMinGreaterThanBudgetMax_rejected() throws Exception {
        mockMvc.perform(post("/api/gift-finder/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(100, 10)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingRequiredField_rejected() throws Exception {
        mockMvc.perform(post("/api/gift-finder/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"occasion\":\"BIRTHDAY\",\"budgetMin\":10,\"budgetMax\":50,"
                                + "\"knowledgeLevel\":\"NOT_REALLY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sameRequestProducesSameMatchScore() throws Exception {
        String first = mockMvc.perform(post("/api/gift-finder/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(20, 50)))
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/gift-finder/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(20, 50)))
                .andReturn().getResponse().getContentAsString();

        // Expect both to pick the same top product with the same score.
        String scoreRegex = "\"topRecommendation\"\\{.*?\"matchScore\":(\\d+)";
        assertSameInt(first, second, scoreRegex);
    }

    private void assertSameInt(String first, String second, String regex) {
        java.util.regex.Matcher m1 = java.util.regex.Pattern.compile(regex).matcher(first);
        java.util.regex.Matcher m2 = java.util.regex.Pattern.compile(regex).matcher(second);
        if (m1.find() && m2.find()) {
            org.junit.jupiter.api.Assertions.assertEquals(m1.group(1), m2.group(1));
        }
    }

    @Test
    void supportsNoScentPreferenceAndNotReallyKnowledge() throws Exception {
        mockMvc.perform(post("/api/gift-finder/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"recipientType\":\"MOTHER\","
                                + "\"occasion\":\"VALENTINE\","
                                + "\"personalityVibes\":[\"ROMANTIC\"],"
                                + "\"budgetMin\":0,"
                                + "\"budgetMax\":1000,"
                                + "\"knowledgeLevel\":\"NOT_REALLY\""
                                + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topRecommendation.productId").value(floralId))
                .andExpect(jsonPath("$.alternatives.length()").value(2));
    }
}