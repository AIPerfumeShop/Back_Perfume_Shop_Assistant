package com.example.spring_boot_project_api.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "createdAt";
    public PaginationUtils(){
        //prevent object creation
    }
    public static Pageable createPageable(Integer page, Integer size, String sortby, String direction){
        int pageNumber = (page == null || page < 0) ? DEFAULT_PAGE : page;
        int pageSize = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String sortField = (sortby == null || sortby.isBlank()) ? DEFAULT_SORT : sortby;
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        return PageRequest.of(pageNumber, pageSize,Sort.by(sortDirection,sortField));
    }
}
