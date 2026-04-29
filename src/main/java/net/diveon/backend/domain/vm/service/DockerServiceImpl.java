package net.diveon.backend.domain.vm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Profile("prod")
public class DockerServiceImpl implements DockerService {

    private final String dockerBaseUrl;
    private final RestTemplate restTemplate = new RestTemplate(); // Spring에서 HTTP 요청 보내는 도구. Docker Remote API가 HTTP 기반이라 이걸로 호출

    public DockerServiceImpl(@Value("${aws.vm-ec2-host}") String vmEc2Host) {
        this.dockerBaseUrl = "http://" + vmEc2Host + ":2375";
    } // dockerBaseUrl → http://{VM EC2 IP}:2375 로 조합. 모든 Docker API 요청이 여기로 감

    @Override
    public String createContainer(Long probId) {
        String image = "jotriever/syslab-prob" + probId + ":latest";

        Map<String, Object> hostConfig = new HashMap<>();
        hostConfig.put("NetworkMode", "none");
        hostConfig.put("Memory", 512 * 1024 * 1024L);
        hostConfig.put("NanoCpus", 500_000_000L);

        Map<String, Object> body = new HashMap<>();
        body.put("Image", image);
        body.put("HostConfig", hostConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        DockerCreateResponse response = restTemplate.postForObject(
                dockerBaseUrl + "/containers/create",
                new HttpEntity<>(body, headers),
                DockerCreateResponse.class
        );

        String containerId = response.getId();

        restTemplate.postForObject(
                dockerBaseUrl + "/containers/" + containerId + "/start",
                new HttpEntity<>(null, headers),
                Void.class
        );

        return containerId;
    }

    @Override
    public void removeContainer(String containerId) {
        restTemplate.postForObject(
                dockerBaseUrl + "/containers/" + containerId + "/stop?t=0",
                null,
                Void.class
        );

        restTemplate.delete(dockerBaseUrl + "/containers/" + containerId);
    }

    private static class DockerCreateResponse {
        private String Id;
        public String getId() { return Id; }
        public void setId(String id) { this.Id = id; }
    }
}