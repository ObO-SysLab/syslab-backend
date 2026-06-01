package net.diveon.backend.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class PasswordResetRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String newPassword;

    public String getEmail() { return email; }
    public String getNewPassword() { return newPassword; }
}
