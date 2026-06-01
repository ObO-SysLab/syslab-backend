package net.diveon.backend.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class PasswordResetRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?])[^\\s]{8,16}$",
             message = "비밀번호는 8~16자이며, 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.")
    private String newPassword;

    public String getEmail() { return email; }
    public String getNewPassword() { return newPassword; }
}
