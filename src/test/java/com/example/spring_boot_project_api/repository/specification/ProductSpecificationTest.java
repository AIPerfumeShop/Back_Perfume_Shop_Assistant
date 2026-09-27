package com.example.spring_boot_project_api.repository.specification;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.product.ProductFilterRequest;
import com.example.spring_boot_project_api.enums.Gender;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.model.Brand;
import com.example.spring_boot_project_api.model.FragranceProfile;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@SuppressWarnings("unchecked")
class ProductSpecificationTest {

    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final Predicate conjunction = mock(Predicate.class);
    private final Path namePath = mock(Path.class);
    private final Path descPath = mock(Path.class);

    private Root<Product> root() {
        Root<Product> root = mock(Root.class);
        when(root.get("name")).thenReturn(namePath);
        when(root.get("description")).thenReturn(descPath);
        when(root.get("isActive")).thenReturn(mock(Path.class));
        when(cb.conjunction()).thenReturn(conjunction);
        when(cb.isTrue(any(Path.class))).thenReturn(mock(Predicate.class));
        when(cb.isFalse(any(Path.class))).thenReturn(mock(Predicate.class));
        when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
        return root;
    }

    private Predicate run(Specification<Product> spec, Root<Product> root) {
        return spec.toPredicate(root, query, cb);
    }

    @Test
    void fromFilter_emptyFilter_marksDistinctAndReturnsConjunction() {
        Root<Product> root = root();

        Predicate predicate = run(ProductSpecification.fromFilter(new ProductFilterRequest()), root);

        assertNotNull(predicate);
        verify(query).distinct(true);
    }

    @Test
    void fromFilter_default_excludesSoftDeletedProducts() {
        Root<Product> root = root();
        Path activePath = mock(Path.class);
        Predicate activePred = mock(Predicate.class);
        when(root.get("isActive")).thenReturn(activePath);
        when(cb.isTrue(activePath)).thenReturn(activePred);

        assertNotNull(run(ProductSpecification.fromFilter(new ProductFilterRequest()), root));
        verify(cb).isTrue(activePath);
    }

    @Test
    void fromFilter_isActiveFalse_selectsOnlyInactiveProducts() {
        Root<Product> root = root();
        Path activePath = mock(Path.class);
        Predicate inactivePred = mock(Predicate.class);
        when(root.get("isActive")).thenReturn(activePath);
        when(cb.isFalse(activePath)).thenReturn(inactivePred);

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setIsActive(false);

        assertNotNull(run(ProductSpecification.fromFilter(filter), root));
        verify(cb).isFalse(activePath);
    }

    @Test
    void fromFilter_searchIncludesProductBrandAndFragranceDetails() {
        Root<Product> root = root();
        Join brand = mock(Join.class);
        Join profile = mock(Join.class);
        when(root.join("brand", JoinType.LEFT)).thenReturn(brand);
        when(root.join("fragranceProfile", JoinType.LEFT)).thenReturn(profile);
        Path brandName = mock(Path.class);
        Path family = mock(Path.class);
        Path notes = mock(Path.class);
        when(brand.get("name")).thenReturn(brandName);
        when(profile.get("fragranceFamily")).thenReturn(family);
        when(profile.get("fragNotes")).thenReturn(notes);
        Predicate[] terms = new Predicate[5];
        Expression<String>[] lowered = new Expression[5];
        Path[] paths = { namePath, descPath, brandName, family, notes };
        for (int i = 0; i < paths.length; i++) {
            lowered[i] = mock(Expression.class);
            terms[i] = mock(Predicate.class);
            when(cb.lower(paths[i])).thenReturn(lowered[i]);
            when(cb.like(lowered[i], "%rose%")).thenReturn(terms[i]);
        }
        Predicate orPred = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);
        when(cb.or(any(Predicate[].class))).thenReturn(orPred);
        when(cb.and(conjunction, orPred)).thenReturn(combined);

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setSearch("ROSE");

