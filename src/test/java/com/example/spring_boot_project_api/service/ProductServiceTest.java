package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.product.ProductFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.product.ProductResponse;
import com.example.spring_boot_project_api.enums.Gender;
import com.example.spring_boot_project_api.mapper.ProductMapper;
import com.example.spring_boot_project_api.model.Brand;
import com.example.spring_boot_project_api.model.Category;
import com.example.spring_boot_project_api.model.FragranceProfile;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.service.impl.ProductServiceImpl;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, new ProductMapper());
    }

    private Brand brand() {
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Lancome");
        return brand;
    }

    private Category category() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Floral");
        return category;
    }

    private FragranceProfile fragranceProfile() {
        FragranceProfile profile = new FragranceProfile();
        profile.setGender(Gender.WOMEN);
        profile.setFragranceFamily("Floral");
        profile.setFragNotes("Rose, Jasmine");
        return profile;
    }

    private Product product(Long id, String name) {
        ProductVariant variant = new ProductVariant();
        variant.setId(id * 10);
        variant.setProduct(null);
        variant.setSizeMl(50);
        variant.setPrice(new java.math.BigDecimal("59.50"));
        variant.setStock(10);
        variant.setIsActive(true);

        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setBrand(brand());
        product.setCategory(category());
        product.setFragranceProfile(fragranceProfile());
        product.setIsActive(true);
        product.setVariants(List.of(variant));

        variant.setProduct(product);
        return product;
    }

    // ---------- getAllProducts ----------

    @Test
    void getAllProducts_returnsPagedWithStats() {
        Product product = product(1L, "Idole");
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        ProductRepository.ProductRatingStat stat = new ProductRepository.ProductRatingStat() {
            @Override public Long getProductId() { return 1L; }
            @Override public Double getAvgRate() { return 4.5; }
            @Override public Long getReviewCount() { return 12L; }
        };
        when(productRepository.findRatingStats(anyCollection()))
                .thenReturn(List.of(stat));

        PagedResponse<ProductResponse> response =
                productService.getAllProducts(new ProductFilterRequest());

        assertNotNull(response);
        assertEquals(1, response.getData().size());
        ProductResponse productResponse = response.getData().get(0);
        assertEquals(1L, productResponse.getId());
        assertEquals("Idole", productResponse.getName());
        assertEquals("Lancome", productResponse.getBrand());
        assertEquals("Floral", productResponse.getCategoryName());
        assertEquals(4.5, productResponse.getAverageRate());
        assertEquals(12, productResponse.getReviewCount());
        assertTrue(productResponse.getInStock());
        assertEquals(Gender.WOMEN, productResponse.getGender());
    }

    @Test
    void getAllProducts_nullFilter_usesDefaults() {
        Page<Product> page = new PageImpl<>(List.of());
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<ProductResponse> response = productService.getAllProducts(null);

        assertNotNull(response);
        assertTrue(response.getData().isEmpty());
    }

    @Test
    void getAllProducts_emptyPage() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PagedResponse<ProductResponse> response =
                productService.getAllProducts(new ProductFilterRequest());

        assertTrue(response.getData().isEmpty());
        assertEquals(0, response.getTotalElements());
    }

    @Test
    void getAllProducts_noStats_returnsZeroReviewCount() {
        Product product = product(1L, "Idole");
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productRepository.findRatingStats(anyCollection()))
                .thenReturn(List.of());

        PagedResponse<ProductResponse> response =
                productService.getAllProducts(new ProductFilterRequest());

        ProductResponse productResponse = response.getData().get(0);
        assertEquals(0, productResponse.getReviewCount());
        assertEquals(null, productResponse.getAverageRate());
    }

    @Test
    void getAllProducts_multipleProducts() {
        Product idole = product(1L, "Idole");
        Product laNuit = product(2L, "La Nuit Tresor");
        Page<Product> page = new PageImpl<>(List.of(idole, laNuit));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productRepository.findRatingStats(anyCollection()))
                .thenReturn(List.of());

        PagedResponse<ProductResponse> response =
                productService.getAllProducts(new ProductFilterRequest());

        assertEquals(2, response.getData().size());
        assertEquals("Idole", response.getData().get(0).getName());
        assertEquals("La Nuit Tresor", response.getData().get(1).getName());
    }
}
