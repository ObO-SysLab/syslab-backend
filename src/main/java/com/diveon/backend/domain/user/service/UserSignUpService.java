package com.diveon.backend.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import com.diveon.backend.domain.user.entity.User;


import com.diveon.backend.domain.user.dto.AuthSignUpRequest;
import com.diveon.backend.domain.user.repository.UserRepository;
import com.diveon.backend.global.exception.InvalidCredentialsException;

import org.springframework.stereotype.Service;


/**
 * <pre>
 * 유저 가입을 위한 서비스
 * 필요시 UserService와 합칠 것.
 * 현재 서비스는  public boolean signup(AuthSignUpRequest signup_request) 를통해서
 * 계정 생성이 되었다면 true, 모든 생성의 반대 되는상황(오류,불가,기존재 등)에는 false를 반환.
 * 아마 충돌나면 무조건 익셉션이니, 변수 받는 쪽에서는 false를 기본값으로 사용할 것.
 * </pre>
 */
@Service
public class UserSignUpService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public UserSignUpService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean signup(AuthSignUpRequest signup_request){
        User user = userRepository.findById(signup_request.getUserId())
                .orElseThrow(InvalidCredentialsException::new);
                //이거 바로 위의 예외처리 안해주면 씹창난다.
        



        //사용자 가입- 즉, 계정 생성에 있어서 문제가 없다면 true, 존재한다면 fasle
        if(true) {
            return true;
        }else{
            return false;
        }
    }


}
