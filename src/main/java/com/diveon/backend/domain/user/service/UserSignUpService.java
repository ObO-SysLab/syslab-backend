package com.diveon.backend.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import com.diveon.backend.domain.user.entity.User;


import com.diveon.backend.domain.user.dto.AuthSignUpRequest;
import com.diveon.backend.domain.user.repository.UserRepository;
// import com.diveon.backend.global.exception.InvalidCredentialsException;

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
        //existsById()를 사용하여 검색만 진행한다.
        //현재 @NonNUll을 이용하라는 것 같음, 아마 검증이 안되는 상태 인 듯 함.
        if(userRepository.existsById(signup_request.getUserId())){
            //이미 존재하는 아이디 임. 따라서 해당 아이디로 신규 가입 불가능
            return false;
        }

        //인코드를 통해서 비밀번호 암호화 진행
        String encodedPassword = passwordEncoder.encode(signup_request.getUserPassword());

        //새로운 유저 객체 생성
        //DTO 정보에서 Entity 정보로 변경
        // 필수값이 아닌 값들은 DTO에서 null로 넘어와도, 생성자를 통해 그대로 생성된다.
        User newUser = new User(
            signup_request.getUserId(),
            encodedPassword, 
            signup_request.getUserNickName(),
            signup_request.getUserEmail(),
            signup_request.getUserBelong(),
            signup_request.getUserInterest()
        );
        

        userRepository.save(newUser);
        return true;
        
        //성공 시나리오
        //사용자 존재 확인 이후- 구현
        // 비밀번호 암호화, passwordEncoder를 사용하면 될 듯하다  - 구현
        //이후 나머지 usertable에 대하여, 할당하고(not NULL 컬럼에 대해서), - 구현
        // 레포지 토리 통해서 새로운 row 삽입 - 구현
        // 이후 true 반환 -구현

        


        //사용자 가입- 즉, 계정 생성에 있어서 문제가 없다면 true, 존재한다면 fasle
    }
}
