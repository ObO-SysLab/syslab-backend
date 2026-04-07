package com.syslab.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthLoginRequest {

    @NotBlank
    private String user_id;

    @NotBlank
    private String password;

    public String getUser_id() { return user_id; }
    public String getPassword() { return password; }
}
