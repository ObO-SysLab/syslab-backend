package net.diveon.backend.domain.vm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service @Profile("prod")
public class DockerServiceImpl implements DockerService {
    private final VmRemoteCommand remote;
    private final ObjectMapper mapper;
    public DockerServiceImpl(VmRemoteCommand remote, ObjectMapper mapper) {
        this.remote = remote; this.mapper = mapper;
    }
    public String runContainer(String image, Target target) {
        if (image == null || !image.matches("[A-Za-z0-9][A-Za-z0-9._:/@-]{0,499}")) throw failure();
        var state = invoke("create", target, image);
        if (!"RUNNING".equals(state.status())) throw failure();
        return state.containerId();
    }
    public State inspect(Target target) { return invoke("inspect", target, null); }
    public void stopAndRemoveContainer(Target target) {
        if (!"MISSING".equals(invoke("remove", target, null).status())) throw failure();
    }
    public void reconcile() { invoke("reconcile", null, null); }
    private State invoke(String operation, Target target, String image) {
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("version", 1); request.put("operation", operation);
            if (target != null) {
                if (target.sessionId() <= 0 || target.userId() <= 0 || target.problemId() <= 0) throw failure();
                request.put("sessionId", target.sessionId()); request.put("userId", target.userId());
                request.put("problemId", target.problemId()); request.put("expiresAtEpochMs", target.expiresAtEpochMs());
                if (target.legacyContainerId() != null) {
                    if (!target.legacyContainerId().matches("[a-f0-9]{64}")) throw failure();
                    request.put("legacyContainerId", target.legacyContainerId());
                }
            }
            if (image != null) request.put("image", image);
            JsonNode result = mapper.readTree(remote.execute(mapper.writeValueAsString(request)));
            if (result == null || !result.path("version").isIntegralNumber() || result.path("version").asInt() != 1) throw failure();
            String status = result.path("status").asText();
            if (target == null) {
                if (!"OK".equals(status)) throw failure();
                return new State(status, null);
            }
            if (!result.path("sessionId").isIntegralNumber() || result.path("sessionId").asLong() != target.sessionId()
                    || !List.of("RUNNING", "STOPPED", "MISSING").contains(status)) throw failure();
            String id = result.path("containerId").isTextual() ? result.path("containerId").asText() : null;
            if (!"MISSING".equals(status) && (id == null || !id.matches("[a-f0-9]{64}"))) throw failure();
            if ("MISSING".equals(status) && !result.path("containerId").isNull()) throw failure();
            if (id != null && target.legacyContainerId() != null && !id.equals(target.legacyContainerId())) throw failure();
            return new State(status, id);
        } catch (Exception e) { throw failure(); }
    }
    private static IllegalStateException failure() { return new IllegalStateException("VM operation could not be verified"); }
}
