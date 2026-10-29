package net.diveon.backend.domain.vm.service;

import com.jcraft.jsch.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Fixed remote helper command; identifiers/image are JSON on stdin, never shell text. */
@Service @Profile("prod")
public class VmRemoteCommand {
    private final String host, user, keyPath, knownHosts;
    public VmRemoteCommand(@Value("${vm.ec2-host}") String host, @Value("${vm.ec2-user}") String user,
            @Value("${vm.ec2-key-path}") String keyPath, @Value("${vm.ssh-known-hosts:}") String knownHosts) {
        this.host = host; this.user = user; this.keyPath = keyPath; this.knownHosts = knownHosts;
    }
    public String execute(String input) {
        if (knownHosts == null || knownHosts.isBlank() || input.length() > 8192) throw failure();
        Session session = null; ChannelExec channel = null;
        try {
            JSch ssh = new JSch(); ssh.setKnownHosts(knownHosts); ssh.addIdentity(keyPath);
            session = ssh.getSession(user, host, 22);
            session.setConfig("StrictHostKeyChecking", "yes"); session.setTimeout(5000); session.connect(5000);
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("python3 /usr/local/libexec/diveon-vm-helper.py");
            channel.setInputStream(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            var output = new LimitedOutput(); var error = new LimitedOutput();
            channel.setOutputStream(output); channel.setErrStream(error); channel.connect(5000);
            long deadline = System.nanoTime() + java.time.Duration.ofSeconds(25).toNanos();
            while (!channel.isClosed() && System.nanoTime() < deadline) Thread.sleep(25);
            if (!channel.isClosed() || channel.getExitStatus() != 0 || output.overflow || error.overflow) throw failure();
            return output.toString(StandardCharsets.UTF_8);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw failure();
        } catch (Exception e) { throw failure(); }
        finally { if (channel != null) channel.disconnect(); if (session != null) session.disconnect(); }
    }
    static final class LimitedOutput extends ByteArrayOutputStream {
        volatile boolean overflow;
        @Override public synchronized void write(int b) { if (count < 65536) super.write(b); else overflow = true; }
        @Override public synchronized void write(byte[] b, int off, int len) {
            int accepted = Math.min(len, 65536 - count);
            if (accepted < len) overflow = true;
            super.write(b, off, accepted);
        }
    }
    private static IllegalStateException failure() { return new IllegalStateException("VM remote command unavailable"); }
}
