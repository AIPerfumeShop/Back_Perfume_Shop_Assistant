package com.example.spring_boot_project_api.dto.request.product;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.example.spring_boot_project_api.enums.Gender;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductFilterRequest {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final String DEFAULT_SORT = "id";
    private static final String DEFAULT_DIRECTION = "asc";
    private static final Set<String> SORTABLE_FIELDS = 
    Set.of(
        "id",
        "name",
        "createdAt",
        "updatedAt",
        "price"
    );
    private String search;
    private Long categoryId;
    private String brand;
    private Gender gender;
    private String fragranceFamily;
    private BigDecimal maxPrice;
    private BigDecimal minPrice;
    private Integer minRate;
    private Boolean inStock;
    //null (default) or true -> only active products; false -> only inactive products
    private Boolean isActive;

    private Integer page;
    private Integer size;
    private String sort;
    private String direction;

    public boolean hasSearch(){
        return search != null && !search.trim().isEmpty();
    }
    public boolean isPriceSort(){
        String s = sort == null || sort.trim().isEmpty() ? DEFAULT_SORT : sort.trim();
        return "price".equalsIgnoreCase(s);
    }
    public PageRequest toPageRequest(){
        int pageNum = page == null || page < 0 ? DEFAULT_PAGE : page;
        int pageSize = size == null || size <= 0 ? DEFAULT_SIZE : size;

        String sortField = sort == null || sort.trim().isEmpty() ? DEFAULT_SORT : sort.trim();
        if(!SORTABLE_FIELDS.contains(sortField)){
            sortField = DEFAULT_SORT;
        }
        if("price".equalsIgnoreCase(sortField)){
            return PageRequest.of(pageNum, pageSize);
        }
        Sort.Direction dir = Sort.Direction.ASC;
        if(direction != null){
            try{
                dir = Sort.Direction.fromString(direction);
            }catch (IllegalArgumentException ignored){
                dir = Sort.Direction.ASC;
            }
        }
        return PageRequest.of(pageNum, pageSize, Sort.by(dir, sortField));
    }
}
