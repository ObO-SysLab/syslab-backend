package net.diveon.backend.domain.vm.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import net.diveon.backend.global.exception.VmCreationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;

@Service
@Profile("prod")
public class DockerServiceImpl implements DockerService {

    private static final Logger log = LoggerFactory.getLogger(DockerServiceImpl.class);

    private final String vmHost;
    private final String vmUser;
    private final String vmKeyPath;

    public DockerServiceImpl( 
        // application-secret.yml에서 @Value로 주입받는 것들
            @Value("${vm.ec2-host}") String vmHost, // VM EC2 IP
            @Value("${vm.ec2-user}") String vmUser, // ec2-user
            @Value("${vm.ec2-key-path}") String vmKeyPath) {  // PEM 키 파일 경로
        this.vmHost = vmHost;
        this.vmUser = vmUser;
        this.vmKeyPath = vmKeyPath;
    }

    // VM EC2에서 docker run 명령어 실행하고 컨테이너 ID 받아오기
    @Override
    public String runContainer(String image, Long userId, Long probId) {
        String command = String.format(
                "docker run -d --memory 256m --cpus 0.3 --network none --name syslab-vm-%d-%d %s sleep infinity",
                userId, probId, image
        );
        CommandResult result = executeCommand(command); // docker run 명령어 만들어서 executeCommand에 넘김

        // exit status가 0이 아니거나 컨테이너 ID가 비어있으면 원격 docker run이 실패한 것
        if (result.exitStatus() != 0 || result.stdout().isBlank()) {
            log.error("VM 컨테이너 생성 실패 (userId={}, probId={}, exitStatus={}, stderr={})",
                    userId, probId, result.exitStatus(), result.stderr());
            throw new VmCreationFailedException("VM 컨테이너 생성에 실패했습니다: " + result.stderr());
        }

        return result.stdout();
    }


    // SSH로 VM EC2 접속 후 컨테이너 중지 및 삭제
    @Override
    public void stopAndRemoveContainer(String containerId) {
        // stop + rm 명령어 만들어서 executeCommand에 넘김
        CommandResult result = executeCommand("docker stop " + containerId + " && docker rm " + containerId);
        if (result.exitStatus() != 0) {
            log.warn("컨테이너 정지/삭제 실패 (containerId={}, exitStatus={}, stderr={})",
                    containerId, result.exitStatus(), result.stderr());
        }
    }


    // 실제 SSH실행 담당
    private CommandResult executeCommand(String command) {
        Session session = null;
        ChannelExec channel = null;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(vmKeyPath); // PEM 키 파일 로드


            // VM EC2 SSH 연결
            session = jsch.getSession(vmUser, vmHost, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            // 연결된 SSH 세션에서 명령어 실행 채널열고, command 실행
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
            channel.setErrStream(errBuffer);

            // 실행결과 (컨테이너 ID)를 한 줄씩 읽어서 반환
            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            // exit status는 채널이 닫힌 뒤에야 확정되므로 닫힐 때까지 잠깐 대기 (최대 5초)
            int waited = 0;
            while (!channel.isClosed() && waited < 100) {
                Thread.sleep(50);
                waited++;
            }

            return new CommandResult(output.toString().trim(), errBuffer.toString().trim(), channel.getExitStatus());
        } catch (Exception e) {
            throw new RuntimeException("VM EC2 SSH 명령 실행 실패: " + e.getMessage(), e);
        } finally { // 끝나면 SSH 연결 끊기 (안 끊으면 연결이 계속 쌓여서 문제 발생 위험)
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }

    private record CommandResult(String stdout, String stderr, int exitStatus) {}
}


/* 요약
1. PEM 키 파일 로드
        ↓
2. VM EC2에 SSH 연결 (10.0.1.19:22)
        ↓
3. "docker run -d ..." 명령어 실행
        ↓
4. VM EC2가 출력한 컨테이너 ID 읽기
        ↓
5. SSH 연결 끊기
        ↓
6. 컨테이너 ID 반환

터미널에서 직접 VM EC2에 SSH 접속해서
docker run 치는 것과 완전히 동일한 동작을
코드로 자동화한 것임
*/