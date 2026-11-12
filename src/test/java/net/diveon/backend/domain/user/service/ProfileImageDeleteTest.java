package net.diveon.backend.domain.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.UserNotFoundException;
import net.diveon.backend.global.s3.ImageUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class ProfileImageDeleteTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ImageUploadService images = mock(ImageUploadService.class);
    private final ProfileService service = new ProfileService(users, mock(PasswordEncoder.class), images);

    private User user(String imageUrl) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 7L);
        ReflectionTestUtils.setField(user, "profileImgUrl", imageUrl);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void deletesUploadedObjectAndClearsUrl() {
        User user = user("https://cdn.example/profiles/users/7");
        service.deleteProfileImage(7L);
        verify(images).delete("profiles/users/7");
        assertNull(user.getProfileImgUrl());
    }

    @Test
    void googlePictureIsClearedToo() {
        // 구글 사진 주소는 우리 버킷 객체가 아니지만, 없는 key 삭제도 성공하므로 같은 key로 호출한다.
        User user = user("https://lh3.googleusercontent.com/a/photo");
        service.deleteProfileImage(7L);
        verify(images).delete("profiles/users/7");
        assertNull(user.getProfileImgUrl());
    }

    @Test
    void keepsUrlWhenStorageDeleteFails() {
        User user = user("https://cdn.example/profiles/users/7");
        doThrow(new RuntimeException("denied")).when(images).delete("profiles/users/7");
        assertThrows(RuntimeException.class, () -> service.deleteProfileImage(7L));
        assertEquals("https://cdn.example/profiles/users/7", user.getProfileImgUrl());
    }

    @Test
    void unknownUserDeletesNothing() {
        when(users.findById(8L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> service.deleteProfileImage(8L));
        verifyNoInteractions(images);
    }
}
