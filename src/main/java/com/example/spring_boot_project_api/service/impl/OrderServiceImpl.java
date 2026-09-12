package com.example.spring_boot_project_api.service.impl;

import com.example.spring_boot_project_api.dto.request.order.CheckoutRequest;
import com.example.spring_boot_project_api.dto.request.order.OrderFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.order.AdminOrderSummaryResponse;
import com.example.spring_boot_project_api.dto.response.order.CheckoutResponse;
import com.example.spring_boot_project_api.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.order.CreateOrderRequest;
import com.example.spring_boot_project_api.dto.request.order.OrderItemRequest;
import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.exception.ForbiddenException;
import com.example.spring_boot_project_api.exception.InvalidOrderException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.OrderMapper;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.OrderItem;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.ProductVariantRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.specification.OrderSpecification;
import com.example.spring_boot_project_api.service.OrderService;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    //Window (seconds) within which an identical checkout from the same user
    //is considered a duplicate and returns the existing order.
    private static final long DUPLICATE_GUARD_WINDOW_SECONDS = 30;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PaymentRepository paymentRepository;
    private final OrderMapper orderMapper;
    private final PaymentService paymentService;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProductVariantRepository productVariantRepository,
            PaymentRepository paymentRepository,
            OrderMapper orderMapper,
            PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productVariantRepository = productVariantRepository;
        this.paymentRepository = paymentRepository;
        this.orderMapper = orderMapper;
        this.paymentService = paymentService;
    }

    //Create order
    @Override
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setPhone(request.getPhone());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = buildOrderItem(itemRequest, order);
            order.getItems().add(item);
            totalAmount = totalAmount.add(item.getSubtotal());
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toResponse(savedOrder);
    }

    //Get order by id
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = findOrder(orderId);

        checkOwnership(order, userId);

        return orderMapper.toResponse(order);
    }

    //Get all orders of a user
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return orderMapper.toResponseList(
                orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
        );
    }

    //Get all orders (admin)
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderMapper.toResponseList(
                orderRepository.findAll()
        );
    }

    //Get all orders with filtering and pagination (admin)
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AdminOrderSummaryResponse> getAllOrdersFiltered(OrderFilterRequest filter) {
        if (filter == null) {
            filter = new OrderFilterRequest();
        }

        Page<Order> orders = orderRepository.findAll(
                OrderSpecification.fromFilter(filter),
                filter.toPageRequest());

        List<AdminOrderSummaryResponse> content = orders.getContent().stream()
                .map(orderMapper::toAdminSummaryResponse)
                .toList();

        return new PagedResponse<>(
                content,
                orders.getTotalElements(),
                orders.getTotalPages(),
                orders.getNumber(),
                orders.getSize());
    }

    //Get order by id (admin)
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAdmin(Long orderId) {
        return orderMapper.toResponse(findOrder(orderId));
    }

    //Update order status
    @Override
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = findOrder(orderId);

        if (order.getStatus() == OrderStatus.CANCELLED &&
                status != OrderStatus.CANCELLED) {
            throw new InvalidOrderException(
                    "Cancelled order cannot change status");
        }

        //Restore stock if order is being cancelled now
        if (status == OrderStatus.CANCELLED &&
                order.getStatus() != OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        order.setStatus(status);

        return orderMapper.toResponse(orderRepository.save(order));
    }

    //Cancel an order
    @Override
    public OrderResponse cancelOrder(Long orderId, Long userId, String reason) {
        Order order = findOrder(orderId);

        checkOwnership(order, userId);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderException("Order is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);

        restoreStock(order);

        return orderMapper.toResponse(orderRepository.save(order));
    }

    //Cancel an order (admin)
    @Override
    public OrderResponse cancelOrderAdmin(Long orderId, String reason) {
        Order order = findOrder(orderId);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderException("Order is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);

        restoreStock(order);

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public CheckoutResponse checkout(Long userId, CheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        //Idempotency guard: if the same user just placed an identical
        //checkout, return the existing pending order instead of creating
        //a duplicate order + payment (double-tap protection).
        Order recent = findRecentDuplicate(userId, request);
        if (recent != null) {
            Payment payment = paymentRepository.findByOrderId(recent.getId())
                    .orElse(null);
            if (payment != null) {
                return toCheckoutResponse(payment, recent);
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setPhone(request.getPhone());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = buildOrderItem(itemRequest, order);
            order.getItems().add(item);
            totalAmount = totalAmount.add(item.getSubtotal());
        }
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        // KHQR payments go through the Bakong gateway: generate a QR code and
        // leave both order and payment PENDING until the customer scans & pays.
        if (isKHQR(request.getPaymentMethod())) {
            Payment payment =
                    paymentService.initBakongPayment(savedOrder);
            return toCheckoutResponse(payment, savedOrder);
        }

        //Initialize payment
        Payment payment =
                paymentService.initPayment(savedOrder, request.getPaymentMethod());
        boolean paymentSuccess =
                paymentService.processPayment(payment.getId());

        //Rollback if payment failed
        if (!paymentSuccess) {
            restoreStock(savedOrder);
            savedOrder.setStatus(OrderStatus.CANCELLED);
            savedOrder.setCancelReason("Payment failed");
            orderRepository.save(savedOrder);
        }

        return toCheckoutResponse(payment, savedOrder);
    }

    private CheckoutResponse toCheckoutResponse(Payment payment, Order order) {
        CheckoutResponse response = new CheckoutResponse();
        response.setOrderId(order.getId());
        response.setTotalAmount(order.getTotalAmount());
        response.setOrderStatus(order.getStatus().name());
        response.setPaymentId(payment.getId());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setPaymentStatus(payment.getStatus());
        response.setTransactionId(payment.getTransactionId());
        response.setQr(payment.getQrText());
        response.setMd5(payment.getMd5());
        return response;
    }

    //Find a recent pending order for the same user with the same shipping
    //details and items, within the duplicate-guard window.
    private Order findRecentDuplicate(Long userId, CheckoutRequest request) {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusSeconds(DUPLICATE_GUARD_WINDOW_SECONDS);

        List<Order> recent = orderRepository
                .findTop10ByUserIdAndCreatedAtAfterAndStatusNotOrderByCreatedAtDesc(
                        userId, cutoff, OrderStatus.CANCELLED);

        for (Order candidate : recent) {
            if (candidate.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }
            if (!sameShipping(request, candidate)) {
                continue;
            }
            if (sameItems(request, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean sameShipping(CheckoutRequest request, Order candidate) {
        return java.util.Objects.equals(
                        request.getShippingAddress(), candidate.getShippingAddress())
                && java.util.Objects.equals(request.getPhone(), candidate.getPhone());
    }

    private boolean sameItems(CheckoutRequest request, Order candidate) {
        List<OrderItem> existingItems = candidate.getItems();
        if (existingItems == null
                || existingItems.size() != request.getItems().size()) {
            return false;
        }

        Map<Long, Integer> orderedQuantities = new HashMap<>();
        for (OrderItem item : existingItems) {
            Long variantId = item.getVariant() != null
                    ? item.getVariant().getId()
                    : null;
            orderedQuantities.put(variantId,
                    item.getQuantity());
        }

        for (OrderItemRequest itemRequest : request.getItems()) {
            Integer quantity = orderedQuantities.get(itemRequest.getVariantId());
            if (quantity == null || quantity.intValue() != itemRequest.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    private boolean isKHQR(String paymentMethod) {
        return paymentMethod != null
                && paymentMethod.trim().equalsIgnoreCase("KHQR");
    }

    //Build a single order item from request, snapshotting the variant data
    private OrderItem buildOrderItem(OrderItemRequest itemRequest, Order order) {
        ProductVariant variant = productVariantRepository
                .findById(itemRequest.getVariantId())
                .orElseThrow(() ->
                        new InvalidOrderException(
                                "Product variant not found with ID : "
                                        + itemRequest.getVariantId()));

        if (!Boolean.TRUE.equals(variant.getIsActive())) {
            throw new InvalidOrderException(
                    "Product variant is not available : "
                            + itemRequest.getVariantId());
        }

        int quantity = itemRequest.getQuantity();
        if (variant.getStock() == null || variant.getStock() < quantity) {
            throw new InvalidOrderException(
                    "Insufficient stock for product : "
                            + variant.getProduct().getName());
        }

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setVariant(variant);
        item.setProductName(variant.getProduct().getName());

        String brandName = variant.getProduct().getBrand() != null
                ? variant.getProduct().getBrand().getName()
                : null;
        item.setBrand(brandName);

        item.setVariantSize(String.valueOf(variant.getSizeMl()) + "ml");
        item.setQuantity(quantity);
        item.setUnitPrice(variant.getPrice());
        item.setSubtotal(
                variant.getPrice().multiply(BigDecimal.valueOf(quantity))
        );

        //Decrease stock
        variant.setStock(variant.getStock() - quantity);
        productVariantRepository.save(variant);

        return item;
    }

    //Restore stock when an order is cancelled
    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getVariant();
            if (variant == null) {
                continue;
            }
            variant.setStock(variant.getStock() + item.getQuantity());
            productVariantRepository.save(variant);
        }
    }

    //Find an order or throw 404
    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with ID : " + orderId));
    }

    //Ownership check (de-facto authorization, no JWT yet)
    private void checkOwnership(Order order, Long userId) {
        if (!order.getUser().getId().equals(userId)) {
            throw new ForbiddenException(
                    "You do not have access to this order");
        }
    }
}