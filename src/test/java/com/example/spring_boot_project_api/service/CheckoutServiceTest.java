package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.request.order.CheckoutRequest;
import com.example.spring_boot_project_api.dto.request.order.OrderItemRequest;
import com.example.spring_boot_project_api.dto.response.order.CheckoutResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.enums.PaymentMethod;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.exception.InvalidOrderException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.OrderMapper;
import com.example.spring_boot_project_api.model.Brand;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.ProductVariantRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.OrderServiceImpl;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private PaymentService paymentService;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository, userRepository, productVariantRepository,
                new OrderMapper(), paymentService);
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setName("Chan Dara");
        return user;
    }

    private ProductVariant variant(Long id, int stock, BigDecimal price) {
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Lancome");

        Product product = new Product();
        product.setId(10L);
        product.setName("Idole");
        product.setBrand(brand);

        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setProduct(product);
        variant.setSizeMl(50);
        variant.setPrice(price);
        variant.setStock(stock);
        variant.setIsActive(true);
        return variant;
    }

    private CheckoutRequest checkoutRequest() {
        CheckoutRequest request = new CheckoutRequest();
        request.setShippingAddress("Phnom Penh");
        request.setPhone("012345678");
        request.setPaymentMethod("ABA");
        return request;
    }

    private OrderItemRequest itemRequest(Long variantId, int qty) {
        OrderItemRequest item = new OrderItemRequest();
        item.setVariantId(variantId);
        item.setQuantity(qty);
        return item;
    }

    @Test
    void checkout_success_createsOrderAndProcessesPayment() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        ProductVariant variant = variant(5L, 10, new BigDecimal("59.50"));
        when(productVariantRepository.findById(5L)).thenReturn(Optional.of(variant));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> {
                    Order order = inv.getArgument(0);
                    order.setId(1L);
                    return order;
                });

        Payment payment = new Payment();
        payment.setId(99L);
        payment.setTransactionId("txn-checkout");
        payment.setPaymentMethod(PaymentMethod.ABA);
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentService.initPayment(any(Order.class), eq("ABA"))).thenReturn(payment);
        when(paymentService.processPayment(99L)).thenReturn(true);

        CheckoutRequest request = checkoutRequest();
        request.setItems(List.of(itemRequest(5L, 2)));

        CheckoutResponse response = orderService.checkout(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.getOrderId());
        assertEquals(new BigDecimal("119.00"), response.getTotalAmount());
        assertEquals(OrderStatus.PENDING.name(), response.getOrderStatus());
        assertEquals(99L, response.getPaymentId());
        assertEquals(PaymentMethod.ABA, response.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
        assertEquals("txn-checkout", response.getTransactionId());

        //Stock was decremented
        assertEquals(8, variant.getStock());
        verify(productVariantRepository).save(variant);
        verify(paymentService).initPayment(any(Order.class), eq("ABA"));
        verify(paymentService).processPayment(99L);
    }

    @Test
    void checkout_paymentFailure_cancelsOrderAndRestoresStock() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        ProductVariant variant = variant(5L, 10, new BigDecimal("59.50"));
        when(productVariantRepository.findById(5L)).thenReturn(Optional.of(variant));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> {
                    Order order = inv.getArgument(0);
                    order.setId(2L);
                    return order;
                });

        Payment payment = new Payment();
        payment.setId(7L);
        payment.setTransactionId("txn-fail");
        payment.setPaymentMethod(PaymentMethod.ABA);
        when(paymentService.initPayment(any(Order.class), eq("ABA"))).thenReturn(payment);
        when(paymentService.processPayment(7L)).thenReturn(false);

        CheckoutRequest request = checkoutRequest();
        request.setItems(List.of(itemRequest(5L, 3)));

        CheckoutResponse response = orderService.checkout(1L, request);

        assertEquals(OrderStatus.CANCELLED.name(), response.getOrderStatus());
        //Stock was restored: 10 - 3 + 3 = 10
        assertEquals(10, variant.getStock());
        verify(paymentService).processPayment(7L);
    }

    @Test
    void checkout_userNotFound_throwsResourceNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        CheckoutRequest request = checkoutRequest();
        request.setItems(List.of(itemRequest(5L, 1)));

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.checkout(1L, request));
    }

    @Test
    void checkout_variantNotFound_throwsInvalidOrder() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(productVariantRepository.findById(500L)).thenReturn(Optional.empty());

        CheckoutRequest request = checkoutRequest();
        request.setItems(List.of(itemRequest(500L, 1)));

        assertThrows(InvalidOrderException.class,
                () -> orderService.checkout(1L, request));
    }

    @Test
    void checkout_insufficientStock_throwsInvalidOrder() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        ProductVariant variant = variant(5L, 2, new BigDecimal("59.50"));
        when(productVariantRepository.findById(5L)).thenReturn(Optional.of(variant));

        CheckoutRequest request = checkoutRequest();
        request.setItems(List.of(itemRequest(5L, 5)));

        assertThrows(InvalidOrderException.class,
                () -> orderService.checkout(1L, request));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_inactiveVariant_throwsInvalidOrder() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        ProductVariant variant = variant(5L, 10, new BigDecimal("59.50"));
        variant.setIsActive(false);
        when(productVariantRepository.findById(5L)).thenReturn(Optional.of(variant));

        CheckoutRequest request = checkoutRequest();
        request.setItems(List.of(itemRequest(5L, 1)));

        assertThrows(InvalidOrderException.class,
                () -> orderService.checkout(1L, request));
    }

    @Test
    void checkout_totalsSubtotalOfAllItems() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        ProductVariant cheap = variant(1L, 10, new BigDecimal("10.00"));
        ProductVariant expensive = variant(2L, 10, new BigDecimal("20.50"));
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(cheap));
        when(productVariantRepository.findById(2L)).thenReturn(Optional.of(expensive));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Payment payment = new Payment();
        payment.setId(1L);
        when(paymentService.initPayment(any(Order.class), any(String.class)))
                .thenReturn(payment);
        when(paymentService.processPayment(anyLong())).thenReturn(true);

        CheckoutRequest request = checkoutRequest();
        request.setItems(List.of(itemRequest(1L, 2), itemRequest(2L, 3)));

        CheckoutResponse response = orderService.checkout(1L, request);

        // 2 * 10.00 = 20.00 ; 3 * 20.50 = 61.50 ; total = 81.50
        assertEquals(new BigDecimal("81.50"), response.getTotalAmount());
    }

    @Test
    void checkout_singleItemQuantityOne() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        ProductVariant variant = variant(5L, 10, new BigDecimal("59.50"));
        when(productVariantRepository.findById(5L)).thenReturn(Optional.of(variant));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> {
                    Order order = inv.getArgument(0);
                    order.setId(3L);
                    return order;
                });

        Payment payment = new Payment();
        payment.setId(3L);
        payment.setTransactionId("txn-single");
        payment.setPaymentMethod(PaymentMethod.ABA);
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentService.initPayment(any(Order.class), eq("ABA"))).thenReturn(payment);
        when(paymentService.processPayment(3L)).thenReturn(true);

        CheckoutRequest request = checkoutRequest();
        request.setItems(List.of(itemRequest(5L, 1)));

        CheckoutResponse response = orderService.checkout(1L, request);

        assertEquals(new BigDecimal("59.50"), response.getTotalAmount());
        assertEquals(9, variant.getStock());
    }

    @Test
    void checkout_emptyItems_succeedsWithZeroTotal() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> {
                    Order order = inv.getArgument(0);
                    order.setId(4L);
                    return order;
                });

        Payment payment = new Payment();
        payment.setId(4L);
        payment.setTransactionId("txn-empty");
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentService.initPayment(any(Order.class), eq("CASH"))).thenReturn(payment);
        when(paymentService.processPayment(4L)).thenReturn(true);

        CheckoutRequest request = checkoutRequest();
        request.setPaymentMethod("CASH");
        request.setItems(List.of());

        CheckoutResponse response = orderService.checkout(1L, request);

        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalAmount()));
        assertEquals(PaymentMethod.CASH, response.getPaymentMethod());
    }

    @Test
    void checkout_stockAfterMultipleCheckouts() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        ProductVariant variant = variant(5L, 10, new BigDecimal("59.50"));
        when(productVariantRepository.findById(5L)).thenReturn(Optional.of(variant));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Payment payment1 = new Payment();
        payment1.setId(10L);
        when(paymentService.initPayment(any(Order.class), eq("ABA"))).thenReturn(payment1);
        when(paymentService.processPayment(10L)).thenReturn(true);

        CheckoutRequest request = checkoutRequest();
        request.setItems(List.of(itemRequest(5L, 3)));
        orderService.checkout(1L, request);

        assertEquals(7, variant.getStock());

        Payment payment2 = new Payment();
        payment2.setId(11L);
        when(paymentService.initPayment(any(Order.class), eq("ABA"))).thenReturn(payment2);
        when(paymentService.processPayment(11L)).thenReturn(true);

        orderService.checkout(1L, request);

        assertEquals(4, variant.getStock());
    }
}