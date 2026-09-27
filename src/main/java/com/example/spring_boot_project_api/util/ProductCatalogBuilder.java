package com.example.spring_boot_project_api.util;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.repository.ProductRepository;

@Component
public class ProductCatalogBuilder {

    private static final int MAX_CATALOG_SIZE = 100;

    private final ProductRepository productRepository;

    public ProductCatalogBuilder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Builds a compact snapshot of products customers can currently buy.
    public String build() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(
                "Blossom Fragrance product catalog. Recommend ONLY products "
                + "from this list, using their exact names. Never invent names "
                + "or prices:");
        int added = 0;
        for (Product product : products) {
            if (added >= MAX_CATALOG_SIZE) {
                break;
            }
            if (!Boolean.TRUE.equals(product.getIsActive())) {
                continue;
            }
            BigDecimal price = null;
            if (product.getVariants() != null) {
                for (ProductVariant variant : product.getVariants()) {
                    if (!Boolean.TRUE.equals(variant.getIsActive())) {
                        continue;
                    }
                    if (variant.getStock() != null && variant.getStock() > 0
                            && variant.getPrice() != null
                            && (price == null || variant.getPrice().compareTo(price) < 0)) {
                        price = variant.getPrice();
                    }
                }
            }
            // Do not give the model unavailable products as recommendation
            // candidates. The backend's ranked recommendation endpoint uses
            // the same active, in-stock rule.
            if (price == null) {
                continue;
            }
            sb.append("\n- ").append(product.getName())
                    .append(" (brand: ")
                    .append(product.getBrand() != null ? product.getBrand().getName() : "unknown")
                    .append(", from $").append(price)
                    .append(")");
            added++;
        }
        if (added == 0) {
            return "No active, in-stock perfumes with a listed price are currently available. "
                    + "Do not recommend a product until the catalog has available items.";
        }
        return sb.toString();
    }
}
