package net.diveon.backend.domain.user.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
public class LocalGoogleLoginTestController {

    @GetMapping(value = "/local/google-login-test.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> googleLoginTestPage() {
        return ResponseEntity.ok(new ClassPathResource("local-static/google-login-test.html"));
    }
}
