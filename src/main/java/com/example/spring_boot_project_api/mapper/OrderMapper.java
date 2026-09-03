package com.example.spring_boot_project_api.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.order.OrderItemResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.OrderItem;

@Component
public class OrderMapper {

    //Order -> OrderResponse
    public OrderResponse toResponse(Order order){
        if (order == null) {
            return null;
        }
        OrderResponse response = new OrderResponse();

        response.setId(order.getId());
        if (order.getUser() != null) {
            response.setUserId(order.getUser().getId());
            response.setUserName(order.getUser().getName());
        }
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setShippingAddress(order.getShippingAddress());
        response.setPhone(order.getPhone());
        response.setCancelReason(order.getCancelReason());
        if (order.getItems() != null) {
            response.setItems(
                order.getItems().stream()
                    .map(this::toItemResponse)
                    .toList()
            );
        }
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }

    //OrderItem -> OrderItemResponse
    public OrderItemResponse toItemResponse(OrderItem item){
        if (item == null) {
            return null;
        }
        OrderItemResponse response = new OrderItemResponse();

        response.setId(item.getId());
        if (item.getVariant() != null) {
            response.setVariantId(item.getVariant().getId());
        }
        response.setProductName(item.getProductName());
        response.setBrand(item.getBrand());
        response.setVariantSize(item.getVariantSize());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setSubtotal(item.getSubtotal());
        return response;
    }

    //List<Order> -> List<OrderResponse>
    public List<OrderResponse> toResponseList(List<Order> orders){
        return orders.stream()
                .map(this::toResponse)
                .toList();
    }
}