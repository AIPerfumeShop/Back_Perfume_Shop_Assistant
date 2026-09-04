package com.example.spring_boot_project_api.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> data;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
