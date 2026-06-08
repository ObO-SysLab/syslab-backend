package net.diveon.backend.global.util;

import net.diveon.backend.global.exception.InvalidImageFileException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public final class ImageFileValidator {

    private static final long MAX_SIZE_BYTES = 1024 * 1024;           // 1MB
    private static final long MAX_SIZE_BYTES_GIF = 5 * 1024 * 1024;  // 5MB (gif)
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp", "gif");

    private ImageFileValidator() {
    }

    // 검증 후 확장자를 반환한다 (S3 저장 키 생성에 사용)
    public static String validateAndGetExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageFileException("이미지 파일이 없습니다.");
        }

        String filename = file.getOriginalFilename();
        String extension = (filename != null && filename.contains("."))
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                : "";

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidImageFileException("허용되지 않는 확장자입니다. (jpg, jpeg, png, webp, gif만 가능)");
        }

        long maxSize = "gif".equals(extension) ? MAX_SIZE_BYTES_GIF : MAX_SIZE_BYTES;
        if (file.getSize() > maxSize) {
            throw new InvalidImageFileException("이미지 파일은 최대 " + (maxSize / (1024 * 1024)) + "MB까지 업로드할 수 있습니다.");
        }

        return extension;
    }
}
