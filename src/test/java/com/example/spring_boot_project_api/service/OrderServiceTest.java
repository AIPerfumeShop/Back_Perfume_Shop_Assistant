package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.order.CreateOrderRequest;
import com.example.spring_boot_project_api.dto.request.order.OrderFilterRequest;
import com.example.spring_boot_project_api.dto.request.order.OrderItemRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.order.AdminOrderSummaryResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.exception.ForbiddenException;
import com.example.spring_boot_project_api.exception.InvalidOrderException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.OrderMapper;
import com.example.spring_boot_project_api.model.Brand;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.OrderItem;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.ProductVariantRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.OrderServiceImpl;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Chan Dara");
        user.setEmail("dara@example.com");
        return user;
    }

    private ProductVariant variant(Long id, int stock) {
        Brand brand = new Brand();
        brand.setName("Lancome");

        Product product = new Product();
        product.setId(10L);
        product.setName("Idole");
        product.setBrand(brand);

        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setProduct(product);
        variant.setSizeMl(50);
        variant.setPrice(new BigDecimal("59.50"));
        variant.setStock(stock);
        variant.setIsActive(true);
        return variant;
    }

    private Order order(Long id, User owner, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setUser(owner);
        order.setStatus(status);
        order.setShippingAddress("Phnom Penh");
        order.setPhone("012345678");
        order.setTotalAmount(new BigDecimal("59.50"));
        return order;
    }

    private OrderItem orderItem(Long variantId, int qty) {
        ProductVariant variant = variant(variantId, 5);
        OrderItem item = new OrderItem();
        item.setVariant(variant);
        item.setQuantity(qty);
        return item;
    }

    private CreateOrderRequest createOrderRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setShippingAddress("Phnom Penh");
        request.setPhone("012345678");

        OrderItemRequest item = new OrderItemRequest();
        item.setVariantId(5L);
        item.setQuantity(1);
        request.setItems(List.of(item));
        return request;
    }

    // ---------- createOrder ----------

    @Test
    void createOrder_success_returnsOrderResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        ProductVariant variant = variant(5L, 10);
        when(productVariantRepository.findById(5L)).thenReturn(Optional.of(variant));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> {
                    Order order = inv.getArgument(0);
                    order.setId(1L);
                    return order;
                });

        OrderResponse response = orderService.createOrder(createOrderRequest());

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("Chan Dara", response.getUserName());
        assertEquals(new BigDecimal("59.50"), response.getTotalAmount());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(1, response.getItems().size());
        assertEquals(9, variant.getStock());
    }

    @Test
    void createOrder_multipleItems_sumsTotal() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        ProductVariant v1 = variant(1L, 10);
        ProductVariant v2 = variant(2L, 10);
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(v1));
        when(productVariantRepository.findById(2L)).thenReturn(Optional.of(v2));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setShippingAddress("Phnom Penh");
        request.setPhone("012345678");
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setVariantId(1L);
        item1.setQuantity(2);
        OrderItemRequest item2 = new OrderItemRequest();
        item2.setVariantId(2L);
        item2.setQuantity(3);
        request.setItems(List.of(item1, item2));

        OrderResponse response = orderService.createOrder(request);

        // 2*59.50 + 3*59.50 = 5*59.50 = 297.50
        assertEquals(new BigDecimal("297.50"), response.getTotalAmount());
        assertEquals(2, response.getItems().size());
    }

    @Test
    void createOrder_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        CreateOrderRequest request = createOrderRequest();
        request.setUserId(99L);

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.createOrder(request));
    }

    @Test
    void createOrder_variantNotFound_throwsInvalidOrder() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

        CreateOrderRequest request = createOrderRequest();
        request.getItems().get(0).setVariantId(999L);

        assertThrows(InvalidOrderException.class,
                () -> orderService.createOrder(request));
    }

    @Test
    void createOrder_insufficientStock_throwsInvalidOrder() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        ProductVariant variant = variant(5L, 2);
        when(productVariantRepository.findById(5L)).thenReturn(Optional.of(variant));

        CreateOrderRequest request = createOrderRequest();
        request.getItems().get(0).setQuantity(5);

        assertThrows(InvalidOrderException.class,
                () -> orderService.createOrder(request));
    }

    @Test
    void createOrder_inactiveVariant_throwsInvalidOrder() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        ProductVariant variant = variant(5L, 10);
        variant.setIsActive(false);
        when(productVariantRepository.findById(5L)).thenReturn(Optional.of(variant));

        assertThrows(InvalidOrderException.class,
                () -> orderService.createOrder(createOrderRequest()));
    }

    // ---------- get order by id (ownership) ----------

    @Test
    void getOrderById_success_returnsResponse() {
        Order order = order(1L, user(1L), OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1L, 1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
    }

    @Test
    void getOrderById_otherUser_throwsForbidden() {
        Order order = order(1L, user(1L), OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(ForbiddenException.class,
                () -> orderService.getOrderById(1L, 2L));
    }

    @Test
    void getOrderById_notFound_throws() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrderById(404L, 1L));
    }

    // ---------- list orders ----------

    @Test
    void getUserOrders_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(order(1L, user(1L), OrderStatus.PENDING)));

        List<OrderResponse> responses = orderService.getUserOrders(1L);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
    }

    @Test
    void getAllOrders_success() {
        when(orderRepository.findAll())
                .thenReturn(List.of(
                        order(1L, user(1L), OrderStatus.CONFIRMED),
                        order(2L, user(2L), OrderStatus.SHIPPED)));

        List<OrderResponse> responses = orderService.getAllOrders();

        assertEquals(2, responses.size());
    }

    // ---------- paged admin list ----------

    @Test
    void getAllOrdersFiltered_returnsPagedResponse() {
        OrderFilterRequest filter = new OrderFilterRequest();
        Order order = order(1L, user(1L), OrderStatus.PENDING);
        order.getItems().add(orderItem(5L, 2));
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<AdminOrderSummaryResponse> response =
                orderService.getAllOrdersFiltered(filter);

        assertNotNull(response);
        assertEquals(1, response.getData().size());
        AdminOrderSummaryResponse summary = response.getData().get(0);
        assertEquals(1L, summary.getId());
        assertEquals(1L, summary.getUserId());
        assertEquals("Chan Dara", summary.getUserName());
        assertEquals("dara@example.com", summary.getUserEmail());
        assertEquals(OrderStatus.PENDING, summary.getStatus());
        assertEquals(1, summary.getItemCount());
    }

    @Test
    void getAllOrdersFiltered_nullFilter_usesDefaults() {
        Page<Order> page = new PageImpl<>(List.of());
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<AdminOrderSummaryResponse> response =
                orderService.getAllOrdersFiltered(null);

        assertNotNull(response);
        assertTrue(response.getData().isEmpty());
        assertEquals(0, response.getTotalElements());
    }

    // ---------- admin single order ----------

    @Test
    void getOrderByIdAdmin_success() {
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order(5L, user(1L), OrderStatus.DELIVERED)));

        OrderResponse response = orderService.getOrderByIdAdmin(5L);

        assertEquals(5L, response.getId());
        assertEquals(OrderStatus.DELIVERED, response.getStatus());
    }

    @Test
    void getOrderByIdAdmin_notFound_throws() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrderByIdAdmin(404L));
    }

    // ---------- update status ----------

    @Test
    void updateOrderStatus_success() {
        Order order = order(1L, user(1L), OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
    }

    @Test
    void updateOrderStatus_cancelledOrderCannotChange_throws() {
        Order order = order(1L, user(1L), OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderException.class,
                () -> orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED));
    }

    @Test
    void updateOrderStatus_cancelRestoresStock() {
        Order order = order(1L, user(1L), OrderStatus.PENDING);
        order.getItems().add(orderItem(5L, 2));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductVariant variant = order.getItems().get(0).getVariant();
        variant.setStock(3);

        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals(5, variant.getStock());
        verify(productVariantRepository).save(variant);
    }

    // ---------- cancel order (user) ----------

    @Test
    void cancelOrder_success_setsReasonAndRestoresStock() {
        Order order = order(1L, user(1L), OrderStatus.PENDING);
        order.getItems().add(orderItem(5L, 2));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductVariant variant = order.getItems().get(0).getVariant();
        variant.setStock(4);

        OrderResponse response = orderService.cancelOrder(1L, 1L, "Changed my mind");

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals("Changed my mind", response.getCancelReason());
        assertEquals(6, variant.getStock());
    }

    @Test
    void cancelOrder_otherUser_throwsForbidden() {
        Order order = order(1L, user(1L), OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(ForbiddenException.class,
                () -> orderService.cancelOrder(1L, 9L, "no"));
    }

    @Test
    void cancelOrder_alreadyCancelled_throws() {
        Order order = order(1L, user(1L), OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderException.class,
                () -> orderService.cancelOrder(1L, 1L, "again"));
    }

    // ---------- cancel order (admin) ----------

    @Test
    void cancelOrderAdmin_success_noOwnershipCheck() {
        Order order = order(1L, user(2L), OrderStatus.PROCESSING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOrderAdmin(1L, "Policy violation");

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals("Policy violation", response.getCancelReason());
    }

    @Test
    void cancelOrderAdmin_alreadyCancelled_throws() {
        Order order = order(1L, user(2L), OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderException.class,
                () -> orderService.cancelOrderAdmin(1L, "again"));
    }

    // ---------- edge cases ----------

    @Test
    void updateOrderStatus_notFound_throws() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.updateOrderStatus(404L, OrderStatus.SHIPPED));
    }

    @Test
    void cancelOrder_notFound_throws() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.cancelOrder(404L, 1L, "no"));
    }

    @Test
    void cancelOrderAdmin_notFound_throws() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.cancelOrderAdmin(404L, "no"));
    }

    @Test
    void getUserOrders_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getUserOrders(99L));
    }

    @Test
    void getUserOrders_emptyList() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        List<OrderResponse> responses = orderService.getUserOrders(1L);

        assertTrue(responses.isEmpty());
    }

    @Test
    void getAllOrders_emptyList() {
        when(orderRepository.findAll()).thenReturn(List.of());

        List<OrderResponse> responses = orderService.getAllOrders();

        assertTrue(responses.isEmpty());
    }

    @Test
    void cancelOrder_restoresStockForMultipleItems() {
        Order order = order(1L, user(1L), OrderStatus.PENDING);
        order.getItems().add(orderItem(5L, 2));
        order.getItems().add(orderItem(6L, 3));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductVariant variant5 = order.getItems().get(0).getVariant();
        variant5.setStock(3);
        ProductVariant variant6 = order.getItems().get(1).getVariant();
        variant6.setStock(7);

        OrderResponse response = orderService.cancelOrder(1L, 1L, "Changed mind");

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals(5, variant5.getStock());
        assertEquals(10, variant6.getStock());
    }
}