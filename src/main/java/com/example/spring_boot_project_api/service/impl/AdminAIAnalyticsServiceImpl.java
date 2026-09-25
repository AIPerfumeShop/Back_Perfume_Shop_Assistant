package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.ai.AdminAnomalyDetectionResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminBusinessBriefingResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminCustomerIntelligenceResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminCustomerPainPointsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminConversionIntelligenceResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminInventoryForecastResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminInventoryForecastResponse.ForecastItem;
import com.example.spring_boot_project_api.dto.response.ai.AdminSpendingIntelligenceResponse;
import com.example.spring_boot_project_api.enums.AnalyticsEventType;
import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.enums.TicketStatus;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.model.AIMessage;
import com.example.spring_boot_project_api.repository.AIMessageRepository;
import com.example.spring_boot_project_api.repository.AnalyticsEventRepository;
import com.example.spring_boot_project_api.repository.ExpenseRepository;
import com.example.spring_boot_project_api.repository.OrderItemRepository;
import com.example.spring_boot_project_api.repository.OrderItemRepository.CategoryPerformanceStat;
import com.example.spring_boot_project_api.repository.OrderItemRepository.VariantSalesStat;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.ReviewRepository;
import com.example.spring_boot_project_api.repository.SupportTicketRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.AdminAIAnalyticsService;
import com.example.spring_boot_project_api.service.OpenRouterService;

