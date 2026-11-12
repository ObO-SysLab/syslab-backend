package net.diveon.backend.domain.user.service;

import net.diveon.backend.domain.user.dto.PasswordUpdateRequest;
import net.diveon.backend.domain.user.dto.ProfileShowResponse;
import net.diveon.backend.domain.user.dto.ProfileUpdateRequest;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.InvalidCredentialsException;
import net.diveon.backend.global.exception.UserNotFoundException;
import net.diveon.backend.global.s3.ImageUploadService;
import net.diveon.backend.global.util.ImageFileValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageUploadService imageUploadService;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           ImageUploadService imageUploadService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.imageUploadService = imageUploadService;
    }

    public ProfileShowResponse getProfile(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return new ProfileShowResponse(new ProfileShowResponse.UserInfo(user));
    }

    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        user.updateProfile(
                request.getNickname(),
                request.getSelfComment(),
                request.getBelong(),
                request.getInterest()
        );
    }

    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional
    public String uploadProfileImage(Long userId, MultipartFile image) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        ImageFileValidator.validateAndGetExtension(image);
        String imageUrl = imageUploadService.upload(profileImageKey(userId), image);

        user.updateProfileImage(imageUrl);
        return imageUrl;
    }

    // 프로필 사진을 지운다. 업로드한 S3 객체를 먼저 지우고(없어도 성공), DB 주소를 비운다.
    // 일부 화면은 사용자 번호로 S3 주소를 직접 만들어 쓰므로 객체까지 지워야 옛 사진이 남지 않는다.
    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        imageUploadService.delete(profileImageKey(userId));
        user.updateProfileImage(null);
    }

    public static String profileImageKey(Long userId) {
        return "profiles/users/" + userId;
    }
}
