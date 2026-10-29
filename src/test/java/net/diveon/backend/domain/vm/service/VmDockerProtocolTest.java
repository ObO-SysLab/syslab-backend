package net.diveon.backend.domain.vm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VmDockerProtocolTest {
    VmRemoteCommand remote = mock(VmRemoteCommand.class);
    ObjectMapper mapper = new ObjectMapper();
    DockerServiceImpl docker = new DockerServiceImpl(remote, mapper);
    DockerService.Target target = new DockerService.Target(10, 7, 9, 12345, null);
    String cid = "a".repeat(64);
    String response(String status) { return "{\"version\":1,\"sessionId\":10,\"status\":\"" + status + "\",\"containerId\":\"" + cid + "\"}"; }
    @Test void protocolCarriesIdentityWithoutShellCommands() throws Exception {
        when(remote.execute(any())).thenReturn(response("RUNNING"));
        assertEquals(cid, docker.runContainer("registry/practice:latest", target));
        var capture = ArgumentCaptor.forClass(String.class); verify(remote).execute(capture.capture());
        var request = mapper.readTree(capture.getValue());
        assertEquals("create", request.path("operation").asText()); assertEquals(10, request.path("sessionId").asLong());
        assertEquals(7, request.path("userId").asLong()); assertEquals(9, request.path("problemId").asLong());
        assertEquals(12345, request.path("expiresAtEpochMs").asLong()); assertFalse(request.has("legacyContainerId"));
    }
    @Test void malformedWrongSessionAndUnknownResultsFailClosed() {
        for (var value : new String[]{"", "garbage", "{}", response("RUNNING").replace("10", "11"), response("UNKNOWN"),
                response("RUNNING").replace(cid, "abc"), response("RUNNING").replace("\"version\":1", "\"version\":2")}) {
            when(remote.execute(any())).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> docker.inspect(target));
        }
    }
    @Test void deletionMustConfirmMissing() {
        when(remote.execute(any())).thenReturn(response("STOPPED"));
        assertThrows(IllegalStateException.class, () -> docker.stopAndRemoveContainer(target));
        when(remote.execute(any())).thenReturn("{\"version\":1,\"sessionId\":10,\"status\":\"MISSING\",\"containerId\":null}");
        assertDoesNotThrow(() -> docker.stopAndRemoveContainer(target));
    }
    @Test void legacyIdCannotBeSubstituted() {
        when(remote.execute(any())).thenReturn(response("RUNNING"));
        var legacy = new DockerService.Target(10, 7, 9, 12345, "b".repeat(64));
        assertThrows(IllegalStateException.class, () -> docker.inspect(legacy));
    }
    @Test void invalidImageNeverReachesSsh() {
        for (var image : new String[]{"ubuntu;id", "$(id)", "--privileged", "ubuntu\nwhoami"})
            assertThrows(IllegalStateException.class, () -> docker.runContainer(image, target));
        verifyNoInteractions(remote);
    }
    @Test void sshFailureDoesNotLeakDetails() {
        when(remote.execute(any())).thenThrow(new IllegalStateException("private host and secret"));
        assertEquals("VM operation could not be verified", assertThrows(IllegalStateException.class, () -> docker.inspect(target)).getMessage());
    }
    @Test void outputCaptureIsBoundedAndRecordsOverflow() {
        var output = new VmRemoteCommand.LimitedOutput(); output.write(new byte[65536], 0, 65536);
        assertFalse(output.overflow); output.write(1); assertTrue(output.overflow); assertEquals(65536, output.size());
        output.write(new byte[100000], 0, 100000); assertEquals(65536, output.size());
    }
    @Test void missingKnownHostsFailsBeforeNetwork() {
        var command = new VmRemoteCommand("invalid.invalid", "user", "unused", "");
        assertThrows(IllegalStateException.class, () -> command.execute("{}"));
    }
}
