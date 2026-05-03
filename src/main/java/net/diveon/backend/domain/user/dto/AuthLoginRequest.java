package net.diveon.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthLoginRequest {

    @NotBlank
    private String loginId;

    @NotBlank
    private String password;

    public String getLoginId() { return loginId; }
    public String getPassword() { return password; }
}
