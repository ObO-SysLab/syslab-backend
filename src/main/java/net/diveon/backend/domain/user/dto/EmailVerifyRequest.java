package net.diveon.backend.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class EmailVerifyRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;

    public String getEmail() { return email; }
    public String getCode() { return code; }
}
