package com.example.spring_boot_project_api.repository.specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.brand.BrandFilterRequest;
import com.example.spring_boot_project_api.model.Brand;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

class BrandSpecificationTest {

    @SuppressWarnings("unchecked")
    private Root<Brand> mockRoot() {
        Root<Brand> root = mock(Root.class);
        Path<Object> namePath = mock(Path.class);
        Path<Object> activePath = mock(Path.class);
        when(root.get("name")).thenReturn(namePath);
        when(root.get("isActive")).thenReturn(activePath);
        return root;
    }

    @Test
    void fromFilter_emptyFilter_buildsConjunction() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Brand> root = mockRoot();
        Predicate conjunction = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjunction);

        Specification<Brand> spec = BrandSpecification.fromFilter(new BrandFilterRequest());
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        assertEquals(conjunction, result);
    }

    @Test
    void fromFilter_searchAndActive_appliesLikeAndEqual() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Brand> root = mockRoot();

        Predicate conjunction = mock(Predicate.class);
        Predicate likePred = mock(Predicate.class);
        Predicate activePred = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(cb.conjunction()).thenReturn(conjunction);
        when(cb.lower(root.get("name"))).thenReturn(mock(Path.class));
        when(cb.like(any(Path.class), eq("%chan%"))).thenReturn(likePred);
        when(cb.equal(root.get("isActive"), true)).thenReturn(activePred);
        when(cb.and(conjunction, likePred)).thenReturn(combined);
        when(cb.and(combined, activePred)).thenReturn(activePred);

        BrandFilterRequest filter = new BrandFilterRequest();
        filter.setSearch("CHAN");
        filter.setIsActive(true);

        Predicate result = BrandSpecification.fromFilter(filter).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).like(any(Path.class), eq("%chan%"));
        verify(cb).equal(root.get("isActive"), true);
    }

    @Test
    void fromFilter_activeOnly_appliesEqual() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Brand> root = mockRoot();

        Predicate conjunction = mock(Predicate.class);
        Predicate activePred = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjunction);
        when(cb.equal(root.get("isActive"), false)).thenReturn(activePred);
        when(cb.and(conjunction, activePred)).thenReturn(activePred);

        BrandFilterRequest filter = new BrandFilterRequest();
        filter.setIsActive(false);

        Predicate result = BrandSpecification.fromFilter(filter).toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(root.get("isActive"), false);
    }
}