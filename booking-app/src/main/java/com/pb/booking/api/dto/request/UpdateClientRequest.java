package com.pb.booking.api.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateClientRequest {

    private String name;

    private String phone;

    @Email
    private String email;
}
