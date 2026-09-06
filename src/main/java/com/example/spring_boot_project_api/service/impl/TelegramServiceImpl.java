package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.spring_boot_project_api.config.TelegramBotProperties;
import com.example.spring_boot_project_api.dto.response.order.OrderItemResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.TelegramService;

import jakarta.annotation.PostConstruct;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Service
public class TelegramServiceImpl implements TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TelegramBotProperties properties;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    private TelegramLongPollingBot bot;

    public TelegramServiceImpl(TelegramBotProperties properties,
                               UserRepository userRepository,
                               PaymentRepository paymentRepository) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
    }

    @PostConstruct
    public void init() {
        if (!properties.hasToken()) {
            log.warn("Telegram bot is not configured (telegram.bot.token missing) - notifications disabled");
            return;
        }
        bot = new TelegramLongPollingBot(properties.getToken()) {
            @Override
            public String getBotUsername() {
                return properties.getUsername();
            }

            @Override
            public void onUpdateReceived(Update update) {
                // Configuration only - incoming chat messages are not handled.
            }
        };
        try {
            new TelegramBotsApi(DefaultBotSession.class).registerBot(bot);
            log.info("Telegram bot '{}' registered", properties.getUsername());
        } catch (TelegramApiException e) {
            log.warn("Failed to register Telegram bot: {}", e.getMessage());
            bot = null;
        }
    }

    @Override
    public boolean isEnabled() {
        return bot != null && properties.isEnabled();
    }

    @Override
    public void sendMessage(String text) {
        if (!isEnabled() || text == null || text.isBlank()) {
            return;
        }
        try {
            bot.execute(SendMessage.builder()
                    .chatId(properties.getChatId())
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Telegram send failed: {}", e.getMessage());
        }
    }

    @Override
    public void sendOrderNotification(OrderResponse order) {
        sendMessage(buildOrderNotification(order));
    }

    @Override
    public void sendRecommendationSummary(String userName, List<String> productLines) {
        if (productLines == null || productLines.isEmpty()) {
            return;
        }
        StringBuilder text = new StringBuilder("AI recommendation for " + userName);
        for (String line : productLines) {
            text.append("\n- ").append(line);
        }
        sendMessage(text.toString());
    }

    private String buildOrderNotification(OrderResponse order) {
        StringBuilder text = new StringBuilder("🛒 New Order Received!")
                .append("\n\nOrder Number: ORD-").append(order.getId())
                .append("\nUser: ").append(formatUser(order))
                .append("\nPhone: ").append(nullToNa(order.getPhone()))
                .append("\nAddress: ").append(nullToNa(order.getShippingAddress()));

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            text.append("\n\nItems:");
            for (OrderItemResponse item : order.getItems()) {
                text.append("\n• ").append(item.getProductName());
                if (item.getVariantSize() != null) {
                    text.append(" [").append(item.getVariantSize()).append("]");
                }
                text.append("\n   Qty: ").append(item.getQuantity())
                        .append("\n   Price: $").append(item.getUnitPrice());
            }
        }

        text.append("\n\nSubtotal: $").append(computeSubtotal(order.getItems()))
                .append("\nTotal: $").append(order.getTotalAmount())
                .append("\n\nPayment Method: ").append(formatPaymentMethod(order))
                .append("\nOrder Date: ").append(order.getCreatedAt() != null
                        ? order.getCreatedAt().format(DATE_TIME)
                        : "N/A");
        return text.toString();
    }

    private String formatUser(OrderResponse order) {
        String name = order.getUserName() != null ? order.getUserName() : "User #" + order.getUserId();
        String email = null;
        if (order.getUserId() != null) {
            email = userRepository.findById(order.getUserId())
                    .map(User::getEmail)
                    .orElse(null);
        }
        return email != null ? name + " (" + email + ")" : name;
    }

    private String formatPaymentMethod(OrderResponse order) {
        if (order.getId() == null) {
            return "N/A";
        }
        return paymentRepository.findByOrderId(order.getId())
                .map(Payment::getPaymentMethod)
                .map(Enum::name)
                .orElse("N/A");
    }

    private BigDecimal computeSubtotal(List<OrderItemResponse> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(OrderItemResponse::getSubtotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String nullToNa(String value) {
        return value != null && !value.isBlank() ? value : "N/A";
    }
}