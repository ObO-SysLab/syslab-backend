package net.diveon.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync // 비동기 채점 위한 추가
@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	//지금 배포 버그 수정을 위한 아무 주석추가 0503

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
