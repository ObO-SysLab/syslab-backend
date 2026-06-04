package net.diveon.backend.domain.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.group.repository.GroupRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.UserExitNotAllowedException;
import net.diveon.backend.global.exception.UserNotFoundException;

@Service
public class UserExitService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    private final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final GroupRepository groupRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PasswordEncoder passwordEncoder;

    public UserExitService(UserRepository userRepository,
                           ProblemRepository problemRepository,
                           GroupRepository groupRepository,
                           ContestParticipantRepository contestParticipantRepository,
                           RedisTemplate<String, String> redisTemplate,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.groupRepository = groupRepository;
        this.contestParticipantRepository = contestParticipantRepository;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void exit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        validateCanExit(userId);

        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        anonymizeUser(user);
    }

    private void validateCanExit(Long userId) {
        if (problemRepository.existsByAuthorId(userId)) {
            throw new UserExitNotAllowedException("생성한 문제가 있는 사용자는 회원 탈퇴가 불가능합니다.");
        }

        if (groupRepository.existsByLeaderId(userId)) {
            throw new UserExitNotAllowedException("그룹 관리자인 사용자는 회원 탈퇴가 불가능합니다.");
        }

        if (contestParticipantRepository.existsByUserIdAndContestEndTimeAfter(userId, LocalDateTime.now())) {
            throw new UserExitNotAllowedException("종료되지 않은 대회에 참여 중인 사용자는 회원 탈퇴가 불가능합니다.");
        }
    }

    private void anonymizeUser(User user) {
        LocalDateTime now = LocalDateTime.now();
        int loginIdRandom = secureRandom.nextInt();
        int nicknameRandom = nextDifferentInt(loginIdRandom);
        int emailRandom = nextDifferentInt(loginIdRandom, nicknameRandom);
        int passwordRandom = secureRandom.nextInt();

        String anonymizedLoginId = hashSeed(user.getId(), now, loginIdRandom);
        String anonymizedNickname = hashSeed(user.getId(), now, nicknameRandom);
        String anonymizedEmail = hashSeed(user.getId(), now, emailRandom);
        String encodedPassword = passwordEncoder.encode(buildSeed(user.getId(), now, passwordRandom));

        user.exit(anonymizedLoginId, anonymizedNickname, anonymizedEmail, encodedPassword);
    }

    private int nextDifferentInt(int... existingValues) {
        int candidate;
        do {
            candidate = secureRandom.nextInt();
        } while (contains(existingValues, candidate));
        return candidate;
    }

    private boolean contains(int[] values, int target) {
        for (int value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }

    private String hashSeed(Long userId, LocalDateTime now, int randomValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(buildSeed(userId, now, randomValue).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private String buildSeed(Long userId, LocalDateTime now, int randomValue) {
        return userId + ":" + now + ":" + randomValue;
    }
}
