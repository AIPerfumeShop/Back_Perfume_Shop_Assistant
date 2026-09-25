package com.example.spring_boot_project_api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.request.order.CheckoutRequest;
import com.example.spring_boot_project_api.dto.response.order.CheckoutResponse;
import com.example.spring_boot_project_api.enums.PaymentMethod;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.exception.InvalidOrderException;
import com.example.spring_boot_project_api.mapper.OrderMapper;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.Settings;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.OrderStatusHistoryRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.ProductVariantRepository;
import com.example.spring_boot_project_api.repository.SettingsRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.NotificationService;
import com.example.spring_boot_project_api.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PaymentService paymentService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SettingsRepository settingsRepository;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository, userRepository, productVariantRepository,
                paymentRepository, orderStatusHistoryRepository, orderMapper,
                paymentService, notificationService, settingsRepository);
    }

    private CheckoutRequest khqrRequest() {
        CheckoutRequest request = new CheckoutRequest();
        request.setShippingAddress("123 Main St");
        request.setPhone("0123456789");
        request.setCity("Phnom Penh");
        request.setItems(List.of());
        request.setPaymentMethod("KHQR");
        return request;
    }

    private Settings khqrSetting(String value) {
        Settings settings = new Settings();
        settings.setSettingKey("payment_khqr");
        settings.setValue(value);
        return settings;
    }

    private void stubUser() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void checkout_rejectsKhqrWhenToggleDisabled() {
        stubUser();
        when(settingsRepository.findBySettingKey("payment_khqr"))
                .thenReturn(Optional.of(khqrSetting("false")));

        assertThrows(InvalidOrderException.class,
                () -> orderService.checkout(1L, khqrRequest()));

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentService, never()).initBakongPayment(any(Order.class));
    }

    @Test
    void checkout_allowsKhqrWhenToggleEnabled() {
        stubUser();
        when(settingsRepository.findBySettingKey("payment_khqr"))
                .thenReturn(Optional.of(khqrSetting("true")));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = new Payment();
        payment.setId(7L);
        payment.setPaymentMethod(PaymentMethod.KHQR);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId("tx-1");
        when(paymentService.initBakongPayment(any(Order.class)))
                .thenReturn(payment);

        CheckoutResponse response = orderService.checkout(1L, khqrRequest());

        assertEquals(7L, response.getPaymentId());
        assertEquals(PaymentMethod.KHQR, response.getPaymentMethod());
    }

    @Test
    void checkout_allowsKhqrByDefaultWhenSettingAbsent() {
        stubUser();
        when(settingsRepository.findBySettingKey("payment_khqr"))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = new Payment();
        payment.setId(7L);
        payment.setPaymentMethod(PaymentMethod.KHQR);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId("tx-1");
        when(paymentService.initBakongPayment(any(Order.class)))
                .thenReturn(payment);

        CheckoutResponse response = orderService.checkout(1L, khqrRequest());

        assertEquals(7L, response.getPaymentId());
    }
}