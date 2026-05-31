package net.diveon.backend.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class EmailSendRequest {

    @NotBlank
    @Email
    private String email;

    public String getEmail() { return email; }
}
