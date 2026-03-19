package com.pb.booking.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ClientResponse {

    private UUID id;
    private String name;
    private String phone;
    private String email;
    private int visitCount;
    private Instant createdAt;
    private Instant updatedAt;
}
