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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
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
        when(cb.conjunction()).thenReturn(conjunction);
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
    void fromFilter_search_combinesNameOrDescription() {
        Root<Product> root = root();
        Predicate nameLike = mock(Predicate.class);
        Predicate descLike = mock(Predicate.class);
        Predicate orPred = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);
        Expression<String> lowerName = mock(Expression.class);
        Expression<String> lowerDesc = mock(Expression.class);

        when(cb.lower(namePath)).thenReturn(lowerName);
        when(cb.lower(descPath)).thenReturn(lowerDesc);
        when(cb.like(lowerName, "%rose%")).thenReturn(nameLike);
        when(cb.like(lowerDesc, "%rose%")).thenReturn(descLike);
        when(cb.or(nameLike, descLike)).thenReturn(orPred);
        when(cb.and(conjunction, orPred)).thenReturn(combined);

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setSearch("ROSE");

        Predicate predicate = run(ProductSpecification.fromFilter(filter), root);

        assertNotNull(predicate);
        verify(cb).lower(namePath);
        verify(cb).lower(descPath);
        verify(cb).like(lowerName, "%rose%");
        verify(cb).like(lowerDesc, "%rose%");
        verify(cb).or(nameLike, descLike);
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
        Join variant = mock(Join.class);
        Path pricePath = mock(Path.class);
        Predicate pricePred = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(root.join("variants")).thenReturn(variant);
        when(variant.get("price")).thenReturn(pricePath);
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
        Predicate stockPred = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(root.join("variants")).thenReturn(variant);
        when(variant.get("stock")).thenReturn(stockPath);
        when(cb.greaterThan(any(Expression.class), eq(0))).thenReturn(stockPred);
        when(cb.and(conjunction, stockPred)).thenReturn(combined);

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
        filter.setMinRate(4);

        assertNotNull(run(ProductSpecification.fromFilter(filter), root));
        verify(cb).avg(any());
        verify(query).having(havingPred);
    }

    @Test
    void fromFilter_priceSortAsc_ordersByLowestVariantPrice() {
        Root<Product> root = root();
        Join variant = mock(Join.class);
        Expression min = mock(Expression.class);

        jakarta.persistence.criteria.Order ascOrder = mock(jakarta.persistence.criteria.Order.class);
        jakarta.persistence.criteria.Subquery subquery = mock(jakarta.persistence.criteria.Subquery.class);
        Root subVariant = mock(Root.class);
        Path productPath = mock(Path.class);

        when(query.subquery(BigDecimal.class)).thenReturn(subquery);
        when(subquery.from(ProductVariant.class)).thenReturn(subVariant);
        when(subVariant.get("product")).thenReturn(productPath);
        when(cb.min(any())).thenReturn(min);
        when(cb.asc(any())).thenReturn(ascOrder);

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setSort("price");

        assertNotNull(run(ProductSpecification.fromFilter(filter), root));
        verify(cb).min(any());
        verify(cb).asc(any());
        verify(query).orderBy(ascOrder);
    }

    @Test
    void fromFilter_priceSortDesc_ordersDescending() {
        Root<Product> root = root();
        Join variant = mock(Join.class);
        Expression min = mock(Expression.class);

        jakarta.persistence.criteria.Order descOrder = mock(jakarta.persistence.criteria.Order.class);
        jakarta.persistence.criteria.Subquery subquery = mock(jakarta.persistence.criteria.Subquery.class);
        Root subVariant = mock(Root.class);
        Path productPath = mock(Path.class);

        when(query.subquery(BigDecimal.class)).thenReturn(subquery);
        when(subquery.from(ProductVariant.class)).thenReturn(subVariant);
        when(subVariant.get("product")).thenReturn(productPath);
        when(cb.min(any())).thenReturn(min);
        when(cb.desc(any())).thenReturn(descOrder);

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setSort("price");
        filter.setDirection("desc");

        assertNotNull(run(ProductSpecification.fromFilter(filter), root));
        verify(cb).min(any());
        verify(cb).desc(any());
        verify(query).orderBy(descOrder);
    }
}