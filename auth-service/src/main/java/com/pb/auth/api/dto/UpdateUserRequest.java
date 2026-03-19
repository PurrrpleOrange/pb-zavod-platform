package com.pb.auth.api.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Email(message = "Email must be valid")
    private String email;

    private String phone;

    private String status;
}
