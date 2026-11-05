package net.diveon.backend.domain.vm.service;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class DockerServiceMock implements DockerService {
    private final ConcurrentHashMap<Long, State> sessions = new ConcurrentHashMap<>();
    public String runContainer(String image, Target target) {
        return sessions.computeIfAbsent(target.sessionId(),
                id -> new State("RUNNING", String.format("%064x", id))).containerId();
    }
    public State inspect(Target target) { return sessions.getOrDefault(target.sessionId(), new State("MISSING", null)); }
    public void stopAndRemoveContainer(Target target) { sessions.remove(target.sessionId()); }
    public void reconcile() {}
}
