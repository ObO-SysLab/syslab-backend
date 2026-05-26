package net.diveon.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public class PasswordUpdateRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    private String newPassword;

    public String getCurrentPassword() { return currentPassword; }
    public String getNewPassword() { return newPassword; }
}
