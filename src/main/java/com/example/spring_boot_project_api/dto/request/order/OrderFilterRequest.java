package com.example.spring_boot_project_api.dto.request.order;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import com.example.spring_boot_project_api.enums.OrderStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderFilterRequest {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final String DEFAULT_SORT = "createdAt";
    private static final String DEFAULT_DIRECTION = "desc";
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "totalAmount", "status", "createdAt", "updatedAt");

    private OrderStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private String search;

    private Integer page;
    private Integer size;
    private String sort;
    private String direction;

    public boolean hasSearch() {
        return search != null && !search.trim().isEmpty();
    }

    public PageRequest toPageRequest() {
        int pageNum = page == null || page < 0 ? DEFAULT_PAGE : page;
        int pageSize = size == null || size <= 0 ? DEFAULT_SIZE : size;

        String sortField = sort == null || sort.trim().isEmpty()
                ? DEFAULT_SORT
                : sort.trim();
        if (!SORTABLE_FIELDS.contains(sortField)) {
            sortField = DEFAULT_SORT;
        }

        Sort.Direction dir = Sort.Direction.DESC;
        if (direction != null) {
            try {
                dir = Sort.Direction.fromString(direction);
            } catch (IllegalArgumentException ignored) {
                dir = Sort.Direction.DESC;
            }
        }

        return PageRequest.of(pageNum, pageSize, Sort.by(dir, sortField));
    }
}