@Service
@Transactional(readOnly = true)
public class AdminAIAnalyticsServiceImpl implements AdminAIAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AdminAIAnalyticsServiceImpl.class);

    private static final int DEFAULT_WINDOW_DAYS = 30;
    private static final int LOW_STOCK_COVER_DAYS = 14;
    private static final int SLOW_MOVING_MIN_STOCK = 5;
    private static final long SLOW_MOVING_MAX_UNITS = 0L;
    private static final int TOP_N = 5;
    private static final int LOW_RATED_MAX_RATING = 3;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ExpenseRepository expenseRepository;
    private final ReviewRepository reviewRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AIMessageRepository aiMessageRepository;
        private final AnalyticsEventRepository analyticsEventRepository;
    private final UserRepository userRepository;
    private final OpenRouterService openRouterService;

    private final boolean insightEnabled;

    public AdminAIAnalyticsServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ExpenseRepository expenseRepository,
            ReviewRepository reviewRepository,
            SupportTicketRepository supportTicketRepository,
            AIMessageRepository aiMessageRepository,
            AnalyticsEventRepository analyticsEventRepository,
            UserRepository userRepository,
            OpenRouterService openRouterService,
            @Value("${admin-ai-analytics.insight-enabled:true}") boolean insightEnabled) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.expenseRepository = expenseRepository;
        this.reviewRepository = reviewRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.aiMessageRepository = aiMessageRepository;
        this.analyticsEventRepository = analyticsEventRepository;
        this.userRepository = userRepository;
        this.openRouterService = openRouterService;
        this.insightEnabled = insightEnabled;
    }

    // =========================================================
    // 1. SPENDING & PROFIT INTELLIGENCE
    // =========================================================

    @Override
    public AdminSpendingIntelligenceResponse getSpendingIntelligence(AnalyticsFilterRequest filter) {
        Window window = resolveWindow(filter);

        BigDecimal revenue = orderRepository.sumTotalAmountBetween(
                window.start(), window.end(), OrderStatus.CANCELLED);
        long orders = orderRepository.countByCreatedAtBetween(
                window.start(), window.end(), OrderStatus.CANCELLED);
        BigDecimal expenses = expenseRepository.sumAmountBetween(window.from(), window.to());
        Map<String, BigDecimal> byCategory = expenseByCategory(window.from(), window.to());

        BigDecimal profit = safe(revenue).subtract(safe(expenses));
        BigDecimal margin = safe(revenue).compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profit.multiply(BigDecimal.valueOf(100))
                        .divide(safe(revenue), 2, RoundingMode.HALF_UP);
        BigDecimal aov = orders > 0
                ? safe(revenue).divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        String topCategory = byCategory.isEmpty() ? null : byCategory.keySet().iterator().next();

        AdminSpendingIntelligenceResponse response = new AdminSpendingIntelligenceResponse();
        response.setFrom(window.from());
        response.setTo(window.to());
        response.setTotalRevenue(safe(revenue));
        response.setTotalExpenses(safe(expenses));
        response.setTotalProfit(profit);
        response.setProfitMargin(margin);
        response.setTotalOrders(orders);
        response.setAverageOrderValue(aov);
        response.setExpensesByCategory(byCategory);
        response.setTopExpenseCategory(topCategory);

        StringBuilder data = new StringBuilder();
        data.append("Period: ").append(window.from()).append(" to ").append(window.to()).append('\n');
        data.append("Revenue: $").append(money(revenue)).append('\n');
        data.append("Expenses: $").append(money(expenses)).append('\n');
        data.append("Profit: $").append(money(profit))
                .append(" (margin ").append(margin).append("%)\n");
        data.append("Orders: ").append(orders)
                .append(", average order value: $").append(aov).append('\n');
        data.append("Expense breakdown: ").append(describeExpenses(byCategory)).append('\n');
        if (topCategory != null) {
            data.append("Largest expense category: ").append(topCategory)
                    .append(" ($").append(byCategory.get(topCategory)).append(")\n");
        }

        Insight insight = generate(
                "You are a business finance analyst for an online perfume shop. "
                        + "Summarize this spending/profit picture in 3-5 short bullet points. "
                        + "Highlight profit health, biggest expense, and one action to consider. "
                        + "Never invent numbers. Use only the data below.\n\n" + data,
                fallbackSpending(profit, margin, topCategory));
        response.setAiGenerated(insight.aiGenerated());
        response.setInsight(insight.text());
        response.setInsights(insight.bullets());
        return response;
    }

    private List<String> fallbackSpending(BigDecimal profit, BigDecimal margin, String topCategory) {
        List<String> bullets = new ArrayList<>();
        bullets.add(profit.signum() >= 0
                ? "Profitable in this window with a " + margin + "% margin."
                : "Expenses exceeded revenue in this window (loss margin " + margin + "%).");
        if (topCategory != null) {
            bullets.add("Biggest expense category: " + topCategory + ".");
        } else {
            bullets.add("No expenses were recorded in this window.");
        }
        bullets.add("Review recurring expenses to protect the margin.");
        return bullets;
    }

    // =========================================================
    // 2. INVENTORY FORECAST
    // =========================================================

    @Override
    public AdminInventoryForecastResponse getInventoryForecast(AnalyticsFilterRequest filter) {
        Window window = resolveWindow(filter);
        long daysInWindow = Math.max(1, ChronoUnit.DAYS.between(window.from(), window.to()) + 1);

        List<VariantSalesStat> sales = orderItemRepository.findVariantSales(
                window.start(), window.end(), OrderStatus.CANCELLED);

        List<ForecastItem> atRisk = new ArrayList<>();
        List<ForecastItem> slowMoving = new ArrayList<>();
        for (VariantSalesStat stat : sales) {
            ForecastItem item = toForecastItem(stat, daysInWindow);
            if (isOutOfStockOrRunningOut(item)) {
                atRisk.add(item);
            } else if (isSlowMoving(item)) {
                slowMoving.add(item);
            }
        }
        atRisk.sort(Comparator.comparingInt(ForecastItem::getDaysOfCover)
                .thenComparing(ForecastItem::getStock, Comparator.nullsLast(Comparator.naturalOrder())));
        slowMoving.sort(Comparator.comparingInt(ForecastItem::getStock).reversed());

        AdminInventoryForecastResponse response = new AdminInventoryForecastResponse();
        response.setFrom(window.from());
        response.setTo(window.to());
        response.setDaysInWindow(daysInWindow);
        response.setAtRiskProducts(atRisk.subList(0, Math.min(TOP_N, atRisk.size())));
        response.setSlowMovingProducts(slowMoving.subList(0, Math.min(TOP_N, slowMoving.size())));

        StringBuilder data = new StringBuilder();
        data.append("Window: ").append(daysInWindow).append(" days (")
                .append(window.from()).append(" to ").append(window.to()).append(")\n");
        data.append("At risk of running out:\n");
        for (ForecastItem item : response.getAtRiskProducts()) {
            data.append("- ").append(item.getProductName())
                    .append(" (").append(nullToUnknown(item.getBrand()))
                    .append(", ").append(item.getSizeMl()).append("ml): stock ")
                    .append(item.getStock()).append(", sold ")
                    .append(item.getUnitsSoldInWindow()).append(", cover ")
                    .append(item.getDaysOfCover()).append(" days\n");
        }
        data.append("Slow moving:\n");
        for (ForecastItem item : response.getSlowMovingProducts()) {
            data.append("- ").append(item.getProductName())
                    .append(": stock ").append(item.getStock())
                    .append(", sold ").append(item.getUnitsSoldInWindow()).append('\n');
        }

        Insight insight = generate(
                "You are an inventory planner for an online perfume shop. "
                        + "In 3-5 short bullet points, explain which products need restocking and "
                        + "which are slow movers, and suggest one action each. "
                        + "Use only the data below; do not invent products.\n\n" + data,
                fallbackInventory(response.getAtRiskProducts(), response.getSlowMovingProducts()));
        response.setAiGenerated(insight.aiGenerated());
        response.setInsight(insight.text());
        response.setInsights(insight.bullets());
        return response;
    }

    private ForecastItem toForecastItem(VariantSalesStat stat, long daysInWindow) {
        ForecastItem item = new ForecastItem();
        item.setVariantId(stat.getVariantId());
        item.setProductId(stat.getProductId());
        item.setProductName(stat.getProductName());
        item.setBrand(stat.getBrand());
        item.setSizeMl(stat.getSizeMl());
        int stock = stat.getStock() == null ? 0 : stat.getStock();
        long units = stat.getUnitsSold() == null ? 0L : stat.getUnitsSold();
        item.setStock(stock);
        item.setUnitsSoldInWindow(units);
        double dailyDemand = daysInWindow > 0 ? (double) units / daysInWindow : 0;
        item.setDailyDemand(round2(dailyDemand));
        if (dailyDemand > 0) {
            item.setDaysOfCover((int) Math.floor(stock / dailyDemand));
            item.setRecommendation("Replenish: about " + item.getDaysOfCover()
                    + " days of stock left at current demand.");
        } else {
            item.setDaysOfCover(Integer.MAX_VALUE);
            item.setRecommendation(stock == 0
                    ? "Out of stock with no recent sales history."
                    : "No sales in this window; consider promotion or review placement.");
        }
        return item;
    }

    private boolean isOutOfStockOrRunningOut(ForecastItem item) {
        int stock = item.getStock() == null ? 0 : item.getStock();
        if (stock == 0) {
            return true;
        }
        return item.getDailyDemand() > 0 && item.getDaysOfCover() <= LOW_STOCK_COVER_DAYS;
    }

    private boolean isSlowMoving(ForecastItem item) {
        int stock = item.getStock() == null ? 0 : item.getStock();
        return stock >= SLOW_MOVING_MIN_STOCK
                && item.getUnitsSoldInWindow() <= SLOW_MOVING_MAX_UNITS;
    }

    private List<String> fallbackInventory(List<ForecastItem> atRisk, List<ForecastItem> slowMoving) {
        List<String> bullets = new ArrayList<>();
        bullets.add(atRisk.size() + " variant(s) need restocking soon.");
        bullets.add(slowMoving.size() + " variant(s) are slow moving with no sales in the window.");
        if (!atRisk.isEmpty()) {
            bullets.add("Prioritize restocking: " + atRisk.get(0).getProductName() + ".");
        }
        if (!slowMoving.isEmpty()) {
            bullets.add("Consider a promotion for " + slowMoving.get(0).getProductName() + ".");
        }
        return bullets;
    }

    // =========================================================
    // 3. ANOMALY DETECTION
    // =========================================================

    @Override
    public AdminAnomalyDetectionResponse getAnomalyDetection(AnalyticsFilterRequest filter) {
        Window window = resolveWindow(filter);
        long length = Math.max(1, ChronoUnit.DAYS.between(window.from(), window.to()) + 1);
        Window previous = new Window(
                window.from().minusDays(length),
                window.from().minusDays(1));

        List<AdminAnomalyDetectionResponse.AnomalyItem> anomalies = new ArrayList<>();
        compare(anomalies, "Revenue",
                orderRepository.sumTotalAmountBetween(
                        window.start(), window.end(), OrderStatus.CANCELLED),
                orderRepository.sumTotalAmountBetween(
                        previous.start(), previous.end(), OrderStatus.CANCELLED));
        compare(anomalies, "Orders",
                BigDecimal.valueOf(orderRepository.countByCreatedAtBetween(
                        window.start(), window.end(), OrderStatus.CANCELLED)),
                BigDecimal.valueOf(orderRepository.countByCreatedAtBetween(
                        previous.start(), previous.end(), OrderStatus.CANCELLED)));
        compare(anomalies, "Units sold",
                BigDecimal.valueOf(safeLong(orderItemRepository.sumQuantityBetween(
                        window.start(), window.end(), OrderStatus.CANCELLED))),
                BigDecimal.valueOf(safeLong(orderItemRepository.sumQuantityBetween(
                        previous.start(), previous.end(), OrderStatus.CANCELLED))));
        compare(anomalies, "Expenses",
                expenseRepository.sumAmountBetween(window.from(), window.to()),
                expenseRepository.sumAmountBetween(previous.from(), previous.to()));

        AdminAnomalyDetectionResponse response = new AdminAnomalyDetectionResponse();
        response.setFrom(window.from());
        response.setTo(window.to());
        response.setCompareFrom(previous.from());
        response.setCompareTo(previous.to());
        response.setAnomalies(anomalies);

        StringBuilder data = new StringBuilder();
        data.append("Current ").append(window.from()).append(" to ").append(window.to())
                .append(" vs previous ").append(previous.from()).append(" to ")
                .append(previous.to()).append('\n');
        for (AdminAnomalyDetectionResponse.AnomalyItem item : anomalies) {
            data.append("- ").append(item.getMetric()).append(": ")
                    .append(item.getDirection()).append(" ")
                    .append(Math.abs(item.getChangePercent())).append("% (")
                    .append(item.getSeverity()).append(" severity)\n");
        }

        Insight insight = generate(
                "You are a business analyst for an online perfume shop. "
                        + "In 3-5 short bullet points, explain what these period-over-period "
                        + "changes mean and flag anything that needs attention. "
                        + "Use only the data below; do not invent numbers.\n\n" + data,
                fallbackAnomalies(anomalies));
        response.setAiGenerated(insight.aiGenerated());
        response.setInsight(insight.text());
        response.setInsights(insight.bullets());
        return response;
    }

    private void compare(List<AdminAnomalyDetectionResponse.AnomalyItem> anomalies,
                         String metric, BigDecimal current, BigDecimal previous) {
        BigDecimal cur = safe(current);
        BigDecimal prev = safe(previous);
        double change = prev.compareTo(BigDecimal.ZERO) == 0
                ? (cur.compareTo(BigDecimal.ZERO) == 0 ? 0 : 100.0)
                : cur.subtract(prev)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(prev.abs(), 2, RoundingMode.HALF_UP)
                        .doubleValue();
        String direction = change > 0 ? "up" : (change < 0 ? "down" : "flat");
        String severity = severity(Math.abs(change));
        if ("flat".equals(direction)) {
            return;
        }

        AdminAnomalyDetectionResponse.AnomalyItem item =
                new AdminAnomalyDetectionResponse.AnomalyItem();
        item.setMetric(metric);
        item.setCurrentValue(cur);
        item.setPreviousValue(prev);
        item.setChangePercent(change);
        item.setDirection(direction);
        item.setSeverity(severity);
        item.setNote(metric + " " + direction + " " + Math.abs(change)
                + "% vs previous period (" + severity + " severity).");
        anomalies.add(item);
    }

    private String severity(double absChange) {
        if (absChange >= 50) {
            return "high";
        }
        if (absChange >= 20) {
            return "medium";
        }
        return "low";
    }

    private List<String> fallbackAnomalies(List<AdminAnomalyDetectionResponse.AnomalyItem> anomalies) {
        List<String> bullets = new ArrayList<>();
        if (anomalies.isEmpty()) {
            bullets.add("No notable changes versus the previous period.");
            return bullets;
        }
        for (AdminAnomalyDetectionResponse.AnomalyItem item : anomalies) {
            bullets.add(item.getMetric() + " " + item.getDirection() + " "
                    + Math.abs(item.getChangePercent()) + "% (" + item.getSeverity() + ").");
        }
        return bullets;
    }

    // =========================================================
    // 4. CUSTOMER PAIN POINTS
    // =========================================================

    @Override
    public AdminCustomerPainPointsResponse getCustomerPainPoints(AnalyticsFilterRequest filter) {
        Window window = resolveWindow(filter);

        ReviewRepository.ReviewRatingStat ratingStat =
                reviewRepository.findRatingSummaryBetween(window.start(), window.end());
        long totalReviews = ratingStat.getReviewCount() == null ? 0L : ratingStat.getReviewCount();
        Double avgRating = ratingStat.getAvgRating();

        List<ReviewRepository.LowRatedReviewStat> lowRated =
                reviewRepository.findLowRatedReviews(LOW_RATED_MAX_RATING, window.start(),
                        window.end(), PageRequest.of(0, TOP_N));

        long openTickets = supportTicketRepository.countByStatus(TicketStatus.OPEN);
        long resolvedTickets = supportTicketRepository.countByStatus(TicketStatus.RESOLVED);
        List<String> topReasons = topTicketReasons(window.start(), window.end());
        List<String> popularQuestions = aiMessageRepository
                .findPopularQuestions(window.start(), window.end(), MessageSender.USER,
                        PageRequest.of(0, TOP_N))
                .stream()
                .map(stat -> stat.getQuestion() + " (x" + stat.getTimesAsked() + ")")
                .toList();

        AdminCustomerPainPointsResponse response = new AdminCustomerPainPointsResponse();
        response.setFrom(window.from());
        response.setTo(window.to());
        response.setTotalReviews(totalReviews);
        response.setAverageRating(avgRating == null ? null : round2(avgRating));

        List<AdminCustomerPainPointsResponse.LowRatedReviewItem> lowRatedItems = new ArrayList<>();
        for (ReviewRepository.LowRatedReviewStat stat : lowRated) {
            AdminCustomerPainPointsResponse.LowRatedReviewItem item =
                    new AdminCustomerPainPointsResponse.LowRatedReviewItem();
            item.setReviewId(stat.getReviewId());
            item.setRating(stat.getRating());
            item.setComment(stat.getComment());
            item.setProductName(stat.getProductName());
            item.setUserName(stat.getUserName());
            lowRatedItems.add(item);
        }
        response.setLowRatedReviews(lowRatedItems);
        response.setOpenTickets(openTickets);
        response.setResolvedTickets(resolvedTickets);
        response.setTopTicketReasons(topReasons);
        response.setPopularQuestions(popularQuestions);

        StringBuilder data = new StringBuilder();
        data.append("Period: ").append(window.from()).append(" to ").append(window.to()).append('\n');
        data.append("Reviews: ").append(totalReviews)
                .append(", average rating: ").append(avgRating == null ? "n/a" : round2(avgRating)).append('\n');
        data.append("Open tickets: ").append(openTickets)
                .append(", resolved tickets: ").append(resolvedTickets).append('\n');
        if (!topReasons.isEmpty()) {
            data.append("Top ticket reasons: ").append(String.join("; ", topReasons)).append('\n');
        }
        if (!popularQuestions.isEmpty()) {
            data.append("Popular AI questions: ").append(String.join("; ", popularQuestions)).append('\n');
        }
        for (ReviewRepository.LowRatedReviewStat stat : lowRated) {
            data.append("- Low review (" ).append(stat.getRating()).append(" stars) on ")
                    .append(stat.getProductName()).append(": \"")
                    .append(trim(stat.getComment(), 120)).append("\"\n");
        }

        Insight insight = generate(
                "You are a customer experience analyst for an online perfume shop. "
                        + "In 3-5 short bullet points, explain the main customer pain points "
                        + "and the top 1-2 things the shop should fix first. "
                        + "Use only the data below; do not invent issues.\n\n" + data,
                fallbackPainPoints(totalReviews, lowRated.size(), topReasons));
        response.setAiGenerated(insight.aiGenerated());
        response.setInsight(insight.text());
        response.setInsights(insight.bullets());
        return response;
    }

    private List<String> topTicketReasons(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (var ticket : supportTicketRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(start, end)) {
            String reason = ticket.getReason();
            if (reason == null || reason.isBlank()) {
                continue;
            }
            String key = reason.trim();
            counts.put(key, counts.getOrDefault(key, 0L) + 1L);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_N)
                .map(e -> e.getKey() + " (x" + e.getValue() + ")")
                .toList();
    }

    private List<String> fallbackPainPoints(long totalReviews, int lowRatedCount, List<String> topReasons) {
        List<String> bullets = new ArrayList<>();
        bullets.add(totalReviews + " review(s) in the window, " + lowRatedCount + " low-rated.");
        if (!topReasons.isEmpty()) {
            bullets.add("Top ticket reason: " + topReasons.get(0) + ".");
        } else {
            bullets.add("No concentrated ticket reasons this window.");
        }
        bullets.add("Respond to low-rated reviews to recover trust.");
        return bullets;
    }

    // =========================================================
    // 5. BUSINESS BRIEFING
    // =========================================================

    @Override
    public AdminBusinessBriefingResponse getBusinessBriefing(AnalyticsFilterRequest filter) {
        Window window = resolveWindow(filter);

        BigDecimal revenue = orderRepository.sumTotalAmountBetween(
                window.start(), window.end(), OrderStatus.CANCELLED);
        long orders = orderRepository.countByCreatedAtBetween(
                window.start(), window.end(), OrderStatus.CANCELLED);
        BigDecimal expenses = expenseRepository.sumAmountBetween(window.from(), window.to());
        BigDecimal profit = safe(revenue).subtract(safe(expenses));
        long newCustomers = userRepository.countByRoleAndCreatedAtBetween(
                Role.CUSTOMER, window.start(), window.end());
        long productsSold = safeLong(orderItemRepository.sumQuantityBetween(
                window.start(), window.end(), OrderStatus.CANCELLED));

        Map<String, BigDecimal> categoryRevenue = new LinkedHashMap<>();
        List<CategoryPerformanceStat> categoryStats = orderItemRepository.findCategoryPerformance(
                window.start(), window.end(), OrderStatus.CANCELLED);
        for (CategoryPerformanceStat stat : categoryStats) {
            if (categoryRevenue.size() >= TOP_N) {
                break;
            }
            categoryRevenue.put(stat.getCategoryName(), safe(stat.getTotalRevenue()));
        }

        AdminBusinessBriefingResponse response = new AdminBusinessBriefingResponse();
        response.setFrom(window.from());
        response.setTo(window.to());
        response.setTotalRevenue(safe(revenue));
        response.setTotalExpenses(safe(expenses));
        response.setTotalProfit(profit);
        response.setTotalOrders(orders);
        response.setNewCustomers(newCustomers);
        response.setProductsSold(productsSold);
        response.setCategoryRevenue(categoryRevenue);

        StringBuilder data = new StringBuilder();
        data.append("Briefing period: ").append(window.from()).append(" to ")
                .append(window.to()).append('\n');
        data.append("Revenue: $").append(money(revenue)).append('\n');
        data.append("Expenses: $").append(money(expenses)).append('\n');
        data.append("Profit: $").append(money(profit)).append('\n');
        data.append("Orders: ").append(orders)
                .append(", products sold: ").append(productsSold)
                .append(", new customers: ").append(newCustomers).append('\n');
        data.append("Category revenue: ").append(formatCategoryRevenue(categoryRevenue)).append('\n');

        Insight insight = generate(
                "You are the business assistant for an online perfume shop. "
                        + "Write a concise admin briefing in 4-6 short bullet points covering "
                        + "revenue/profit health, growth (orders & new customers), top category, "
                        + "and one strategic recommendation. Use only the data below.\n\n" + data,
                fallbackBriefing(profit, orders, newCustomers, categoryRevenue));
        response.setAiGenerated(insight.aiGenerated());
        response.setInsight(insight.text());
        response.setInsights(insight.bullets());
        return response;
    }

    private List<String> fallbackBriefing(BigDecimal profit, long orders, long newCustomers,
                                          Map<String, BigDecimal> categoryRevenue) {
        List<String> bullets = new ArrayList<>();
        bullets.add("Profit: $" + money(profit) + " across " + orders + " orders.");
        bullets.add(newCustomers + " new customer(s) acquired.");
        if (!categoryRevenue.isEmpty()) {
            String top = categoryRevenue.keySet().iterator().next();
            bullets.add("Top category: " + top + " ($" + categoryRevenue.get(top) + ").");
        }
        bullets.add(profit.signum() >= 0
                ? "Double down on what is working this period."
                : "Costs are high relative to revenue; review expenses.");
        return bullets;
    }

    // =========================================================
    // 5. CUSTOMER INTELLIGENCE
    // =========================================================

    @Override
    public AdminCustomerIntelligenceResponse getCustomerIntelligence(AnalyticsFilterRequest filter) {
        Window window = resolveWindow(filter);
        long length = Math.max(1, ChronoUnit.DAYS.between(window.from(), window.to()) + 1);
        Window previous = new Window(
                window.from().minusDays(length),
                window.from().minusDays(1));

        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        long newCustomers = userRepository.countByRoleAndCreatedAtBetween(
                Role.CUSTOMER, window.start(), window.end());
        List<Long> activeIds = orderRepository.findDistinctCustomerIdsBetween(
                window.start(), window.end(), OrderStatus.CANCELLED);
        List<Long> returningIds = orderRepository.findDistinctCustomerIdsBetween(
                previous.start(), previous.end(), OrderStatus.CANCELLED);
        List<OrderRepository.CustomerOrderCountStat> aggregates =
                orderRepository.findCustomerOrderCountsBetween(
                        window.start(), window.end(), OrderStatus.CANCELLED);

        long activeCustomers = activeIds.size();
        long repeatCustomers = aggregates.stream()
                .filter(stat -> safeLong(stat.getOrderCount()) >= 2)
                .count();
        long purchaseIntervals = aggregates.stream()
                .mapToLong(stat -> Math.max(0, safeLong(stat.getOrderCount()) - 1))
                .sum();

        Double repeatPurchaseRate = activeCustomers == 0
                ? 0.0
                : round2(repeatCustomers * 100.0 / activeCustomers);
        Double purchaseFrequency = activeCustomers == 0
                ? 0.0
                : round2(((double) purchaseIntervals + activeCustomers) / activeCustomers);
        BigDecimal revenue = orderRepository.sumTotalAmountBetween(
                window.start(), window.end(), OrderStatus.CANCELLED);
        BigDecimal averageRevenuePerCustomer = activeCustomers == 0
                ? BigDecimal.ZERO
                : safe(revenue).divide(BigDecimal.valueOf(activeCustomers),
                        2, RoundingMode.HALF_UP);
        BigDecimal lifetimeSpend = orderRepository.sumTotalAmount(OrderStatus.CANCELLED);
        BigDecimal customerLifetimeValue = totalCustomers == 0
                ? BigDecimal.ZERO
                : safe(lifetimeSpend).divide(BigDecimal.valueOf(totalCustomers),
                        2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> topBrands = new LinkedHashMap<>();
        for (OrderItemRepository.BrandPerformanceStat stat
                : orderItemRepository.findBrandPerformance(
                        window.start(), window.end(), OrderStatus.CANCELLED)) {
            if (topBrands.size() >= TOP_N) {
                break;
            }
            topBrands.put(stat.getBrandName(), safe(stat.getTotalRevenue()));
        }

        List<AdminCustomerIntelligenceResponse.FragranceFamilyItem> families = new ArrayList<>();
        for (OrderItemRepository.FragranceFamilyPerformanceStat stat
                : orderItemRepository.findFragranceFamilyPerformance(
                        window.start(), window.end(), OrderStatus.CANCELLED)) {
            AdminCustomerIntelligenceResponse.FragranceFamilyItem item =
                    new AdminCustomerIntelligenceResponse.FragranceFamilyItem();
            item.setFragranceFamily(stat.getFragranceFamily());
            item.setUnitsSold(safeLong(stat.getQuantitySold()));
            item.setTotalRevenue(safe(stat.getTotalRevenue()));
            families.add(item);
        }

        List<AdminCustomerIntelligenceResponse.SegmentItem> segments = new ArrayList<>();
        segments.add(segment("New", newCustomers));
        segments.add(segment("Active", activeCustomers));
        segments.add(segment("Repeat", repeatCustomers));
        segments.add(segment("Returning from previous period", returningIds.stream()
                .filter(activeIds::contains)
                .count()));
        segments.add(segment("At risk (active before, no order now)", returningIds.stream()
                .filter(id -> !activeIds.contains(id))
                .count()));

        AdminCustomerIntelligenceResponse response = new AdminCustomerIntelligenceResponse();
        response.setFrom(window.from());
        response.setTo(window.to());
        response.setTotalCustomers(totalCustomers);
        response.setActiveCustomers(activeCustomers);
        response.setNewCustomers(newCustomers);
        response.setReturningCustomers(returningIds.stream().filter(activeIds::contains).count());
        response.setRepeatCustomers(repeatCustomers);
        response.setRepeatPurchaseRate(repeatPurchaseRate);
        response.setPurchaseFrequency(purchaseFrequency);
        response.setAverageRevenuePerCustomer(averageRevenuePerCustomer);
        response.setCustomerLifetimeValue(customerLifetimeValue);
        response.setTopBrands(topBrands);
        response.setTopFragranceFamilies(families);
        response.setSegments(segments);

        StringBuilder data = new StringBuilder();
        data.append("Window: ").append(window.from()).append(" to ").append(window.to()).append('\n');
        data.append("Registered customers: ").append(totalCustomers).append('\n');
        data.append("New customers: ").append(newCustomers).append('\n');
        data.append("Active (ordered in window): ").append(activeCustomers)
                .append(", repeat buyers: ").append(repeatCustomers)
                .append(" (").append(repeatPurchaseRate).append("%)\n");
        data.append("Avg revenue per active customer: $")
                .append(averageRevenuePerCustomer).append('\n');
        data.append("Avg lifetime revenue per registered customer: $")
                .append(customerLifetimeValue).append('\n');
        data.append("Popular brands: ").append(formatMap(topBrands)).append('\n');
        data.append("Popular fragrance families: ");
        if (families.isEmpty()) {
            data.append("none");
        } else {
            List<String> familyParts = new ArrayList<>();
            for (AdminCustomerIntelligenceResponse.FragranceFamilyItem item : families) {
                familyParts.add(item.getFragranceFamily() + " ("
                        + item.getUnitsSold() + " pcs, $" + item.getTotalRevenue() + ")");
            }
            data.append(String.join(", ", familyParts));
        }
        data.append('\n');

        Insight insight = generate(
                "You are a customer insights analyst for an online perfume shop. "
                        + "In 3-5 short bullet points, summarize customer health: growth, "
                        + "retention/repeat behavior, favorite brands and fragrance families, "
                        + "and one actionable recommendation. Use only the data below.\n\n" + data,
                fallbackCustomerInsight(newCustomers, activeCustomers, repeatCustomers,
                        repeatPurchaseRate, topBrands, families));
        response.setAiGenerated(insight.aiGenerated());
        response.setInsight(insight.text());
        response.setInsights(insight.bullets());
        return response;
    }

    @Override
    public AdminConversionIntelligenceResponse getConversionIntelligence(AnalyticsFilterRequest filter) {
        Window window = resolveWindow(filter);
        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (OrderRepository.OrderStatusStat stat : orderRepository.countOrdersByStatusInRange(
                window.start(), window.end())) {
            ordersByStatus.put(stat.getStatus().name(), safeLong(stat.getOrderCount()));
        }

        long totalOrders = ordersByStatus.values().stream().mapToLong(Long::longValue).sum();
        long cancelledOrders = ordersByStatus.getOrDefault(OrderStatus.CANCELLED.name(), 0L);
        long pendingOrders = ordersByStatus.getOrDefault(OrderStatus.PENDING.name(), 0L);
        long deliveredOrders = ordersByStatus.getOrDefault(OrderStatus.DELIVERED.name(), 0L);
        long paidOrLaterOrders = totalOrders - pendingOrders - cancelledOrders;
        Map<String, Long> eventCounts = new LinkedHashMap<>();
        for (AnalyticsEventRepository.EventCountStat stat : analyticsEventRepository
                .countEventsBetween(window.start(), window.end())) {
            eventCounts.put(stat.getEventType().name(), safeLong(stat.getEventCount()));
        }
        long trackedVisitors = analyticsEventRepository.countDistinctVisitorsBetween(
                AnalyticsEventType.PAGE_VIEW, window.start(), window.end());
        long productViews = eventCounts.getOrDefault(AnalyticsEventType.PRODUCT_VIEW.name(), 0L);
        long addToCartEvents = eventCounts.getOrDefault(AnalyticsEventType.ADD_TO_CART.name(), 0L);
        long checkoutStartedEvents = eventCounts.getOrDefault(
                AnalyticsEventType.CHECKOUT_STARTED.name(), 0L);
        long paymentCompletedEvents = eventCounts.getOrDefault(
                AnalyticsEventType.PAYMENT_COMPLETED.name(), 0L);
        long completedEvents = eventCounts.getOrDefault(AnalyticsEventType.ORDER_COMPLETED.name(), 0L);

        AdminConversionIntelligenceResponse response = new AdminConversionIntelligenceResponse();
        response.setFrom(window.from());
        response.setTo(window.to());
        response.setTotalOrders(totalOrders);
        response.setPaidOrLaterOrders(paidOrLaterOrders);
        response.setDeliveredOrders(deliveredOrders);
        response.setCancelledOrders(cancelledOrders);
        response.setPendingOrders(pendingOrders);
        response.setPaymentProgressRate(rate(paidOrLaterOrders, totalOrders));
        response.setDeliveryCompletionRate(rate(deliveredOrders, totalOrders));
        response.setCancellationRate(rate(cancelledOrders, totalOrders));
        response.setTrackedVisitors(trackedVisitors);
        response.setProductViews(productViews);
        response.setAddToCartEvents(addToCartEvents);
        response.setCheckoutStartedEvents(checkoutStartedEvents);
        response.setPaymentCompletedEvents(paymentCompletedEvents);
        response.setCompletedEvents(completedEvents);
        response.setViewToCartRate(rate(addToCartEvents, productViews));
        response.setCheckoutToPaymentRate(rate(paymentCompletedEvents, checkoutStartedEvents));
        response.setOrdersByStatus(ordersByStatus);
        response.setEventTrackingAvailable(!eventCounts.isEmpty());

        List<String> fallback = new ArrayList<>();
        fallback.add(paidOrLaterOrders + " of " + totalOrders
                + " order(s) reached payment or later in the lifecycle.");
        fallback.add(deliveredOrders + " order(s) were delivered, a "
                + response.getDeliveryCompletionRate() + "% delivery completion rate.");
        fallback.add(cancelledOrders > 0
                ? cancelledOrders + " order(s) were cancelled; review cancellation reasons."
                : "No cancelled orders were recorded in this window.");
        if (eventCounts.isEmpty()) {
            fallback.add("Visitor, product-view, cart, and checkout events are not tracked yet.");
        } else {
            fallback.add(trackedVisitors + " visitor(s), " + productViews + " product view(s), and "
                    + addToCartEvents + " add-to-cart event(s) were tracked.");
            fallback.add("Checkout-to-payment progression was "
                    + response.getCheckoutToPaymentRate() + "%; use event volume to investigate drop-off.");
        }

        Insight insight = generate(
                "You are an ecommerce operations analyst for an online perfume shop. "
                        + "Summarize this order-lifecycle funnel in 3-4 short bullet points. "
                        + "Do not call these visitor conversion metrics and do not invent missing event data. "
                        + "Use only the data below.\n\nOrders by status: " + ordersByStatus
                        + "\nEvent counts: " + eventCounts,
                fallback);
        response.setAiGenerated(insight.aiGenerated());
        response.setInsight(insight.text());
        response.setInsights(insight.bullets());
        return response;
    }

    private Double rate(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : round2(numerator * 100.0 / denominator);
    }

    private AdminCustomerIntelligenceResponse.SegmentItem segment(String name, long count) {
        AdminCustomerIntelligenceResponse.SegmentItem item =
                new AdminCustomerIntelligenceResponse.SegmentItem();
        item.setSegment(name);
        item.setCount(count);
        return item;
    }

    private List<String> fallbackCustomerInsight(long newCustomers, long activeCustomers,
                                                 long repeatCustomers, Double repeatRate,
                                                 Map<String, BigDecimal> topBrands,
                                                 List<AdminCustomerIntelligenceResponse.FragranceFamilyItem> families) {
        List<String> bullets = new ArrayList<>();
        bullets.add(newCustomers + " new customer(s); " + activeCustomers + " active in the window.");
        bullets.add("Repeat purchase rate: " + repeatRate + "% ("
                + repeatCustomers + " repeat buyer(s)).");
        if (!topBrands.isEmpty()) {
            bullets.add("Top brand: " + topBrands.keySet().iterator().next() + ".");
        }
        if (!families.isEmpty()) {
            bullets.add("Favorite fragrance family: " + families.get(0).getFragranceFamily() + ".");
        }
        bullets.add(repeatRate != null && repeatRate >= 30
                ? "Strong repeat behavior; focus on retention campaigns."
                : "Most customers are first-time; encourage follow-up purchases.");
        return bullets;
    }

    private String formatMap(Map<String, BigDecimal> map) {
        if (map.isEmpty()) {
            return "none";
        }
        List<String> parts = new ArrayList<>();
        map.forEach((k, v) -> parts.add(k + " $" + v));
        return String.join(", ", parts);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private record Insight(String text, List<String> bullets, boolean aiGenerated) {
    }

    private Insight generate(String prompt, List<String> fallbackBullets) {
        if (!insightEnabled) {
            return new Insight(String.join("\n", fallbackBullets), fallbackBullets, false);
        }
        try {
            AIMessage userMessage = new AIMessage();
            userMessage.setSender(MessageSender.USER);
            userMessage.setMessage(prompt);
            String aiText = openRouterService.generateResponse(List.of(userMessage), null);
            if (aiText != null && !aiText.isBlank()) {
                String trimmed = aiText.trim();
                List<String> bullets = trimmed.lines()
                        .map(String::strip)
                        .filter(line -> !line.isEmpty())
                        .toList();
                return new Insight(trimmed, bullets, true);
            }
        } catch (Exception ex) {
            log.warn("AI analytics insight generation failed, using deterministic summary", ex);
        }
        return new Insight(String.join("\n", fallbackBullets), fallbackBullets, false);
    }

    private record Window(LocalDate from, LocalDate to) {
        LocalDateTime start() {
            return from.atStartOfDay();
        }

        LocalDateTime end() {
            return to.plusDays(1).atStartOfDay();
        }
    }

    private Window resolveWindow(AnalyticsFilterRequest filter) {
        AnalyticsFilterRequest req = filter == null ? new AnalyticsFilterRequest() : filter;
        LocalDate from = req.getFrom() != null
                ? req.getFrom()
                : LocalDate.now().minusDays(DEFAULT_WINDOW_DAYS - 1);
        LocalDate to = req.getTo() != null ? req.getTo() : LocalDate.now();
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' date cannot be after 'to' date");
        }
        return new Window(from, to);
    }

    private Map<String, BigDecimal> expenseByCategory(LocalDate from, LocalDate to) {
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (ExpenseRepository.ExpenseCategoryStat stat : expenseRepository
                .sumAmountByCategoryBetween(from, to)) {
            String key = stat.getCategory() == null || stat.getCategory().isBlank()
                    ? "Uncategorized" : stat.getCategory();
            byCategory.put(key, safe(stat.getTotal()));
        }
        List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(byCategory.entrySet());
        sorted.sort(Map.Entry.<String, BigDecimal>comparingByValue(
                Comparator.nullsLast(Comparator.reverseOrder())));
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : sorted) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private String describeExpenses(Map<String, BigDecimal> byCategory) {
        if (byCategory.isEmpty()) {
            return "none recorded";
        }
        List<String> parts = new ArrayList<>();
        byCategory.forEach((k, v) -> parts.add(k + " $" + v));
        return String.join(", ", parts);
    }

    private String formatCategoryRevenue(Map<String, BigDecimal> categoryRevenue) {
        if (categoryRevenue.isEmpty()) {
            return "none";
        }
        List<String> parts = new ArrayList<>();
        categoryRevenue.forEach((k, v) -> parts.add(k + " $" + v));
        return String.join(", ", parts);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String money(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String nullToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown brand" : value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}