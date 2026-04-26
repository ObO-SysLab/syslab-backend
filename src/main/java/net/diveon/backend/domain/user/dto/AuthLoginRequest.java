package net.diveon.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthLoginRequest {

    @NotBlank
    private String login_id;

    @NotBlank
    private String password;

    public String getLogin_id() { return login_id; }
    public String getPassword() { return password; }
}
