package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.order.CreateOrderRequest;
import com.example.spring_boot_project_api.dto.request.order.OrderFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.order.AdminOrderSummaryResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.dto.request.order.CheckoutRequest;
import com.example.spring_boot_project_api.dto.response.order.CheckoutResponse;


public interface OrderService {
    //Create a new order
    OrderResponse createOrder(CreateOrderRequest request);

    //Get order by id, with ownership check
    OrderResponse getOrderById(Long orderId, Long userId);

    //Checkout an order, with payment processing
    CheckoutResponse checkout(CheckoutRequest request);

    //Get all orders of a user
    List<OrderResponse> getUserOrders(Long userId);

    //Get all orders (admin)
    List<OrderResponse> getAllOrders();

    //Get all orders with filtering and pagination (admin)
    PagedResponse<AdminOrderSummaryResponse> getAllOrdersFiltered(OrderFilterRequest filter);

    //Get order by id (admin, no ownership check)
    OrderResponse getOrderByIdAdmin(Long orderId);

    //Update order status
    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);

    //Cancel an order, with ownership check
    OrderResponse cancelOrder(Long orderId, Long userId, String reason);

    //Cancel an order (admin, no ownership check)
    OrderResponse cancelOrderAdmin(Long orderId, String reason);
}