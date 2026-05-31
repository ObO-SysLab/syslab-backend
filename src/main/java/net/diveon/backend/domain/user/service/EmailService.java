package net.diveon.backend.domain.user.service;

import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.EmailNotVerifiedException;
import net.diveon.backend.global.exception.EmailVerificationCodeInvalidException;
import net.diveon.backend.global.exception.UserAlreadyExistException;
import net.diveon.backend.global.exception.UserNotFoundException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String VERIFY_KEY_PREFIX = "email_verify:";
    private static final String VERIFIED_KEY_PREFIX = "email_verified:";
    private static final long CODE_TTL_MINUTES = 10;
    private static final long VERIFIED_TTL_MINUTES = 30;

    public EmailService(JavaMailSender mailSender, RedisTemplate<String, String> redisTemplate, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void sendVerificationCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistException();
        }
        String code = String.format("%06d", new Random().nextInt(999999));

        redisTemplate.opsForValue()
                .set(VERIFY_KEY_PREFIX + email, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("noreply@diveon.net");
        msg.setTo(email);
        msg.setSubject("[DiveOn] 이메일 인증 코드");
        msg.setText("인증 코드: " + code + "\n\n10분 내에 입력해주세요.");
        mailSender.send(msg);
    }

    public void verifyCode(String email, String code) {
        String stored = redisTemplate.opsForValue().get(VERIFY_KEY_PREFIX + email);
        if (stored == null || !stored.equals(code)) {
            throw new EmailVerificationCodeInvalidException();
        }
        redisTemplate.delete(VERIFY_KEY_PREFIX + email);
        redisTemplate.opsForValue()
                .set(VERIFIED_KEY_PREFIX + email, "true", VERIFIED_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public boolean isVerified(String email) {
        return "true".equals(redisTemplate.opsForValue().get(VERIFIED_KEY_PREFIX + email));
    }

    public void clearVerified(String email) {
        redisTemplate.delete(VERIFIED_KEY_PREFIX + email);
    }

    public void sendPasswordResetCode(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new UserNotFoundException();
        }
        String code = String.format("%06d", new Random().nextInt(999999));

        redisTemplate.opsForValue()
                .set(VERIFY_KEY_PREFIX + email, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("noreply@diveon.net");
        msg.setTo(email);
        msg.setSubject("[DiveOn] 비밀번호 재설정 인증 코드");
        msg.setText("인증 코드: " + code + "\n\n10분 내에 입력해주세요.");
        mailSender.send(msg);
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        if (!isVerified(email)) {
            throw new EmailNotVerifiedException();
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
        user.updatePassword(passwordEncoder.encode(newPassword));
        clearVerified(email);
    }

    public void sendLoginId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("noreply@diveon.net");
        msg.setTo(email);
        msg.setSubject("[DiveOn] 아이디 찾기");
        msg.setText("회원님의 아이디는 [" + user.getLoginId() + "] 입니다.");
        mailSender.send(msg);
    }
}
