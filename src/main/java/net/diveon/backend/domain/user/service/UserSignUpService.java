package net.diveon.backend.domain.user.service;

import net.diveon.backend.domain.user.dto.AuthSignUpRequest;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.EmailNotVerifiedException;
import net.diveon.backend.global.exception.UserAlreadyExistException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UserSignUpService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserSignUpService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public boolean isLoginIdAvailable(String loginId) {
        return !userRepository.existsByLoginId(loginId);
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    public void signup(AuthSignUpRequest signup_request) {
        if (!emailService.isVerified(signup_request.getEmail())) {
            throw new EmailNotVerifiedException();
        }

        String loginId = Objects.requireNonNull(signup_request.getLoginId(), "아이디는 필수 입력값입니다.");
        String encodedPassword = passwordEncoder.encode(signup_request.getPassword());

        if (userRepository.existsByLoginId(loginId)) {
            throw new UserAlreadyExistException();
        }

        User newUser = new User(
            signup_request.getLoginId(),
            encodedPassword,
            signup_request.getNickname(),
            signup_request.getEmail(),
            signup_request.getBelong(),
            signup_request.getInterest()
        );
        userRepository.save(newUser);
        emailService.clearVerified(signup_request.getEmail());
    }
}
