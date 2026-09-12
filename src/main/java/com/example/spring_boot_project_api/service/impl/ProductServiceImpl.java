package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.product.ProductFilterRequest;
import com.example.spring_boot_project_api.dto.request.product.ProductRequest;
import com.example.spring_boot_project_api.dto.request.product.ProductStockRequest;
import com.example.spring_boot_project_api.dto.request.product.ProductVariantRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.product.ProductResponse;
import com.example.spring_boot_project_api.enums.Gender;
import com.example.spring_boot_project_api.enums.Intensity;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.ProductMapper;
import com.example.spring_boot_project_api.model.Brand;
import com.example.spring_boot_project_api.model.Category;
import com.example.spring_boot_project_api.model.FragranceProfile;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductImage;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.repository.BrandRepository;
import com.example.spring_boot_project_api.repository.CategoryRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.ProductRepository.ProductRatingStat;
import com.example.spring_boot_project_api.repository.ProductVariantRepository;
import com.example.spring_boot_project_api.repository.specification.ProductSpecification;
import com.example.spring_boot_project_api.service.ProductService;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;

    public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper,
                              BrandRepository brandRepository,
                              CategoryRepository categoryRepository,
                              ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAllProducts(ProductFilterRequest filter) {
        if (filter == null) {
            filter = new ProductFilterRequest();
        }

        Page<Product> products = productRepository.findAll(
                ProductSpecification.fromFilter(filter),
                filter.toPageRequest());

        List<Long> ids = products.getContent().stream()
                .map(Product::getId)
                .toList();
        Map<Long, ProductRatingStat> statByProductId = ids.isEmpty()
                ? Map.of()
                : productRepository.findRatingStats(ids).stream()
                        .collect(Collectors.toMap(
                                ProductRatingStat::getProductId,
                                stat -> stat));

        List<ProductResponse> content = products.getContent().stream()
                .map(product -> {
                    ProductRatingStat stat = statByProductId.get(product.getId());
                    Double avgRate = stat != null ? stat.getAvgRate() : null;
                    Integer reviewCount = stat != null
                            ? stat.getReviewCount().intValue()
                            : 0;
                    return productMapper.toResponse(product, avgRate, reviewCount);
                })
                .toList();

        return new PagedResponse<>(
                content,
                products.getTotalElements(),
                products.getTotalPages(),
                products.getNumber(),
                products.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProductsByBrand(Long brandId, ProductFilterRequest filter) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with ID : " + brandId));
        if (filter == null) {
            filter = new ProductFilterRequest();
        }
        filter.setBrand(brand.getName());
        return getAllProducts(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .filter(Product::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID : " + id));

        ProductRatingStat stat = productRepository.findRatingStats(List.of(id))
                .stream()
                .findFirst()
                .orElse(null);
        Double avgRate = stat != null ? stat.getAvgRate() : null;
        Integer reviewCount = stat != null ? Math.toIntExact(stat.getReviewCount()) : 0;
        return productMapper.toResponse(product, avgRate, reviewCount);
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        if (request == null) {
            throw new BadRequestException("Product request cannot be null");
        }

        Category category = requireCategory(request.getCategoryId());
        Brand brand = getOrCreateBrand(request.getBrand());

        Product product = new Product();
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setCategory(category);
        product.setBrand(brand);
        product.setIsActive(request.getActive() == null ? Boolean.TRUE : request.getActive());

        applyVariants(product, request.getVariants());
        applyImages(product, request.getImages());
        applyFragranceProfile(product, request);

        productRepository.save(product);
        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        if (request == null) {
            throw new BadRequestException("Product request cannot be null");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID : " + id));

        Category category = requireCategory(request.getCategoryId());
        Brand brand = getOrCreateBrand(request.getBrand());

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setCategory(category);
        product.setBrand(brand);
        product.setIsActive(request.getActive() == null ? product.getIsActive() : request.getActive());

        applyVariants(product, request.getVariants());
        applyImages(product, request.getImages());
        applyFragranceProfile(product, request);

        productRepository.save(product);
        return productMapper.toResponse(product);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID : " + id));
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Override
    public ProductResponse updateVariantStock(Long productId, Long variantId, ProductStockRequest request) {
        if (request == null) {
            throw new BadRequestException("Stock request cannot be null");
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with ID : " + variantId));

        if (variant.getProduct() == null || !variant.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Variant does not belong to the given product");
        }

        variant.setStock(request.getStock());
        productVariantRepository.save(variant);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID : " + productId));
        return productMapper.toResponse(product);
    }

    private Category requireCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BadRequestException("Category is required");
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID : " + categoryId));
    }

    private Brand getOrCreateBrand(String brandName) {
        if (brandName == null || brandName.isBlank()) {
            throw new BadRequestException("Brand is required");
        }
        String name = brandName.trim();
        return brandRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Brand brand = new Brand();
            brand.setName(name);
            brand.setIsActive(true);
            return brandRepository.save(brand);
        });
    }

    private void applyVariants(Product product, List<ProductVariantRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("At least one variant is required");
        }

        Map<String, ProductVariant> existingBySku = product.getVariants().stream()
                .collect(Collectors.toMap(
                        variant -> variant.getSku().toLowerCase(),
                        variant -> variant,
                        (first, second) -> first));

        List<ProductVariant> kept = new ArrayList<>();
        for (ProductVariantRequest request : requests) {
            String sku = request.getSku().trim();
            ProductVariant variant = existingBySku.remove(sku.toLowerCase());

            if (variant == null) {
                // only conflict if the SKU is owned by another product
                ProductVariant other = productVariantRepository.findBySku(sku).orElse(null);
                if (other != null && (other.getProduct() == null
                        || !other.getProduct().getId().equals(product.getId()))) {
                    throw new BadRequestException("SKU already exists : " + sku);
                }
                variant = new ProductVariant();
                variant.setProduct(product);
                variant.setSku(sku);
            }

            variant.setSizeMl(request.getSizeMl());
            variant.setPrice(request.getPrice());
            variant.setStock(request.getStock() == null ? 0 : request.getStock());
            variant.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
            kept.add(variant);
        }

        product.getVariants().clear();
        product.getVariants().addAll(kept);
    }

    private void applyImages(Product product, List<String> imageUrls) {
        product.getImages().clear();
        if (imageUrls == null) {
            return;
        }
        for (int index = 0; index < imageUrls.size(); index++) {
            String url = imageUrls.get(index);
            if (url == null || url.isBlank()) {
                continue;
            }
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(url.trim());
            image.setIsPrimary(index == 0);
            image.setDisplayOrder(index);
            product.getImages().add(image);
        }
    }

    private void applyFragranceProfile(Product product, ProductRequest request) {
        boolean hasProfileInfo = request.getGender() != null
                || request.getFragranceFamily() != null
                || request.getIntensity() != null
                || (request.getFragNotes() != null && !request.getFragNotes().isEmpty());

        if (!hasProfileInfo) {
            return;
        }

        if (product.getFragranceProfile() == null) {
            FragranceProfile profile = new FragranceProfile();
            profile.setProduct(product);
            product.setFragranceProfile(profile);
        }

        FragranceProfile profile = product.getFragranceProfile();
        profile.setGender(parseGender(request.getGender()));
        profile.setFragranceFamily(request.getFragranceFamily());
        profile.setIntensity(parseIntensity(request.getIntensity()));
        profile.setFragNotes(joinFragNotes(request.getFragNotes()));
    }

    private Gender parseGender(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("MALE".equals(normalized)) {
            return Gender.MEN;
        }
        if ("FEMALE".equals(normalized)) {
            return Gender.WOMEN;
        }
        try {
            return Gender.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid gender: " + value);
        }
    }

    private Intensity parseIntensity(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Intensity.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            String normalized = value.trim().toLowerCase();
            if (normalized.contains("eau de toilette")) {
                return Intensity.LIGHT;
            }
            if (normalized.contains("eau de parfum")) {
                return Intensity.MEDIUM;
            }
            if (normalized.contains("parfum") || normalized.contains("extrait")) {
                return Intensity.STRONG;
            }
            throw new BadRequestException("Invalid intensity: " + value);
        }
    }

    private String joinFragNotes(List<String> fragNotes) {
        if (fragNotes == null) {
            return null;
        }
        return fragNotes.stream()
                .filter(note -> note != null && !note.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(", "));
    }
}