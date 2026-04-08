package net.diveon.backend.domain.user.controller;

import net.diveon.backend.domain.user.dto.AuthLoginRequest;
import net.diveon.backend.domain.user.dto.AuthLoginResponse;
import net.diveon.backend.domain.user.dto.AuthSignUpRequest;
import net.diveon.backend.domain.user.service.UserService;
import net.diveon.backend.domain.user.service.UserSignUpService;
import net.diveon.backend.global.response.ApiResponse;
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
    private final UserSignUpService userSignUpService;
    public UserController(UserService userService, UserSignUpService userSignUpService) {
        this.userService = userService;
        this.userSignUpService = userSignUpService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> login(@Valid @RequestBody AuthLoginRequest request) {
        AuthLoginResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공하였습니다.", response));
    }

    @PostMapping("/signup")
    //제네릭에는 참조타임만 가능, void x, Void o
    public ResponseEntity<ApiResponse<Void>> postMethodName(@RequestBody AuthSignUpRequest signup_requset) {
        //TODO: 현재는 껍데기(인풋, 반환 선언등)만 구현됨, 반드시 중간의 서비스를 부르고 처리하는 과정 구현 필요 2026.04.07 - 해결시 지울 것
        //TODO: 입력된 requset에 대한 검증이 이루어 지지 아니함, 따라서 위의 login과 같이 검증할수 있도록 변경 필요 - 2026.04.07 - 해결시 지울 것.
        boolean isSignedUp = userSignUpService.signup(signup_requset);
        
        if(isSignedUp){
            return ResponseEntity.ok(ApiResponse.success("회원가입에 성공했습니다.", null));
        }else{
            return ResponseEntity.ok(ApiResponse.success("회원가입에 실패했습니다.", null));
        }
    }
    
}
