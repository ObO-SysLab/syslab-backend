package com.diveon.backend.domain.user.controller;

import com.diveon.backend.domain.user.dto.AuthLoginRequest;
import com.diveon.backend.domain.user.dto.AuthLoginResponse;
import com.diveon.backend.domain.user.dto.AuthSignUpRequest;
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

    @PostMapping("/signup")
    //제네릭에는 참조타임만 가능, void x, Void o
    public ResponseEntity<ApiResponse<Void>> postMethodName(@RequestBody AuthSignUpRequest sing_requset) {
        //TODO: 현재는 껍데기(인풋, 반환 선언등)만 구현됨, 반드시 중간의 서비스를 부르고 처리하는 과정 구현 필요 2026.04.07 - 해결시 지울 것
        
        return ResponseEntity.ok(ApiResponse.success("회원가입에 성공했습니다.", null));
    }
    
}
