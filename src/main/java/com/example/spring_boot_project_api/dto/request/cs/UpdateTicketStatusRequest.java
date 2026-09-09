package com.example.spring_boot_project_api.dto.request.cs;

import com.example.spring_boot_project_api.enums.TicketStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTicketStatusRequest {
    @NotNull(message = "Status is required")
    private TicketStatus status;
}