        Predicate predicate = run(ProductSpecification.fromFilter(filter), root);

        assertNotNull(predicate);
        for (int i = 0; i < paths.length; i++) {
            verify(cb).lower(paths[i]);
            verify(cb).like(lowered[i], "%rose%");
        }
        verify(cb).or(terms);
    }

    @Test
    void fromFilter_gender_joinsFragranceProfile() {
        Root<Product> root = root();
        Join profile = mock(Join.class);
        Path genderPath = mock(Path.class);
        Predicate genderPred = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(root.join("fragranceProfile")).thenReturn(profile);
        when(profile.get("gender")).thenReturn(genderPath);
        when(cb.equal(genderPath, Gender.WOMEN)).thenReturn(genderPred);
        when(cb.and(conjunction, genderPred)).thenReturn(combined);

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setGender(Gender.WOMEN);

        assertNotNull(run(ProductSpecification.fromFilter(filter), root));
        verify(cb).equal(genderPath, Gender.WOMEN);
    }

    @Test
    void fromFilter_priceRange_appliesBetweenOnVariant() {
        Root<Product> root = root();
        stubLowestPrice(root);
        Predicate pricePred = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);
        when(cb.between(any(Expression.class), any(Comparable.class), any(Comparable.class))).thenReturn(pricePred);
        when(cb.and(conjunction, pricePred)).thenReturn(combined);

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setMinPrice(BigDecimal.TEN);
        filter.setMaxPrice(new BigDecimal("100"));

        assertNotNull(run(ProductSpecification.fromFilter(filter), root));
        verify(cb).between(any(Expression.class), any(Comparable.class), any(Comparable.class));
    }

    @Test
    void fromFilter_inStock_requiresPositiveStock() {
        Root<Product> root = root();
        Join variant = mock(Join.class);
        Path stockPath = mock(Path.class);
        Path activePath = mock(Path.class);
        Predicate stockPred = mock(Predicate.class);
        Predicate activePred = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(root.join("variants")).thenReturn(variant);
        when(variant.get("stock")).thenReturn(stockPath);
        when(variant.get("isActive")).thenReturn(activePath);
        when(cb.isTrue(activePath)).thenReturn(activePred);
        when(cb.greaterThan(any(Expression.class), eq(0))).thenReturn(stockPred);
        when(cb.and(conjunction, activePred)).thenReturn(combined);
        when(cb.and(combined, stockPred)).thenReturn(combined);

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setInStock(true);

        assertNotNull(run(ProductSpecification.fromFilter(filter), root));
        verify(cb).greaterThan(any(Expression.class), eq(0));
    }

    @Test
    void fromFilter_minRate_groupsByProductWithAverageHaving() {
        Root<Product> root = root();
        Join review = mock(Join.class);
        Expression avg = mock(Expression.class);
        Predicate havingPred = mock(Predicate.class);

        when(root.join("reviews")).thenReturn(review);
        when(cb.avg(any())).thenReturn(avg);
        when(cb.greaterThanOrEqualTo(any(), eq(4.0))).thenReturn(havingPred);

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setMinRate(4.0);

        assertNotNull(run(ProductSpecification.fromFilter(filter), root));
        verify(cb).avg(any());
        verify(query).having(havingPred);
    }

    @Test
    void fromFilter_priceSortAsc_ordersByLowestVariantPrice() {
        Root<Product> root = root();
        Expression<BigDecimal> min = stubLowestPrice(root);

        jakarta.persistence.criteria.Order ascOrder = mock(jakarta.persistence.criteria.Order.class);
        when(cb.asc(any())).thenReturn(ascOrder);

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setSort("price");

        assertNotNull(run(ProductSpecification.fromFilter(filter), root));
        verify(cb, org.mockito.Mockito.times(2)).min(any());
        verify(cb, org.mockito.Mockito.times(2)).asc(any());
        verify(query).orderBy(ascOrder, ascOrder);
    }

    @Test
    void fromFilter_priceSortDesc_ordersDescending() {
        Root<Product> root = root();
        stubLowestPrice(root);

        jakarta.persistence.criteria.Order descOrder = mock(jakarta.persistence.criteria.Order.class);
        when(cb.desc(any())).thenReturn(descOrder);

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setSort("price");
        filter.setDirection("desc");

        assertNotNull(run(ProductSpecification.fromFilter(filter), root));
        verify(cb, org.mockito.Mockito.times(2)).min(any());
        verify(cb).desc(any());
        verify(query).orderBy(descOrder, null);
    }

    @Test
    void fromFilter_averageRateSort_ordersByReviewAverage() {
        Root<Product> root = root();
        jakarta.persistence.criteria.Subquery<Double> averageRate = mock(jakarta.persistence.criteria.Subquery.class);
        Root review = mock(Root.class);
        Path productPath = mock(Path.class);
        Path productId = mock(Path.class);
        Path rootId = mock(Path.class);
        Path rating = mock(Path.class);
        Expression<Double> average = mock(Expression.class);
        Expression<Double> coalesced = mock(Expression.class);
        jakarta.persistence.criteria.Order descOrder = mock(jakarta.persistence.criteria.Order.class);
        when(query.subquery(Double.class)).thenReturn(averageRate);
        when(averageRate.from(com.example.spring_boot_project_api.model.Review.class)).thenReturn(review);
        when(review.get("product")).thenReturn(productPath);
        when(productPath.get("id")).thenReturn(productId);
        when(root.get("id")).thenReturn(rootId);
        when(review.get("rating")).thenReturn(rating);
        when(cb.avg(rating)).thenReturn(average);
        when(cb.coalesce(averageRate, 0.0)).thenReturn(coalesced);
        when(cb.equal(productId, rootId)).thenReturn(mock(Predicate.class));
        when(cb.desc(coalesced)).thenReturn(descOrder);

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setSort("averageRate");
        filter.setDirection("desc");

        assertNotNull(run(ProductSpecification.fromFilter(filter), root));
        verify(query).orderBy(descOrder, null);
    }

    private Expression<BigDecimal> stubLowestPrice(Root<Product> root) {
        jakarta.persistence.criteria.Subquery<BigDecimal> subquery =
                mock(jakarta.persistence.criteria.Subquery.class);
        Root<ProductVariant> variant = mock(Root.class);
        Path variantProduct = mock(Path.class);
        Path variantProductId = mock(Path.class);
        Path productId = mock(Path.class);
        Path activePath = mock(Path.class);
        Path pricePath = mock(Path.class);
        Predicate matches = mock(Predicate.class);
        Predicate active = mock(Predicate.class);
        Predicate available = mock(Predicate.class);
        Expression<BigDecimal> minimum = mock(Expression.class);
        when(query.subquery(BigDecimal.class)).thenReturn(subquery);
        when(subquery.from(ProductVariant.class)).thenReturn(variant);
        when(variant.get("product")).thenReturn(variantProduct);
        when(variantProduct.get("id")).thenReturn(variantProductId);
        when(root.get("id")).thenReturn(productId);
        when(variant.get("isActive")).thenReturn(activePath);
        when(variant.get("price")).thenReturn(pricePath);
        when(cb.equal(variantProductId, productId)).thenReturn(matches);
        when(cb.isTrue(activePath)).thenReturn(active);
        when(cb.and(matches, active)).thenReturn(available);
        when(cb.min(pricePath)).thenReturn(minimum);
        when(cb.coalesce(any(Expression.class), any(Expression.class))).thenReturn((Expression) minimum);
        when(subquery.select(minimum)).thenReturn(subquery);
        when(subquery.where(available)).thenReturn(subquery);
        return subquery;
    }
}
