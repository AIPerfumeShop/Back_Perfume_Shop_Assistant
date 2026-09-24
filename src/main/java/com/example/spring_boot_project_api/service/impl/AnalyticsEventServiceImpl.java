package com.example.spring_boot_project_api.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsEventRequest;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.model.AnalyticsEvent;
import com.example.spring_boot_project_api.repository.AnalyticsEventRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.service.AnalyticsEventService;
import com.example.spring_boot_project_api.util.SecurityUtils;

@Service
@Transactional
public class AnalyticsEventServiceImpl implements AnalyticsEventService {
    private final AnalyticsEventRepository eventRepository;
    private final ProductRepository productRepository;

    public AnalyticsEventServiceImpl(AnalyticsEventRepository eventRepository,
                                     ProductRepository productRepository) {
        this.eventRepository = eventRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void record(AnalyticsEventRequest request) {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setEventType(request.getEventType());
        event.setVisitorId(request.getVisitorId().trim());
        event.setSessionId(normalize(request.getSessionId()));
        event.setMetadata(normalize(request.getMetadata()));
        if (request.getProductId() != null) {
            event.setProduct(productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found")));
        }
        SecurityUtils.currentUser().ifPresent(event::setUser);
        eventRepository.save(event);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}