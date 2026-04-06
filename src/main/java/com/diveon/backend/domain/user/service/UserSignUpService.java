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


    /**<pre>
     * 사용자 가입 함수 - 계정생성
     * 계정 생성이 되었다면 true, 모든 생성의 반대 되는상황(오류,불가,기존재 등)에는 false를 반환.
     * 아마 충돌나면 무조건 익셉션이니, 변수 받는 쪽에서는 false를 기본값으로 사용할 것.
     * </pre>
     * 
     * @param signup_request
     * @return boolen
     */
    public boolean signup(AuthSignUpRequest signup_request){

        //사용자 엔티티를, ID를 통해서 전부 가져온다.
        //아마 추가적인 조치가 필요할 것으로 보임(사용자가 없는순간 익셉션으로 추정됨)
        User user = userRepository.findById(signup_request.getUserId()).orElseThrow(InvalidCredentialsException::new);
        //이거 바로 위의 예외처리 안해주면 붉은색 오류나오는것을 보니 추가 확인 필요.


        //성공 시나리오
        //사용자 존재 확인 이후
        // 비밀번호 암호화, passwordEncoder를 사용하면 될 듯하다 
        //이후 나머지 usertable에 대하여, 할당하고(not NULL 컬럼에 대해서),
        // 레포지 토리 통해서 새로운 row 삽입
        // 이후 true 반환

        


        //사용자 가입- 즉, 계정 생성에 있어서 문제가 없다면 true, 존재한다면 fasle
        if(true) {
            return true;
        }else{
            return false;
        }
    }


}
