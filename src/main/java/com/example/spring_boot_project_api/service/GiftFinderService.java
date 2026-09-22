package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.gift.GiftFinderRequest;
import com.example.spring_boot_project_api.dto.response.gift.GiftFinderResponse;

public interface GiftFinderService {

    GiftFinderResponse recommend(GiftFinderRequest request);
}