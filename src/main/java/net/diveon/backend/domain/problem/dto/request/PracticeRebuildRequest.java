package net.diveon.backend.domain.problem.dto.request;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** multipart/form-data: an optional new Dockerfile zip and the complete allowed-command list (empty = none). */
public class PracticeRebuildRequest {
    private MultipartFile dockerfile;
    private List<String> commandAllowlist;

    public MultipartFile getDockerfile() { return dockerfile; }
    public void setDockerfile(MultipartFile dockerfile) { this.dockerfile = dockerfile; }
    public List<String> getCommandAllowlist() { return commandAllowlist; }
    public void setCommandAllowlist(List<String> commandAllowlist) { this.commandAllowlist = commandAllowlist; }
}
