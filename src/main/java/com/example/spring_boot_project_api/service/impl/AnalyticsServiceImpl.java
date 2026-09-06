package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.analytics.BrandAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.analytics.BrandPerformanceResponse;
import com.example.spring_boot_project_api.dto.response.analytics.CategoryAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.analytics.CategoryPerformanceResponse;
import com.example.spring_boot_project_api.dto.response.analytics.CustomerAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.analytics.CustomerPerformanceResponse;
import com.example.spring_boot_project_api.dto.response.analytics.DailySalesResponse;
import com.example.spring_boot_project_api.dto.response.analytics.DashboardResponse;
import com.example.spring_boot_project_api.dto.response.analytics.ProductAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.analytics.ProductPerformanceResponse;
import com.example.spring_boot_project_api.dto.response.analytics.SalesAnalyticsResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.repository.OrderItemRepository;
import com.example.spring_boot_project_api.repository.OrderItemRepository.BrandPerformanceStat;
import com.example.spring_boot_project_api.repository.OrderItemRepository.CategoryPerformanceStat;
import com.example.spring_boot_project_api.repository.OrderItemRepository.DailySalesStat;
import com.example.spring_boot_project_api.repository.OrderItemRepository.ProductPerformanceStat;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.OrderRepository.CustomerOrderStat;
import com.example.spring_boot_project_api.repository.OrderRepository.OrderStatusStat;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.AnalyticsService;

