package com.example.spring_boot_project_api.dto.response.wishlist;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WishlistResponse {
    private Long id;
    private Long userId;
    private List<WishlistItemResponse> items = new ArrayList<>();
    private Integer totalItems = 0;
}