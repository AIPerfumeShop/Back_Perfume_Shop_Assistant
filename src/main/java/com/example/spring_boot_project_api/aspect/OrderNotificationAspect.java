package com.example.spring_boot_project_api.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.service.TelegramService;

@Aspect
@Component
public class OrderNotificationAspect {

    private final TelegramService telegramService;

    public OrderNotificationAspect(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @Around("execution(public com.example.spring_boot_project_api.dto.response.order.OrderResponse "
            + "com.example.spring_boot_project_api.service.impl.OrderServiceImpl.createOrder(..))")
    public Object notifyOnOrderCreated(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        if (result instanceof OrderResponse order) {
            telegramService.sendOrderNotification(order);
        }
        return result;
    }
}