@Service
@Transactional
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final int TOP_LIMIT = 10;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public AnalyticsServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SalesAnalyticsResponse getSalesAnalytics(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        BigDecimal totalRevenue = orderRepository.sumTotalAmountBetween(start, end);
        long totalOrders = orderRepository.countByCreatedAtBetween(start, end);

        SalesAnalyticsResponse response = new SalesAnalyticsResponse();
        response.setTotalRevenue(totalRevenue);
        response.setTotalOrders(totalOrders);
        response.setAverageOrderValue(totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        response.setOrdersByStatus(orderStatusCounts(start, end));
        response.setDailySales(mapDailySales(orderItemRepository.findDailySales(start, end)));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAnalyticsResponse getProductAnalytics(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        List<ProductPerformanceStat> stats = orderItemRepository.findProductPerformance(
                start, end, PageRequest.of(0, TOP_LIMIT));

        ProductAnalyticsResponse response = new ProductAnalyticsResponse();
        response.setTotalProductsSold(stats.stream()
                .mapToLong(stat -> stat.getQuantitySold() == null ? 0L : stat.getQuantitySold())
                .sum());
        response.setTotalProductRevenue(stats.stream()
                .map(ProductPerformanceStat::getTotalRevenue)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setTopProducts(stats.stream()
                .map(this::toProductPerformanceResponse)
                .toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryAnalyticsResponse getCategoryAnalytics(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        List<CategoryPerformanceStat> stats = orderItemRepository.findCategoryPerformance(start, end);

        CategoryAnalyticsResponse response = new CategoryAnalyticsResponse();
        response.setTotalQuantitySold(stats.stream()
                .mapToLong(stat -> stat.getQuantitySold() == null ? 0L : stat.getQuantitySold())
                .sum());
        response.setTotalRevenue(stats.stream()
                .map(CategoryPerformanceStat::getTotalRevenue)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setCategories(stats.stream()
                .map(this::toCategoryPerformanceResponse)
                .toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BrandAnalyticsResponse getBrandAnalytics(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        List<BrandPerformanceStat> stats = orderItemRepository.findBrandPerformance(start, end);

        BrandAnalyticsResponse response = new BrandAnalyticsResponse();
        response.setTotalQuantitySold(stats.stream()
                .mapToLong(stat -> stat.getQuantitySold() == null ? 0L : stat.getQuantitySold())
                .sum());
        response.setTotalRevenue(stats.stream()
                .map(BrandPerformanceStat::getTotalRevenue)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setBrands(stats.stream()
                .map(this::toBrandPerformanceResponse)
                .toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerAnalyticsResponse getCustomerAnalytics(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        long newCustomers = userRepository.countByRoleAndCreatedAtBetween(
                Role.CUSTOMER, start, end);
        BigDecimal rangeRevenue = orderRepository.sumTotalAmountBetween(start, end);

        CustomerAnalyticsResponse response = new CustomerAnalyticsResponse();
        response.setTotalCustomers(totalCustomers);
        response.setNewCustomers(newCustomers);
        response.setAverageOrderValuePerCustomer(totalCustomers > 0
                ? rangeRevenue.divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        response.setTopCustomers(orderRepository.findTopCustomers(
                start, end, PageRequest.of(0, TOP_LIMIT)).stream()
                .map(this::toCustomerPerformanceResponse)
                .toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(AnalyticsFilterRequest filter) {
        DashboardResponse response = new DashboardResponse();
        response.setSales(getSalesAnalytics(filter));
        response.setProducts(getProductAnalytics(filter));
        response.setCategories(getCategoryAnalytics(filter));
        response.setBrands(getBrandAnalytics(filter));
        response.setCustomers(getCustomerAnalytics(filter));
        return response;
    }

    private Map<OrderStatus, Long> orderStatusCounts(LocalDateTime start, LocalDateTime end) {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, 0L);
        }
        for (OrderStatusStat stat : orderRepository.countOrdersByStatusInRange(start, end)) {
            counts.put(stat.getStatus(), stat.getOrderCount());
        }
        return counts;
    }

    private List<DailySalesResponse> mapDailySales(List<DailySalesStat> stats) {
        return stats.stream()
                .map(stat -> {
                    DailySalesResponse dto = new DailySalesResponse();
                    dto.setDate(stat.getDay() != null ? stat.getDay().toLocalDate() : null);
                    dto.setRevenue(stat.getRevenue());
                    dto.setOrders(stat.getOrders() == null ? 0L : stat.getOrders());
                    return dto;
                })
                .toList();
    }

    private ProductPerformanceResponse toProductPerformanceResponse(ProductPerformanceStat stat) {
        ProductPerformanceResponse dto = new ProductPerformanceResponse();
        dto.setProductName(stat.getProductName());
        dto.setBrand(stat.getBrand());
        dto.setQuantitySold(stat.getQuantitySold());
        dto.setTotalRevenue(stat.getTotalRevenue());
        return dto;
    }

    private CategoryPerformanceResponse toCategoryPerformanceResponse(CategoryPerformanceStat stat) {
        CategoryPerformanceResponse dto = new CategoryPerformanceResponse();
        dto.setCategoryName(stat.getCategoryName());
        dto.setQuantitySold(stat.getQuantitySold());
        dto.setTotalRevenue(stat.getTotalRevenue());
        return dto;
    }

    private BrandPerformanceResponse toBrandPerformanceResponse(BrandPerformanceStat stat) {
        BrandPerformanceResponse dto = new BrandPerformanceResponse();
        dto.setBrandName(stat.getBrandName());
        dto.setQuantitySold(stat.getQuantitySold());
        dto.setTotalRevenue(stat.getTotalRevenue());
        return dto;
    }

    private CustomerPerformanceResponse toCustomerPerformanceResponse(CustomerOrderStat stat) {
        CustomerPerformanceResponse dto = new CustomerPerformanceResponse();
        dto.setCustomerName(stat.getCustomerName());
        dto.setEmail(stat.getEmail());
        dto.setOrderCount(stat.getOrderCount());
        dto.setTotalSpent(stat.getTotalSpent());
        return dto;
    }

    private AnalyticsFilterRequest normalize(AnalyticsFilterRequest filter) {
        return filter == null ? new AnalyticsFilterRequest() : filter;
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' date cannot be after 'to' date");
        }
    }

    private LocalDateTime resolveStart(AnalyticsFilterRequest filter) {
        AnalyticsFilterRequest request = normalize(filter);
        validateRange(request.getFrom(), request.getTo());
        return request.getFrom() != null
                ? request.getFrom().atStartOfDay()
                : LocalDateTime.of(1970, 1, 1, 0, 0);
    }

    private LocalDateTime resolveEnd(AnalyticsFilterRequest filter) {
        AnalyticsFilterRequest request = normalize(filter);
        return request.getTo() != null
                ? request.getTo().plusDays(1).atStartOfDay()
                : LocalDateTime.of(9999, 12, 31, 23, 59, 59);
    }
}