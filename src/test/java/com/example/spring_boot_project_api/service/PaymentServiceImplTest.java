package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.request.payment.PaymentRequest;
import com.example.spring_boot_project_api.dto.response.payment.PaymentResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.enums.PaymentMethod;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.PaymentMapper;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.service.impl.PaymentServiceImpl;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment newPayment(Order order, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrder(order);
        payment.setPaymentMethod(PaymentMethod.ABA);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(status);
        payment.setTransactionId("txn-123");
        return payment;
    }

    private Order newOrder() {
        Order order = new Order();
        order.setId(10L);
        order.setTotalAmount(new BigDecimal("59.50"));
        order.setStatus(OrderStatus.PENDING);
        return order;
    }

    @Test
    void createPayment_savesPaymentAndReturnsResponse() {
        Order order = newOrder();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(Payment.class)))
                .thenAnswer(inv -> {
                    Payment p = inv.getArgument(0);
                    PaymentResponse r = new PaymentResponse();
                    r.setId(p.getId());
                    r.setOrderId(p.getOrder().getId());
                    r.setPaymentMethod(p.getPaymentMethod());
                    r.setStatus(p.getStatus());
                    r.setTransactionId(p.getTransactionId());
                    r.setAmount(p.getAmount());
                    return r;
                });

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(10L);
        request.setPaymentMethod("aba");

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals(10L, response.getOrderId());
        assertEquals(PaymentMethod.ABA, response.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertNotNull(response.getTransactionId());
    }

    @Test
    void createPayment_invalidMethod_throwsBadRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(10L);
        request.setPaymentMethod("not-a-method");

        assertThrows(BadRequestException.class,
                () -> paymentService.createPayment(request));
    }

    @Test
    void createPayment_orderNotFound_throwsResourceNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(999L);
        request.setPaymentMethod("ABA");

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.createPayment(request));
    }

    @Test
    void validatePayment_validPayment_returnsResponse() {
        Order order = newOrder();
        Payment payment = newPayment(order, PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(payment)).thenAnswer(inv -> {
            PaymentResponse r = new PaymentResponse();
            r.setId(payment.getId());
            r.setOrderId(payment.getOrder().getId());
            r.setStatus(payment.getStatus());
            return r;
        });

        PaymentResponse response = paymentService.validatePayment(1L);

        assertNotNull(response);
        assertNull(payment.getErrorMessage());
    }

    @Test
    void validatePayment_alreadySuccessful_throwsBadRequest() {
        Order order = newOrder();
        Payment payment = newPayment(order, PaymentStatus.SUCCESSFUL);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(BadRequestException.class,
                () -> paymentService.validatePayment(1L));
    }

    @Test
    void processPayment_success_updatesStatusAndPaidAt() {
        Order order = newOrder();
        Payment payment = newPayment(order, PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean success = paymentService.processPayment(1L);

        assertTrue(success);
        assertEquals(PaymentStatus.SUCCESSFUL, payment.getStatus());
        assertNotNull(payment.getPaidAt());
    }

    @Test
    void processPayment_invalidAmount_fails() {
        Payment payment = newPayment(newOrder(), PaymentStatus.PENDING);
        payment.setAmount(null);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean success = paymentService.processPayment(1L);

        assertFalse(success);
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertNull(payment.getPaidAt());
    }

    @Test
    void getPaymentByTransactionId_notFound_throws() {
        when(paymentRepository.findByTransactionId("nope")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getPaymentByTransactionId("nope"));
    }
}