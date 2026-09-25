package com.example.spring_boot_project_api.service.impl;

import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.spring_boot_project_api.dto.request.ai.FragranceProfileRequest;
import com.example.spring_boot_project_api.dto.response.ai.CustomerFragranceProfileResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.model.CustomerFragranceProfile;
import com.example.spring_boot_project_api.repository.CustomerFragranceProfileRepository;
import com.example.spring_boot_project_api.repository.OrderItemRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.CustomerFragranceProfileService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerFragranceProfileServiceImpl implements CustomerFragranceProfileService {
    private final CustomerFragranceProfileRepository profileRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CustomerFragranceProfileResponse getOrGenerate(Long userId) {
        return profileRepository.findByUserId(userId).map(this::response)
                .orElseGet(() -> generateFromHistory(userId));
    }

    @Override
    @Transactional
    public CustomerFragranceProfileResponse update(Long userId, FragranceProfileRequest request) {
        CustomerFragranceProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CustomerFragranceProfile created = new CustomerFragranceProfile();
                    created.setUser(userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found")));
                    return created;
                });
        if (request.getSweetness() != null) profile.setSweetness(request.getSweetness());
        if (request.getFloral() != null) profile.setFloral(request.getFloral());
        if (request.getFresh() != null) profile.setFresh(request.getFresh());
        if (request.getWoody() != null) profile.setWoody(request.getWoody());
        if (request.getIntensity() != null) profile.setIntensity(request.getIntensity().toUpperCase(Locale.ROOT));
        if (request.getPersonality() != null && !request.getPersonality().isBlank())
            profile.setPersonality(request.getPersonality().trim());
        profile = profileRepository.save(profile);
        CustomerFragranceProfileResponse result = response(profile);
        return CustomerFragranceProfileResponse.builder().userId(result.getUserId())
                .personality(result.getPersonality()).sweetness(result.getSweetness())
                .floral(result.getFloral()).fresh(result.getFresh()).woody(result.getWoody())
                .intensity(result.getIntensity()).generatedFromPurchaseHistory(false).build();
    }

    private CustomerFragranceProfileResponse generateFromHistory(Long userId) {
        var preferences = orderItemRepository.findUserFragrancePreferences(userId, OrderStatus.CANCELLED);
        int sweetness = 50, floral = 50, fresh = 50, woody = 50;
        String personality = "Balanced Explorer";
        if (!preferences.isEmpty()) {
            String family = preferences.get(0).getFragranceFamily().toLowerCase(Locale.ROOT);
            if (family.contains("sweet") || family.contains("gourmand") || family.contains("vanilla")) {
                sweetness = 85; floral = 65; fresh = 45; woody = 30; personality = "Sweet Floral Romantic";
            } else if (family.contains("floral")) {
                sweetness = 65; floral = 85; fresh = 60; woody = 30; personality = "Floral Romantic";
            } else if (family.contains("fresh") || family.contains("citrus")) {
                sweetness = 35; floral = 45; fresh = 90; woody = 40; personality = "Fresh and Energetic";
            } else if (family.contains("wood") || family.contains("amber")) {
                sweetness = 40; floral = 30; fresh = 35; woody = 85; personality = "Warm and Grounded";
            }
        }
        return CustomerFragranceProfileResponse.builder().userId(userId).sweetness(sweetness)
                .floral(floral).fresh(fresh).woody(woody).intensity("MEDIUM")
                .personality(personality).generatedFromPurchaseHistory(!preferences.isEmpty()).build();
    }

    private CustomerFragranceProfileResponse response(CustomerFragranceProfile profile) {
        return CustomerFragranceProfileResponse.builder().userId(profile.getUser().getId())
                .sweetness(profile.getSweetness()).floral(profile.getFloral()).fresh(profile.getFresh())
                .woody(profile.getWoody()).intensity(profile.getIntensity())
                .personality(profile.getPersonality()).generatedFromPurchaseHistory(false).build();
    }
}
