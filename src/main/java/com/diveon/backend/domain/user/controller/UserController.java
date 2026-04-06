package com.diveon.backend.domain.user.controller;

import com.diveon.backend.domain.user.dto.AuthLoginRequest;
import com.diveon.backend.domain.user.dto.AuthLoginResponse;
import com.diveon.backend.domain.user.service.UserService;
import com.diveon.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> login(@Valid @RequestBody AuthLoginRequest request) {
        AuthLoginResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공하였습니다.", response));
    }
}
