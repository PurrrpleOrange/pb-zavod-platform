package com.pb.gateway.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayErrorResponse {

    private String code;
    private String message;
    private List<String> details;
    private String traceId;
    private Instant timestamp;
}
