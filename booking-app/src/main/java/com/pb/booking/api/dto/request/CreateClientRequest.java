package com.pb.booking.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateClientRequest {

    @NotBlank
    private String name;

    private String phone;

    @Email
    private String email;
}
