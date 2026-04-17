package net.diveon.backend.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import net.diveon.backend.domain.user.entity.User;


import net.diveon.backend.domain.user.dto.AuthSignUpRequest;
import net.diveon.backend.domain.user.repository.UserRepository;
// import net.diveon.backend.global.exception.InvalidCredentialsException;
import net.diveon.backend.global.exception.UserAlreadyExistException;

import java.util.Objects;


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
    // 수정사항 - 04.18 - 안상완 - 표준 에러 출력을 위해, 기존의 boolean 반환에서 exception throw로 변경
    public void signup(AuthSignUpRequest signup_request){



        // //사용자 엔티티를, ID를 통해서 전부 가져온다.
        // //existsById()를 사용하여 검색만 진행한다.
        // 이 아래의 한줄은 우리가 의도한 표준 에러가 아니라서 차후에 수정이 필요할 것임 2026.04.18 - 안상완
        // 일반 @valid 어노테이션으로 걸러지는거 확인은 했는데, 일단은 살려두겟습니다 - 2026.04.18 - 안상완
        String userId = Objects.requireNonNull(signup_request.getUserId(), "아이디는 필수 입력값입니다.");

        //인코드를 통해서 비밀번호 암호화 진행
        //수정사항 04.08 dto 변경되면서, getter 메서드 변화로 인한 수정
        String encodedPassword = passwordEncoder.encode(signup_request.getPassword());

        // 만약 아이디가 존재한다면, exception 발생
        if(userRepository.existsById(userId)){
            //이미 존재하는 아이디 임. 따라서 해당 아이디로 신규 가입 불가능
            throw new UserAlreadyExistException();
        }
        //새로운 유저 객체 생성
        //DTO 정보에서 Entity 정보로 변경
        // 필수값이 아닌 값들은 DTO에서 null로 넘어와도, 생성자를 통해 그대로 생성된다.
        //수정사항 04.08 dto가 변경되면서 수정
        User newUser = new User(
            signup_request.getUserId(),
            encodedPassword, 
            signup_request.getNickName(),
            signup_request.getEmail(),
            signup_request.getBelong(),
            signup_request.getInterest()
        );
        userRepository.save(newUser);
        return;
    }
}
