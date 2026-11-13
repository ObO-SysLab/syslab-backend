package net.diveon.backend.domain.problem.service;

import java.util.regex.Pattern;

/** Exact, versioned source identity. A problem number alone cannot identify a build. */
public record PracticeBuildIdentity(long problemId, String requestId) {
    private static final String UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final Pattern KEY = Pattern.compile("prob-([1-9][0-9]*)/builds/(" + UUID + ")/source\\.zip");
    private static final Pattern BUILD = Pattern.compile("[A-Za-z0-9_-]+:(" + UUID + ")");

    public PracticeBuildIdentity {
        if (problemId <= 0 || requestId == null || !requestId.matches(UUID))
            throw new IllegalArgumentException("Invalid practice build identity");
    }

    public String sourceKey() { return "prob-" + problemId + "/builds/" + requestId + "/source.zip"; }

    /** Author-facing failure reason the trusted build writes next to the source (never triggers a build). */
    public String resultKey() { return "prob-" + problemId + "/builds/" + requestId + "/result.json"; }

    public static PracticeBuildIdentity fromKey(String key) {
        var match = KEY.matcher(key == null ? "" : key);
        if (!match.matches()) throw new IllegalArgumentException("Invalid practice build source key");
        return new PracticeBuildIdentity(Long.parseLong(match.group(1)), match.group(2));
    }

    public static String imageTag(String buildId) {
        var match = BUILD.matcher(buildId == null ? "" : buildId);
        if (!match.matches()) throw new IllegalArgumentException("Invalid CodeBuild ID");
        return "build-" + match.group(1);
    }
}
