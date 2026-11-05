package net.diveon.backend.domain.vm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import net.diveon.backend.domain.vm.service.TerminalAccessService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class TerminalController {
    private final TerminalAccessService access;
    public TerminalController(TerminalAccessService access) { this.access = access; }

    public record TicketRequest(@NotNull @Positive Long probId,
            @NotNull @Pattern(regexp = "[a-f0-9]{64}") String containerId) {}
    public record ConnectRequest(@NotNull @Pattern(regexp = "[A-Za-z0-9_-]{43}") String ticket,
            @NotNull @Pattern(regexp = "[a-f0-9]{64}") String containerId) {}
    public record CheckRequest(@NotNull @Pattern(regexp = "[A-Za-z0-9_-]{43}") String lease) {}

    @PostMapping("/api/vm/terminal-ticket")
    public ResponseEntity<TerminalAccessService.Ticket> issue(@AuthenticationPrincipal String userId,
            @Valid @RequestBody TicketRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(access.issue(Long.valueOf(userId), request.probId(), request.containerId()));
    }

    // These routes use a separate machine credential, never the login JWT key.
    @PostMapping("/internal/terminal/connect")
    public ResponseEntity<TerminalAccessService.Connection> connect(
            @RequestHeader(value = "X-Terminal-Service-Key", required = false) String key,
            @Valid @RequestBody ConnectRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(access.consume(key, request.ticket(), request.containerId()));
    }

    @PostMapping("/internal/terminal/check")
    public ResponseEntity<TerminalAccessService.Status> check(
            @RequestHeader(value = "X-Terminal-Service-Key", required = false) String key,
            @Valid @RequestBody CheckRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(access.check(key, request.lease()));
    }
}
