package com.example.spring_boot_project_api.dto.request.cs;

import com.example.spring_boot_project_api.enums.TicketPriority;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTicketPriorityRequest {
    @NotNull(message = "Priority is required")
    private TicketPriority priority;
}