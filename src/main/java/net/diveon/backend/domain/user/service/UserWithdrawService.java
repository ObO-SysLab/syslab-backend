package net.diveon.backend.domain.user.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import net.diveon.backend.domain.group.entity.GroupUser;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.group.service.GroupService;
import net.diveon.backend.domain.notification.service.NotificationService;
import net.diveon.backend.domain.store.service.StoreService;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.DomainRuleException;
import net.diveon.backend.global.s3.ImageUploadService;
import net.diveon.backend.global.security.WithdrawnUsers;

// 계정 탈퇴: 개인정보를 지우고 계정을 비활성화한다. 문제·제출·댓글·대회 기록은 '탈퇴한 사용자' 이름으로 남는다.
@Service
public class UserWithdrawService {

    private static final Logger log = LoggerFactory.getLogger(UserWithdrawService.class);
    static final String NICKNAME_PREFIX = "탈퇴한 사용자#";

    private final UserRepository users;
    private final GroupUserRepository groupUsers;
    private final GroupService groupService;
    private final NotificationService notifications;
    private final StoreService store;
    private final ImageUploadService images;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final WithdrawnUsers withdrawnUsers;
    private final JdbcTemplate jdbc;
    private final long accessTokenSeconds;

    public UserWithdrawService(UserRepository users, GroupUserRepository groupUsers, GroupService groupService,
                               NotificationService notifications, StoreService store, ImageUploadService images,
                               PasswordEncoder passwordEncoder, UserService userService, WithdrawnUsers withdrawnUsers,
                               JdbcTemplate jdbc,
                               @Value("${auth.jwt.token-validity-in-seconds}") long accessTokenSeconds) {
        this.users = users;
        this.groupUsers = groupUsers;
        this.groupService = groupService;
        this.notifications = notifications;
        this.store = store;
        this.images = images;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.withdrawnUsers = withdrawnUsers;
        this.jdbc = jdbc;
        this.accessTokenSeconds = accessTokenSeconds;
    }

    protected LocalDateTime now() {
        return LocalDateTime.now();
    }

    @Transactional
    public void withdraw(long userId, String confirmNickname) {
        User user = users.lockVmOwner(userId)
                .orElseThrow(() -> new DomainRuleException(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다."));
        if (user.isDeleted()) throw new DomainRuleException(HttpStatus.NOT_FOUND, "이미 탈퇴한 계정입니다.");
        if (confirmNickname == null || !confirmNickname.trim().equals(user.getNickname())) {
            throw new DomainRuleException(HttpStatus.BAD_REQUEST, "닉네임이 일치하지 않습니다.");
        }

        // 1) 막는 경우: 다른 멤버가 있는 그룹의 그룹장, 아직 끝나지 않은 대회의 주최자
        List<GroupUser> memberships = groupUsers.findAllByUserId(userId);
        List<Long> soloGroups = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        for (GroupUser membership : memberships) {
            if (membership.getRole() != GroupUser.GroupRole.LEADER) continue;
            long groupId = membership.getGroup().getId();
            if (groupUsers.countByGroupId(groupId) > 1) blockers.add("그룹 '" + membership.getGroup().getTitle() + "'");
            else soloGroups.add(groupId);
        }
        blockers.addAll(jdbc.query("SELECT title FROM contest WHERE created_by = ? AND end_time > ? ORDER BY id",
                (rs, i) -> "대회 '" + rs.getString(1) + "'", userId, Timestamp.valueOf(now())));
        if (!blockers.isEmpty()) {
            throw new DomainRuleException(HttpStatus.CONFLICT,
                    String.join(", ", blockers) + "을(를) 먼저 정리해 주세요. 그룹은 그룹장을 넘기거나 삭제하고, 대회는 끝나거나 삭제되어야 탈퇴할 수 있습니다.");
        }

        // 2) 혼자인 그룹은 함께 지우고, 일반 멤버로 속한 그룹과 대기 중인 가입 신청에서 빠진다.
        for (Long groupId : soloGroups) groupService.deleteGroup(groupId, userId);
        jdbc.update("DELETE FROM group_user WHERE user_id = ? AND role = 'MEMBER'", userId);
        jdbc.update("DELETE FROM group_assign_request WHERE user_id = ? AND status = 'PENDING'", userId);

        // 3) 알림·착용 정리, 프로필 사진 삭제(실패하면 탈퇴 전체를 되돌린다)
        notifications.deleteAllFor(userId);
        store.unequipAll(userId);
        images.delete(ProfileService.profileImageKey(userId));

        // 4) 개인정보 삭제와 이름 바꾸기
        String nickname = NICKNAME_PREFIX + userId;
        if (users.existsByNickname(nickname)) nickname = nickname + "-" + UUID.randomUUID().toString().substring(0, 4);
        String loginId = "deleted_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8);
        user.withdraw(loginId, nickname, passwordEncoder.encode(UUID.randomUUID().toString()), now());

        // 5) 커밋 뒤 로그인 차단: 리프레시 토큰 삭제, 남은 액세스 토큰 차단
        afterCommit(() -> {
            try {
                userService.logout(String.valueOf(userId));
                withdrawnUsers.mark(userId, Duration.ofSeconds(accessTokenSeconds));
            } catch (RuntimeException e) {
                log.warn("탈퇴 후 토큰 정리 실패 userId={}: {}", userId, e.getMessage());
            }
        });
    }

    private static void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
