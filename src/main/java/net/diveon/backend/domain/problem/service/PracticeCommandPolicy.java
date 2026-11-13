package net.diveon.backend.domain.problem.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Allowed-command policy of a practice image. The same contract is implemented by the trusted
 * CodeBuild script (syslab-infra vm/codebuild/build_image.py): metadata format, name rule and marker tag.
 */
public final class PracticeCommandPolicy {
    public static final String METADATA_KEY = "diveon-commands";
    public static final int MAX_COMMANDS = 30;
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._+-]{0,31}");

    private PracticeCommandPolicy() {}

    /** Sorted unique names, or null when nothing was entered (unrestricted). */
    public static List<String> normalize(List<String> names) {
        if (names == null) return null;
        var unique = new TreeSet<String>();
        for (String name : names) {
            String trimmed = name == null ? "" : name.strip();
            if (trimmed.isEmpty()) continue;
            if (!NAME.matcher(trimmed).matches()) throw new IllegalArgumentException("Invalid command name");
            unique.add(trimmed);
        }
        if (unique.size() > MAX_COMMANDS) throw new IllegalArgumentException("Too many commands");
        return unique.isEmpty() ? null : List.copyOf(unique);
    }

    public static boolean isCommandName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /** S3 user metadata value for a normalized policy. */
    public static String metadataValue(List<String> policy) {
        return "v1:" + String.join(",", requireNormalized(policy));
    }

    /** ECR tag the build pushes on the same digest once this exact policy has been applied. */
    public static String markerTag(String buildId, List<String> policy) {
        String canonical = "v1\n" + String.join("\n", requireNormalized(policy));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.US_ASCII));
            return PracticeBuildIdentity.imageTag(buildId) + "-cmd-" + HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> requireNormalized(List<String> policy) {
        if (policy == null || !policy.equals(normalize(policy))) throw new IllegalArgumentException("Policy is not normalized");
        return policy;
    }
}
