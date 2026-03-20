package com.foodflow.order_service.common.exception;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private String timestamp;
    private Map<String, String> details;
}