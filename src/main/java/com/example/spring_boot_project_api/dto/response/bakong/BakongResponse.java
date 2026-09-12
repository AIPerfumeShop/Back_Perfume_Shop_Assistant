package com.example.spring_boot_project_api.dto.response.bakong;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BakongResponse(
        int responseCode,
        String responseMessage,
        Integer errorCode,
        Object data) {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public boolean isSuccess() {
        return responseCode == 0;
    }
}