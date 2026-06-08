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

        String extension = ImageFileValidator.validateAndGetExtension(image);
        String key = "profiles/" + userId + "." + extension;
        String imageUrl = imageUploadService.upload(key, image);

        user.updateProfileImage(imageUrl);
        return imageUrl;
    }
}
