package com.example.spring_boot_project_api.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.product.ProductFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.product.ProductResponse;
import com.example.spring_boot_project_api.mapper.ProductMapper;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.ProductRepository.ProductRatingStat;
import com.example.spring_boot_project_api.repository.specification.ProductSpecification;
import com.example.spring_boot_project_api.service.ProductService;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
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
}