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

    // Builds a compact text snapshot of the shop catalog so the model only
    // recommends products that actually exist (name, brand, price, stock).
    public String build() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(
                "Blossom Fragrance product catalog. Recommend ONLY products "
                + "from this list, using their exact names. Never invent names, "
                + "prices, or products that are not listed here:");
        int added = 0;
        for (Product product : products) {
            if (added >= MAX_CATALOG_SIZE) {
                break;
            }
            if (!Boolean.TRUE.equals(product.getIsActive())) {
                continue;
            }
            boolean inStock = false;
            BigDecimal price = null;
            if (product.getVariants() != null) {
                for (ProductVariant variant : product.getVariants()) {
                    if (!Boolean.TRUE.equals(variant.getIsActive())) {
                        continue;
                    }
                    if (variant.getStock() != null && variant.getStock() > 0) {
                        inStock = true;
                    }
                    if (variant.getPrice() != null
                            && (price == null || variant.getPrice().compareTo(price) < 0)) {
                        price = variant.getPrice();
                    }
                }
            }
            sb.append("\n- ").append(product.getName())
                    .append(" (brand: ")
                    .append(product.getBrand() != null ? product.getBrand().getName() : "unknown")
                    .append(", from ").append(price != null ? "$" + price : "price unavailable")
                    .append(", in stock: ").append(inStock ? "yes" : "no")
                    .append(")");
            added++;
        }
        return sb.toString();
    }
}