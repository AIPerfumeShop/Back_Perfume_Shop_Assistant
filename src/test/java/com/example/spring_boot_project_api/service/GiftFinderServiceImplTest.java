package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.gift.GiftFinderRequest;
import com.example.spring_boot_project_api.dto.response.gift.GiftFinderAIExplanationDTO;
import com.example.spring_boot_project_api.dto.response.gift.GiftFinderResponse;
import com.example.spring_boot_project_api.dto.response.gift.GiftRecommendationDTO;
import com.example.spring_boot_project_api.enums.Gender;
import com.example.spring_boot_project_api.enums.GiftConfidence;
import com.example.spring_boot_project_api.enums.GiftKnowledgeLevel;
import com.example.spring_boot_project_api.enums.GiftOccasion;
import com.example.spring_boot_project_api.enums.GiftPersonalityVibe;
import com.example.spring_boot_project_api.enums.GiftRecipientType;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.model.Brand;
import com.example.spring_boot_project_api.model.FragranceProfile;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.repository.FragranceProfileRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.ProductRepository.ProductRatingStat;
import com.example.spring_boot_project_api.repository.ProductVariantRepository;
import com.example.spring_boot_project_api.service.impl.GiftFinderServiceImpl;

@ExtendWith(MockitoExtension.class)
class GiftFinderServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private FragranceProfileRepository fragranceProfileRepository;

    @Mock
    private OpenRouterService openRouterService;

    private GiftFinderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GiftFinderServiceImpl(
                productRepository, productVariantRepository, fragranceProfileRepository,
                openRouterService, false);
    }

    private Product product(long id, String name, String brandName,
                            Gender gender, String family, String notes) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setIsActive(true);
        Brand brand = new Brand();
        brand.setName(brandName);
        p.setBrand(brand);
        FragranceProfile profile = new FragranceProfile();
        profile.setProduct(p);
        profile.setGender(gender);
        profile.setFragranceFamily(family);
        profile.setFragNotes(notes);
        p.setFragranceProfile(profile);
        return p;
    }

    private ProductVariant variant(Product product, BigDecimal price, int stock, boolean active) {
        ProductVariant v = new ProductVariant();
        v.setProduct(product);
        v.setPrice(price);
        v.setStock(stock);
        v.setIsActive(active);
        v.setSizeMl(50);
        v.setSku("SKU-" + product.getId());
        return v;
    }

    private GiftFinderRequest request() {
        GiftFinderRequest req = new GiftFinderRequest();
        req.setRecipientType(GiftRecipientType.PARTNER);
        req.setOccasion(GiftOccasion.BIRTHDAY);
        req.setPersonalityVibes(List.of(
                GiftPersonalityVibe.ROMANTIC, GiftPersonalityVibe.ELEGANT));
        req.setBudgetMin(new BigDecimal("20"));
        req.setBudgetMax(new BigDecimal("50"));
        req.setKnowledgeLevel(GiftKnowledgeLevel.NOT_REALLY);
        req.setScentPreference(List.of());
        return req;
    }

    private void stubCandidates(List<Product> products, List<ProductVariant> variants) {
        Page<Product> page = new PageImpl<>(products);
        when(productRepository.findAll(
                any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productVariantRepository.findAllByProductIdIn(any())).thenReturn(variants);
        when(fragranceProfileRepository.findAllByProductIdIn(any())).thenReturn(
                products.stream().map(Product::getFragranceProfile).toList());
        when(productRepository.findRatingStats(any())).thenReturn(List.of());
    }

    @Test
    void returnsTopRecommendationAndTwoAlternatives() {
        Product floral = product(1, "Rose Bloom", "Maison", Gender.WOMEN, "Floral", "rose, vanilla");
        Product fresh = product(2, "Ocean Mist", "Maison", Gender.WOMEN, "Fresh", "citrus, aquatic");
        Product woody = product(3, "Oud Night", "Maison", Gender.MEN, "Woody", "oud, leather");

        stubCandidates(List.of(floral, fresh, woody), List.of(
                variant(floral, new BigDecimal("45.00"), 5, true),
                variant(fresh, new BigDecimal("30.00"), 5, true),
                variant(woody, new BigDecimal("25.00"), 5, true)));

        GiftFinderResponse response = service.recommend(request());

        assertTrue(response.isExactMatchFound());
        assertNull(response.getMessage());
        assertNotNull(response.getTopRecommendation());
        assertEquals(1L, response.getTopRecommendation().getProductId());
        assertEquals(GiftConfidence.HIGH, response.getTopRecommendation().getGiftConfidence());
        assertEquals(2, response.getAlternatives().size());
        assertTrue(response.getTopRecommendation().getMatchScore() >= 80);
        assertFalse(response.getTopRecommendation().getReasons().isEmpty());
    }

    @Test
    void neverReturnsMoreThanThreeProducts() {
        Product p1 = product(1, "P1", "B", Gender.UNISEX, "Floral", "rose");
        Product p2 = product(2, "P2", "B", Gender.UNISEX, "Fresh", "citrus");
        Product p3 = product(3, "P3", "B", Gender.UNISEX, "Woody", "woods");
        Product p4 = product(4, "P4", "B", Gender.UNISEX, "Fruity", "berry");

        stubCandidates(List.of(p1, p2, p3, p4), List.of(
                variant(p1, new BigDecimal("30.00"), 2, true),
                variant(p2, new BigDecimal("30.00"), 2, true),
                variant(p3, new BigDecimal("30.00"), 2, true),
                variant(p4, new BigDecimal("30.00"), 2, true)));

        GiftFinderResponse response = service.recommend(request());

        int total = (response.getTopRecommendation() == null ? 0 : 1)
                + response.getAlternatives().size();
        assertEquals(3, total);
    }

    @Test
    void samesRequestProducesSameScore() {
        Product floral = product(1, "Rose Bloom", "Maison", Gender.WOMEN, "Floral", "rose");
        stubCandidates(List.of(floral), List.of(variant(floral, new BigDecimal("40.00"), 3, true)));

        GiftFinderResponse first = service.recommend(request());
        GiftFinderResponse second = service.recommend(request());

        assertEquals(first.getTopRecommendation().getMatchScore(),
                second.getTopRecommendation().getMatchScore());
    }

    @Test
    void excludesOutOfStockProducts() {
        Product available = product(1, "Rose Bloom", "Maison", Gender.WOMEN, "Floral", "rose");
        Product gone = product(2, "Sold Out", "Maison", Gender.WOMEN, "Floral", "rose");

        stubCandidates(List.of(available, gone), List.of(
                variant(available, new BigDecimal("40.00"), 5, true),
                variant(gone, new BigDecimal("40.00"), 0, true)));

        GiftFinderResponse response = service.recommend(request());

        assertNotNull(response.getTopRecommendation());
        assertEquals(1L, response.getTopRecommendation().getProductId());
        boolean mentionsGone = response.getAlternatives().stream()
                .anyMatch(r -> r.getProductId() == 2L);
        assertFalse(mentionsGone);
    }

    @Test
    void fallsBackToRelaxedSearchWhenBudgetHasNoExactMatch() {
        Product floral = product(1, "Rose Bloom", "Maison", Gender.WOMEN, "Floral", "rose");
        when(productRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()), new PageImpl<>(List.of(floral)));
        when(productVariantRepository.findAllByProductIdIn(any()))
                .thenReturn(List.of(variant(floral, new BigDecimal("45.00"), 5, true)));
        when(fragranceProfileRepository.findAllByProductIdIn(any()))
                .thenReturn(List.of(floral.getFragranceProfile()));
        when(productRepository.findRatingStats(any())).thenReturn(List.of());

        GiftFinderResponse response = service.recommend(request());

        assertFalse(response.isExactMatchFound());
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().toLowerCase().contains("budget"));
        assertNull(response.getTopRecommendation());
        assertEquals(1, response.getAlternatives().size());
        assertEquals(1L, response.getAlternatives().get(0).getProductId());
    }

    @Test
    void returnsEmptyWhenNoProductsMatchAtAll() {
        when(productRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()), new PageImpl<>(List.of()));

        GiftFinderResponse response = service.recommend(request());

        assertFalse(response.isExactMatchFound());
        assertNotNull(response.getMessage());
        assertNull(response.getTopRecommendation());
        assertTrue(response.getAlternatives().isEmpty());
    }

    @Test
    void rejectsBudgetMinGreaterThanBudgetMax() {
        GiftFinderRequest req = request();
        req.setBudgetMin(new BigDecimal("100"));
        req.setBudgetMax(new BigDecimal("10"));

        org.junit.jupiter.api.Assertions.assertThrows(
                BadRequestException.class, () -> service.recommend(req));
    }

    @Test
    void aiExplanationUsedWhenEnabledAndSuccessful() {
        service = new GiftFinderServiceImpl(
                productRepository, productVariantRepository, fragranceProfileRepository,
                openRouterService, true);

        Product floral = product(1, "Rose Bloom", "Maison", Gender.WOMEN, "Floral", "rose");
        stubCandidates(List.of(floral), List.of(variant(floral, new BigDecimal("40.00"), 3, true)));
        when(openRouterService.generateResponse(any(), any()))
                .thenReturn("This gourmet floral is a lovely romantic gift.");

        GiftFinderResponse response = service.recommend(request());

        GiftFinderAIExplanationDTO explanation = response.getAiExplanation();
        assertNotNull(explanation);
        assertEquals("This gourmet floral is a lovely romantic gift.", explanation.getSummary());
        assertFalse(explanation.getReasons().isEmpty());
    }

    @Test
    void aiExplanationFallsBackWhenAiFails() {
        service = new GiftFinderServiceImpl(
                productRepository, productVariantRepository, fragranceProfileRepository,
                openRouterService, true);

        Product floral = product(1, "Rose Bloom", "Maison", Gender.WOMEN, "Floral", "rose");
        stubCandidates(List.of(floral), List.of(variant(floral, new BigDecimal("40.00"), 3, true)));
        when(openRouterService.generateResponse(any(), any()))
                .thenThrow(new RuntimeException("OpenRouter down"));

        GiftFinderResponse response = service.recommend(request());

        GiftFinderAIExplanationDTO explanation = response.getAiExplanation();
        assertNotNull(explanation);
        assertNotNull(explanation.getSummary());
        assertFalse(explanation.getReasons().isEmpty());
    }

    @Test
    void aiExplanationIsNullWhenDisabled() {
        Product floral = product(1, "Rose Bloom", "Maison", Gender.WOMEN, "Floral", "rose");
        stubCandidates(List.of(floral), List.of(variant(floral, new BigDecimal("40.00"), 3, true)));

        GiftFinderResponse response = service.recommend(request());

        assertNull(response.getAiExplanation());
    }

    @Test
    void knowledgeLevelNotReallyStillReturnsRecommendationWithoutScentPreference() {
        Product floral = product(1, "Rose Bloom", "Maison", Gender.WOMEN, "Floral", "rose");
        stubCandidates(List.of(floral), List.of(variant(floral, new BigDecimal("40.00"), 3, true)));
        GiftFinderRequest req = request();
        req.setKnowledgeLevel(GiftKnowledgeLevel.NOT_REALLY);
        req.setScentPreference(null);

        GiftFinderResponse response = service.recommend(req);

        assertNotNull(response.getTopRecommendation());
        assertTrue(response.getTopRecommendation().getMatchScore() >= 0);
    }

    @Test
    void scentPreferenceBoostsMatchingProduct() {
        service = new GiftFinderServiceImpl(
                productRepository, productVariantRepository, fragranceProfileRepository,
                openRouterService, false);

        Product floral = product(1, "Rose Bloom", "Maison", Gender.WOMEN, "Floral", "rose");
        Product woody = product(2, "Oud Night", "Maison", Gender.MEN, "Woody", "oud");

        GiftFinderRequest req = request();
        req.setKnowledgeLevel(GiftKnowledgeLevel.VERY_WELL);
        req.setScentPreference(List.of(com.example.spring_boot_project_api.enums.ScentPreference.FLORAL));

        stubCandidates(List.of(floral, woody), List.of(
                variant(floral, new BigDecimal("40.00"), 3, true),
                variant(woody, new BigDecimal("40.00"), 3, true)));

        GiftFinderResponse response = service.recommend(req);

        assertNotNull(response.getTopRecommendation());
        assertEquals(1L, response.getTopRecommendation().getProductId());
    }

    @Test
    void ratingStatBoostsPerformanceFactor() {
        service = new GiftFinderServiceImpl(
                productRepository, productVariantRepository, fragranceProfileRepository,
                openRouterService, false);

        Product a = product(1, "Rated", "Maison", Gender.WOMEN, "Floral", "rose");
        Product b = product(2, "Unrated", "Maison", Gender.WOMEN, "Floral", "rose");

        Page<Product> page = new PageImpl<>(List.of(a, b));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productVariantRepository.findAllByProductIdIn(any())).thenReturn(List.of(
                variant(a, new BigDecimal("40.00"), 3, true),
                variant(b, new BigDecimal("40.00"), 3, true)));
        when(fragranceProfileRepository.findAllByProductIdIn(any())).thenReturn(List.of(
                a.getFragranceProfile(), b.getFragranceProfile()));

        ProductRatingStat stat = mock(ProductRatingStat.class);
        when(stat.getProductId()).thenReturn(1L);
        when(stat.getAvgRate()).thenReturn(5.0);
        when(stat.getReviewCount()).thenReturn(50L);
        when(productRepository.findRatingStats(any())).thenReturn(List.of(stat));

        GiftFinderResponse response = service.recommend(request());

        assertNotNull(response.getTopRecommendation());
        assertEquals(1L, response.getTopRecommendation().getProductId());
        assertEquals(GiftConfidence.HIGH, response.getTopRecommendation().getGiftConfidence());
    }
}