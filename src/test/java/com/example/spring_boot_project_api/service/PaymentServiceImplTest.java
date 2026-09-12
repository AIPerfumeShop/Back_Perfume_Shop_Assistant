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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.request.payment.PaymentRequest;
import com.example.spring_boot_project_api.dto.response.bakong.BakongResponse;
import com.example.spring_boot_project_api.dto.response.payment.PaymentResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.enums.PaymentMethod;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.BakongException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.PaymentMapper;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.service.BakongService;
import com.example.spring_boot_project_api.service.impl.PaymentServiceImpl;

import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import kh.gov.nbc.bakong_khqr.model.KHQRStatus;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private BakongService bakongService;

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

    @Test
    void getPaymentByTransactionId_blank_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> paymentService.getPaymentByTransactionId("  "));
    }

    @Test
    void getPayment_notFound_throws() {
        when(paymentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getPayment(404L));
    }

    @Test
    void processPayment_alreadySuccessful_throwsBadRequest() {
        Payment payment = newPayment(newOrder(), PaymentStatus.SUCCESSFUL);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(BadRequestException.class,
                () -> paymentService.processPayment(1L));
    }

    @Test
    void processPayment_alreadyRefunded_throwsBadRequest() {
        Payment payment = newPayment(newOrder(), PaymentStatus.REFUNDED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(BadRequestException.class,
                () -> paymentService.processPayment(1L));
    }

    @Test
    void validatePayment_refunded_throwsBadRequest() {
        Payment payment = newPayment(newOrder(), PaymentStatus.REFUNDED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(BadRequestException.class,
                () -> paymentService.validatePayment(1L));
    }

    @Test
    void validatePayment_missingOrder_throwsBadRequest() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setPaymentMethod(PaymentMethod.ABA);
        payment.setAmount(new BigDecimal("50.00"));
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(BadRequestException.class,
                () -> paymentService.validatePayment(1L));
        assertEquals("Payment is not linked to an order", payment.getErrorMessage());
    }

    @Test
    void initPayment_returnsPendingPaymentWithTransaction() {
        Order order = newOrder();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = paymentService.initPayment(order, "CASH");

        assertNotNull(payment);
        assertEquals(order, payment.getOrder());
        assertEquals(PaymentMethod.CASH, payment.getPaymentMethod());
        assertEquals(order.getTotalAmount(), payment.getAmount());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNotNull(payment.getTransactionId());
    }

    @Test
    void getPaymentHistoryByOrder_delegatesToMapper() {
        when(paymentRepository.findAllByOrderIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(newPayment(newOrder(), PaymentStatus.SUCCESSFUL)));

        PaymentResponse mapped = new PaymentResponse();
        mapped.setId(1L);
        mapped.setOrderId(10L);
        mapped.setStatus(PaymentStatus.SUCCESSFUL);
        when(paymentMapper.toResponseList(any()))
                .thenReturn(List.of(mapped));

        List<PaymentResponse> responses = paymentService.getPaymentHistoryByOrder(10L);

        assertEquals(1, responses.size());
        assertEquals(10L, responses.get(0).getOrderId());
    }

    @Test
    void getPaymentHistoryByOrder_emptyList() {
        when(paymentRepository.findAllByOrderIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of());
        when(paymentMapper.toResponseList(any())).thenReturn(List.of());

        List<PaymentResponse> responses = paymentService.getPaymentHistoryByOrder(10L);

        assertTrue(responses.isEmpty());
    }

    @Test
    void getPaymentHistoryByUser_delegatesToRepository() {
        when(paymentRepository.findAllByOrderUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(newPayment(newOrder(), PaymentStatus.SUCCESSFUL)));

        PaymentResponse mapped = new PaymentResponse();
        mapped.setId(1L);
        mapped.setOrderId(10L);
        mapped.setStatus(PaymentStatus.SUCCESSFUL);
        when(paymentMapper.toResponseList(any()))
                .thenReturn(List.of(mapped));

        List<PaymentResponse> responses = paymentService.getPaymentHistoryByUser(10L);

        assertEquals(1, responses.size());
        assertEquals(10L, responses.get(0).getOrderId());
    }

    @Test
    void getPaymentHistoryByUser_emptyList() {
        when(paymentRepository.findAllByOrderUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of());
        when(paymentMapper.toResponseList(any())).thenReturn(List.of());

        List<PaymentResponse> responses = paymentService.getPaymentHistoryByUser(10L);

        assertTrue(responses.isEmpty());
    }

    @Test
    void getPaymentStatus_delegatesToGetPayment() {
        Order order = newOrder();
        Payment payment = newPayment(order, PaymentStatus.SUCCESSFUL);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment)).thenAnswer(inv -> {
            PaymentResponse r = new PaymentResponse();
            r.setId(payment.getId());
            r.setStatus(payment.getStatus());
            return r;
        });

        PaymentResponse response = paymentService.getPaymentStatus(1L);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESSFUL, response.getStatus());
    }

    @Test
    void validatePayment_notFound_throws() {
        when(paymentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.validatePayment(404L));
    }

    @Test
    void processPayment_notFound_throws() {
        when(paymentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.processPayment(404L));
    }

    @Test
    void createPayment_cashMethod_succeeds() {
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
        request.setPaymentMethod("CASH");

        PaymentResponse response = paymentService.createPayment(request);

        assertEquals(PaymentMethod.CASH, response.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
    }

    @Test
    void initBakongPayment_success_setsQrAndMd5() {
        Order order = newOrder();
        KHQRData data = new KHQRData();
        data.setQr("00020101021XKHQR");
        data.setMd5("abc123");
        KHQRStatus status = new KHQRStatus();
        status.setCode(0);
        KHQRResponse<KHQRData> qrResponse = new KHQRResponse<>();
        qrResponse.setKHQRStatus(status);
        qrResponse.setData(data);

        when(bakongService.generateQR(any()))
                .thenReturn(qrResponse);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = paymentService.initBakongPayment(order);

        assertNotNull(payment);
        assertEquals(PaymentMethod.KHQR, payment.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals("00020101021XKHQR", payment.getQrText());
        assertEquals("abc123", payment.getMd5());
    }

    @Test
    void initBakongPayment_gatewayFailure_throwsBakong() {
        Order order = newOrder();
        KHQRStatus status = new KHQRStatus();
        status.setCode(1);
        status.setMessage("missing field");
        KHQRResponse<KHQRData> qrResponse = new KHQRResponse<>();
        qrResponse.setKHQRStatus(status);

        when(bakongService.generateQR(any())).thenReturn(qrResponse);

        assertThrows(BakongException.class,
                () -> paymentService.initBakongPayment(order));
    }

    @Test
    void verifyBakongPayment_success_marksPaidAndUpdatesOrder() {
        Order order = newOrder();
        Payment payment = newPayment(order, PaymentStatus.PENDING);
        payment.setMd5("abc123");

        Map<String, Object> data = Map.of("externalRef", "100FT123");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(bakongService.checkTransactionByMD5(any()))
                .thenReturn(new BakongResponse(0, "Success", null, data));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(payment)).thenAnswer(inv -> toResponse(payment));

        PaymentResponse result = paymentService.verifyBakongPayment(1L);

        assertEquals(PaymentStatus.SUCCESSFUL, result.getStatus());
        assertNotNull(result.getPaidAt());
        assertEquals("100FT123", result.getExternalRef());
        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    void verifyBakongPayment_notPaid_staysPending() {
        Order order = newOrder();
        Payment payment = newPayment(order, PaymentStatus.PENDING);
        payment.setMd5("abc123");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(bakongService.checkTransactionByMD5(any()))
                .thenReturn(new BakongResponse(1, "Not found", 1, null));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(payment)).thenAnswer(inv -> toResponse(payment));

        PaymentResponse result = paymentService.verifyBakongPayment(1L);

        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    private PaymentResponse toResponse(Payment payment) {
        PaymentResponse r = new PaymentResponse();
        r.setId(payment.getId());
        r.setOrderId(payment.getOrder() != null ? payment.getOrder().getId() : null);
        r.setPaymentMethod(payment.getPaymentMethod());
        r.setStatus(payment.getStatus());
        r.setTransactionId(payment.getTransactionId());
        r.setPaidAt(payment.getPaidAt());
        r.setErrorMessage(payment.getErrorMessage());
        r.setExternalRef(payment.getExternalRef());
        r.setQrText(payment.getQrText());
        r.setMd5(payment.getMd5());
        return r;
    }
}