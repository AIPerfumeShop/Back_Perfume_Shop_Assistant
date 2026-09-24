package com.example.spring_boot_project_api.service.impl;

import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.spring_boot_project_api.config.TelegramBotProperties;
import com.example.spring_boot_project_api.dto.response.payment.PaymentResponse;
import com.example.spring_boot_project_api.model.OrderItem;
import com.example.spring_boot_project_api.repository.OrderRepository;
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
    private final OrderRepository orderRepository;

    private TelegramLongPollingBot bot;

    public TelegramServiceImpl(TelegramBotProperties properties,
                               UserRepository userRepository,
                               PaymentRepository paymentRepository,
                               OrderRepository orderRepository) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.warn("Telegram bot disabled (telegram.bot.enabled=false or creds missing) - notifications disabled");
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
    public void sendPaymentNotification(PaymentResponse payment) {
        sendMessage(buildPaymentNotification(payment));
    }

    private String buildPaymentNotification(PaymentResponse payment) {
        StringBuilder text = new StringBuilder("💰 Payment Received!")
                .append("\n\nPayment ID: PAY-").append(payment.getId())
                .append("\nOrder Number: ORD-")
                .append(payment.getOrderId() != null ? payment.getOrderId() : "N/A")
                .append("\nPayment Method: ")
                .append(payment.getPaymentMethod() != null
                        ? payment.getPaymentMethod().name()
                        : "N/A")
                .append("\nAmount: $")
                .append(payment.getAmount() != null ? payment.getAmount() : "N/A")
                .append("\nTransaction ID: ")
                .append(nullToNa(payment.getTransactionId()))
                .append("\nPaid At: ").append(payment.getPaidAt() != null
                ? payment.getPaidAt().format(DATE_TIME)
                : "N/A")
                .append("\nExternal Ref: ").append(nullToNa(payment.getExternalRef()));

        if (payment.getOrderUserId() != null) {
            text.append("\n\nUser: ").append(formatUserById(payment.getOrderUserId()));
        }

        if (payment.getOrderId() != null) {
            orderRepository.findById(payment.getOrderId())
                    .filter(order -> order.getItems() != null
                            && !order.getItems().isEmpty())
                    .ifPresent(order -> {
                        text.append("\n\nItems:");
                        for (OrderItem item : order.getItems()) {
                            text.append("\n• ").append(item.getProductName());
                            if (item.getVariantSize() != null) {
                                text.append(" [").append(item.getVariantSize()).append("]");
                            }
                            text.append("\n   Qty: ").append(item.getQuantity())
                                    .append("\n   Price: $").append(item.getUnitPrice());
                        }
                        text.append("\n\nTotal: $").append(order.getTotalAmount())
                                .append("\nAddress: ")
                                .append(nullToNa(order.getShippingAddress()))
                                .append("\nPhone: ").append(nullToNa(order.getPhone()));
                    });
        }

        return text.toString();
    }

    private String formatUserById(Long userId) {
        if (userId == null) {
            return "N/A";
        }
        return userRepository.findById(userId)
                .map(u -> {
                    String name = u.getName() != null ? u.getName() : "User #" + u.getId();
                    return u.getEmail() != null
                            ? name + " (" + u.getEmail() + ")"
                            : name;
                })
                .orElse("User #" + userId);
    }

    private String nullToNa(String value) {
        return value != null && !value.isBlank() ? value : "N/A";
    }
    
}