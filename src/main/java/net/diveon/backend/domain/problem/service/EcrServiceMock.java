package net.diveon.backend.domain.problem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class EcrServiceMock implements EcrService {

    private static final Logger log = LoggerFactory.getLogger(EcrServiceMock.class);

    @Override
    public void deleteImage(Long probId) {
        log.info("[Mock] ECR 이미지 삭제 생략 - probId: {}, tag: prob-{}", probId, probId);
    }
}
